package org.dstadler.github.search;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;

import static org.dstadler.github.search.MavenPomSearch.PATTERN_NO_VERSION;
import static org.junit.jupiter.api.Assertions.*;

public class MavenPomSearchTest {
    private final Multimap<String, String> versions = HashMultimap.create();

    @Test
    public void testRegex() {
        assertFalse(PATTERN_NO_VERSION.matcher("").find());
        assertFalse(PATTERN_NO_VERSION.matcher("compile 'org.apache.poi:poi:3.16-beta1'").find());
        assertFalse(PATTERN_NO_VERSION.matcher("""
                <dependency>
                      <groupId>org.apache.poi</groupId>
                      <artifactId>poi</artifactId>
                      <version>3.15</version>
                </dependency>""").find());

        assertTrue(PATTERN_NO_VERSION.matcher("""
                <dependency>
                                        <groupId>org.apache.poi</groupId>
                                        <artifactId>poi</artifactId>
                                </dependency>""").find());
        assertTrue(PATTERN_NO_VERSION.matcher("""
                <dependency>
                                        <groupId>org.apache.poi</groupId>
                                        <artifactId>poi-scratchpad</artifactId>
                                </dependency>""").find());
    }

    @Test
    public void addVersionSimple() {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "1.0");
        assertEquals("{1.0=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    @Test
    public void addVersionFINAL() {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "1.0-FINAL");
        assertEquals("{1.0=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    @Test
    public void addVersionBracket() {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "[1.0");
        assertEquals("{1.0=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    @Test
    public void addVersionVersionVariableNotFound() {
        MavenPomSearch.addVersion(versions, "http://github.com/centic9/jgit-cookbook", "", "${variable}");
        assertEquals("{${variable}=[http://github.com/centic9/jgit-cookbook]}", versions.toString());
    }

    /*
    Could not find parent pom at https://raw.githubusercontent.com/seeidea/utils/5200e0cffc33cc24a09ac5a6c4486115608a10a5/pom.xml for
    https://github.com/seeidea/utils/blob/5200e0cffc33cc24a09ac5a6c4486115608a10a5/utils-poi/pom.xml
    */
    @Test
    public void addVersionVersionVariableParentNotFound() {
        MavenPomSearch.addVersion(versions, "https://github.com/seeidea/utils/blob/5200e0cffc33cc24a09ac5a6c4486115608a10a5/utils-poi/pom.xml", "", "${variable}");
        assertEquals("{${variable}=[https://github.com/seeidea/utils/blob/5200e0cffc33cc24a09ac5a6c4486115608a10a5/utils-poi/pom.xml]}", versions.toString());
    }

    @Test
    public void getExcludeRegex() {
        String excludeRegex = new MavenPomSearch().getExcludeRegex();
        assertNotNull(excludeRegex);

        assertFalse("".matches(excludeRegex));
        assertFalse("org.apache.poi".matches(excludeRegex));

        String str =
                """
                <groupId>org.apache.poi</groupId>
                    <artifactId>poi-parent</artifactId>
                    <packaging>pom</packaging>""";
        assertTrue(str.matches(excludeRegex));

        str = "<module.name>org.apache.poi</module.name>";
        assertTrue(str.matches(excludeRegex));

        str = "<org.apache.poi.util.POILogger>org.apache.poi.util.NullLogger</org.apache.poi.util.POILogger>";
        assertTrue(str.matches(excludeRegex));

        str = """
                <dependency>
                      <groupId>org.apache.poi</groupId>
                      <artifactId>poi-ooxml</artifactId>
                      <type>jar</type>
                    </dependency>""";
        assertTrue(str.matches(excludeRegex));

        str = """
                <dependency>
                      <groupId>org.apache.poi</groupId>
                      <artifactId>poi-ooxml-schemas</artifactId>
                      <type>jar</type>
                    </dependency>""";
        assertTrue(str.matches(excludeRegex));

        str = "exclude group:'org.apache.poi',module:'poi-ooxml'";
        assertTrue(str.matches(excludeRegex));

        str = "<replacevalue>org.apache.poi.POIXMLTypeLoader</replacevalue>";
        assertTrue(str.matches(excludeRegex));
    }

    @Test
    public void testParseVersion() {
        MavenPomSearch search = new MavenPomSearch();
        search.parseVersion(versions, "", "", "org.apache.poi");
        assertTrue(versions.isEmpty(), "Had: " + versions);

        search.parseVersion(versions, "url", "",
                """
                <groupId>org.apache.poi</groupId>
                <artifactId>some</artifactId>
                <version>3.15</version>""");
        assertEquals(1, versions.size(), "Had: " + versions);
        assertEquals("3.15", versions.keySet().iterator().next());
        assertEquals("url", versions.values().iterator().next());

        versions.clear();
        search.parseVersion(versions, "url", "",
                """
                <artifactId>some</artifactId>
                <groupId>org.apache.poi</groupId>
                <version>3.15</version>""");
        assertEquals(1, versions.size(), "Had: " + versions);
        assertEquals("3.15", versions.keySet().iterator().next());
        assertEquals("url", versions.values().iterator().next());

        versions.clear();
        search.parseVersion(versions, "url", "",
                """
                <dependency>
                <groupId>org.apache.poi</groupId>
                <artifactId>poi-some</artifactId>
                </dependency>""");
        assertEquals(1, versions.size(), "Had: " + versions);
        assertEquals("noVersion", versions.keySet().iterator().next());
        assertEquals("url", versions.values().iterator().next());
    }
}