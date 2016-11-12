package org.dstadler.github;

import com.google.common.collect.SetMultimap;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.dstadler.github.ProcessResults.compareToPrevious;
import static org.junit.Assert.assertEquals;

public class ProcessResultsTest {
    private final static String LINE_PREV = "{\"date\":\"2016-11-11\",\"versions\":{\"3.15-beta2\":[\"https://github.com/centic9/poi-mail-merge/blob/074d96b0d798ded4fb349d7fdf301d1d8a4daa2d//build.gradle\"]}}";
    private final static String LINE_NOW = "{\"date\":\"2016-11-12\",\"versions\":{\"3.15\":[\"https://github.com/centic9/poi-mail-merge/blob/837194207f83c9274bfd175ca94fbac45282f5e4//build.gradle\"]}}";

    @Test
    public void testCompareToPrevious() throws IOException {
        // compare two lines where we had a difference
        SetMultimap<String, String> previousVersions = JSONWriter.mapper.readValue(LINE_PREV, JSONWriter.Holder.class).getRepositoryVersions();
        SetMultimap<String, String> versions = JSONWriter.mapper.readValue(LINE_NOW, JSONWriter.Holder.class).getRepositoryVersions();

        // print out if we found projects that switched versions
        List<ProcessResults.VersionChange> changes = new ArrayList<>();
        compareToPrevious("", previousVersions, versions, changes);

        assertEquals("Had: " + changes, 1, changes.size());
        assertEquals("Had: " + changes, "centic9/poi-mail-merge", changes.get(0).repository);
        assertEquals("Had: " + changes, "3.15-beta2", changes.get(0).versionBefore);
        assertEquals("Had: " + changes, "3.15", changes.get(0).versionNow);
    }
}
