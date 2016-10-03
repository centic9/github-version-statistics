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

    private static final String TEMPLATE_PIE =
            "<html>\n" +
                    "<head>" +
                    "</head>\n" +
                    "<body>" +
                    "<div id=\"container\" style=\"min-width: 310px; height: 400px; margin: 0 auto\">\n" +
                    "</div>\n" +
                    "\n" +
                    "<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js\">\n" +
                    "</script>\n" +
                    "<script src=\"https://code.highcharts.com/highcharts.js\">\n" +
                    "</script>\n" +
                    "<script src=\"https://code.highcharts.com/modules/exporting.js\">\n" +
                    "</script>\n" +
                    "\n" +
                    "<script type=\"text/javascript\">\n" +
                    "\n" +
                    "$(function () {\n" +
                    "        $('#container').highcharts({\n" +
                    "            chart: {\n" +
                    "                        plotBackgroundColor: null,\n" +
                    "                        plotBorderWidth: null,\n" +
                    "                        plotShadow: false,\n" +
                    "                        type: 'pie'\n" +
                    "                    },\n" +
                    "            title: {\n" +
                    "                text: 'Current version distribution',\n" +
                    "                x: -20 //center\n" +
                    "            },\n" +
                    "            subtitle: {\n" +
                    "                text: 'Fetched from the first 1000 results of a Github search',\n" +
                    "                x: -20\n" +
                    "            },\n" +
                    "            legend: {\n" +
                    "                layout: 'vertical',\n" +
                    "                align: 'right',\n" +
                    "                verticalAlign: 'middle',\n" +
                    "                borderWidth: 0\n" +
                    "            },\n" +
                    "            plotOptions: {\n" +
                    "                pie: {\n" +
                    "                    allowPointSelect: true,\n" +
                    "                    cursor: 'pointer',\n" +
                    "                    dataLabels: {\n" +
                    "                        enabled: true,\n" +
                    "                        format: '<b>{point.name}</b>: {point.y:.0f}',\n" +
                    "                        style: {\n" +
                    "                            color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'\n" +
                    "                        }\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            },\n" +
                    "            series: [{\n" +
                    "                name: 'Version',\n" +
                    "                data: [${piedata}]\n" +
                    "            }]\n" +
                    "        });\n" +
                    "    });\n" +
                    "\n" +
                    "</script>" +
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
                String versionKey = version;
                if(!VERSION_PATTERN.matcher(version).matches()) {
                    versionKey = "other";
                }

                Integer value = values.get(date, versionKey);
                if(value == null) {
                    value = versions.get(version).size();
                } else {
                    value += versions.get(version).size();
                }
                values.put(date, versionKey, value);
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
        Set<String> columns = new TreeSet<>(Collections.reverseOrder(new VersionComparator()));
        columns.addAll(values.columnKeySet());

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

        File results = new File("docs", "results.html");
        FileUtils.writeStringToFile(results, html, "UTF-8");

        File pie = new File("docs", "resultsCurrent.html");
        writeCurrentResults(pie, values.row(maxDateStr));

        System.out.println("Wrote results to " + results + " and " + pie);
    }

    private static void writeCurrentResults(File pie, Map<String, Integer> row) throws IOException {
        Map<String,Integer> versions = new TreeMap<>(Collections.reverseOrder(new VersionComparator()));
        versions.putAll(row);

        // add pie-data
        /*
        {name: '3.10', y: 3.9}, 4.2, 5.7, 8.5, 11.9, 15.2, 17.0, 16.6, 14.2, 10.3, 6.6, 4.8
         */
        StringBuilder pieData = new StringBuilder();
        for(Map.Entry<String,Integer> entry : versions.entrySet()) {
            if(entry.getValue() != 0) {
                pieData.append(String.format("{name: '%s', y:%d},", entry.getKey(), entry.getValue()));
            }
        }
        // cut off trailing comma
        pieData.setLength(pieData.length()-1);
        String html = TEMPLATE_PIE.replace("${piedata}", pieData);

        FileUtils.writeStringToFile(pie, html, "UTF-8");
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
