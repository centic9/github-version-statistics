package org.dstadler.github.packaging;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.dstadler.github.search.BaseSearch;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.dstadler.github.packaging.ListArtifacts.REPO_DEBIAN_PACKAGES;
import static org.dstadler.github.packaging.ListArtifacts.WORKFLOW_ID;

public class TriggerBuilds {
    // add some other GitHub repos for building
    private static final List<Pair<String,String>> REPOS_TO_ADD = List.of(
        Pair.of("ottok/debcraft", "main")
    );

    // allow to blacklist some repos which are not necessary any more
    private static final List<String> REPOS_TO_IGNORE = List.of(
            "bless-ppa",
            "debhelper-ppa",
            "digikam-ppa",
            "dpkg-ppa",
            "git-ftp-ppa",
            "git-ppa",
            "kdesvn-ppa",
            "konsole-ppa",
            "libmp3splt-ppa",
            "marble-qt4-ppa",
            "mp3gain-ppa",
            "mp3splt-gtk-ppa",
            "mp3splt-ppa",
            "ntp-ppa",
            "ocserv-ppa",
            "onedrive-ppa",
            "openambit-ppa",
            "openconnect-ppa",
            "scribus-ppa",
            "soundkonverter-ppa",
            "subversion-19-ppa",
            "subversion-ppa",
            "tpm2-tss-ppa"
    );

    // some version-comparisons are too complicated for the simple comparison below
    private static final Multimap<String,String> IGNORED_BRANCHES = HashMultimap.create();
    static {
        IGNORED_BRANCHES.put("laminar-ppa", "ppa/1.1-1build1");
    }


    public static void main(String[] args) throws IOException {
        GitHub github = BaseSearch.connect();

        // Fetch all repositories for user "centic9" where the name ends with "-ppa"
        // and select which branch should be built
        List<Pair<String,String>> builds = new ArrayList<>();
        for (GHRepository repo : github.getUser("centic9").listRepositories()) {
            if (repo.getName().endsWith("-ppa") && !REPOS_TO_IGNORE.contains(repo.getName())) {
                //System.out.println("Repository: " + repo.getName());

                // For branches where the name starts with "ppa/", determine the latest branch by version
                repo.getBranches().entrySet().stream()
                        // only branches where the name starts with "ppa/"
                        .filter(entry -> entry.getKey().startsWith("ppa/"))
                        // filter out some "known" unwanted branches
                        .filter(entry -> !IGNORED_BRANCHES.containsEntry(repo.getName(), entry.getKey()))
                        // choose the "latest" branch by version-comparison
                        .max(Comparator.comparing(entry -> extractVersion(entry.getKey())))
                        .ifPresent(entry -> {
                            System.out.println("Latest branch for " + repo.getName() + ": " + entry.getKey());
                            builds.add(Pair.of(repo.getName(), entry.getKey()));
                        });
            }
        }

        builds.addAll(REPOS_TO_ADD);

        List<DownloadPackages.Artifact> artifacts = DownloadPackages.getAvailableArtifacts(github);
        System.out.println("Found " + artifacts.size() + " artifacts and " + builds.size() + " repos to build: " + builds);

        // remove successfully built items
        // TODO: check if there is a newer build available on the branch
        // TODO: check per "distribution"
        builds.removeIf(build ->
                artifacts.stream().anyMatch(a ->
                        a.repo.equals(build.getKey().replace("/", "_")) &&
                        a.ref.equals(build.getValue().replace("/", "_"))));

        System.out.println("Found " + builds.size() + " repos to build: " + builds);

        // trigger workflow builds
        System.out.println("Opening repository " + REPO_DEBIAN_PACKAGES);
        GHRepository repository = github.getRepository(REPO_DEBIAN_PACKAGES);

        for (Pair<String, String> build : builds) {
            // trigger workflow for this repository and the found "latest" branch
            System.out.println("Trigger package-build for repository " + build.getKey() + " at " + build.getValue());
            repository.getWorkflow(WORKFLOW_ID).dispatch("main", Map.of(
                    "gh_repo", build.getKey(),
                    "gh_ref", build.getValue()));
        }
    }

    // Extracts version numbers from branch names and converts them to a comparable format
    private static ComparableVersion extractVersion(String branchName) {
        Pattern pattern = Pattern.compile("ppa/(.*)");
        Matcher matcher = pattern.matcher(branchName);
        if (matcher.find()) {
            return new ComparableVersion(matcher.group(1));
        }

        throw new IllegalStateException("Failed for branchName");
    }
}

// Helper class to compare version strings
class ComparableVersion implements Comparable<ComparableVersion> {
    private final String version;

    public ComparableVersion(String version) {
        this.version = version;
    }

    @Override
    public int compareTo(ComparableVersion other) {
        String[] parts1 = this.version.split("[:.-]");
        String[] parts2 = other.version.split("[:.-]");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            String part1 = i < parts1.length ? parts1[i] : "";
            String part2 = i < parts2.length ? parts2[i] : "";
            boolean isNum1 = part1.matches("\\d+");
            boolean isNum2 = part2.matches("\\d+");
            int comparison;
            if (isNum1 && isNum2) {
                comparison = Integer.compare(Integer.parseInt(part1), Integer.parseInt(part2));
            } else {
                comparison = part1.compareTo(part2);
            }
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private int parsePart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
