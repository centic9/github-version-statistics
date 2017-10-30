package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.dstadler.commons.testing.TestHelpers;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.dstadler.github.BaseSearch.GROUP;
import static org.dstadler.github.GradleBuildSearch.EXCLUDE_REGEX;
import static org.junit.Assert.*;

public class GradleBuildSearchTest {
    private static final Pattern EXCLUDE_PATTERN = Pattern.compile(EXCLUDE_REGEX);

    private final Multimap<String, String> versions = HashMultimap.create();

    @Test
    public void testRegex() throws Exception {
        assertFalse(EXCLUDE_PATTERN.matcher("").find());
        assertFalse(EXCLUDE_PATTERN.matcher("compile 'org.apache.poi:poi:3.16-beta1'").find());

        assertTrue(EXCLUDE_PATTERN.matcher("group: 'org.apache.poi', name: 'openxml4j'").find());
        assertTrue(EXCLUDE_PATTERN.matcher("\"org.apache.poi:ooxml-schemas:1.4\"").find());
        assertTrue(EXCLUDE_PATTERN.matcher("compile group: 'org.apache.poi', name: 'ooxml-schemas', version: '1.3'").find());
        assertTrue(EXCLUDE_PATTERN.matcher(" compile files('libs/org.apache.poi.xwpf.converter.xhtml-1.0.0.jar')").find());

        assertEquals("", EXCLUDE_PATTERN.matcher("group: 'org.apache.poi', name: 'openxml4j'").replaceAll(""));
    }

    @Test
    public void addVersionSimple() throws Exception {
        GradleBuildSearch.addVersion(versions, "http://github.com/centic9/poi-mail-merge", "", "1.0");
        assertEquals("{1.0=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void addVersionFinal() throws Exception {
        GradleBuildSearch.addVersion(versions, "http://github.com/centic9/poi-mail-merge", "", "1.0-FINAL");
        assertEquals("{1.0=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void addVersionBracket() throws Exception {
        GradleBuildSearch.addVersion(versions, "http://github.com/centic9/poi-mail-merge", "", "[1.0");
        assertEquals("{1.0=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void addVersionVariables() throws Exception {
        GradleBuildSearch.addVersion(versions, "http://github.com/centic9/poi-mail-merge", "def var = 3.16; '+var", "var");
        assertEquals("{3.16=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void addVersionVarBracket() throws Exception {
        final String url = "http://github.com/centic9/poi-mail-merge";
        final String match = "poiVersion)";
        GradleBuildSearch.addVersion(versions, url, "def poiVersion = 3.16; '+poiVersion)", match);
        assertEquals("{3.16=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void addVersionInvalidRegex() throws Exception {
        final String url = "http://github.com/centic9/poi-mail-merge";
        final String match = "poiVersion))";
        try {
            GradleBuildSearch.addVersion(versions, url, "def var = 3.16; '+poiVersion))", match);
            fail("Should catch an exception here");
        } catch (IllegalStateException e) {
            TestHelpers.assertContains(e, url, match);
        }
    }

    @Test
    public void addVersionInvalidRegex2() throws Exception {
        final String url = "http://github.com/centic9/poi-mail-merge";
        final String match = "poiVersion)a";
        try {
            GradleBuildSearch.addVersion(versions, url, "def var = 3.16; '+poiVersion)a", match);
            fail("Should catch an exception here");
        } catch (IllegalStateException e) {
            TestHelpers.assertContains(e, url, match);
        }
    }

    @Test
    public void addVersionDollarVariableBracket() throws Exception {
        GradleBuildSearch.addVersion(versions, "http://github.com/centic9/poi-mail-merge", "var = '3.16';", "${var}");
        assertEquals("{3.16=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void addVersionDollarVariable() throws Exception {
        GradleBuildSearch.addVersion(versions, "http://github.com/centic9/poi-mail-merge", "var = '3.16';", "$var");
        assertEquals("{3.16=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void addVersionVariablesNotFound() throws Exception {
        GradleBuildSearch.addVersion(versions, "http://github.com/centic9/poi-mail-merge", "def var = 3.16;", "var");
        assertEquals("{var=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionShort() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile '" + GROUP + ":poi:3.13");
        assertEquals("{3.13=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionShortVar() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile '" + GROUP + ":poi:' +poiver");
        assertEquals("{poiver=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionNoVersion() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile 'org.apache.poi:poi'");
        assertEquals("{noVersion=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionNoVersionDoubleQuotes() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile \"org.apache.poi:poi\"");
        assertEquals("{noVersion=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionLong() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile group: \t '" + GROUP + "', name: 'poi',  version: '3.15'");
        assertEquals("{3.15=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionNoLog() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile 'fr.opensagres.xdocreport:org.apache.poi.");
        assertEquals("{}", versions.toString());

        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "main = 'org.apache.poi.benchmark");
        assertEquals("{}", versions.toString());
    }

    @Test
    public void parseVersionNotFound() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", GROUP);
        assertEquals("{}", versions.toString());
    }

    @Test
    public void getExcludeRegex() throws Exception {
        String excludeRegex = new GradleBuildSearch().getExcludeRegex();
        assertNotNull(excludeRegex);

        assertFalse("".matches(excludeRegex));
        assertFalse("org.apache.poi".matches(excludeRegex));

        String str =
                "<groupId>org.apache.poi</groupId>\n" +
                        "    <artifactId>poi-parent</artifactId>\n" +
                        "    <packaging>pom</packaging>";
        assertFalse(str.matches(excludeRegex));

        str = "<module.name>org.apache.poi</module.name>";
        assertFalse(str.matches(excludeRegex));

        str = "exclude group: 'org.apache.poi', module: 'poi'";
        assertTrue(str.matches(excludeRegex));

        str = "org.apache.poi.xwpf.";
        assertTrue(str.matches(excludeRegex));
    }
}
