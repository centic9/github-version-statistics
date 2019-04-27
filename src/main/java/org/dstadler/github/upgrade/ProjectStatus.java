package org.dstadler.github.upgrade;

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
