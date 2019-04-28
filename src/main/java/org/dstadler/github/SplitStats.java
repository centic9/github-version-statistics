package org.dstadler.github;

import com.google.common.collect.SetMultimap;
import org.apache.commons.io.FileUtils;
import org.dstadler.github.util.JSONWriter;
import org.dstadler.github.util.JSONWriter.Holder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tool used to split existing stats by date. This was used
 * when migrating from one large file to one-file-per-day.
 */
public class SplitStats {
    public static void main(String[] args) throws IOException {
        List<String> lines = FileUtils.readLines(new File("stats.json"), "UTF-8");

        // collect all to combine
        Map<String, SetMultimap<String,String>> byDate = new HashMap<>();
        for(String line : lines) {
            Holder holder = JSONWriter.mapper.readValue(line, Holder.class);
            SetMultimap<String, String> existing = byDate.get(holder.getDate());
            if(existing != null) {
                existing.putAll(holder.getVersions());
            } else {
                byDate.put(holder.getDate(), holder.getVersions());
            }
        }

        for (Entry<String, SetMultimap<String, String>> entry : byDate.entrySet()) {
            JSONWriter.write(entry.getKey(), entry.getValue());
        }
    }
}
