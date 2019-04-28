package org.dstadler.github.upgrade;

/**
 * Information about one project, mostly GitHub-name and any result from
 * trying to download, build and upgrade the project
 */
class ProjectStatus {
    private final String project;
    private final UpgradeStatus status;

    public ProjectStatus(String project, UpgradeStatus status) {
        this.project = project;
        this.status = status;
    }

    public String getProject() {
        return project;
    }

    public UpgradeStatus getStatus() {
        return status;
    }
}
