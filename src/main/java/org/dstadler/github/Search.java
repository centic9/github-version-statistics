package org.dstadler.github;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Date;

import static org.dstadler.github.JSONWriter.DATE_FORMAT;

/**
 * Combined application which calls the various code-searches
 */
public class Search {
    public static void main(String[] args) throws IOException {
        GitHub github = BaseSearch.connect();

        // use a SetMultimap here to not record duplicates
        SetMultimap<String,String> versions = HashMultimap.create();
        new GradleBuildSearch().search(github, versions);
        new MavenPomSearch().search(github, versions);

        System.out.println("Had " + versions.keySet().size() + " different versions for " + versions.size() + " projects");
        for(String version : versions.keySet()) {
            System.out.println("Had: " + version + ' ' + versions.get(version).size() + " times");
        }

        JSONWriter.write(DATE_FORMAT.format(new Date()), versions);
    }
}
