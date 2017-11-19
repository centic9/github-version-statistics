package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;

import static org.dstadler.github.MavenPomSearch.PATTERN_NO_VERSION;
import static org.junit.Assert.*;

public class MavenPomSearchTest {
    private final Multimap<String, String> versions = HashMultimap.create();

    @Test
    public void testRegex() throws Exception {
        assertFalse(PATTERN_NO_VERSION.matcher("").find());
        assertFalse(PATTERN_NO_VERSION.matcher("compile 'org.apache.poi:poi:3.16-beta1'").find());
        assertFalse(PATTERN_NO_VERSION.matcher("<dependency>\n" +
                "      <groupId>org.apache.poi</groupId>\n" +
                "      <artifactId>poi</artifactId>\n" +
                "      <version>3.15</version>\n" +
                "</dependency>").find());

        assertTrue(PATTERN_NO_VERSION.matcher("<dependency>\n" +
                "                        <groupId>org.apache.poi</groupId>\n" +
                "                        <artifactId>poi</artifactId>\n" +
                "                </dependency>").find());
        assertTrue(PATTERN_NO_VERSION.matcher("<dependency>\n" +
                "                        <groupId>org.apache.poi</groupId>\n" +
                "                        <artifactId>poi-scratchpad</artifactId>\n" +
                "                </dependency>").find());
    }

    @Test
    public void addVersionSimple() throws Exception {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "1.0");
        assertEquals("{1.0=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    @Test
    public void addVersionFINAL() throws Exception {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "1.0-FINAL");
        assertEquals("{1.0=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    @Test
    public void addVersionBracket() throws Exception {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "[1.0");
        assertEquals("{1.0=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    @Test
    public void addVersionVersionVariableNotFound() throws Exception {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "${variable}");
        assertEquals("{${variable}=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    /*
    Could not find parent pom at https://raw.githubusercontent.com/seeidea/utils/5200e0cffc33cc24a09ac5a6c4486115608a10a5/pom.xml for
    https://github.com/seeidea/utils/blob/5200e0cffc33cc24a09ac5a6c4486115608a10a5/utils-poi/pom.xml
    */
    @Test
    public void addVersionVersionVariableParentNotFound() throws Exception {
        MavenPomSearch.addVersion(versions, "https://github.com/seeidea/utils/blob/5200e0cffc33cc24a09ac5a6c4486115608a10a5/utils-poi/pom.xml", "", "${variable}");
        assertEquals("{${variable}=[https://github.com/seeidea/utils/blob/5200e0cffc33cc24a09ac5a6c4486115608a10a5/utils-poi/pom.xml]}", versions.toString());
    }

    @Test
    public void getExcludeRegex() throws Exception {
        String excludeRegex = new MavenPomSearch().getExcludeRegex();
        assertNotNull(excludeRegex);

        assertFalse("".matches(excludeRegex));
        assertFalse("org.apache.poi".matches(excludeRegex));

        String str =
                "<groupId>org.apache.poi</groupId>\n" +
                "    <artifactId>poi-parent</artifactId>\n" +
                "    <packaging>pom</packaging>";
        assertTrue(str.matches(excludeRegex));

        str = "<module.name>org.apache.poi</module.name>";
        assertTrue(str.matches(excludeRegex));

        str = "<org.apache.poi.util.POILogger>org.apache.poi.util.NullLogger</org.apache.poi.util.POILogger>";
        assertTrue(str.matches(excludeRegex));

        str = "<dependency>\n" +
                "      <groupId>org.apache.poi</groupId>\n" +
                "      <artifactId>poi-ooxml</artifactId>\n" +
                "      <type>jar</type>\n" +
                "    </dependency>";
        assertTrue(str.matches(excludeRegex));

        str = "<dependency>\n" +
                "      <groupId>org.apache.poi</groupId>\n" +
                "      <artifactId>poi-ooxml-schemas</artifactId>\n" +
                "      <type>jar</type>\n" +
                "    </dependency>";
        assertTrue(str.matches(excludeRegex));
    }

    @Test
    public void testParseVersion() {
        MavenPomSearch search = new MavenPomSearch();
        search.parseVersion(versions, "", "", "org.apache.poi");
        assertTrue("Had: " + versions, versions.isEmpty());

        search.parseVersion(versions, "url", "",
                "<groupId>org.apache.poi</groupId>\n"
                + "<artifactId>some</artifactId>\n"
                + "<version>3.15</version>");
        assertEquals("Had: " + versions, 1, versions.size());
        assertEquals("3.15", versions.keySet().iterator().next());
        assertEquals("url", versions.values().iterator().next());

        versions.clear();
        search.parseVersion(versions, "url", "",
                "<artifactId>some</artifactId>\n"
                + "<groupId>org.apache.poi</groupId>\n"
                + "<version>3.15</version>");
        assertEquals("Had: " + versions, 1, versions.size());
        assertEquals("3.15", versions.keySet().iterator().next());
        assertEquals("url", versions.values().iterator().next());

        versions.clear();
        search.parseVersion(versions, "url", "",
                "<dependency>\n"
                        + "<groupId>org.apache.poi</groupId>\n"
                        + "<artifactId>poi-some</artifactId>\n"
                        + "</dependency>");
        assertEquals("Had: " + versions, 1, versions.size());
        assertEquals("noVersion", versions.keySet().iterator().next());
        assertEquals("url", versions.values().iterator().next());
    }
}