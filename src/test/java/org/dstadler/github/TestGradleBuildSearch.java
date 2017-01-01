package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.dstadler.github.GradleBuildSearch.EXCLUDE_REGEX;
import static org.junit.Assert.*;

public class TestGradleBuildSearch {
    private Multimap<String, String> versions = HashMultimap.create();

    @Test
    public void testRegex() throws Exception {
        Pattern excludeRegex = Pattern.compile(EXCLUDE_REGEX);
        assertFalse(excludeRegex.matcher("").find());
        assertFalse(excludeRegex.matcher("compile 'org.apache.poi:poi:3.16-beta1'").find());

        assertTrue(excludeRegex.matcher("group: 'org.apache.poi', name: 'openxml4j'").find());
        assertTrue(excludeRegex.matcher("\"org.apache.poi:ooxml-schemas:1.4\"").find());

        assertEquals("", "group: 'org.apache.poi', name: 'openxml4j'".replaceAll(EXCLUDE_REGEX, ""));
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
                "centic9/poi-mail-merge", "compile 'org.apache.poi:poi:3.13");
        assertEquals("{3.13=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionShortVar() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile 'org.apache.poi:poi:' +poiver");
        assertEquals("{poiver=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionNoVersion() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile 'org.apache.poi:poi'");
        assertEquals("{noVersion=[http://github.com/centic9/poi-mail-merge]}", versions.toString());
    }

    @Test
    public void parseVersionLong() throws Exception {
        new GradleBuildSearch().parseVersion(versions, "http://github.com/centic9/poi-mail-merge",
                "centic9/poi-mail-merge", "compile group: \t 'org.apache.poi', name: 'poi',  version: '3.15'");
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
                "centic9/poi-mail-merge", BaseSearch.GROUP);
        assertEquals("{}", versions.toString());
    }

    @Test
    public void getExcludeRegex() throws Exception {
        assertNotNull(new GradleBuildSearch().getExcludeRegex());
    }
}
