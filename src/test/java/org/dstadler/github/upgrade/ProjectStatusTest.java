package org.dstadler.github.upgrade;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProjectStatusTest {
    @Test
    public void test() {
        ProjectStatus status = new ProjectStatus("project1", UpgradeStatus.BuildFailed);
        assertEquals("project1", status.getProject());
        assertEquals(UpgradeStatus.BuildFailed, status.getStatus());
    }
}
