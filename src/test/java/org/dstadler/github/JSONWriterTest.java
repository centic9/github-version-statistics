package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class JSONWriterTest {
    private static final String URL_1 = "https://github.com/centic9/poi-mail-merge/blob/074d96b0d798ded4fb349d7fdf301d1d8a4daa2d//build.gradle";
    private static final String URL_2 = "https://github.com/centic9/poi-mail-merge/blob/837194207f83c9274bfd175ca94fbac45282f5e4//build.gradle";

    private static final String JSON =
            "{\"date\":\"2016-11-11\",\"versions\":{" +
                    "\"3.15-beta2\":[\"https://github.com/centic9/poi-mail-merge/blob/074d96b0d798ded4fb349d7fdf301d1d8a4daa2d//build.gradle\"], " +
                    "\"3.15\":[\"https://github.com/centic9/poi-mail-merge/blob/837194207f83c9274bfd175ca94fbac45282f5e4//build.gradle\"]}}";

    @Test
    public void write() throws Exception {
        File dir = File.createTempFile("github-version-statistics", ".dir");
        assertTrue(dir.delete());
        assertTrue(dir.mkdir());
        JSONWriter.STATS_DIR = dir;
        File testFile = new File(dir, "stats2016-01-01.json");
        try {
            SetMultimap<String, String> map = HashMultimap.create();
            JSONWriter.write("2016-01-01", map);
            assertTrue(dir.exists());
            assertEquals(1, FileUtils.readLines(testFile, "UTF-8").size());

            map.put("1.0", URL_1);
            map.put("2.0", URL_1);
            assertEquals(2, map.size());
            JSONWriter.write("2016-01-01", map);
            assertEquals(2, FileUtils.readLines(testFile, "UTF-8").size());

            map.put("3.0", URL_1);
            assertEquals(3, map.size());
            JSONWriter.write("2016-01-01", map);
            List<String> lines = FileUtils.readLines(testFile, "UTF-8");
            assertEquals(3, lines.size());

            JSONWriter.Holder holder = JSONWriter.mapper.readValue(lines.get(0), JSONWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(0, holder.getVersions().size());

            holder = JSONWriter.mapper.readValue(lines.get(1), JSONWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(1, holder.getVersions().size());

            holder = JSONWriter.mapper.readValue(lines.get(2), JSONWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(1, holder.getVersions().size());
        } finally {
            assertTrue(dir.exists());
            assertTrue(testFile.exists());
            assertTrue(testFile.delete());
            assertTrue(dir.delete());
        }
    }

    @Test
    public void testMap() {
        SetMultimap<String, String> map = HashMultimap.create();

        map.put("1.0", URL_1);
        map.put("2.0", URL_1);
        assertEquals(2, map.size());

        map.put("3.0", URL_1);
        assertEquals(3, map.size());

        // adding multiple works
        map.put("3.0", URL_2);
        assertEquals(4, map.size());

        // adding the same does not add new lines
        map.put("3.0", URL_1);
        map.put("3.0", URL_2);
        assertEquals(4, map.size());
    }

    @Test
    public void testGetVersions() throws IOException {
        JSONWriter.Holder holder = JSONWriter.mapper.readValue(JSON, JSONWriter.Holder.class);

        SetMultimap<String, String> repos = holder.getVersions();
        assertEquals("Had: " + repos, 1, repos.keySet().size());
        assertEquals("Had: " + repos, "3.15", repos.keySet().iterator().next());

        assertEquals("Had: " + repos, 1, repos.values().size());
        assertEquals("Had: " + repos, "https://github.com/centic9/poi-mail-merge/blob/837194207f83c9274bfd175ca94fbac45282f5e4//build.gradle", repos.values().iterator().next());
    }

    @Test
    public void testGetRepositoryVersions() throws IOException {
        JSONWriter.Holder holder = JSONWriter.mapper.readValue(JSON, JSONWriter.Holder.class);

        SetMultimap<String, String> repos = holder.getRepositoryVersions();
        assertEquals("Had: " + repos, 1, repos.keySet().size());
        assertEquals("Had: " + repos, "3.15", repos.keySet().iterator().next());

        assertEquals("Had: " + repos, 1, repos.values().size());
        assertEquals("Had: " + repos, "centic9/poi-mail-merge", repos.values().iterator().next());
    }
}
