package org.dstadler.github.upgrade;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

public class ProjectStatusesTest {
    @Test
    public void test() {
        ProjectStatuses statuses = new ProjectStatuses();
        assertNull(statuses.get("project1"));
        statuses.add(new ProjectStatus("project2", UpgradeStatus.BuildSucceeded));

        assertNull(statuses.get("project1"));
        ProjectStatus project2 = statuses.get("project2");
        assertEquals("project2", project2.getProject());
        assertEquals(UpgradeStatus.BuildSucceeded, project2.getStatus());
    }

    @Test
    public void testReadWrite() throws IOException {
        ProjectStatuses statuses = ProjectStatuses.read();

        assertNotNull(statuses.get("blezek/Notion"));

        // for now only write the same data back
        statuses.write();
    }

    @Test(expected = FileNotFoundException.class)
    public void testReadInvalid() throws IOException {
        ProjectStatuses statuses = new ProjectStatuses();
        statuses.read("unknownFile.csv");
    }

    @Test
    public void testReadSingleLine() throws IOException {
        File tempFile = File.createTempFile("ProjectStatusesTest", ".csv");

        try {
            FileUtils.writeStringToFile(tempFile,
                    "Project,Status\n" +
                    "shameel0784/ajexp,UnknownBuildSystem\n",
                    "UTF-8");

            ProjectStatuses statuses = new ProjectStatuses();
            statuses.read(tempFile.getAbsolutePath());

            assertNotNull(statuses.get("shameel0784/ajexp"));
            assertEquals(1, statuses.size());
        } finally {
            if(tempFile.exists()) {
                assertTrue(tempFile.delete());
            }
        }
    }
}
