package org.dstadler.github.packaging;

import org.dstadler.github.search.BaseSearch;
import org.kohsuke.github.GHArtifact;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHWorkflow;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;

import java.io.IOException;

public class ListArtifacts {

    public static final String REPO_DEBIAN_PACKAGES = "centic9/debian-packages";

    // "Build Debian Packages with Debcraft"
    public static final long WORKFLOW_ID = 177606114L;

    public static void main(String[] args) throws IOException {
        GitHub github = BaseSearch.connect();

        /*for (GHRepository repository : github.getUser("centic9").listRepositories()) {
            if (!repository.getName().endsWith("-ppa")) {
                continue;
            }

            System.out.println("Repository: " + repository.getName());
        }*/

        GHRepository repository = github.getRepository(REPO_DEBIAN_PACKAGES);
        for (GHWorkflow workflow : repository.listWorkflows()) {
            System.out.println("Workflow: " + workflow.getName() + ": " + workflow.getId());
        }

        for (GHWorkflowRun run : repository.getWorkflow(WORKFLOW_ID).listRuns()) {
            System.out.println("Run: " + run.getName() + ": " + run.getArtifactsUrl());

            for (GHArtifact artifact : run.listArtifacts()) {
                System.out.println("Artifact: " + artifact.getName());
            }
        }
    }
}
