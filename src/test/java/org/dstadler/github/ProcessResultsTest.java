package org.dstadler.github;

import com.google.common.collect.SetMultimap;
import org.dstadler.github.util.JSONWriter;
import org.dstadler.github.util.JSONWriter.Holder;
import org.dstadler.github.ProcessResults.VersionChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dstadler.github.util.JSONWriter.addHigherVersions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.dstadler.github.ProcessResults.compareToPrevious;

public class ProcessResultsTest {
    private final static String LINE_PREV = "{\"date\":\"2016-11-11\",\"versions\":{\"3.15-beta2\":[\"https://github.com/centic9/poi-mail-merge/blob/074d96b0d798ded4fb349d7fdf301d1d8a4daa2d//build.gradle\"]}}";
    private final static String LINE_NOW = "{\"date\":\"2016-11-12\",\"versions\":{\"3.15\":[\"https://github.com/centic9/poi-mail-merge/blob/837194207f83c9274bfd175ca94fbac45282f5e4//build.gradle\"]}}";

    // compare two lines where we had a difference
    private SetMultimap<String, String> versions;

    // print out if we found projects that switched versions
    private final List<VersionChange> changes = new ArrayList<>();
    private final Map<String, String> seenRepositoryVersions = new HashMap<>();

    @BeforeEach
    public void setUp() throws IOException {
        versions = JSONWriter.mapper.readValue(LINE_NOW, Holder.class).getRepositoryVersions();
    }

    @Test
    public void testCompareToPreviousNewRepo() {
        compareToPrevious("", versions, changes, seenRepositoryVersions);

        assertEquals(1, changes.size(), "Had: " + changes);
        assertEquals("centic9/poi-mail-merge", changes.get(0).repository, "Had: " + changes);
        assertEquals("<new>", changes.get(0).versionBefore, "Had: " + changes);
        assertEquals("3.15", changes.get(0).versionNow, "Had: " + changes);
    }

    @Test
    public void testCompareToPreviousWithLowerPrevVersion() {
        seenRepositoryVersions.put("centic9/poi-mail-merge", "3.15-beta2");
        compareToPrevious("", versions, changes, seenRepositoryVersions);

        assertEquals(1, changes.size(), "Had: " + changes);
        assertEquals("centic9/poi-mail-merge", changes.get(0).repository, "Had: " + changes);
        assertEquals("3.15-beta2", changes.get(0).versionBefore, "Had: " + changes);
        assertEquals("3.15", changes.get(0).versionNow, "Had: " + changes);
    }

    @Test
    public void testCompareToPreviousWithEqualPrevVersion() {
        seenRepositoryVersions.put("centic9/poi-mail-merge", "3.15");
        compareToPrevious("", versions, changes, seenRepositoryVersions);

        assertEquals(0, changes.size(), "Had: " + changes);
    }

    @Test
    public void testCompareToPreviousWithHigherPrevVersion() {
        seenRepositoryVersions.put("centic9/poi-mail-merge", "3.16");
        compareToPrevious("", versions, changes, seenRepositoryVersions);

        assertEquals(0, changes.size(), "Had: " + changes);
    }

    @Test
    public void testAddHigherVersion() throws IOException {
        SetMultimap<String, String> previousVersions = JSONWriter.mapper.readValue(LINE_PREV, Holder.class).getRepositoryVersions();

        assertEquals(0, seenRepositoryVersions.size());

        addHigherVersions(seenRepositoryVersions, previousVersions);
        assertEquals(1, seenRepositoryVersions.size());
        assertEquals("3.15-beta2", seenRepositoryVersions.values().iterator().next());

        addHigherVersions(seenRepositoryVersions, versions);
        assertEquals(1, seenRepositoryVersions.size());
        assertEquals("3.15", seenRepositoryVersions.values().iterator().next());

        addHigherVersions(seenRepositoryVersions, previousVersions);
        assertEquals(1, seenRepositoryVersions.size());
        assertEquals("3.15", seenRepositoryVersions.values().iterator().next());
    }
}
