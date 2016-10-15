package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class JSONWriterTest {
    @Test
    public void write() throws Exception {
        File file = File.createTempFile("github-version-statistics", ".json");
        assertTrue(file.delete());
        try {
            SetMultimap<String, String> map = HashMultimap.create();
            JSONWriter.write(file, map);
            assertTrue(file.exists());
            assertEquals(1, FileUtils.readLines(file, "UTF-8").size());

            map.put("1.0", "blabla");
            map.put("2.0", "blabla");
            assertEquals(2, map.size());
            JSONWriter.write(file, map);
            assertEquals(2, FileUtils.readLines(file, "UTF-8").size());

            map.put("3.0", "blabla");
            assertEquals(3, map.size());
            JSONWriter.write(file, map);
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            assertEquals(3, lines.size());

            JSONWriter.Holder holder = JSONWriter.mapper.readValue(lines.get(0), JSONWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(0, holder.getVersions().size());

            holder = JSONWriter.mapper.readValue(lines.get(1), JSONWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(2, holder.getVersions().size());

            holder = JSONWriter.mapper.readValue(lines.get(2), JSONWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(3, holder.getVersions().size());

            // adding multiple works
            map.put("3.0", "blublu");
            assertEquals(4, map.size());

            // adding the same does not add new lines
            map.put("3.0", "blabla");
            map.put("3.0", "blublu");
            assertEquals(4, map.size());
        } finally {
            assertTrue(file.exists());
            assertTrue(file.delete());
        }
    }
}