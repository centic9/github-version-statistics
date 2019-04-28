package org.dstadler.github.upgrade;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.dstadler.commons.exec.ExecutionHelper;
import org.dstadler.github.search.BaseSearch;
import org.dstadler.github.util.JSONWriter.Holder;
import org.dstadler.github.ProcessResults;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TryToUpgrade {
    public static void main(String[] args) throws IOException {
        ProjectStatuses projectStatuses = new ProjectStatuses();

        projectStatuses.read();

        File[] files = ProcessResults.getStatsFiles();

        // project, version
        Map<String, String> projects = new HashMap<>();

        readLines(files, projects);

        System.out.println("Having " + projects.size() + " unique projects");

        Map<String, String> projectsOfInterest = filterForProjectsOfInterest(projects);

        System.out.println("Found " + projectsOfInterest.size() + " repositories with stars or watchers");

        filterProjects(projectsOfInterest, projectStatuses);

        cloneBuildAndUpgrade(projectsOfInterest, projectStatuses);

        projectStatuses.write();
    }

    private static void filterProjects(Map<String, String> projectsOfInterest, ProjectStatuses projectStatuses) {
        Iterator<Map.Entry<String, String>> it = projectsOfInterest.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, String> project = it.next();
            ProjectStatus projectStatus = projectStatuses.get(project.getKey());

            //noinspection VariableNotUsedInsideIf
            if(projectStatus != null) {
                // for now do not try again if we tried a project already
                it.remove();
            }
        }
    }

    private static void cloneBuildAndUpgrade(Map<String, String> projects, ProjectStatuses projectStatuses) throws IOException {
        for (String project : projects.keySet()) {
            try {
                cloneBuildAndUpgradeOneProject(project, projectStatuses);
            } catch (GitAPIException e) {
                System.out.println("Failed for project " + project + ": " + e);
                projectStatuses.add(new ProjectStatus(project, UpgradeStatus.NotAccessible));
            }
        }
    }

    private static void cloneBuildAndUpgradeOneProject(String project, ProjectStatuses projectStatuses) throws IOException, GitAPIException {
        // prepare a new folder for the cloned repository
        File localPath = File.createTempFile("UpgradeGitRepository" + project, "");
        if(!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }

        String remoteUrl = "https://github.com/" + project;

        System.out.println("Cloning from " + remoteUrl + " to " + localPath);
        try (Git result = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(localPath)
                .setProgressMonitor(new TextProgressMonitor())
                .call()) {
            //noinspection resource
            System.out.println("Having repository: " + result.getRepository().getDirectory());

            try {
                if (new File(localPath, "gradlew").exists()) {
                    // build using Gradle Wrapper
                    buildViaGradleWrapper(project, localPath);
                    projectStatuses.add(new ProjectStatus(project, UpgradeStatus.BuildSucceeded));
                } else if (new File(localPath, "build.gradle").exists()) {
                    // build using Gradle Wrapper
                    buildViaGradle(project, localPath);
                    projectStatuses.add(new ProjectStatus(project, UpgradeStatus.BuildSucceeded));
                } else if (new File(localPath, "pom.xml").exists()) {
                    // build using Maven
                    buildViaMaven(project, localPath);
                    projectStatuses.add(new ProjectStatus(project, UpgradeStatus.BuildSucceeded));
                } else {
                    System.out.println("Don't know how to build project " + remoteUrl);
                    projectStatuses.add(new ProjectStatus(project, UpgradeStatus.UnknownBuildSystem));
                }
            } catch (ExecuteException e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
                projectStatuses.add(new ProjectStatus(project, UpgradeStatus.BuildFailed));
            }
        } finally {
            // clean up here to not keep the sources and build-results
            FileUtils.deleteDirectory(localPath);
        }
    }

    private static void buildViaMaven(String project, File localPath) throws IOException {
        try (OutputStream out = createLogFile(project)) {
            CommandLine cmd = new CommandLine("/usr/bin/mvn");
            cmd.addArgument("package");
            ExecutionHelper.getCommandResultIntoStream(cmd, localPath,
                    0, TimeUnit.HOURS.toMillis(1), out);
        }
    }

    protected static void buildViaGradle(String project, File localPath) throws IOException {
        try (OutputStream out = createLogFile(project)) {
            CommandLine cmd = new CommandLine("/usr/bin/gradle");
            cmd.addArgument("check");
            ExecutionHelper.getCommandResultIntoStream(cmd, localPath,
                    0, TimeUnit.HOURS.toMillis(1), out);
        }
    }

    private static void buildViaGradleWrapper(String project, File localPath) throws IOException {
        try (OutputStream out = createLogFile(project)) {
            CommandLine cmd = new CommandLine("bash");
            cmd.addArgument("-c");
            cmd.addArgument("gradlew");
            cmd.addArgument("check");
            ExecutionHelper.getCommandResultIntoStream(cmd, localPath,
                    0, TimeUnit.HOURS.toMillis(1), out);
        }
    }

    private static FileOutputStream createLogFile(String project) throws FileNotFoundException {
        return new FileOutputStream("/tmp/github_build_" + project.replace("/", "_") + ".log");
    }

    private static Map<String, String> filterForProjectsOfInterest(Map<String, String> projects) throws IOException {
        GitHub github = BaseSearch.connect();

        Map<String, String> projectsOfInterest = new HashMap<>();
        Iterator<Map.Entry<String, String>> it = projects.entrySet().iterator();
        for(int i = 0;i < 50;i++) {
            Map.Entry<String, String> repo = it.next();
            try {
                GHRepository repository = github.getRepository(repo.getKey());
                int stargazersCount = repository.getStargazersCount();
                int watchers = repository.getWatchers();
                System.out.println(i + "-Repo: " + repo + ": Had stars: " + stargazersCount + ", watchers: " + watchers);
                if(stargazersCount > 0 || watchers > 0) {
                    projectsOfInterest.put(repo.getKey(), repo.getValue());
                }
            } catch (GHFileNotFoundException e) {
                System.out.println(i + "-Repo: " + repo + ": Not found: " + e);
            } catch (HttpException e) {
                if(e.getResponseCode() == 403) {
                    System.out.println(i + "-Repo: " + repo + ": Forbidden: " + e);
                } else {
                    throw e;
                }
            }
        }
        return projectsOfInterest;
    }

    protected static void readLines(File[] files, Map<String, String> projects) throws IOException {
        for(File file : files) {
            Holder.readFile(projects, file);
        }
    }
}
