package org.dstadler.github;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.*;
import java.util.Map;

public class JSONWriter {
    protected static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new GuavaModule())
            ;

    public static File STATS_DIR = new File("stats");

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    private static VersionComparator COMPARATOR = new VersionComparator();

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
}
