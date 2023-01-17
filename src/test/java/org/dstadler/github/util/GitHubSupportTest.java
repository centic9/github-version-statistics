package org.dstadler.github.util;

import com.google.common.collect.ImmutableSet;
import org.dstadler.github.search.BaseSearch;
import org.dstadler.github.upgrade.ProjectStatuses;
import org.dstadler.github.upgrade.UpgradeStatus;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class GitHubSupportTest {
	private static final String HOME;
	static {
		String h = System.getenv("HOME");
		if (h == null) {
			h = System.getProperty("user.home");
		}

		assertNotNull("Could not read home-directory: \n" +
						System.getenv() + "\n" +
						System.getProperties(),
				h);

		HOME = h;
	}

    @Before
    public void setUp() {
		File credFile = new File(HOME + "/.github");
        if(!credFile.exists()) {
            //noinspection ConstantConditions
            Assume.assumeFalse("Credentials need to exist at " + credFile.getAbsolutePath() + " for this test", true);
        }
    }

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

    @Test//(expected = HttpException.class)
    public void testInvalidProject() throws IOException {
        Map<String, String> projects = new HashMap<>();
        projects.put("centic9/invalid project", "1.0");   // invalid project name

        ProjectStatuses projectStatuses = new ProjectStatuses();
        Map<String, String> map = GitHubSupport.filterForProjectsOfInterest(projects, projectStatuses, 10);
        assertTrue(map.isEmpty());

        /*GitHub github = BaseSearch.connect();
        GHRepository repository = github.getRepository(projects.keySet().iterator().next());
        assertTrue(0 < repository.getStargazersCount());
        assertTrue(0 < repository.getWatchers());

        fail("Should catch Exception, but had: " + map);*/
    }

    @Test
    public void testInvalidProjectName() throws IOException {
        Map<String, String> projects = new HashMap<>();
        projects.put("centic9/invalid project &/()", "1.0");   // invalid project name

        ProjectStatuses projectStatuses = new ProjectStatuses();
        assertThrows(IllegalArgumentException.class,
                () -> GitHubSupport.filterForProjectsOfInterest(projects, projectStatuses, 10));
    }

    @Test
    public void testNullPointerException() throws IOException {
        GitHub github = GitHub.connect();

        final PagedSearchIterable<GHContent> list = github.searchContent().
                repo("mutasDev/cryptApiSet").
                filename("build.gradle").list();

        for(GHContent match : list) {
            System.out.println("Reading " + match.getHtmlUrl());
            //noinspection ProhibitedExceptionCaught
            try (final InputStream stream = match.read()) {
                assertNotNull(stream);
            } catch (NullPointerException e) {
                Assume.assumeNoException("See https://github.com/github-api/github-api/issues/729", e);
            }
        }
    }

    private static final ImmutableSet<String> IGNORED_REPO_ENDS_WITH = ImmutableSet.of(
            "-ppa",
            ".ppa",
            "-fuzz",
            "jacococoveragecolumn-plugin",
            "gwt-gradle-example-issue81"
    );

    @Test
    public void listRepositories() throws IOException {
        GitHub github = BaseSearch.connect();

        for (GHRepository repository : github.getUser("centic9").listRepositories()) {
            if (repository.isFork()) {
                continue;
            }

            if (isIgnored(repository)) {
                continue;
            }

            printRepo("centic9/" + repository.getName());
        }

        // include some others
        printRepo("openambitproject/openambit");
        printRepo("apache/poi");
        printRepo("apache/xmlbeans");
        printRepo("jenkinsci/jacoco-plugin");
        printRepo("jajuk-team/jajuk");

        // this is manually uploaded to the following page
        // https://github.com/centic9/centic9.github.io/blob/master/STATUS.md
    }

    private void printRepo(String repository) {
        //System.out.println("Had repository: " + repository.getName());
        // | [rhasspy](https://github.com/rhasspy/rhasspy) | [![Tests](https://github.com/rhasspy/rhasspy/workflows/Tests/badge.svg)](https://github.com/rhasspy/rhasspy/actions) | [![Open issues](https://img.shields.io/github/issues-raw/rhasspy/rhasspy)](https://github.com/rhasspy/rhasspy/issues) | [![Open pull requests](https://img.shields.io/github/issues-pr-raw/rhasspy/rhasspy)](https://github.com/rhasspy/rhasspy/pulls) |
        System.out.format("| [%s](https://github.com/%s) | " +
                        "[![Tests](https://github.com/%s/workflows/Build%%20and%%20check/badge.svg)](https://github.com/%s/actions) | " +
                        "[![Open issues](https://img.shields.io/github/issues-raw/%s)](https://github.com/%s/issues) | " +
                        "[![Open pull requests](https://img.shields.io/github/issues-pr-raw/%s)](https://github.com/%s/pulls) |\n",
                repository, repository, repository, repository, repository, repository, repository, repository);
    }

    private boolean isIgnored(GHRepository repository) {
        for (String ignoredRepo : IGNORED_REPO_ENDS_WITH) {
            if (repository.getName().endsWith(ignoredRepo)) {
                return true;
            }
        }
        return false;
    }
}
