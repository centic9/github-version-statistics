package org.dstadler.github.upgrade;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProjectBuilderTest {
    @Test
    public void testCloneAndBuildEmpty() throws IOException {
        Map<String, String> projects = new HashMap<>();
        ProjectStatuses projectStatuses = new ProjectStatuses();
        ProjectBuilder.cloneBuildAndUpgrade(projects, projectStatuses);
    }

    @Test
    public void testCloneAndBuild() throws IOException {
        Map<String, String> projects = new HashMap<>();
        ProjectStatuses projectStatuses = new ProjectStatuses();

        projects.put("centic9/jgit-cookbook", "1.0");

        ProjectBuilder.cloneBuildAndUpgrade(projects, projectStatuses);

        ProjectStatus projectStatus = projectStatuses.get("centic9/jgit-cookbook");
        assertNotNull(projectStatus);
        assertEquals(UpgradeStatus.BuildSucceeded, projectStatus.getStatus());
    }
}