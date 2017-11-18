package org.dstadler.github;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.dstadler.commons.net.UrlUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenPomSearch extends BaseSearch {
    /*
<dependency>
      <groupId>org.eclipse.jgit</groupId>
      <artifactId>org.eclipse.jgit</artifactId>
      <version>4.5.0.201609210915-r</version>
</dependency>
     */
    private final static String NEWLINE = "[\\n\\r\\s]*";
    private static final String GROUP_ID_PATTERN = "<groupId>" + GROUP_REGEX + "</groupId>";

    // compile 'org.apache.poi:poi:3.13'
    private final static Pattern PATTERN_DEPENDENCY = Pattern.compile(
            GROUP_ID_PATTERN + NEWLINE +
            "<artifactId>.*</artifactId>" + NEWLINE +
            "<version>" + VERSION + "</version>");
    private final static Pattern PATTERN_DEPENDENCY_2 = Pattern.compile(
            "<artifactId>.*</artifactId>" + NEWLINE +
            GROUP_ID_PATTERN + NEWLINE +
            "<version>" + VERSION + "</version>");
    protected final static Pattern PATTERN_NO_VERSION = Pattern.compile(
            "<dependency>" + NEWLINE +
            GROUP_ID_PATTERN + NEWLINE +
            "<artifactId>poi(?:-[a-z]+)?</artifactId>" + NEWLINE +
            "</dependency>");

    //private final static Pattern PATTERN_SHORT_VAR = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + QUOTE + "\\s*\\+\\s*" + VERSION);

    // exclude some pattern that caused false versions to be reported, we currently simple remove these from the found file before looking for the version
    private static final String EXCLUDE_REGEX = "(?:" +
                "<artifactId>ooxml-schemas</artifactId>" + NEWLINE + "<version>" + VERSION + "</version>|" +
                "<artifactId>" + GROUP_REGEX + "\\.xwpf\\.converter\\.[a-z]+</artifactId>|" +
                GROUP_ID_PATTERN + NEWLINE + "<artifactId>poi-parent</artifactId>" + NEWLINE + "<packaging>pom</packaging>|" +
                "<module\\.name>" + GROUP_REGEX + "</module\\.name>|" +
                '<' + GROUP_REGEX + "\\.util\\.POILogger>" + GROUP_REGEX + "\\.util\\.[a-zA-Z]+Logger</" + GROUP_REGEX + "\\.util\\.POILogger>|" +
                "<dependency>" + NEWLINE + GROUP_ID_PATTERN + NEWLINE + "<artifactId>poi(?:-[a-z]+)?</artifactId>" + NEWLINE + "<type>jar</type>" + NEWLINE + "</dependency>" +
            ')';
    private static final int TIMEOUT = 30_000;

    @Override
    String getExcludeRegex() {
        return EXCLUDE_REGEX;
    }

    @Override
    protected void search(GitHub github, Multimap<String, String> versions) throws IOException {
        // start search
        final PagedSearchIterable<GHContent> list = github.searchContent().filename("pom.xml").in("file").language("maven").q(GROUP_REGEX).list();
        System.out.println("Had: " + list.getTotalCount() + " total results");

        // paginate through results, filtering out interesting files
        processResults(github, versions, list);
    }

    @Override
    protected void parseVersion(Multimap<String, String> versions, String htmlUrl, String repo, String str) {
        Matcher matcher = PATTERN_DEPENDENCY.matcher(str);
        if(matcher.find()) {
            addVersion(versions, htmlUrl, str, matcher.group(1));
        } else {
            matcher = PATTERN_DEPENDENCY_2.matcher(str);
            if(matcher.find()) {
                addVersion(versions, htmlUrl, str, matcher.group(1));
            } else {
                matcher = PATTERN_NO_VERSION.matcher(str);
                if (matcher.find()) {
                    versions.put("noVersion", htmlUrl);
                    /*matcher = PATTERN_SHORT_VAR.matcher(str);
                    if(matcher.find()) {
                        addVersion(versions, htmlUrl, repo, str, matcher.group(1));*/
                } else if (!str.contains("'com.springsource.org.apache.poi'") &&
                        !str.contains("org.apache.poi.xwpf.converter")) {
                    System.out.println("Did not find a version for repo " + repo + " in file at " + htmlUrl + " with content: \n" + reducedContent(str, htmlUrl) + '\n');
                }
            }
        }
    }

    private static String getVariable(CharSequence str, String version) {
        final String var = StringUtils.removeStart(StringUtils.removeEnd(version.substring(1), "}"), "{");
        Matcher matcher = Pattern.compile(
                // <poi.version>3.14</poi.version>
                '<' + var + '>' + VERSION + "</" + var + '>').matcher(str);
        if(matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static String getVariableRecursive(CharSequence htmlUrl, CharSequence str, String version) {
        while (true) {
            // first try with the current content
            String newVersion = getVariable(str, version);
            if (newVersion != null) {
                return newVersion;
            }

            // if not found, try to retrieve the parent pom and continue from there
            // https://github.com/cesardl/code-examples/blob/456f7b2d282eb1b9fa56f4b90fdae34c72e6db5e/apache-poi/pom.xml
            // the regex selects the direct parent directory of the current pom.xml
            Matcher matcher = Pattern.compile("https://github\\.com/([-a-zA-Z0-9_.]+/[-a-zA-Z0-9_.]+/)blob/([a-f0-9]+/.*?)[^/]+/pom.xml").matcher(htmlUrl);
            if (matcher.find()) {
                // https://raw.githubusercontent.com/seeyoula/sbs/576bd28004562d235b1472504d3c5849790fc343/tools/com.sbs.tools/pom.xml
                final String url = "https://raw.githubusercontent.com/" + matcher.group(1) + matcher.group(2) + "pom.xml";
                try {
                    String parent = UrlUtils.retrieveData(url, TIMEOUT);

                    // recurse here to check the content and step up more directory levels if necessary
                    htmlUrl = url;
                    str = parent;
                    continue;
                } catch (IOException ignored) {
                    System.out.println("Could not find parent pom at " + url + " for " + htmlUrl);
                }
            }

            return null;
        }
    }

    protected static void addVersion(Multimap<String, String> versions, String htmlUrl, CharSequence str, String match) {
        String version = match;

        // try to resolve simple variables
        if(version.startsWith("$")) {
            String newVersion = getVariableRecursive(htmlUrl, str, version);
            if(newVersion != null) {
                version = newVersion;
            }
        }

        // remove a trailing "-FINAL" that was used sometimes to make the comparisons easier
        version = StringUtils.removeEnd(version, "-FINAL");
        // sanitize versions like [3.8-beta5,)
        version = StringUtils.removeStart(version, "[");

        //System.out.println("Found " + version + " for repo " + repo + " at " + htmlUrl);
        versions.put(version, htmlUrl);
    }
}
