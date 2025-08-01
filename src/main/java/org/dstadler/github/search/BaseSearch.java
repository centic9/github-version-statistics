package org.dstadler.github.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.RateLimitChecker;
import org.kohsuke.github.RateLimitTarget;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for different code-searchers
 */
public abstract class BaseSearch {
    // main definition of which library you are looking for
    protected final static String GROUP_REGEX = "org\\.apache\\.poi";
    protected final static String GROUP = "org.apache.poi";

    protected final static String VERSION = "([-0-9A-Za-z.$_{}()\\[\\]+]+)";

    protected final static String QUOTE = "[\"']?";

    // parse out the name of the repository from the URL returned by the GitHub search
    private final static Pattern REPO_NAME = Pattern.compile("https://github\\.com/([-a-zA-Z0-9_.]+/[-a-zA-Z0-9_.]+)/blob/.*");

    protected void processResults(GitHub github, Multimap<String, String> versions, Iterable<GHContent> list) throws IOException {
        // try up to three times to cater for some connection issues that
        // we see from time to time.

        // This is done around the whole loop as the iterator also
        // fetches new pages.
        // Note that getNonForkRepository() does it's own retry as well
        int retries = 3;
        while(true) {
            try {
                int i = 0;
                for(GHContent match : list) {
                    i++;

                    final String htmlUrl = match.getHtmlUrl();
                    String repo = getNonForkRepository(github, htmlUrl);
                    if (repo == null) {
                        continue;
                    }

                    String str = readFileContent(match, htmlUrl, repo);
                    if (str == null) {
                        continue;
                    }

                    try {
                        parseVersion(versions, htmlUrl, repo, str);
                    } catch (RuntimeException e) {
                        throw new IllegalStateException("Failed for " + htmlUrl + ", repo; " + repo + ", str: " + str, e);
                    }

                    if(i % 1000 == 0) {
                        System.out.println("Having " + i + " results");
                    }
                }

                System.out.println("Processed " + i + " results overall");

                //noinspection BreakStatement
                break;
            } catch (HttpException e) {
                retries--;

                if(retries <= 0) {
                    throw e;
                }

                // retry once more
                System.out.println("Retry " + retries + " after failing to talk to Github");
                e.printStackTrace(System.out);
            }
        }
    }

    protected String readFileContent(GHContent match, String htmlUrl, String repo) throws IOException {
        // This is a workaround for https://github.com/github-api/github-api/issues/729
        match.refresh();
        if(match.getEncoding() == null) {
            System.out.println("Could not read content of " + htmlUrl + " of repo " + repo + ", encoding is not set: " +
                    match.getHtmlUrl());
            return null;
        }

        final InputStream stream;
        try {
            stream = match.read();
        } catch (IOException e) {
            System.out.println("Could not read content of " + htmlUrl + " of repo " + repo + ": " + e);
            return null;
        }

        String str = IOUtils.toString(stream, StandardCharsets.UTF_8);

        // filter out some unwanted matches
        str = str.replaceAll(getExcludeRegex(), "");

        // skip this if the group-tag is not found any more now
        if(!str.contains(GROUP)) {
            //System.out.println("Did not find " + GROUP + " in content of repo " + repo + " at " + htmlUrl);
            return null;
        }
        return str;
    }

    abstract void search(GitHub github, Multimap<String, String> versions) throws IOException;

    abstract String getExcludeRegex();

    abstract void parseVersion(Multimap<String, String> versions, String htmlUrl, String repo, String str);

    protected String reducedContent(String str, String htmlUrl) {
        int pos = str.indexOf(GROUP);
        Preconditions.checkState(pos >= 0, "Did not find " + GROUP + " at " + htmlUrl);

        return str.substring(Math.max(0, pos - 100), Math.min(str.length(), pos + 100));
    }

    protected String getNonForkRepository(GitHub github, CharSequence htmlUrl) throws IOException {
        String repo = getRepository(htmlUrl);
        if(repo == null) {
            return null;
        }

        // try up to three times to cater for some connection issues that
        // we see from time to time
        int retries = 3;
        while(true) {
            try {
                final GHRepository repository = github.getRepository(repo);
                if (repository.isFork()) {
                    //System.out.println("Ignoring forked repo " + repo);
                    return null;
                }
                return repo;
            } catch (HttpException e) {
                retries--;

                if(retries <= 0) {
                    throw e;
                }

                // retry once more
                System.out.println("Retry " + retries + " after failing to talk to Github");
                e.printStackTrace(System.out);
            }
        }
    }

    public static String getRepository(CharSequence htmlUrl) {
        Matcher matcher = REPO_NAME.matcher(htmlUrl);
        if(!matcher.matches()) {
            System.out.println("Could not parse repo of " + htmlUrl + " with regex " + REPO_NAME.pattern());
            return null;
        }
        return matcher.group(1);
    }

    public static GitHub connect() throws IOException {
        GitHubBuilder builder = GitHubBuilder.fromEnvironment();

        // this can increase rate-limits considerably
        String token = System.getenv("GITHUB_TOKEN");
        if (StringUtils.isNotBlank(token)) {
            System.out.println("Using provided GITHUB_TOKEN");
            builder.withJwtToken(token);
        }

        return builder.
                // observe rate-limits and wait if we get near the returned remaining number of requests per timeframe
                withRateLimitChecker(new RateLimitChecker.LiteralValue(1), RateLimitTarget.SEARCH).
                build();
    }
}
