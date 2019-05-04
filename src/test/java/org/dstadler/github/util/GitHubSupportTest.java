package org.dstadler.github.util;

import org.dstadler.github.upgrade.ProjectStatuses;
import org.dstadler.github.upgrade.UpgradeStatus;
import org.junit.Test;
import org.kohsuke.github.HttpException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GitHubSupportTest {
    @Test
    public void testFilter() throws IOException {
        Map<String, String> projects = new HashMap<>();
        projects.put("project/project", "1.0");     // non-existing
        projects.put("muthu1809/RB", "3.16");       // zero stars/watchers
        projects.put("centic9/jgit-cookbook", "1.0");   // has stars/watchers
        //projects.put("centic9/invalid project &/()", "1.0");   // invalid project name

        ProjectStatuses projectStatuses = new ProjectStatuses();
        Map<String, String> projectsOfInterest = GitHubSupport.filterForProjectsOfInterest(projects, projectStatuses, 10);

        assertEquals("Original map not changed, but had: " + projects,
                3, projects.size());

        assertEquals("Two projects should be filtered out, but had: " + projects,
                1, projectsOfInterest.size());
        assertEquals("1.0", projectsOfInterest.get("centic9/jgit-cookbook"));

        assertEquals(UpgradeStatus.NotAccessible, projectStatuses.get("project/project").getStatus());
        assertEquals(UpgradeStatus.NoStarsOrWatchers, projectStatuses.get("muthu1809/RB").getStatus());
        assertNull(projectStatuses.get("centic9/jgit-cookbook"));
        assertEquals(2, projectStatuses.size());
    }

    @Test(expected = HttpException.class)
    public void testInvalidProject() throws IOException {
        Map<String, String> projects = new HashMap<>();
        projects.put("centic9/invalid project &/()", "1.0");   // invalid project name

        ProjectStatuses projectStatuses = new ProjectStatuses();
        GitHubSupport.filterForProjectsOfInterest(projects, projectStatuses, 10);
    }
}
