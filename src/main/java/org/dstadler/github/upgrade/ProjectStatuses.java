package org.dstadler.github.upgrade;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class ProjectStatuses {
    private final List<ProjectStatus> projectStatuses = new ArrayList<>();

    public void add(ProjectStatus status) {
        projectStatuses.add(status);
    }

    public ProjectStatus get(String project) {
        for (ProjectStatus projectStatus : projectStatuses) {
            if (projectStatus.getProject().equals(project)) {
                return projectStatus;
            }
        }

        return null;
    }


    public void read() throws IOException {
        try (Reader in = new FileReader("upgrades.csv");
             CSVParser parser = CSVFormat.DEFAULT.
                     withFirstRecordAsHeader().
                     parse(in)) {
            for (CSVRecord record : parser) {
                ProjectStatus status = new ProjectStatus(record.get("Project"), UpgradeStatus.valueOf(record.get("Status")));

                projectStatuses.add(status);
            }
        }
    }

    public void write() throws IOException {
        try(Writer out = new FileWriter("upgrade.csv");
            CSVPrinter printer = CSVFormat.DEFAULT.printer()) {
            printer.printRecord("Project", "Status");
            for (ProjectStatus projectStatus : projectStatuses) {
                printer.printRecord(projectStatus.getProject(), projectStatus.getStatus());
            }
        }
    }
}
