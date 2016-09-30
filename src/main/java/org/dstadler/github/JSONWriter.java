package org.dstadler.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.*;
import java.util.Date;

public class JSONWriter {
    protected static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new GuavaModule())
            ;

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");

    public static void write(File file, Multimap<String, String> versions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            Holder holder = new Holder(DATE_FORMAT.format(new Date()), versions);

            StringWriter strWriter = new StringWriter();
            try {
                mapper.writeValue(strWriter, holder);
            } finally {
                strWriter.close();
            }

            writer.write(strWriter.toString() + "\n");
        }
    }

    @SuppressWarnings("unused")
    protected static class Holder {
        private String date;
        private Multimap<String, String> versions;

        public Holder() {
        }

        public Holder(String date, Multimap<String, String> versions) {
            this.date = date;
            this.versions = versions;
        }

        public String getDate() {
            return date;
        }

        public Multimap<String, String> getVersions() {
            return versions;
        }
    }
}
