package org.dstadler.github.upgrade;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TryToUpgradeTest {
    @Ignore
    @Test
    public void testLocalBuild() throws IOException {
        TryToUpgrade.buildViaGradle("nd-team/goddess-java", new File("/tmp/TestGitRepository6087905793861044471"));
    }
}
