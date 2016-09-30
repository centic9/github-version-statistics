package org.dstadler.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.Multimap;

import java.io.*;
import java.util.Date;

public class CSVWriter {
    protected static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new GuavaModule())
            ;
    public static void write(File file, Multimap<String, String> versions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            Holder holder = new Holder(new Date(), versions);

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
        private Date date;
        private Multimap<String, String> versions;

        public Holder() {
        }

        public Holder(Date date, Multimap<String, String> versions) {
            this.date = date;
            this.versions = versions;
        }

        public Date getDate() {
            return date;
        }

        public Multimap<String, String> getVersions() {
            return versions;
        }
    }
}
