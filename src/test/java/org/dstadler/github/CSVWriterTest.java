package org.dstadler.github;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static javafx.scene.input.KeyCode.M;
import static org.junit.Assert.*;

public class CSVWriterTest {
    @Test
    public void write() throws Exception {
        File file = File.createTempFile("github-version-statistics", ".json");
        assertTrue(file.delete());
        try {
            ArrayListMultimap<String, String> map = ArrayListMultimap.create();
            CSVWriter.write(file, map);
            assertTrue(file.exists());
            assertEquals(1, FileUtils.readLines(file, "UTF-8").size());

            map.put("1.0", "blabla");
            map.put("2.0", "blabla");
            CSVWriter.write(file, map);
            assertEquals(2, FileUtils.readLines(file, "UTF-8").size());

            map.put("3.0", "blabla");
            CSVWriter.write(file, map);
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            assertEquals(3, lines.size());

            CSVWriter.Holder holder = CSVWriter.mapper.readValue(lines.get(0), CSVWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(0, holder.getVersions().size());

            holder = CSVWriter.mapper.readValue(lines.get(1), CSVWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(2, holder.getVersions().size());

            holder = CSVWriter.mapper.readValue(lines.get(2), CSVWriter.Holder.class);
            assertNotNull(holder.getDate());
            assertEquals(3, holder.getVersions().size());
        } finally {
            assertTrue(file.exists());
            assertTrue(file.delete());
        }
    }
}