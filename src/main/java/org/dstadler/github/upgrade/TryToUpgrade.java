package org.dstadler.github.upgrade;

import org.dstadler.github.util.GitHubSupport;
import org.dstadler.github.util.JSONWriter.Holder;
import org.dstadler.github.util.Stats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TryToUpgrade {
    public static void main(String[] args) throws IOException {
        ProjectStatuses projectStatuses = new ProjectStatuses();

        projectStatuses.read();

        File[] files = Stats.getFiles();

        // project, version
        Map<String, String> projects = new HashMap<>();

        readLines(files, projects);

        System.out.println("Having " + projects.size() + " unique projects");

        Map<String, String> projectsOfInterest = GitHubSupport.filterForProjectsOfInterest(projects);

        System.out.println("Found " + projectsOfInterest.size() + " repositories with stars or watchers");

        filterProjects(projectsOfInterest, projectStatuses);

        ProjectBuilder.cloneBuildAndUpgrade(projectsOfInterest, projectStatuses);

        projectStatuses.write();
    }

    private static void filterProjects(Map<String, String> projectsOfInterest, ProjectStatuses projectStatuses) {
        Iterator<Map.Entry<String, String>> it = projectsOfInterest.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, String> project = it.next();
            ProjectStatus projectStatus = projectStatuses.get(project.getKey());

            //noinspection VariableNotUsedInsideIf
            if(projectStatus != null) {
                // for now do not try again if we tried a project already
                it.remove();
            }
        }
    }

    protected static void readLines(File[] files, Map<String, String> projects) throws IOException {
        for(File file : files) {
            Holder.readFile(projects, file);
        }
    }
}
