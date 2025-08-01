package org.dstadler.github.packaging;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.dstadler.github.search.BaseSearch;
import org.kohsuke.github.GHArtifact;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.dstadler.github.packaging.ListArtifacts.REPO_DEBIAN_PACKAGES;
import static org.dstadler.github.packaging.ListArtifacts.WORKFLOW_ID;

public class DownloadPackages {
    // bookworm@amd64@ottok_debcraft@main@512b4c356c842ecf7aecb81d23cb4947a910942a
    protected static final Pattern NAME_REGEX = Pattern.compile("([a-z]+)@([a-z0-9]+)@([-a-z0-9_]+)@([-a-z0-9_+]+)@([0-9a-f]+)");

    private static final String ROOT_DIR = "/opt/cowbuilder/github";

    public static void main(String[] args) throws IOException {
        GitHub github = BaseSearch.connect();

        System.out.println("Opening repository " + REPO_DEBIAN_PACKAGES);
        GHRepository repository = github.getRepository(REPO_DEBIAN_PACKAGES);

        List<Artifact> artifacts = new ArrayList<>();
        for (GHWorkflowRun run : repository.getWorkflow(WORKFLOW_ID).listRuns()) {
            for (GHArtifact artifact : run.listArtifacts()) {
                String name = artifact.getName();
                System.out.println("Artifact: " + name + " at " + artifact.getUrl() + " for " + run.getName());

                Matcher matcher = NAME_REGEX.matcher(name);
                if (matcher.matches()) {
                    System.out.println("Match: " + matcher.group(1) + "/"  + matcher.group(2) + "/" + matcher.group(3) + "/" + matcher.group(4));
                    artifacts.add(Artifact.populate(name, artifact.getUrl(), matcher, artifact));
                }
            }
        }

        System.out.println("Found " + artifacts.size() + " artifacts, downloading to " + ROOT_DIR);
        Preconditions.checkState(new File(ROOT_DIR).isDirectory() || new File(ROOT_DIR).mkdirs(),
            "Could not create directories at %s", ROOT_DIR);

        for (Artifact artifact : artifacts) {
            String name = artifact.name;

            // target-dir

            File checkFile = new File(ROOT_DIR, name + ".chk");
            if (checkFile.exists()) {
                System.out.println("Artifact already downloaded: " + name);
                continue;
            }

            System.out.println("Downloading " + name);

            artifact.ghArtifact.download(stream -> {
                extractArtifactZip(stream, artifact);

                return "";
            });

            FileUtils.writeStringToFile(checkFile, "", StandardCharsets.UTF_8);
        }
    }

    private static void extractArtifactZip(InputStream stream, Artifact artifact) throws IOException {
        File rootDir = new File(ROOT_DIR, artifact.distribution + "-" + artifact.architecture);
        Preconditions.checkState(rootDir.isDirectory() || rootDir.mkdirs(),
                "Could not create directories at %s", rootDir);

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(stream));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = new File(rootDir, zipEntry.getName());
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // sometimes files are listed before directories
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                byte[] buffer = new byte[1024];
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    private static class Artifact {
        String name;
        URL url;
        String distribution;
        String architecture;
        String repo;
        String ref;
        String commit;
        GHArtifact ghArtifact;

        static Artifact populate(String name, URL url, Matcher matcher, GHArtifact ghArtifact) {
            Artifact artifact = new Artifact();

            artifact.name = name;
            artifact.url = url;
            artifact.distribution = matcher.group(1);
            artifact.architecture = matcher.group(2);
            artifact.repo = matcher.group(3);
            artifact.ref = matcher.group(4);
            artifact.commit = matcher.group(5);
            artifact.ghArtifact = ghArtifact;

            return artifact;
        }
    }
}
