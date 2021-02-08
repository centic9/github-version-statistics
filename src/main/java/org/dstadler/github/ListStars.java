package org.dstadler.github;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dstadler.github.search.BaseSearch;
import org.dstadler.github.util.JSONWriter;
import org.dstadler.github.util.Stats;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListStars {
    public static void main(String[] args) throws IOException {
        File[] files = Stats.getFiles();

        Set<String> repositories = readLines(files);
        System.out.println("Found " + repositories.size() + " repositories using Apache POI");

        Multimap<Integer, String> starsAndRepositories = createSortedMultimap();

        GitHub github = BaseSearch.connect();

        int count = 0;
        for (String repository : repositories) {
            try {
                GHRepository repo = github.getRepository(repository);
                starsAndRepositories.put(repo.getStargazersCount(), repository);

                count++;
                if (count % 10 == 0) {
                    System.out.println(count + ": " + repository + ": " + repo.getStargazersCount() + " - " +
                            StringUtils.abbreviate(starsAndRepositories.toString(), 1024));
                }
            } catch (IOException e) {
                starsAndRepositories.put(-1, repository);
                System.out.println(repository + ": " + e);
            }
        }

        System.out.println("Stars: " + starsAndRepositories + ": " +
                StringUtils.abbreviate(starsAndRepositories.toString(), 1024));
    }

    private static Multimap<Integer, String> createSortedMultimap() {
        Comparator<Integer> intSort = Comparator.naturalOrder();
        return TreeMultimap.create(intSort.reversed(), Comparator.naturalOrder());
    }

    private static Set<String> readLines(File[] files) throws IOException {
        Set<String> repositories = new HashSet<>();
        for(File file : files) {
            List<String> lines = FileUtils.readLines(file, "UTF-8");

            for (String line : lines) {
                JSONWriter.Holder holder = JSONWriter.mapper.readValue(line, JSONWriter.Holder.class);

                repositories.addAll(holder.getRepositoryVersions().values());
            }
        }

        return repositories;
    }
}
