package org.dstadler.github.upgrade;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.dstadler.commons.exec.ExecutionHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProjectBuilder {

    public static void cloneBuildAndUpgrade(Map<String, String> projects, ProjectStatuses projectStatuses) throws IOException {
        for (String project : projects.keySet()) {
            try {
                cloneBuildAndUpgradeOneProject(project, projectStatuses);
            } catch (GitAPIException e) {
                System.out.println("Failed for project " + project + ": " + e);
                projectStatuses.add(new ProjectStatus(project, UpgradeStatus.NotAccessible));
            }
        }
    }

    protected static void cloneBuildAndUpgradeOneProject(String project, ProjectStatuses projectStatuses) throws IOException, GitAPIException {
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
                System.out.println("Failed to build " + project + ": " + e);
                projectStatuses.add(new ProjectStatus(project, UpgradeStatus.BuildFailed));
            }
        } finally {
            // clean up here to not keep the sources and build-results
            FileUtils.deleteDirectory(localPath);
        }
    }

    protected static void buildViaMaven(String project, File localPath) throws IOException {
        CommandLine cmd = new CommandLine("/usr/bin/mvn");
        cmd.addArgument("package");

        executeBuild(project, localPath, cmd);
    }

    protected static void buildViaGradle(String project, File localPath) throws IOException {
        CommandLine cmd = new CommandLine("/usr/bin/gradle");
        cmd.addArgument("check");

        executeBuild(project, localPath, cmd);
    }

    protected static void buildViaGradleWrapper(String project, File localPath) throws IOException {
        // make script executable
        CommandLine cmd = new CommandLine("chmod");
        cmd.addArgument("a+x");
        cmd.addArgument("./gradlew");

        executeBuild(project, localPath, cmd);

        cmd = new CommandLine("bash");
        cmd.addArgument("-c");
        cmd.addArgument("./gradlew");
        cmd.addArgument("check");

        executeBuild(project, localPath, cmd);
    }

    private static void executeBuild(String project, File localPath, CommandLine cmd) throws IOException {
        try (OutputStream out = createLogFile(project)) {
            out.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));

            ExecutionHelper.getCommandResultIntoStream(cmd, localPath,
                    0, TimeUnit.HOURS.toMillis(1), out);
        }
    }

    private static FileOutputStream createLogFile(String project) throws FileNotFoundException {
        return new FileOutputStream("/tmp/github_build_" + project.replace("/", "_") + ".log");
    }
}
