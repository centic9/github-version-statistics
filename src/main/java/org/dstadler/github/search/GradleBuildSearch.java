package org.dstadler.github.search;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GradleBuildSearch extends BaseSearch {
    // compile 'org.apache.poi:poi:3.13'
    private final static Pattern PATTERN_SHORT = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + VERSION + QUOTE);

    private final static Pattern PATTERN_SHORT_VAR = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + QUOTE + "\\s*\\+\\s*" + VERSION);

    // compile group: 'org.apache.poi', name: 'poi', version: '3.15'
    private final static Pattern PATTERN_LONG = Pattern.compile("group\\s*:\\s*" + QUOTE + GROUP_REGEX + QUOTE + "\\s*,\\s*name\\s*:\\s*" + QUOTE + "[-a-z]+" + QUOTE + "\\s*,\\s*version\\s*:\\s*" + QUOTE + VERSION + QUOTE);

    // poiVersion = '3.10-FINAL'
    private static final String VERSION_VAR_PATTERN = "\\s*=\\s*" + QUOTE + VERSION + QUOTE;

    // exclude some pattern that caused false versions to be reported,
    // we currently simple remove these from the found file before looking for the version
    protected static final String EXCLUDE_REGEX = ("(?:" +
            '\'' + GROUP_REGEX + ":ooxml-schemas:1\\.\\d+'|" +
            // [group: 'org.apache.poi', name: 'openxml4j', version: '1.0-beta'],
            "group: 'org.apache.poi', name: 'openxml4j'|" +
            // compile group: 'org.apache.poi', name: 'ooxml-schemas', version: '1.3'
            "group: 'org.apache.poi', name: 'ooxml-schemas'|" +
            // compile files('libs/org.apache.poi.xwpf.converter.xhtml-1.0.0.jar')
            // relocate 'javax.xml.namespace', 'org.apache.poi.javax.xml.namespace'
            GROUP_REGEX + "\\.(xwpf|javax)\\.|" +
            // exclude group: 'org.apache.poi', module: 'poi'
            "exclude group: '" + GROUP_REGEX + "', module: '[-a-z]+'|" +
            // me: 'com.springsource.jxl', version: '2.6.6',configuration: "compile", ext : "jar" compile group: 'org.apache.poi', name: 'com.springsource.org.apache.poi', version: '3.9.0.FINAL',configuration: "com
            "group: '" + GROUP_REGEX + "', name: 'com.springsource.org.apache.poi'|" +
            // exclude any commented line
            "^\\s*//.*" +
            ')').
            // adding \\s* and ["'] everywhere makes reading the strings above hard
            replace(" ", "\\s*").
            replace("'", "[\"']");

    @Override
    final String getExcludeRegex() {
        return EXCLUDE_REGEX;
    }

    @Override
    protected void search(GitHub github, Multimap<String, String> versions) throws IOException {
        // start search
        final PagedSearchIterable<GHContent> list = github.searchContent().
                filename("build.gradle").
                in("file").
                language("gradle").
                q(GROUP_REGEX).
                list();

        System.out.println("Had: " + list.getTotalCount() + " total results with " + getClass().getSimpleName());

        // paginate through results, filtering out interesting files
        processResults(github, versions, list);
    }

    @Override
    protected void parseVersion(Multimap<String, String> versions, String htmlUrl, String repo, String str) {
        Matcher matcher = PATTERN_SHORT.matcher(str);
        if(matcher.find()) {
            addVersion(versions, htmlUrl, str, matcher.group(1));
        } else {
            matcher = PATTERN_LONG.matcher(str);
            if (matcher.find()) {
                addVersion(versions, htmlUrl, str, matcher.group(1));
            } else {
                matcher = PATTERN_SHORT_VAR.matcher(str);
                if(matcher.find()) {
                    addVersion(versions, htmlUrl, str, matcher.group(1));
                } else if (str.contains("'org.apache.poi:poi'") ||
                        str.contains("\"org.apache.poi:poi\"")) {
                    versions.put("noVersion", htmlUrl);
                } else if (
                        // don't log for some obvious reasons for not finding a version
                        !str.contains("compile 'fr.opensagres.xdocreport:org.apache.poi.") &&
                        !str.contains("implementation 'fr.opensagres.xdocreport:org.apache.poi.") &&
                        !str.contains("main = 'org.apache.poi.benchmark") &&

                        // from POIAndroidTest and forked/copied projects
                        !str.contains("org.apache.poi.java.awt")) {
                    System.out.println("Did not find a version for repo " + repo + " in file at " + htmlUrl + " with content: \n" + reducedContent(str, htmlUrl) + '\n');
                }
            }
        }
    }

    protected static void addVersion(Multimap<String, String> versions, String htmlUrl, String str, String match) {
        String version = match;

        // try to resolve simple variables
        if(version.startsWith("$")) {
            Matcher matcher = Pattern.compile(
                    Pattern.quote(StringUtils.removeStart(StringUtils.removeEnd(
                                version.substring(1), "}"), "{"))
                            + VERSION_VAR_PATTERN).matcher(str);
            if(matcher.find()) {
                version = matcher.group(1);
            }
        }

        // def poiVersion='3.7'
        // compile 'org.apache.poi:poi:'+poiVersion
        if(str.contains("'+" + version)) {
            try {
                version = matchVersionVar(str, version);
            } catch (PatternSyntaxException e) {
                if(version.endsWith(")") && !version.contains("(")) {
                    try {
                        version = matchVersionVar(str, version.substring(0, version.length() - 1));
                    } catch (PatternSyntaxException e1) {
                        throw new IllegalStateException("For version " + version + " found at " + htmlUrl, e1);
                    }
                } else {
                    throw new IllegalStateException("For version " + version + " found at " + htmlUrl, e);
                }
            }
        }

        // remove a trailing "-FINAL" that was used sometimes to make the comparisons easier
        version = StringUtils.removeEnd(version, "-FINAL");
        // sanitize versions like [3.8-beta5,)
        version = StringUtils.removeStart(version, "[");

        //System.out.println("Found " + version + " for repo " + repo + " at " + htmlUrl);
        versions.put(version, htmlUrl);
    }

    private static String matchVersionVar(CharSequence str, String version) {
        Matcher matcher = Pattern.compile(
                "def\\s+" + version + VERSION_VAR_PATTERN).matcher(str);
        if(matcher.find()) {
            version = matcher.group(1);
        }
        return version;
    }
}
