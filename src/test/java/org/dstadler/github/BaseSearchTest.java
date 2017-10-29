package org.dstadler.github;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Assume;
import org.junit.Test;
import org.kohsuke.github.GitHub;

import com.google.common.collect.Multimap;

public class BaseSearchTest {
    @Test
    public void testConnect() throws Exception {
        final GitHub connect;
        try {
            connect = BaseSearch.connect();
        } catch (IOException e) {
            Assume.assumeFalse("Ignore missing credentials", e.getMessage().contains("Failed to resolve credentials"));
            throw e;
        }

        assertNotNull(connect);
    }

    @Test
    public void testGetRepository() throws Exception {
        assertNull(BaseSearch.getRepository(""));
        assertNull(BaseSearch.getRepository("some url"));

        assertEquals("user/repo", BaseSearch.getRepository("https://github.com/user/repo/blob/some_file"));
    }

    @Test
    public void testGetNonForkRepository() throws Exception {
        BaseSearch search = new MyBaseSearch();

        try {
            assertNull(search.getNonForkRepository(BaseSearch.connect(), ""));
        } catch (IOException e) {
            Assume.assumeFalse("Ignore missing credentials", e.getMessage().contains("Failed to resolve credentials"));
            throw e;
        }

        assertNotNull(search.getNonForkRepository(BaseSearch.connect(),
                "https://github.com/centic9/jgit-cookbook/blob/README.md"));

        assertNull(search.getNonForkRepository(BaseSearch.connect(),
                "https://github.com/a0xec0/Spoon-Knife/blob/README.md"));
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
