package org.dstadler.github;

import org.dstadler.commons.testing.TestHelpers;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VersionComparatorTest {
    @SuppressWarnings("RedundantStringFormatCall")
    @Test
    public void compare() throws Exception {
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "2.0", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "1.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "0.9", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "1.0-beta1", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "1.0-BETA1", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "1.0-SNAPSHOT", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0-beta1", String.format("1.0-beta1"), "1.0", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0-beta1", String.format("1.0-beta1"), "1.0-SNAPSHOT", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.10.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10-beta1", String.format("3.10-beta1"), "3.10.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10-beta1", String.format("3.10-beta1"), "3.10.1.2", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.12.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.9", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.9.1", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.15", String.format("3.15"), "3.9.0", true);

        // 'other' currently is sorted higher than numbers
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "other", false);

        // arbitrary text is still sorted as good as possible
        TestHelpers.ComparatorTest(new VersionComparator(), "blabla1", String.format("blabla1"), "blabla2", false);
    }

    @Test
    public void testSortedSet() {
        Set<String> list = new TreeSet<>(new VersionComparator());
        list.add("other");
        list.add("3.15");
        list.add("3.15-beta2");
        list.add("3.15-beta1");
        list.add("3.14");
        list.add("3.14-beta2");
        list.add("3.14-beta1");
        list.add("3.13");
        list.add("3.13-beta1");
        list.add("3.12");
        list.add("3.12-beta1");
        list.add("3.11");
        list.add("3.10");
        list.add("3.10-beta2");
        list.add("3.10-beta1");
        list.add("3.9.0");
        list.add("3.9");
        list.add("3.8");
        list.add("3.8-beta5");
        list.add("3.8-beta4");
        list.add("3.8-beta3");
        list.add("3.8-beta2");
        list.add("3.7");
        list.add("3.7-beta3");
        list.add("3.6");
        list.add("3.5-beta5");
        list.add("3.5-beta3");
        list.add("3.2");
        list.add("3.14-beta1-20151223");
        list.add("3.13-SNAPSHOT");
        list.add("3.12-bbn");
        list.add("3.12-SNAPSHOT");
        list.add("3.11-beta3");
        list.add("3.11-beta2");
        list.add("3.11-BETA3");
        list.add("3.10.1");
        list.add("3.0.1");
        list.add("3.0");

        assertEquals("[3.0, 3.0.1, 3.2, 3.5-beta3, 3.5-beta5, 3.6, " +
                        "3.7-beta3, 3.7, 3.8-beta2, 3.8-beta3, 3.8-beta4, 3.8-beta5, 3.8, 3.9, 3.9.0, " +
                        "3.10-beta1, 3.10-beta2, 3.10, 3.10.1, 3.11-beta2, 3.11-BETA3, 3.11-beta3, 3.11, 3.12-SNAPSHOT, 3.12-beta1, 3.12, 3.12-bbn, " +
                        "3.13-SNAPSHOT, 3.13-beta1, 3.13, 3.14-beta1, 3.14-beta1-20151223, 3.14-beta2, 3.14, " +
                        "3.15-beta1, 3.15-beta2, 3.15, other]",
                list.toString());
    }

    @Test
    public void testSortedListReverseSortedAdd() {
        List<String> list = new ArrayList<>();
        list.add("other");
        list.add("3.15");
        list.add("3.15-beta2");
        list.add("3.15-beta1");
        list.add("3.14");
        list.add("3.14-beta2");
        list.add("3.14-beta1");
        list.add("3.13");
        list.add("3.13-beta1");
        list.add("3.12");
        list.add("3.12-beta1");
        list.add("3.11");
        list.add("3.10");
        list.add("3.10-beta2");
        list.add("3.10-beta1");
        list.add("3.9.0");
        list.add("3.9");
        list.add("3.8");
        list.add("3.8-beta5");
        list.add("3.8-beta4");
        list.add("3.8-beta3");
        list.add("3.8-beta2");
        list.add("3.7");
        list.add("3.7-beta3");
        list.add("3.6");
        list.add("3.5-beta5");
        list.add("3.5-beta3");
        list.add("3.2");
        list.add("3.14-beta1-20151223");
        list.add("3.13-SNAPSHOT");
        list.add("3.12-bbn");
        list.add("3.12-SNAPSHOT");
        list.add("3.11-beta3");
        list.add("3.11-beta2");
        list.add("3.11-BETA3");
        list.add("3.10.1");
        list.add("3.0.1");
        list.add("3.0");

        Collections.sort(list, new VersionComparator());

        assertEquals("[3.0, 3.0.1, 3.2, 3.5-beta3, 3.5-beta5, 3.6, " +
                        "3.7-beta3, 3.7, 3.8-beta2, 3.8-beta3, 3.8-beta4, 3.8-beta5, 3.8, 3.9, 3.9.0, " +
                        "3.10-beta1, 3.10-beta2, 3.10, 3.10.1, 3.11-beta2, 3.11-BETA3, 3.11-beta3, 3.11, 3.12-SNAPSHOT, 3.12-beta1, 3.12, 3.12-bbn, " +
                        "3.13-SNAPSHOT, 3.13-beta1, 3.13, 3.14-beta1, 3.14-beta1-20151223, 3.14-beta2, 3.14, " +
                        "3.15-beta1, 3.15-beta2, 3.15, other]",
                list.toString());
    }

    @Test
    public void testSortedListArbitraryAdd() {
        List<String> list = new ArrayList<>();
        list.add("3.11-BETA3");
        list.add("3.8-beta3");
        list.add("3.11-beta2");
        list.add("3.14-beta1");
        list.add("3.13");
        list.add("3.13-beta1");
        list.add("3.12");
        list.add("3.15");
        list.add("3.12-beta1");
        list.add("3.10.1");
        list.add("3.10");
        list.add("3.10-beta2");
        list.add("3.0.1");
        list.add("3.5-beta3");
        list.add("3.9.0");
        list.add("3.9");
        list.add("3.8-beta4");
        list.add("3.8");
        list.add("3.0");
        list.add("3.8-beta5");
        list.add("3.8-beta2");
        list.add("3.7");
        list.add("3.7-beta3");
        list.add("3.12-SNAPSHOT");
        list.add("3.6");
        list.add("3.14");
        list.add("3.15-beta2");
        list.add("3.15-beta1");
        list.add("3.5-beta5");
        list.add("3.2");
        list.add("3.14-beta1-20151223");
        list.add("3.13-SNAPSHOT");
        list.add("3.10-beta1");
        list.add("3.14-beta2");
        list.add("3.12-bbn");
        list.add("3.11");
        list.add("other");
        list.add("3.11-beta3");

        Collections.sort(list, new VersionComparator());

        assertEquals("[3.0, 3.0.1, 3.2, 3.5-beta3, 3.5-beta5, 3.6, " +
                        "3.7-beta3, 3.7, 3.8-beta2, 3.8-beta3, 3.8-beta4, 3.8-beta5, 3.8, 3.9, 3.9.0, " +
                        "3.10-beta1, 3.10-beta2, 3.10, 3.10.1, 3.11-beta2, 3.11-BETA3, 3.11-beta3, 3.11, 3.12-SNAPSHOT, 3.12-bbn, 3.12-beta1, 3.12, " +
                        "3.13-SNAPSHOT, 3.13-beta1, 3.13, 3.14-beta1, 3.14-beta1-20151223, 3.14-beta2, 3.14, " +
                        "3.15-beta1, 3.15-beta2, 3.15, other]",
                list.toString());
    }

    @Test
    public void testParse() {
        assertEquals("Version{major=1, minor=0, revision=0, beta=false, snapshot=false, other=false, betaSnapshotOther=0}", VersionComparator.Version.parse("1.0").toString());
        assertEquals("Version{major=3, minor=0, revision=1, beta=false, snapshot=false, other=false, betaSnapshotOther=0}", VersionComparator.Version.parse("3.0.1").toString());
        assertEquals("Version{major=3, minor=5, revision=0, beta=true, snapshot=false, other=false, betaSnapshotOther=0}", VersionComparator.Version.parse("3.5-beta").toString());
        assertEquals("Version{major=3, minor=5, revision=0, beta=true, snapshot=false, other=false, betaSnapshotOther=2}", VersionComparator.Version.parse("3.5-beta2").toString());
        assertEquals("Version{major=3, minor=5, revision=0, beta=true, snapshot=false, other=false, betaSnapshotOther=2}", VersionComparator.Version.parse("3.5-BETA2").toString());
        assertEquals("Version{major=3, minor=5, revision=0, beta=false, snapshot=true, other=false, betaSnapshotOther=0}", VersionComparator.Version.parse("3.5-SNAPSHOT").toString());
        assertEquals("Version{major=3, minor=5, revision=0, beta=false, snapshot=true, other=false, betaSnapshotOther=17}", VersionComparator.Version.parse("3.5-SNAPSHOT17").toString());
        assertEquals("Version{major=3, minor=5, revision=0, beta=false, snapshot=false, other=true, betaSnapshotOther=0}", VersionComparator.Version.parse("3.5-bbn").toString());
        assertEquals("Version{major=3, minor=5, revision=0, beta=true, snapshot=false, other=false, betaSnapshotOther=20151223}", VersionComparator.Version.parse("3.5-beta-20151223").toString());
        assertEquals("Version{major=3, minor=14, revision=0, beta=true, snapshot=false, other=false, betaSnapshotOther=1}", VersionComparator.Version.parse("3.14-beta1-20151223").toString());
        assertEquals("Version{major=3, minor=9, revision=0, beta=false, snapshot=false, other=false, betaSnapshotOther=0}", VersionComparator.Version.parse("3.9.0").toString());
        assertEquals("Version{major=99, minor=0, revision=0, beta=false, snapshot=false, other=false, betaSnapshotOther=0}", VersionComparator.Version.parse("other").toString());
    }

    @Test
    public void testFailingCase1() {
        VersionComparator.Version v1 = VersionComparator.Version.parse("1.0-SNAPSHOT");
        VersionComparator.Version v2 = VersionComparator.Version.parse("1.0");

        assertTrue(v1.compareTo(v1) == 0);
        assertTrue(v2.compareTo(v2) == 0);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);

        final int ret = new VersionComparator().compare("1.0-SNAPSHOT", "1.0");
        assertTrue("Item 'notequal' should be less than item 'equal' in ComparatorTest, but compare was: " + ret,
                ret < 0);
    }

    @Test
    public void testFailingCase2() {
        VersionComparator.Version v1 = VersionComparator.Version.parse("3.0.1");
        VersionComparator.Version v2 = VersionComparator.Version.parse("3.12-SNAPSHOT");

        assertTrue(v1.compareTo(v1) == 0);
        assertTrue(v2.compareTo(v2) == 0);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);

        final int ret = new VersionComparator().compare("3.0.1", "3.12-SNAPSHOT");
        assertTrue("Item 'notequal' should be less than item 'equal' in ComparatorTest, but compare was: " + ret,
                ret < 0);
    }

    @Test
    public void testFailingCase3() {
        VersionComparator.Version v1 = VersionComparator.Version.parse("3.2");
        VersionComparator.Version v2 = VersionComparator.Version.parse("3.8-beta5");

        assertTrue(v1.compareTo(v1) == 0);
        assertTrue(v2.compareTo(v2) == 0);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);

        final int ret = new VersionComparator().compare("3.2", "3.8-beta5");
        assertTrue("Item 'notequal' should be less than item 'equal' in ComparatorTest, but compare was: " + ret,
                ret < 0);
    }

    @Test
    public void testFailingCase4() {
        VersionComparator.Version v1 = VersionComparator.Version.parse("3.2-beta");
        VersionComparator.Version v2 = VersionComparator.Version.parse("3.9");

        assertTrue(v1.compareTo(v1) == 0);
        assertTrue(v2.compareTo(v2) == 0);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);

        final int ret = new VersionComparator().compare("3.2-beta", "3.9");
        assertTrue("Item 'notequal' should be less than item 'equal' in ComparatorTest, but compare was: " + ret,
                ret < 0);
    }

    @Test
    public void testFailingCase5() {
        VersionComparator.Version v1 = VersionComparator.Version.parse("3.2");
        VersionComparator.Version v2 = VersionComparator.Version.parse("3.14-beta1-20151223");

        assertTrue(v1.compareTo(v1) == 0);
        assertTrue(v2.compareTo(v2) == 0);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);

        final int ret = new VersionComparator().compare("3.2", "3.14-beta1-20151223");
        assertTrue("Item 'notequal' should be less than item 'equal' in ComparatorTest, but compare was: " + ret,
                ret < 0);
    }

    @Test
    public void testFailingCase6() {
        VersionComparator.Version v1 = VersionComparator.Version.parse("3.14-beta1-20151223");
        VersionComparator.Version v2 = VersionComparator.Version.parse("3.14-beta2");

        assertTrue(v1.compareTo(v1) == 0);
        assertTrue(v2.compareTo(v2) == 0);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);

        final int ret = new VersionComparator().compare("3.14-beta1-20151223", "3.14-beta2");
        assertTrue("Item 'notequal' should be less than item 'equal' in ComparatorTest, but compare was: " + ret,
                ret < 0);
    }
}