package org.dstadler.github;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.*;
import java.util.*;

public class JSONWriter {
    protected static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new GuavaModule())
            ;

    public static File STATS_DIR = new File("stats");

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    private static final VersionComparator COMPARATOR = new VersionComparator();

    public static void write(String date, SetMultimap<String, String> versions) throws IOException {
        File file = new File(STATS_DIR, "stats" + date + ".json");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            Holder holder = new Holder(date, versions);

            StringWriter strWriter = new StringWriter();
            try {
                mapper.writeValue(strWriter, holder);
            } finally {
                strWriter.close();
            }

            // Need to write in one go as mapper.writeValue() closes the stream...
            writer.write(strWriter.toString() + "\n");
        }
    }

    @SuppressWarnings("unused")
    protected static class Holder {
        private String date;

        /**
         * The Multimap contains the version as key and the list of
         * found files on GitHub
         *
         * We use a SetMultimap on purpose here to not write duplicates
         */
        private SetMultimap<String, String> versions;

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
            for (Map.Entry<String, String> entry : versions.entries()) {
                repoVersions.put(BaseSearch.getRepository(entry.getValue()), entry.getKey());
            }

            return Multimaps.filterEntries(versions, input -> {
                //noinspection ConstantConditions
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
            for (Map.Entry<String, String> entry : getVersions().entries()) {
                repositories.put(entry.getKey(), BaseSearch.getRepository(entry.getValue()));
            }
            return repositories;
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
        File[] files = JSONWriter.STATS_DIR.listFiles((FilenameFilter)new WildcardFileFilter("stats*.json"));
        Preconditions.checkNotNull(files);

        Arrays.sort(files);

        Map<String, String> seenRepositoryVersions = new HashMap<>();
        for(File file : files) {
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            for (String line : lines) {
                JSONWriter.Holder holder = JSONWriter.mapper.readValue(line, JSONWriter.Holder.class);
                // now update the map of highest version per Repository for the next date
                addHigherVersions(seenRepositoryVersions, holder.getRepositoryVersions());
            }
        }

        return seenRepositoryVersions;
    }

    protected static void addHigherVersions(Map<String, String> seenRepositoryVersions, SetMultimap<String, String> repositoryVersions) {
        for (Map.Entry<String, String> entry : repositoryVersions.entries()) {
            String version = seenRepositoryVersions.get(entry.getValue());
            if(version == null || COMPARATOR.compare(version, entry.getKey()) < 0) {
                seenRepositoryVersions.put(entry.getValue(), entry.getKey());
            }
        }
    }
}
