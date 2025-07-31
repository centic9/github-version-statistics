package org.dstadler.github.search;

import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BaseSearchTest {
    @Test
    public void testConnect() throws Exception {
        final GitHub connect;
        try {
            connect = BaseSearch.connect();
        } catch (IOException e) {
            Assumptions.assumeFalse(e.getMessage().contains("Failed to resolve credentials"), "Ignore missing credentials");
            throw e;
        }

        assertNotNull(connect);
    }

    @Test
    public void testGetRepository() {
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
            Assumptions.assumeFalse(e.getMessage().contains("Failed to resolve credentials"), "Ignore missing credentials");
            throw e;
        }

        assertNotNull(search.getNonForkRepository(BaseSearch.connect(),
                "https://github.com/centic9/jgit-cookbook/blob/README.md"));

        assertNull(search.getNonForkRepository(BaseSearch.connect(),
                "https://github.com/centic9/file-leak-detector/blob/master/README.adoc"));
    }

    private static class MyBaseSearch extends BaseSearch {
        MyBaseSearch() {
        }

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
