package org.dstadler.github.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class StatsTest {
    @Test
    public void test() {
        File[] files = Stats.getFiles();

        assertNotNull(files);
        assertTrue(files.length > 800);
    }
}