package org.dstadler.github;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.dstadler.github.JSONWriter.DATE_FORMAT;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class SearchTest {
    @Test
    public void runSearch() throws Exception {
        File dir = File.createTempFile("github-version-statistics", ".dir");
        assertTrue(dir.delete());
        assertTrue(dir.mkdir());
        File backupDir = JSONWriter.STATS_DIR;
        JSONWriter.STATS_DIR = dir;
        try {
            Search.runSearch(new MyBaseSearch());

            File file = new File(dir, "stats" + DATE_FORMAT.format(new Date()) + ".json");
            assertTrue(file.exists());
            assertTrue(file.delete());
            assertTrue(dir.exists());
            assertTrue(dir.delete());
        } catch (IOException e) {
            // ignore test if we do not have .github credentials
            assumeFalse(e.getMessage().contains("Failed to resolve credentials from ~/.github or the environment."));

            // throw any other exception
            throw e;
        } finally {
            JSONWriter.STATS_DIR = backupDir;
        }
    }

    private static class MyBaseSearch extends BaseSearch {
        @Override
        void search(GitHub github, Multimap<String, String> versions) {
        }

        @Override
        String getExcludeRegex() {
            return null;
        }

        @Override
        void parseVersion(Multimap<String, String> versions, String htmlUrl, String repo, String str) {
        }
    }
}