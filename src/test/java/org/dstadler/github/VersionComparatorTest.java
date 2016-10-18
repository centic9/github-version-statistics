package org.dstadler.github;

import org.dstadler.commons.testing.TestHelpers;
import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class VersionComparatorTest {
    @SuppressWarnings("RedundantStringFormatCall")
    @Test
    public void compare() throws Exception {
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "2.0", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "1.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "0.9", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "1.0-beta1", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0-beta1", String.format("1.0-beta1"), "1.0", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.10.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10-beta1", String.format("3.10-beta1"), "3.10.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10-beta1", String.format("3.10-beta1"), "3.10.1.2", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.12.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.9", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.9.1", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.15", String.format("3.15"), "3.9.0", true);

        // 'other' currently is sorted higher than numbers
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "other", false);
    }

    @Test
    public void testSortedSet() {
        Set<String> set = new TreeSet<>(new VersionComparator());
        set.add("other");
        set.add("3.15");
        set.add("3.15-beta2");
        set.add("3.15-beta1");
        set.add("3.14");
        set.add("3.14-beta2");
        set.add("3.14-beta1");
        set.add("3.13");
        set.add("3.13-beta1");
        set.add("3.12");
        set.add("3.12-beta1");
        set.add("3.11");
        set.add("3.10");
        set.add("3.10-beta2");
        set.add("3.10-beta1");
        set.add("3.9.0");
        set.add("3.9");
        set.add("3.8");
        set.add("3.8-beta5");
        set.add("3.8-beta4");
        set.add("3.8-beta3");
        set.add("3.8-beta2");
        set.add("3.7");
        set.add("3.7-beta3");
        set.add("3.6");
        set.add("3.5-beta5");
        set.add("3.5-beta3");
        set.add("3.2");
        set.add("3.14-beta1-20151223");
        set.add("3.13-SNAPSHOT");
        set.add("3.12-bbn");
        set.add("3.12-SNAPSHOT");
        set.add("3.11-beta3");
        set.add("3.11-beta2");
        set.add("3.11-BETA3");
        set.add("3.10.1");
        set.add("3.0.1");
        set.add("3.0");

        assertEquals("[3.0, 3.0.1, 3.11-BETA3, 3.12-SNAPSHOT, 3.12-bbn, 3.2, 3.5-beta3, 3.5-beta5, 3.6, " +
                        "3.7-beta3, 3.7, 3.8-beta2, 3.8-beta3, 3.8-beta4, 3.8-beta5, 3.8, 3.9, 3.9.0, " +
                        "3.10-beta1, 3.10-beta2, 3.10, 3.10.1, 3.11-beta2, 3.11-beta3, 3.11, 3.12-beta1, 3.12, " +
                        "3.13-beta1, 3.13, 3.13-SNAPSHOT, 3.14-beta1, 3.14-beta1-20151223, 3.14-beta2, 3.14, " +
                        "3.15-beta1, 3.15-beta2, 3.15, other]",
                set.toString());
    }
}