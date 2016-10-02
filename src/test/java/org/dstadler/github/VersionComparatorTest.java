package org.dstadler.github;

import org.dstadler.commons.testing.TestHelpers;
import org.junit.Test;

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
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.12.1", false);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.9", true);
        TestHelpers.ComparatorTest(new VersionComparator(), "3.10", String.format("3.10"), "3.9.1", true);

        // 'other' currently is sorted higher than numbers
        TestHelpers.ComparatorTest(new VersionComparator(), "1.0", String.format("1.0"), "other", false);
    }
}