package org.dstadler.github.util;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GitHubSupportTest {
    @Test
    public void testFilter() throws IOException {
        Map<String, String> projects = new HashMap<>();
        projects.put("project/project", "1.0");
        projects.put("centic9/jgit-cookbook", "1.0");

        Map<String, String> projectsOfInterest = GitHubSupport.filterForProjectsOfInterest(projects);

        assertEquals("Original map not changed, but had: " + projects,
                2, projects.size());
        
        assertEquals("One project should be filtered out, but had: " + projects,
                1, projectsOfInterest.size());
        assertEquals("1.0", projectsOfInterest.get("centic9/jgit-cookbook"));
    }
}