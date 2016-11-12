package org.dstadler.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

import static org.dstadler.github.JSONWriter.DATE_FORMAT;

public class ProcessResults {
    private static final Date START_DATE;
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
        "<head>" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/dygraph/1.1.1/dygraph-combined.js\"></script>\n" +
                "<style>#graphdiv { position: absolute; left: 10px; right: 10px; top: 10px; bottom: 10px; }</style>\n" +
        "</head>\n" +
        "<body>" +
                "<div id=\"graphdiv\"></div>\n" +
                /* need to add Jekyll-tags to make this work
                "<div class=\"docs-header-bottom\">\n" +
                "    {% include footer.html %}\n" +
                "</div>\n" +*/
                "\n" +
                "<script type=\"text/javascript\">\n" +
                "  g = new Dygraph(\n" +
                "\n" +
                "    // containing div\n" +
                "    document.getElementById(\"graphdiv\"),\n" +
                "\n" +
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
                "       xRangePad: 20,\n" +

                "       yAxisLabelWidth: 60,\n" +
                //"       yLabelWidth: 100,\n" +

                "       labelsSeparateLines: true,\n" +
                "       legend: 'follow',\n" +

                "   axes: {\n" +
                "       y: {\n" +
                "                valueFormatter: function(y) {\n" +
                "                  return y;\n" +
                "                },\n" +
                "                axisLabelFormatter: function(y) {\n" +
                "                  return y;\n" +
                "                }\n" +
                "              }\n" +
                "   },\n" +

                //"      rollPeriod: 7,\n" +
                //"      showRoller: true,\n" +
                //"connectSeparatedPoints: true,\n" +
                //"       drawPoints: true\n" +

                // taken from http://www.mulinblog.com/a-color-palette-optimized-for-data-visualization/
                "colors: ['#4D4D4D', '#5DA5DA', '#FAA43A', '#60BD68', '#F17CB0', '#B2912F', '#B276B2', '#DECF3F', '#F15854'],\n" +

                "    }\n" +
                "\n" +
                "  );\n" +
                "\n" +
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
        // read stats.json
        List<String> lines = FileUtils.readLines(new File("stats.json"), "UTF-8");
        Table<String,String,Data> values = HashBasedTable.create();
        List<VersionChange> changes = new ArrayList<>();
        String maxDateStr = readLines(lines, values, changes);

        File results = generateHtmlFiles(values, maxDateStr);

        File pie = new File("docs", "resultsCurrent.csv");
        writeCurrentResults(pie, values.row(maxDateStr));

        File changesFile = new File("docs/_data", "versionChanges.csv");
        writeVersionChanges(changesFile, changes);

        System.out.println("Wrote results to " + results + " and " + pie);
    }

    private static class Data {
        public Integer count;
        public String link;

        public Data(Integer count, String link) {
            this.count = count;
            this.link = link;
        }
    }

    private static String readLines(List<String> lines, Table<String, String, Data> dateVersionTable,
                                    List<VersionChange> changes) throws IOException {
        String maxDateStr = null;
        SetMultimap<String, String> previousVersions = null;

        for(String line : lines) {
            JSONWriter.Holder holder = JSONWriter.mapper.readValue(line, JSONWriter.Holder.class);
            SetMultimap<String, String> versions = holder.getVersions();
            String date = holder.getDate();

            maxDateStr = populateTable(dateVersionTable, maxDateStr, versions, date);

            // print out if we found projects that switched versions
            compareToPrevious(date, previousVersions, holder.getRepositoryVersions(), changes);

            previousVersions = holder.getRepositoryVersions();
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
            sb.append(date).append(",").append(repository).append(",").append(versionBefore).append(",").append(versionNow).append("\n");
        }
    }

    protected static void compareToPrevious(String date, SetMultimap<String, String> previousVersions,
                                            SetMultimap<String, String> versions,
                                            List<VersionChange> changes) {
        if(previousVersions != null) {
            for(Map.Entry<String,String> entry : versions.entries()) {
                String version = entry.getKey();
                String repository = entry.getValue();
                if(previousVersions.containsEntry(version, repository)) {
                    // was already in previous holder
                    continue;
                }

                if(previousVersions.containsValue(repository)) {
                    ImmutableMultimap<String, String> inverse = ImmutableMultimap.copyOf(previousVersions).inverse();
                    String versionBefore = inverse.get(repository).iterator().next();
                    System.out.println("Did find a different version for " + repository +
                            ", previously at " + versionBefore +
                            ", now at " + version);

                    changes.add(new VersionChange(date, repository, versionBefore, version));
                }
            }
        }
    }

    private static String populateTable(Table<String, String, Data> dateVersionTable, String maxDateStr, SetMultimap<String, String> versions, String date) {
        System.out.println("Had " + versions.size() + " entries for " + date);
        for(String version : versions.keySet()) {
            // combine all the non-version things like build-script variables, ...
            String versionKey = version;
            if(!VERSION_PATTERN.matcher(version).matches()) {
                versionKey = "other";
            }

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

    private static File generateHtmlFiles(Table<String, String, Data> dateVersionTable, String maxDateStr) throws ParseException, IOException {
        // use a tree-set to get the versions in correct order
        Set<String> versionsSorted = new TreeSet<>(Collections.reverseOrder(new VersionComparator()));
        versionsSorted.addAll(dateVersionTable.columnKeySet());

        Date date = START_DATE;
        StringBuilder data = new StringBuilder();
        while(date.compareTo(DATE_FORMAT.parse(maxDateStr)) <= 0) {
            String dateStr = DATE_FORMAT.format(date);
            Map<String, Data> row = dateVersionTable.row(dateStr);

            // Format: "    \"2008-05-07,75\\n\" +\n" +
            data.append("\"").append(dateStr);
            for(String column : versionsSorted) {
                data.append(",").append(formatValue(row.get(column) == null ? null : row.get(column).count));
            }

            data.append("\\n\" + \n");

            date = DateUtils.addDays(date, 1);
        }

        // remove last trailing "+"
        data.setLength(data.length() - 3);

        String html = TEMPLATE.replace("${data}", data);
        html = html.replace("${dataheader}", "Date" + getHeaderData(versionsSorted));
        html = html.replace("${benchmark}", "Apache POI");

        StringBuilder annotations = new StringBuilder();
        for(String column : versionsSorted) {
            annotations.append("    {series: \"").append(column).append("\",x: \"").append(maxDateStr).
                    append("\",shortText: \"").append(column).append("\",text: \"").append(maxDateStr).append("\", \"width\": 100},\n");
        }

        // cut away trailing comma and newline
        annotations.setLength(annotations.length() - 2);

        html = html.replace("${annotations}", annotations);
        html = addFooter(html);

        File results = new File("docs", "results.html");
        FileUtils.writeStringToFile(results, html, "UTF-8");

        return results;
    }

    private static String addFooter(String html) {
        return html.replace("${footer}", "<br/><br/>Created at " + new Date());
    }

    private static void writeCurrentResults(File pie, Map<String, Data> row) throws IOException {
        Map<String,Data> versions = new TreeMap<>(Collections.reverseOrder(new VersionComparator()));
        versions.putAll(row);

        // add pie-data
        /*
        {name: '3.10', y: 3.9}, 4.2, 5.7, 8.5, 11.9, 15.2, 17.0, 16.6, 14.2, 10.3, 6.6, 4.8
         */
        StringBuilder pieData = new StringBuilder();
        pieData.append("label,count,link\n");
        //noinspection Convert2streamapi
        for(Map.Entry<String,Data> entry : versions.entrySet()) {
            if(entry.getValue().count != 0) {
                pieData.append(String.format("%s,%d,%s\n", entry.getKey(), entry.getValue().count, entry.getValue().link));
            }
        }

        FileUtils.writeStringToFile(pie, pieData.toString(), "UTF-8");
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


    private static String getHeaderData(Collection<String> versions) {
        StringBuilder headers = new StringBuilder();
        for(String version : versions) {
            headers.append(",").append(version);
        }

        return headers.toString();
    }

    private static String formatValue(Integer value) {
        return value == null ? "" : "" + value;
    }
}
