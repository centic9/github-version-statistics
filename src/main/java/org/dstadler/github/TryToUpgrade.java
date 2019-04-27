package org.dstadler.github;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.dstadler.commons.exec.ExecutionHelper;
import org.dstadler.github.JSONWriter.Holder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TryToUpgrade {
    public static void main(String[] args) throws IOException {
        File[] files = ProcessResults.getStatsFiles();

        // project, version
        Map<String, String> projects = new HashMap<>();

        readLines(files, projects);

        System.out.println("Looking at " + projects.size() + " projects");

        Map<String, String> projectsOfInterest = filterForProjectsOfInterest(projects);

        System.out.println("Found " + projectsOfInterest.size() + " repositories with stars or watchers");

        cloneBuildAndUpgrade(projectsOfInterest);
    }

    private static void cloneBuildAndUpgrade(Map<String, String> projects) throws IOException {
        for (String project : projects.keySet()) {
            try {
                cloneBuildAndUpgradeOneProject(project);
            } catch (GitAPIException e) {
                System.out.println("Failed for project " + project + ": " + e);
            }
        }
    }

    private static void cloneBuildAndUpgradeOneProject(String project) throws IOException, GitAPIException {
        // prepare a new folder for the cloned repository
        File localPath = File.createTempFile("TestGitRepository", "");
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

            if(new File(localPath, "gradlew").exists()) {
                // build using Gradle Wrapper
                try (OutputStream out = new FileOutputStream("/tmp/github_build_" + project + ".log")) {
                    CommandLine cmd = new CommandLine("gradlew");
                    cmd.addArgument("check");
                    ExecutionHelper.getCommandResultIntoStream(cmd, localPath,
                            0, TimeUnit.HOURS.toMillis(1), out);
                }
            } else if(new File(localPath, "build.gradle").exists()) {
                // build using Gradle Wrapper
                try (OutputStream out = new FileOutputStream("/tmp/github_build_" + project + ".log")) {
                    CommandLine cmd = new CommandLine("/usr/bin/gradle");
                    cmd.addArgument("check");
                    ExecutionHelper.getCommandResultIntoStream(cmd, localPath,
                            0, TimeUnit.HOURS.toMillis(1), out);
                }
            } else if(new File(localPath, "pom.xml").exists()) {
                // build using Maven
                try (OutputStream out = new FileOutputStream("/tmp/github_build_" + project + ".log")) {
                    CommandLine cmd = new CommandLine("/usr/bin/mvn");
                    cmd.addArgument("package");
                    ExecutionHelper.getCommandResultIntoStream(cmd, localPath,
                            0, TimeUnit.HOURS.toMillis(1), out);
                }
            } else {
                System.out.println("Don't know how to build project " + remoteUrl);
            }
        } finally {
            // clean up here to not keep the sources and build-results
            FileUtils.deleteDirectory(localPath);
        }
    }

    private static Map<String, String> filterForProjectsOfInterest(Map<String, String> projects) throws IOException {
        GitHub github = BaseSearch.connect();

        Map<String, String> projectsOfInterest = new HashMap<>();
        Iterator<Map.Entry<String, String>> it = projects.entrySet().iterator();
        for(int i = 0;i < 100;i++) {
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
            List<String> lines = FileUtils.readLines(file, "UTF-8");

            for (String line : lines) {
                Holder holder = JSONWriter.mapper.readValue(line, Holder.class);

                for (Map.Entry<String, String> entry : holder.getRepositoryVersions().entries()) {
                    projects.put(entry.getValue(), entry.getKey());
                }
            }
        }
    }
}
