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
        ProjectStatuses projectStatuses = ProjectStatuses.read();

        File[] files = Stats.getFiles();

        // project, version
        Map<String, String> projects = new HashMap<>();

        readLines(files, projects);

        System.out.println("Read " + projects.size() + " unique projects from " + files.length + " stats-files");

        // remove projects that were handled before
        filterProjects(projects, projectStatuses);

        System.out.println("Having " + projects.size() + " projects after filtering");

        // look for "interesting" projects, currently we use only the ones with stars or watchers
        Map<String, String> projectsOfInterest = GitHubSupport.filterForProjectsOfInterest(projects, projectStatuses);

        System.out.println("Found " + projectsOfInterest.size() + " repositories with stars or watchers");

        // now try to build and upgrade them
        ProjectBuilder.cloneBuildAndUpgrade(projectsOfInterest, projectStatuses);

        // persist resulting list of projects and their state to not re-do the work for them
        projectStatuses.write();

        System.out.println("Done, having " + projectStatuses.size() + " results overall now");
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
