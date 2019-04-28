package org.dstadler.github.util;

import org.dstadler.github.search.BaseSearch;
import org.dstadler.github.upgrade.ProjectStatus;
import org.dstadler.github.upgrade.ProjectStatuses;
import org.dstadler.github.upgrade.UpgradeStatus;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GitHubSupport {
    public static Map<String, String> filterForProjectsOfInterest(Map<String, String> projects, ProjectStatuses projectStatuses) throws IOException {
        GitHub github = BaseSearch.connect();

        Map<String, String> projectsOfInterest = new HashMap<>();
        Iterator<Map.Entry<String, String>> it = projects.entrySet().iterator();
        for(int i = 0;i < 100 && it.hasNext();i++) {
            Map.Entry<String, String> repo = it.next();
            try {
                GHRepository repository = github.getRepository(repo.getKey());
                int stargazersCount = repository.getStargazersCount();
                int watchers = repository.getWatchers();
                System.out.println(i + "-Repo: " + repo + ": Had stars: " + stargazersCount + ", watchers: " + watchers);
                if(stargazersCount > 0 || watchers > 0) {
                    projectsOfInterest.put(repo.getKey(), repo.getValue());
                } else {
                    projectStatuses.add(new ProjectStatus(repo.getKey(), UpgradeStatus.NoStarsOrWatchers));
                }
            } catch (GHFileNotFoundException e) {
                System.out.println(i + "-Repo: " + repo + ": Not found: " + e);
                projectStatuses.add(new ProjectStatus(repo.getKey(), UpgradeStatus.NotAccessible));
            } catch (HttpException e) {
                if(e.getResponseCode() == 403) {
                    System.out.println(i + "-Repo: " + repo + ": Forbidden: " + e);
                    projectStatuses.add(new ProjectStatus(repo.getKey(), UpgradeStatus.NotAccessible));
                } else {
                    throw e;
                }
            }
        }
        return projectsOfInterest;
    }
}
