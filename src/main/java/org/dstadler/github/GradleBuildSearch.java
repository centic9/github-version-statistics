package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.dstadler.github.JSONWriter.DATE_FORMAT;

public class GradleBuildSearch extends BaseSearch {
    // compile 'org.apache.poi:poi:3.13'
    private final static Pattern PATTERN_SHORT = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + VERSION + QUOTE);

    private final static Pattern PATTERN_SHORT_VAR = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + QUOTE + "\\s*\\+\\s*" + VERSION);

    // compile group: 'org.apache.poi', name: 'poi', version: '3.15'
    private final static Pattern PATTERN_LONG = Pattern.compile("group\\s*:\\s*" + QUOTE + GROUP_REGEX + QUOTE + "\\s*,\\s*name\\s*:\\s*" + QUOTE + "[-a-z]+" + QUOTE + "\\s*,\\s*version\\s*:\\s*" + QUOTE + VERSION + QUOTE);

    // poiVersion = '3.10-FINAL'
    private static final String VERSION_VAR_PATTERN = "\\s*=\\s*" + QUOTE + VERSION + QUOTE;

    // exclude some pattern that caused false versions to be reported, we currently simple remove these from the found file before looking for the version
    private static final String EXCLUDE_REGEX = "[\"']org\\.apache\\.poi:ooxml-schemas:1\\.\\d+['\"]";

    public static void main(String[] args) throws IOException {
        GitHub github = connect();

        SetMultimap<String,String> versions = HashMultimap.create();
        new GradleBuildSearch().search(github, versions);

        System.out.println("Had " + versions.keySet().size() + " different versions for " + versions.size() + " projects");
        for(String version : versions.keySet()) {
            System.out.println("Had: " + version + " " + versions.get(version).size() + " times");
        }

        JSONWriter.write(DATE_FORMAT.format(new Date()), versions);
    }

    @Override
    String getExcludeRegex() {
        return EXCLUDE_REGEX;
    }

    protected void search(GitHub github, Multimap<String, String> versions) throws IOException {
        // start search
        final PagedSearchIterable<GHContent> list = github.searchContent().filename("build.gradle").in("file").language("gradle").q(GROUP_REGEX).list();
        System.out.println("Had: " + list.getTotalCount() + " total results");

        // paginate through results, filtering out interesting files
        for(GHContent match : list) {
            final String htmlUrl = match.getHtmlUrl();
            String repo = getNonForkRepository(github, htmlUrl);
            if (repo == null) {
                continue;
            }

            String str = readFileContent(match, htmlUrl, repo);
            if (str == null) {
                continue;
            }

            Matcher matcher = PATTERN_SHORT.matcher(str);
            if(matcher.find()) {
                addVersion(versions, htmlUrl, repo, str, matcher.group(1));
            } else {
                matcher = PATTERN_LONG.matcher(str);
                if (matcher.find()) {
                    addVersion(versions, htmlUrl, repo, str, matcher.group(1));
                } else {
                    matcher = PATTERN_SHORT_VAR.matcher(str);
                    if(matcher.find()) {
                        addVersion(versions, htmlUrl, repo, str, matcher.group(1));
                    } else if (
                            // don't log for some obvious reasons for not finding a version
                            !str.contains("compile 'org.apache.poi:poi'") &&
                            !str.contains("compile 'fr.opensagres.xdocreport:org.apache.poi.") &&
                            !str.contains("main = 'org.apache.poi.benchmark")) {
                        System.out.println("Did not find a version for repo " + repo + " in content: \n" + reducedContent(str, htmlUrl) + "\n");
                    }
                }
            }
        }
    }

    private static void addVersion(Multimap<String, String> versions, String htmlUrl, String repo, String str, String match) {
        String version = match;

        // try to resolve simple variables
        if(version.startsWith("$")) {
            Matcher matcher = Pattern.compile(
                    StringUtils.removeStart(StringUtils.removeEnd(version.substring(1), "}"), "{")
                            + VERSION_VAR_PATTERN).matcher(str);
            if(matcher.find()) {
                version = matcher.group(1);
            }
        }

        // def poiVersion='3.7'
        // compile 'org.apache.poi:poi:'+poiVersion
        if(str.contains("'+" + version)) {
            Matcher matcher = Pattern.compile(
                    "def\\s+" + version + VERSION_VAR_PATTERN).matcher(str);
            if(matcher.find()) {
                version = matcher.group(1);
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
