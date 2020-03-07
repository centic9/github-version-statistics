package org.dstadler.github.search;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.dstadler.github.util.JSONWriter;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Date;

import static org.dstadler.github.util.JSONWriter.DATE_FORMAT;

/**
 * Combined application which calls the various code-searches
 */
public class Search {
    public static void main(String[] args) throws IOException {
        runSearch(
                // search for build.gradle files
                new GradleBuildSearch(),

                // search for pom.xml files
                new MavenPomSearch()
        );
    }

    protected static void runSearch(BaseSearch... searches) throws IOException {
        GitHub github = BaseSearch.connect();

        // use a SetMultimap here to not record duplicates
        SetMultimap<String,String> versions = HashMultimap.create();
        for (BaseSearch search : searches) {
            System.out.println("Searching with: " + search.getClass().getSimpleName());
            search.search(github, versions);
        }

        System.out.println("Had " + versions.keySet().size() + " different versions for " + versions.size() + " projects");
        for(String version : versions.keySet()) {
            System.out.println("Had: " + version + ' ' + versions.get(version).size() + " times");
        }

        JSONWriter.write(DATE_FORMAT.format(new Date()), versions);
    }
}
