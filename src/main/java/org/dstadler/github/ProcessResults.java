package org.dstadler.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.time.DateUtils;
import org.dstadler.github.JSONWriter.Holder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static org.dstadler.github.JSONWriter.DATE_FORMAT;

public class ProcessResults {
    private static final Date START_DATE;
    private static final Comparator<String> VERSION_COMPARATOR = new VersionComparator();

    static {
        try {
            START_DATE = DATE_FORMAT.parse("2016-09-30");
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse date", e);
        }
    }

    // parse 3.10, 3.10-FINAL, 3.10-beta1
    private final static Pattern VERSION_PATTERN = Pattern.compile("[0-9][0-9.]+[-A-Za-z0-9]*");

    private static final String TEMPLATE =
        "<html>\n" +
        "<head>\n" +
                "    <!-- downloaded from https://cdnjs.cloudflare.com/ajax/libs/dygraph/2.0.0/dygraph.min.css -->\n" +
                "    <link rel=\"stylesheet\" href=\"dygraph.min.css\">\n" +
                '\n' +
                "    <!-- taken from https://cdnjs.cloudflare.com/ajax/libs/dygraph/1.1.1/dygraph-combined.js -->\n" +
                "    <script src=\"dygraph.min.js\"></script>\n" +
                "    <style>#graphdiv { position: absolute; left: 10px; right: 10px; top: 10px; bottom: 10px; }</style>\n" +
        "</head>\n" +
        "<body>" +
                "<div id=\"graphdiv\"></div>\n" +
                /* need to add Jekyll-tags to make this work
                "<div class=\"docs-header-bottom\">\n" +
                "    {% include footer.html %}\n" +
                "</div>\n" +*/
                '\n' +
                "<script type=\"text/javascript\">\n" +
                "  g = new Dygraph(\n" +
                '\n' +
                "    // containing div\n" +
                "    document.getElementById(\"graphdiv\"),\n" +
                '\n' +
                "    // CSV or path to a CSV file.\n" +
                "    \"${dataheader}\\n\" +\n" +
                "   ${data},\n" +
                "    {\n" +

                // http://dygraphs.com/tutorial.html
                // http://dygraphs.com/options.html

                "       title: 'Results for \\'${benchmark}\\'',\n" +

                // Step-Chart
//                "       stepPlot: true,\n" +
                "       fillGraph: true,\n" +
                "       stackedGraph: true,\n" +

                // Dot-Chart
                "       drawPoints: true,\n" +
                //"       strokeWidth: 0.0,\n" +

                "       includeZero: true,\n" +
                "       xRangePad: 40,\n" +

                "       labelsSeparateLines: true,\n" +
                "       legend: 'follow',\n" +

                "   axes: {\n" +
                "       y: {\n" +
                "                valueFormatter: function(y) {\n" +
                "                  return y;\n" +
                "                },\n" +
                "                axisLabelFormatter: function(y) {\n" +
                "                  return y;\n" +
                "                },\n" +
                "                axisLabelWidth: 50\n" +
                //"             labelWidth: 100,\n" +
                "              }\n" +
                "   },\n" +

                //"      rollPeriod: 7,\n" +
                //"      showRoller: true,\n" +
                //"connectSeparatedPoints: true,\n" +
                //"       drawPoints: true\n" +

                // taken from http://www.mulinblog.com/a-color-palette-optimized-for-data-visualization/
                "colors: ['#4D4D4D', '#5DA5DA', '#FAA43A', '#60BD68', '#F17CB0', '#B2912F', '#B276B2', '#DECF3F', '#F15854'],\n" +

                "    }\n" +
                '\n' +
                "  );\n" +
                '\n' +
                "  g.ready(function() {\n" +
                "    g.setAnnotations([\n" +
                "    ${annotations}\n" +
                "    ]);\n" +
                "  });\n" +
                "</script>\n" +
                "${footer}\n" +
        "</body>\n" +
        "</html>\n";


    public static void main(String[] args) throws IOException, ParseException {
        // read stats
        File[] files = JSONWriter.STATS_DIR.listFiles((FilenameFilter)new WildcardFileFilter("stats*.json"));
        Preconditions.checkNotNull(files);

        Arrays.sort(files);

        Table<String,String,Data> values = HashBasedTable.create();
        Table<String,String,Data> valuesAccumulative = HashBasedTable.create();
        List<VersionChange> changes = new ArrayList<>();

        // repo as key, highest seen version as value
        Map<String, String> seenRepositoryVersions = new HashMap<>();

        String maxDateStr = readLines(files, values, valuesAccumulative, changes, seenRepositoryVersions);

        File results = new File("docs", "results.html");
        File resultsPercentage = new File("docs", "resultsPercentage.html");
        generateHtmlFiles(values, maxDateStr, results, resultsPercentage);

        File resultsAll = new File("docs", "resultsAll.html");
        File resultsAllPercentage = new File("docs", "resultsAllPercentage.html");
        generateHtmlFiles(valuesAccumulative, maxDateStr, resultsAll, resultsAllPercentage);

        File current = new File("docs", "resultsCurrent.csv");
        writeCurrentResults(current, values.row(maxDateStr));

        File all = new File("docs", "resultsAll.csv");
        writeAllResults(all, values.row(maxDateStr), seenRepositoryVersions, readRepositories(files[files.length-1]));

        File changesFile = new File("docs/_data", "versionChanges.csv");
        writeVersionChanges(changesFile, changes);

        System.out.println("Wrote results to " + results + ", " + current + " and " + all);
    }

    private static class Data {
        public Integer count;
        public String link;

        public Data(Integer count, String link) {
            this.count = count;
            this.link = link;
        }
    }

    private static Collection<String> readRepositories(File file) throws IOException {
        List<String> lines = FileUtils.readLines(file, "UTF-8");

        Collection<String> map = new HashSet<>();
        for (String line : lines) {
            Holder holder = JSONWriter.mapper.readValue(line, Holder.class);
            SetMultimap<String, String> repositoryVersions = holder.getRepositoryVersions();
            for (Entry<String, String> entry : repositoryVersions.entries()) {
                map.add(entry.getValue());
            }
        }

        return map;
    }

    private static String readLines(File[] files, Table<String, String, Data> dateVersionTable,
                                    Table<String, String, Data> valuesAccumulative, Collection<VersionChange> changes,
                                    Map<String, String> seenRepositoryVersions) throws IOException {
        String maxDateStr = null;

        for(File file : files) {
             List<String> lines = FileUtils.readLines(file, "UTF-8");

            for (String line : lines) {
                Holder holder = JSONWriter.mapper.readValue(line, Holder.class);
                SetMultimap<String, String> versions = holder.getVersions();
                String date = holder.getDate();

                maxDateStr = populateTable(dateVersionTable, maxDateStr, versions, date);

                // print out if we found projects that switched versions
                compareToPrevious(date, holder.getRepositoryVersions(), changes, seenRepositoryVersions);

                // now update the map of highest version per Repository for the next date
                JSONWriter.addHigherVersions(seenRepositoryVersions, holder.getRepositoryVersions());

                // add the current values to the combined table
                for(Entry<String,String> entry : seenRepositoryVersions.entrySet()) {
                    // combine the unparsable versions into "other"
                    final String version = getPrintableVersion(entry.getValue());
                    Data data = valuesAccumulative.get(date, version);
                    if(data == null) {
                        valuesAccumulative.put(date, version, new Data(1, entry.getKey()));
                    } else {
                        data.count++;
                    }
                }
            }

        }

        Preconditions.checkNotNull(maxDateStr, "Should have a max date now!");

        return maxDateStr;
    }

    public static class VersionChange {
        public final String date;
        public final String repository;
        public final String versionBefore;
        public final String versionNow;

        public VersionChange(String date, String repository, String versionBefore, String versionNow) {
            this.date = date;
            this.repository = repository;
            this.versionBefore = versionBefore;
            this.versionNow = versionNow;
        }

        public void writeCSV(StringBuilder sb) {
            sb.append(date).append(',').append(repository).append(',').append(versionBefore).append(',').append(versionNow).append('\n');
        }
    }

    protected static void compareToPrevious(String date, Multimap<String, String> versions,
                                            Collection<VersionChange> changes, Map<String, String> seenRepositoryVersions) {
        for(Entry<String,String> entry : versions.entries()) {
            String version = entry.getKey();
            String repository = entry.getValue();
            String prevRepoVersion = seenRepositoryVersions.get(repository);

            if(prevRepoVersion == null || VERSION_COMPARATOR.compare(prevRepoVersion, version) < 0) {
                final String versionBefore;
                if(prevRepoVersion == null) {
                    versionBefore = "<new>";
                    /*System.out.println("Did find a new repository for " + repository +
                            ", now at " + version);*/
                } else {
                    versionBefore = prevRepoVersion;
                    System.out.println("Did find a different version for " + repository +
                            ", previously at " + versionBefore +
                            ", now at " + version);
                }

                changes.add(new VersionChange(date, repository, versionBefore, version));
            }
        }
    }

    private static String populateTable(Table<String, String, Data> dateVersionTable, String maxDateStr, Multimap<String, String> versions, String date) {
        System.out.println("Had " + versions.size() + " entries for " + date);
        for(String version : versions.keySet()) {
            // combine all the non-version things like build-script variables, ...
            String versionKey = getPrintableVersion(version);

            // add the count in the table, there can be multiple lines with the same date!
            Data value = dateVersionTable.get(date, versionKey);
            if(value == null) {
                value = new Data(versions.get(version).size(), versions.get(version).iterator().next());
            } else {
                value.count += versions.get(version).size();
            }
            dateVersionTable.put(date, versionKey, value);

            if(maxDateStr == null || maxDateStr.compareTo(date) <= 0) {
                maxDateStr = date;
            }
        }
        return maxDateStr;
    }

    private static String getPrintableVersion(String version) {
        String versionKey = version;
        // put everything that we cannot parse and some versions where we historically used the ooxml-schema version into "other"
        if(!VERSION_PATTERN.matcher(version).matches() || "1.0".equals(version) || "1.1".equals(version) || "1.3".equals(version)) {
            versionKey = "other";
        }
        return versionKey;
    }

    private static void generateHtmlFiles(Table<String, String, Data> dateVersionTable, String maxDateStr, File results, File percentageResults) throws ParseException, IOException {
        // use a tree-set to get the versions in correct order
        Collection<String> versionsSorted = new TreeSet<>(Collections.reverseOrder(new VersionComparator()));
        versionsSorted.addAll(dateVersionTable.columnKeySet());

        Date date = START_DATE;
        StringBuilder data = new StringBuilder();
        StringBuilder dataPercentage = new StringBuilder();
        while(date.compareTo(DATE_FORMAT.parse(maxDateStr)) <= 0) {
            String dateStr = DATE_FORMAT.format(date);
            Map<String, Data> row = dateVersionTable.row(dateStr);

            int count = 0;
            for(String column : versionsSorted) {
                count += row.get(column) == null ? 0 : row.get(column).count;
            }

            // Format: "    \"2008-05-07,75\\n\" +\n" +
            data.append('"').append(dateStr);
            dataPercentage.append('"').append(dateStr);
            for(String column : versionsSorted) {
                final Data value = row.get(column);
                data.append(',').append(formatValue(value == null ? null : value.count));
                dataPercentage.append(',').append(formatValue(value == null ? null : (double)(value.count)/count*100));
            }

            data.append("\\n\" + \n");
            dataPercentage.append("\\n\" + \n");

            date = DateUtils.addDays(date, 1);
        }

        // remove last trailing "+"
        data.setLength(data.length() - 3);
        dataPercentage.setLength(dataPercentage.length() - 3);

        writeResultsToTemplate(dateVersionTable, maxDateStr, results, versionsSorted, data);
        writeResultsToTemplate(dateVersionTable, maxDateStr, percentageResults, versionsSorted, dataPercentage);
    }

    private static void writeResultsToTemplate(Table<String, String, Data> dateVersionTable, String maxDateStr,
                                               File results, Iterable<String> versionsSorted, CharSequence data) throws IOException {
        String html = TEMPLATE.replace("${data}", data);
        html = html.replace("${dataheader}", "Date" + getHeaderData(versionsSorted));
        html = html.replace("${benchmark}", "Apache POI");

        StringBuilder annotations = new StringBuilder();
        for(String column : versionsSorted) {
            Data currentVersions = dateVersionTable.row(maxDateStr).get(column);
            annotations.append("    ").
                    append("{series: \"").append(column).append("\",x: \"").append(maxDateStr).
                    append("\",shortText: \"").append(column).
                    append("\",text: \"On ").append(maxDateStr).append(": ").append(currentVersions == null ? "0" : currentVersions.count).
                    append("\",width: ").append(column.length() * 10).append("},\n");
        }

        // cut away trailing comma and newline
        annotations.setLength(annotations.length() - 2);

        html = html.replace("${annotations}", annotations);
        html = addFooter(html);

        FileUtils.writeStringToFile(results, html, "UTF-8");
    }

    private static String addFooter(String html) {
        return html.replace("${footer}", "<br/><br/>Created at " + new Date());
    }

    private static void writeCurrentResults(File file, Map<String, Data> row) throws IOException {
        Map<String,Data> versions = new TreeMap<>(Collections.reverseOrder(new VersionComparator()));
        versions.putAll(row);

        // add pie-data
        /*
        {name: '3.10', y: 3.9}, 4.2, 5.7, 8.5, 11.9, 15.2, 17.0, 16.6, 14.2, 10.3, 6.6, 4.8
         */
        StringBuilder pieData = new StringBuilder();
        pieData.append("label,count,link\n");

        for(Entry<String,Data> entry : versions.entrySet()) {
            if(entry.getValue().count != 0) {
                pieData.append(String.format("%s,%d,%s\n", entry.getKey(), entry.getValue().count, entry.getValue().link));
            }
        }

        FileUtils.writeStringToFile(file, pieData.toString(), "UTF-8");

        System.out.println("Writing " + versions.size() + " versions" +
                " and " + countRepositories(versions) + " repositories.");
    }

    private static void writeAllResults(File all, Map<String, Data> row,
                                        Map<String, String> seenRepositoryVersions,
                                        Collection<String> repositories) throws IOException {
        // use the current found versions
        Map<String, Data> versions = new HashMap<>(row);

        // add all previously found repositories that we do not have in the list yet
        int count = 0;
        for(Entry<String,String> entry : seenRepositoryVersions.entrySet()) {
            String repository = entry.getKey();
            // only count repositories that we did not found today
            if(!repositories.contains(repository)) {
                String version = getPrintableVersion(entry.getValue());
                if (versions.containsKey(version)) {
                    versions.get(version).count++;
                } else {
                    versions.put(version, new Data(1, repository));
                }
                count++;
            }
        }

        System.out.println("Added " + count + " versions to 'all' from repositories that were seen previously.");

        // use the existing method to actually write the data out
        writeCurrentResults(all, versions);
    }

    private static int countRepositories(Map<String, Data> versions) {
        return versions.values().stream().mapToInt(value -> value.count).sum();
    }

    private static void writeVersionChanges(File changesFile, List<VersionChange> changes) throws IOException {
        // reverse order to have the latest changes on top
        Collections.reverse(changes);

        StringBuilder changesData = new StringBuilder("date,repository,versionBefore,versionNow\n");
        for(VersionChange change : changes) {
            change.writeCSV(changesData);
        }

        FileUtils.writeStringToFile(changesFile, changesData.toString(), "UTF-8");
    }


    private static String getHeaderData(Iterable<String> versions) {
        StringBuilder headers = new StringBuilder();
        for(String version : versions) {
            headers.append(',').append(version);
        }

        return headers.toString();
    }

    private static String formatValue(Number value) {
        if(value == null) {
            return "";
        }

        return (value instanceof Double ?
                        // round double to two decimal places
                        Double.toString((double)Math.round(100*value.doubleValue())/100) :
                        Integer.toString((Integer)value));
    }
}
