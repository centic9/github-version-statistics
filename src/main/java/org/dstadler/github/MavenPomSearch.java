package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.StringUtils;
import org.dstadler.commons.net.UrlUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.dstadler.github.JSONWriter.DATE_FORMAT;

public class MavenPomSearch extends BaseSearch {
    /*
<dependency>
      <groupId>org.eclipse.jgit</groupId>
      <artifactId>org.eclipse.jgit</artifactId>
      <version>4.5.0.201609210915-r</version>
</dependency>
     */
    private final static String NEWLINE = "[\\n\\r\\s]*";

    // compile 'org.apache.poi:poi:3.13'
    private final static Pattern PATTERN_DEPENDENCY = Pattern.compile("<groupId>" + GROUP_REGEX + "</groupId>" + NEWLINE + "<artifactId>.*</artifactId>" + NEWLINE + "<version>" + VERSION + "</version>");

    //private final static Pattern PATTERN_SHORT_VAR = Pattern.compile(QUOTE + GROUP_REGEX + ":[-a-z]+:" + QUOTE + "\\s*\\+\\s*" + VERSION);

    // exclude some pattern that caused false versions to be reported, we currently simple remove these from the found file before looking for the version
    private static final String EXCLUDE_REGEX = "(<artifactId>ooxml-schemas</artifactId>" + NEWLINE + "<version>" + VERSION + "</version>|<artifactId>org\\.apache\\.poi\\.xwpf\\.converter\\.[a-z]+</artifactId>)";

    public static void main(String[] args) throws IOException {
        GitHub github = connect();

        SetMultimap<String,String> versions = HashMultimap.create();
        new MavenPomSearch().search(github, versions);

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
        final PagedSearchIterable<GHContent> list = github.searchContent().filename("pom.xml").in("file").language("maven").q(GROUP_REGEX).list();
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

            Matcher matcher = PATTERN_DEPENDENCY.matcher(str);
            if(matcher.find()) {
                addVersion(versions, htmlUrl, repo, str, matcher.group(1));
            } else {
                /*matcher = PATTERN_SHORT_VAR.matcher(str);
                if(matcher.find()) {
                    addVersion(versions, htmlUrl, repo, str, matcher.group(1));
                } else*/ {
                    System.out.println("Did not find for repo " + repo + " in content: \n" + reducedContent(str, htmlUrl) + "\n");
                }
            }
        }
    }

    private static String getVariable(String str, String version) {
        final String var = StringUtils.removeStart(StringUtils.removeEnd(version.substring(1), "}"), "{");
        Matcher matcher = Pattern.compile(
                // <poi.version>3.14</poi.version>
                "<" + var + ">" + VERSION + "</" + var + ">").matcher(str);
        if(matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static String getVariableRecursive(String htmlUrl, String str, String version) {
        // first try with the current content
        String newVersion = getVariable(str, version);
        if(newVersion != null) {
            return newVersion;
        }

        // if not found, try to retrieve the parent pom and continue from there
        // https://github.com/cesardl/code-examples/blob/456f7b2d282eb1b9fa56f4b90fdae34c72e6db5e/apache-poi/pom.xml
        // the regex selects the direct parent directory of the current pom.xml
        Matcher matcher = Pattern.compile("https://github\\.com/([-a-zA-Z0-9_.]+/[-a-zA-Z0-9_.]+/)blob/([a-f0-9]+/.*?)[^/]+/pom.xml").matcher(htmlUrl);
        if(matcher.find()) {
            // https://raw.githubusercontent.com/seeyoula/sbs/576bd28004562d235b1472504d3c5849790fc343/tools/com.sbs.tools/pom.xml
            final String url = "https://raw.githubusercontent.com/" + matcher.group(1) + matcher.group(2) + "pom.xml";
            try {
                String parent = UrlUtils.retrieveData(url, 30_000);

                // recurse here to check the content and step up more directory levels if necessary
                return getVariableRecursive(url, parent, version);
            } catch (IOException e) {
                System.out.println("Could not find parent pom at " + url + " for " + htmlUrl);
            }
        }

        return null;
    }

    private static void addVersion(Multimap<String, String> versions, String htmlUrl, String repo, String str, String match) {
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

        System.out.println("Found " + version + " for repo " + repo + " at " + htmlUrl);
        versions.put(version, htmlUrl);
    }
}
