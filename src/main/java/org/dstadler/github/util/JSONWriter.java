package org.dstadler.github.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.time.FastDateFormat;
import org.dstadler.github.search.BaseSearch;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class JSONWriter {
    public static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new GuavaModule())
            ;

    public static File STATS_DIR = new File("stats");

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    private static final Comparator<String> COMPARATOR = new VersionComparator();

    public static void write(String date, SetMultimap<String, String> versions) throws IOException {
        File file = new File(STATS_DIR, "stats" + date + ".json");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            Holder holder = new Holder(date, versions);

            StringWriter strWriter = new StringWriter();
            try (strWriter) {
                mapper.writeValue(strWriter, holder);
            }

            // Need to write in one go as mapper.writeValue() closes the stream...
            writer.write(strWriter.toString() + '\n');
        }
    }

    public static class Holder {
        private String date;

        /**
         * The Multimap contains the version as key and the list of
         * found files on GitHub
         *
         * We use a SetMultimap on purpose here to not write duplicates
         */
        private SetMultimap<String, String> versions;

        @SuppressWarnings("unused")
        public Holder() {
        }

        public Holder(String date, SetMultimap<String, String> versions) {
            this.date = date;
            this.versions = versions;
        }

        public String getDate() {
            return date;
        }

        /**
         * @return A multimap with version as key and found file-URLs as value.
         */
        public SetMultimap<String, String> getVersions() {
            final Multimap<String, String> repoVersions = HashMultimap.create();
            for (Entry<String, String> entry : versions.entries()) {
                repoVersions.put(BaseSearch.getRepository(entry.getValue()), entry.getKey());
            }

            return Multimaps.filterEntries(versions, input -> {
                for(String version1 : repoVersions.get(BaseSearch.getRepository(input.getValue()))) {
                    if(COMPARATOR.compare(version1, input.getKey()) > 0) {
                        return false;
                    }
                }

                return true;
            });
        }

        /**
         *
         * @return A Multimap with version as key and the repositories that use
         *          this version.
         */
        @JsonIgnore
        public SetMultimap<String, String> getRepositoryVersions() {
            SetMultimap<String, String> repositories = HashMultimap.create();
            for (Entry<String, String> entry : getVersions().entries()) {
                repositories.put(entry.getKey(), BaseSearch.getRepository(entry.getValue()));
            }
            return repositories;
        }

        /**
         * Read the contents of the stats-file and add it to the map of GitHub-project name
         * and version
         *
         * @param projects The map to populate with the projects from the given stats file
         * @param file The JSON-file to read.
         * @throws IOException If reading the file fails.
         */
        public static void readFile(Map<String, String> projects, File file) throws IOException {
            List<String> lines = FileUtils.readLines(file, "UTF-8");

            for (String line : lines) {
                Holder holder = mapper.readValue(line, Holder.class);

                for (Map.Entry<String, String> entry : holder.getRepositoryVersions().entries()) {
                    projects.put(entry.getValue(), entry.getKey());
                }
            }
        }
    }

    /**
     * Returns all repositories found until now with the highest version that we
     * found for them.
     *
     * @return repo as key, highest seen version as value
     * @throws IOException If a file cannot be read
     */
    public static Map<String, String> getHighestVersions() throws IOException {
        // read stats
        File[] files = STATS_DIR.listFiles((FilenameFilter)new WildcardFileFilter("stats*.json"));
        Preconditions.checkNotNull(files);

        Arrays.sort(files);

        Map<String, String> seenRepositoryVersions = new HashMap<>();
        for(File file : files) {
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            for (String line : lines) {
                Holder holder = mapper.readValue(line, Holder.class);
                // now update the map of highest version per Repository for the next date
                addHigherVersions(seenRepositoryVersions, holder.getRepositoryVersions());
            }
        }

        return seenRepositoryVersions;
    }

    /**
     * Populate the given map with the highest version ever found for a repository.
     *
     * @param seenRepositoryVersions A set of repositories and their versions as values
     * @param repositoryVersions A multimap with version as key and the matching repositories as value
     */
    public static void addHigherVersions(Map<String, String> seenRepositoryVersions, Multimap<String, String> repositoryVersions) {
        // entry is <version, repository>
        for (Entry<String, String> entry : repositoryVersions.entries()) {
            String version = seenRepositoryVersions.get(entry.getValue());
            if(version == null || COMPARATOR.compare(version, entry.getKey()) < 0) {
                seenRepositoryVersions.put(entry.getValue(), entry.getKey());
            }
        }
    }
}
