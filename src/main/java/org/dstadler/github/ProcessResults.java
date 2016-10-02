package org.dstadler.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
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
                "colors: ['#000000', '#ff0000', '#ff8000', '#ffff00', '#40ff00', '#0040ff', '#ff00ff', '#757e83', '#75c5d5', '#663300'],\n" +
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
        "</body>\n" +
        "</html>\n";

    public static void main(String[] args) throws IOException, ParseException {
        // read stats.json
        List<String> lines = FileUtils.readLines(new File("stats.json"), "UTF-8");
        Table<String,String,Integer> values = HashBasedTable.create();
        String maxDateStr = readLines(lines, values);

        generateHtmlFiles(values, maxDateStr);
    }

    private static String readLines(List<String> lines, Table<String, String, Integer> values) throws IOException {
        String maxDateStr = null;
        for(String line : lines) {
            JSONWriter.Holder holder = JSONWriter.mapper.readValue(line, JSONWriter.Holder.class);

            Multimap<String, String> versions = holder.getVersions();
            for(String version : versions.keySet()) {
                String date = holder.getDate();

                // combine all the non-version things like build-script variables, ...
                if(!VERSION_PATTERN.matcher(version).matches()) {
                    version = "other";
                }

                values.put(date, version, versions.get(version).size());
                if(maxDateStr == null || maxDateStr.compareTo(date) <= 0) {
                    maxDateStr = date;
                }
            }
        }

        Preconditions.checkNotNull(maxDateStr, "Should have a max date now!");

        return maxDateStr;
    }

    private static void generateHtmlFiles(Table<String, String, Integer> values, String maxDateStr) throws ParseException, IOException {
        // use a tree-set to have a simple sorting by version, this will not
        // work well for -beta, we can improve on it via a custom comparator later
        Set<String> columns = new TreeSet<>(values.columnKeySet());

        Date date = START_DATE;
        StringBuilder data = new StringBuilder();
        while(date.compareTo(DATE_FORMAT.parse(maxDateStr)) <= 0) {
            String dateStr = DATE_FORMAT.format(date);
            Map<String, Integer> row = values.row(dateStr);

            // Format: "    \"2008-05-07,75\\n\" +\n" +
            data.append("\"").append(dateStr);
            for(String column : columns) {
                data.append(",").append(formatValue(row.get(column)));
            }

            data.append("\\n\" + \n");

            date = DateUtils.addDays(date, 1);
        }

        // remove last trailing "+"
        data.setLength(data.length() - 3);

        String html = TEMPLATE.replace("${data}", data);
        html = html.replace("${dataheader}", "Date" + getHeaderData(columns));
        html = html.replace("${benchmark}", "Apache POI");

        StringBuilder annotations = new StringBuilder();
        for(String column : columns) {
            annotations.append("    {series: \"").append(column).append("\",x: \"").append(maxDateStr).
                    append("\",shortText: \"").append(column).append("\",text: \"").append(maxDateStr).append("\", \"width\": 100},\n");
        }

        // cut away trailing comma and newline
        annotations.setLength(annotations.length() - 2);

        html = html.replace("${annotations}", annotations);

        FileUtils.writeStringToFile(new File("results", "results.html"), html, "UTF-8");
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
