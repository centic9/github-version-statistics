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
import java.util.Collection;

/**
 * Collection of results from trying to download, build and upgrade projects.
 *
 * Can be read from a file to include projects tried before.
 * Can be written to a file to persist results across runs.
 */
public class ProjectStatuses {
    private static final String FILE_NAME = "upgrades.csv";

    private final Collection<ProjectStatus> projectStatuses = new ArrayList<>();

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

    public int size() {
        return projectStatuses.size();
    }

    public void read(String fileName) throws IOException {
        try (Reader in = new FileReader(fileName);
             CSVParser parser = CSVFormat.DEFAULT.
                     withFirstRecordAsHeader().
                     parse(in)) {
            for (CSVRecord record : parser) {
                ProjectStatus status = new ProjectStatus(record.get("Project"), UpgradeStatus.valueOf(record.get("Status")));

                projectStatuses.add(status);
            }
        }
    }

    public static ProjectStatuses read() throws IOException {
        ProjectStatuses projectStatuses = new ProjectStatuses();
        projectStatuses.read(FILE_NAME);
        return projectStatuses;
    }

    public void write() throws IOException {
        try(Writer out = new FileWriter(FILE_NAME);
            CSVPrinter printer = CSVFormat.DEFAULT.withHeader("Project", "Status").
                    print(out)) {
            for (ProjectStatus projectStatus : projectStatuses) {
                printer.printRecord(projectStatus.getProject(), projectStatus.getStatus());
            }
        }
    }
}
