package org.dstadler.github;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;

/**
 * Combined application which calls the various code-searches
 */
public class Search {
    public static void main(String[] args) throws IOException {
        GitHub github = BaseSearch.connect();

        Multimap<String,String> versions = ArrayListMultimap.create();
        new GradleBuildSearch().search(github, versions);
        new MavenPomSearch().search(github, versions);

        System.out.println("Had " + versions.keySet().size() + " different versions for " + versions.size() + " projects");
        for(String version : versions.keySet()) {
            System.out.println("Had: " + version + " " + versions.get(version).size() + " times");
        }

        JSONWriter.write(new File("stats.json"), versions);
    }
}
