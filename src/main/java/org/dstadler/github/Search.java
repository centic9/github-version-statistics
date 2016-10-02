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
            if(!str.contains(GROUP)) {
                System.out.println("Did not find " + GROUP + " in content of repo " + repo + " at " + htmlUrl);
                continue;
            }

            Matcher matcher = PATTERN_SHORT.matcher(str);
            if(matcher.find()) {
               System.out.println("Found " + matcher.group(1) + " for repo " + repo + " at " + htmlUrl);
                addVersion(versions, htmlUrl, matcher);
            } else {
                matcher = PATTERN_LONG.matcher(str);
                if (matcher.find()) {
                    System.out.println("Found " + matcher.group(1) + " for repo " + repo + " at " + htmlUrl);
                    addVersion(versions, htmlUrl, matcher);
                } else {
                    matcher = PATTERN_SHORT_VAR.matcher(str);
                    if(matcher.find()) {
                        System.out.println("Found " + matcher.group(1) + " for repo " + repo + " at " + htmlUrl);
                        addVersion(versions, htmlUrl, matcher);
                    } else {
                        System.out.println("Did not find for repo " + repo + " in content: \n" + reducedContent(str) + "\n");
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

    private static void addVersion(Multimap<String, String> versions, String htmlUrl, Matcher matcher) {
        versions.put(StringUtils.remove(matcher.group(1), "-FINAL"), htmlUrl);
    }

    private static String reducedContent(String str) {
        int pos = str.indexOf(GROUP);
        Preconditions.checkState(pos >= 0);

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
