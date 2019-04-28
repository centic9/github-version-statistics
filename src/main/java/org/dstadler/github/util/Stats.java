package org.dstadler.github.util;

import com.google.common.base.Preconditions;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

public class Stats {
    public static File[] getFiles() {
        // read stats
        File[] files = JSONWriter.STATS_DIR.listFiles((FilenameFilter)new WildcardFileFilter("stats*.json"));
        Preconditions.checkNotNull(files);

        Arrays.sort(files);

        System.out.println("Found " + files.length + " stats-files");
        return files;
    }
}
