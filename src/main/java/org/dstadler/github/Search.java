package org.dstadler.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search {
    private final static String GROUP_REGEX = "org\\.apache\\.poi";
    private final static String GROUP = "org.apache.poi";

    private final static String VERSION = "([-0-9A-Za-z.$_{}()\\[\\]+]+)";

    private final static String QUOTE = "[\"']?";

    // compile 'org.apache.poi:poi:3.13'
    private final static Pattern PATTERN_SHORT = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + VERSION + QUOTE);

    private final static Pattern PATTERN_SHORT_VAR = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + QUOTE + "\\s*\\+\\s*" + VERSION);

    // compile group: 'org.apache.poi', name: 'poi', version: '3.15'
    private final static Pattern PATTERN_LONG = Pattern.compile("group\\s*:\\s*" + QUOTE + GROUP_REGEX + QUOTE + "\\s*,\\s*name\\s*:\\s*" + QUOTE + "[-a-z]+" + QUOTE + "\\s*,\\s*version\\s*:\\s*" + QUOTE + VERSION + QUOTE);

    private final static Pattern REPO_NAME = Pattern.compile("https://github\\.com/([-a-zA-Z0-9_.]+/[-a-zA-Z0-9_.]+)/blob/.*");

    public static void main(String[] args) throws IOException {
        GitHub github = connect();

        // start search
        final PagedSearchIterable<GHContent> list = github.searchContent().filename("build.gradle").in("file").language("gradle").q(GROUP_REGEX).list();
        System.out.println("Had: " + list.getTotalCount() + " total results");

        // paginate through results, filtering out interesting files
        Multimap<String,String> versions = ArrayListMultimap.create();
        for(GHContent match : list) {
            final String htmlUrl = match.getHtmlUrl();
            String repo = getRepository(github, htmlUrl);
            if (repo == null) {
                continue;
            }

            final InputStream stream;
            try {
                stream = match.read();
            } catch (IOException e) {
                System.out.println("Could not read content of " + htmlUrl + " of repo " + repo);
                continue;
            }

            String str = IOUtils.toString(stream, "UTF-8");

            // filter out some unwanted matches
            str = str.replaceAll("[\"']org\\.apache\\.poi:ooxml-schemas:1\\.\\d+['\"]", "");

            // skip this if the group-tag is not found any more now
            if(!str.contains(GROUP)) {
                System.out.println("Did not find " + GROUP + " in content of repo " + repo + " at " + htmlUrl);
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
                    } else {
                        System.out.println("Did not find for repo " + repo + " in content: \n" + reducedContent(str, htmlUrl) + "\n");
                    }
                }
            }
        }

        // combine results into the actual statistics, taking into account forks and how many stars a repo has
        System.out.println("Had " + versions.keySet().size() + " different versions for " + versions.size() + " projects");
        for(String version : versions.keySet()) {
            System.out.println("Had: " + version + " " + versions.get(version).size() + " times");
        }

        JSONWriter.write(new File("stats.json"), versions);
    }

    // poiVersion = '3.10-FINAL'
    private static final String VERSION_VAR_PATTERN = "\\s*=\\s*" + QUOTE + VERSION + QUOTE;

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

        System.out.println("Found " + version + " for repo " + repo + " at " + htmlUrl);
        versions.put(version, htmlUrl);
    }

    private static String reducedContent(String str, String htmlUrl) {
        int pos = str.indexOf(GROUP);
        Preconditions.checkState(pos >= 0, "Did not find " + GROUP + " at " + htmlUrl);

        return str.substring(Math.max(0, pos - 100), Math.min(str.length(), pos + 100));
    }

    private static String getRepository(GitHub github, String htmlUrl) throws IOException {
        Matcher matcher = REPO_NAME.matcher(htmlUrl);
        if(!matcher.matches()) {
            System.out.println("Could not parse repo of " + htmlUrl + " with regex " + REPO_NAME.pattern());
            return null;
        }
        String repo = matcher.group(1);
        final GHRepository repository = github.getRepository(repo);
        if(repository.isFork()) {
            System.out.println("Ignoring forked repo " + repo);
            return null;
        }
        return repo;
    }

    private static GitHub connect() throws IOException {
        return GitHub.connect();
    }
}
