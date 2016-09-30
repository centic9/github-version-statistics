package org.dstadler.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ProcessResults {
    private static final Date START_DATE;

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm");

    static {
        try {
            START_DATE = DATE_FORMAT.parse("2016-09-30 00:00");
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse date", e);
        }
    }

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
                "                  return parseFloat(Math.round(y * 100) / 100).toFixed(2) + 's';\n" +
                "                },\n" +
                "                axisLabelFormatter: function(y) {\n" +
                "                  return y + 's';\n" +
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
                /*"  g.ready(function() {\n" +
                "    g.setAnnotations([\n" +
                "    {series: \"Test.TestOOXMLLite\",x: \"2016-08-01\",shortText: \"A\",text: \"OOXMLLite build change\"},\n" +
                "    {series: \"Test.TestIntegration\",x: \"2016-09-15\",shortText: \"B\",text: \"Server upgrade\",attachAtBottom: true},\n" +
                "    {series: \"Test.TestOOXMLLite\",x: \"2016-09-17\",shortText: \"C\",text: \"OOXMLLite enabled again\"},\n" +
                "    ]);\n" +
                "  });\n" +*/
                "</script>\n" +
        "</body>\n" +
        "</html>\n";

    public static void main(String[] args) throws IOException, ParseException {
        // read stats.json
        List<String> lines = FileUtils.readLines(new File("stats.json"), "UTF-8");
        Map<String,Integer> values = new TreeMap<>();
        String maxDateStr = readLines(lines, values);

        generateHtmlFiles(values, maxDateStr);
    }

    private static String readLines(List<String> lines, Map<String, Integer> values) throws IOException {
        String maxDateStr = null;
        for(String line : lines) {
            JSONWriter.Holder holder = JSONWriter.mapper.readValue(line, JSONWriter.Holder.class);

            Multimap<String, String> versions = holder.getVersions();
            for(String version : versions.keySet()) {
                String date = holder.getDate();
                values.put(date, versions.get(version).size());
                if(maxDateStr == null || maxDateStr.compareTo(date) <= 0) {
                    maxDateStr = date;
                }
            }
        }

        Preconditions.checkNotNull(maxDateStr, "Should have a max date now!");

        return maxDateStr;
    }

    private static void generateHtmlFiles(Map<String, Integer> values, String maxDateStr) throws ParseException, IOException {
        StringBuilder data = new StringBuilder();
        Date date = START_DATE;
        while(date.compareTo(DATE_FORMAT.parse(maxDateStr)) <= 0) {
            String dateStr = DATE_FORMAT.format(date);
            Integer value = values.get(dateStr);

            // Format: "    \"2008-05-07,75\\n\" +\n" +
            data.append("\"").append(dateStr).append(",").append(formatValue(value)).append("\\n\" + \n");

            date = DateUtils.addDays(date, 1);
        }

        // remove last trailing "+"
        data.setLength(data.length() - 3);

        String html = TEMPLATE.replace("${data}", data);
        html = html.replace("${dataheader}", "Date,Time");
        html = html.replace("${benchmark}", "Apache POI");

        FileUtils.writeStringToFile(new File("results", "results.html"), html, "UTF-8");
    }

    private static String formatValue(Integer value) {
        return value == null ? "" : "" + value;
    }
}
