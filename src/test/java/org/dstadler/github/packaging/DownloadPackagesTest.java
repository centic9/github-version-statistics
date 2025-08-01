package org.dstadler.github.packaging;

import org.junit.jupiter.api.Test;

import static org.dstadler.github.packaging.DownloadPackages.NAME_REGEX;
import static org.junit.jupiter.api.Assertions.*;

class DownloadPackagesTest {

    @Test
    void testRegex() {
        assertFalse(NAME_REGEX.matcher("").matches());
        assertFalse(NAME_REGEX.matcher("abad").matches());
        assertFalse(NAME_REGEX.matcher("noble-amd64-centic9_mtools-ppa-ppa_4.0.43-1-2b5db1ed8fbcaf7c95af99249fdaf5eebe2e04f4").matches());
        assertFalse(NAME_REGEX.matcher("trixie").matches());
        assertFalse(NAME_REGEX.matcher("@@@@").matches());

        assertTrue(NAME_REGEX.matcher("bookworm@amd64@ottok_debcraft@main@512b4c356c842ecf7aecb81d23cb4947a910942a").matches());
        assertTrue(NAME_REGEX.matcher("trixie@amd64@ottok_debcraft@main@512b4c356c842ecf7aecb81d23cb4947a910942a").matches());
        assertTrue(NAME_REGEX.matcher("bookworm@amd64@centic9_laminar-ppa@ppa_1.1-1.1@443d995519cab2202ac680d8f091590696a2d4e4").matches());
    }
}