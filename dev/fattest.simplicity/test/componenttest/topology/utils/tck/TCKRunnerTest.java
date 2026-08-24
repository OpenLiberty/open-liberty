/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link TCKRunner} helper methods.
 */
public class TCKRunnerTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    // ---------------------------------------------------------------------------
    // versionSuffix
    // ---------------------------------------------------------------------------

    @Test
    public void versionSuffix_typicalJar() {
        assertEquals("1.0.117", TCKRunner.versionSuffix("io.openliberty.jakarta.data.1.1_1.0.117.jar"));
    }

    @Test
    public void versionSuffix_stripsJarExtension() {
        assertEquals("1.0.99", TCKRunner.versionSuffix("some.artifact_1.0.99.jar"));
    }

    @Test
    public void versionSuffix_noUnderscore() {
        // Without an underscore the whole name (minus .jar) is returned
        assertEquals("someartifact", TCKRunner.versionSuffix("someartifact.jar"));
    }

    @Test
    public void versionSuffix_noJarExtension() {
        assertEquals("1.0.5", TCKRunner.versionSuffix("artifact_1.0.5"));
    }

    // ---------------------------------------------------------------------------
    // compareJarVersionsReversed
    // ---------------------------------------------------------------------------

    @Test
    public void compareJarVersionsReversed_higherPatchWins() {
        // 1.0.117 > 1.0.99 → a should sort before b (reversed = negative result for a>b)
        String high = "io.openliberty.jakarta.data.1.1_1.0.117.jar";
        String low  = "io.openliberty.jakarta.data.1.1_1.0.99.jar";

        int cmp = TCKRunner.compareJarVersionsReversed(high, low);
        if (cmp >= 0) {
            fail("Expected " + high + " to sort before " + low +
                                     " but compareJarVersionsReversed returned " + cmp);
        }
    }

    @Test
    public void compareJarVersionsReversed_lowerPatchSortsLater() {
        String high = "io.openliberty.jakarta.data.1.1_1.0.117.jar";
        String low  = "io.openliberty.jakarta.data.1.1_1.0.99.jar";
        // After sorting with the comparator, high should be first
        List<String> jars = Arrays.asList(low, high);
        jars.sort(TCKRunner::compareJarVersionsReversed);
        assertEquals(high, jars.get(0));
        assertEquals(low, jars.get(1));
    }

    @Test
    public void compareJarVersionsReversed_sameVersionEqual() {
        String jar = "artifact_1.0.5.jar";
        assertEquals(0, TCKRunner.compareJarVersionsReversed(jar, jar));
    }

    @Test
    public void compareJarVersionsReversed_majorVersionDifference() {
        String v2 = "artifact_2.0.0.jar";
        String v1 = "artifact_1.9.9.jar";
        List<String> jars = Arrays.asList(v1, v2);
        jars.sort(TCKRunner::compareJarVersionsReversed);
        assertEquals(v2, jars.get(0));
        assertEquals(v1, jars.get(1));
    }

    // ---------------------------------------------------------------------------
    // jarPathInDir — the primary regression test
    // ---------------------------------------------------------------------------

    /**
     * Regression test for the original bug: lexicographic sort chose .99 over .117.
     * With the numeric version comparator .117 must win.
     */
    @Test
    public void jarPathInDir_numericVersionComparison() throws IOException {
        File dir = tmpDir.newFolder("libs");
        createFile(dir, "io.openliberty.jakarta.data.1.1_1.0.99.jar");
        createFile(dir, "io.openliberty.jakarta.data.1.1_1.0.117.jar");

        String result = TCKRunner.jarPathInDir("io.openliberty.jakarta.data.1.1", dir.getAbsolutePath());

        assertEquals("io.openliberty.jakarta.data.1.1_1.0.117.jar", result);
    }

    @Test
    public void jarPathInDir_singleMatchReturned() throws IOException {
        File dir = tmpDir.newFolder("libs");
        createFile(dir, "com.ibm.ws.some.feature_1.0.45.jar");

        String result = TCKRunner.jarPathInDir("com.ibm.ws.some.feature", dir.getAbsolutePath());

        assertEquals("com.ibm.ws.some.feature_1.0.45.jar", result);
    }

    @Test
    public void jarPathInDir_noMatchReturnsNull() throws IOException {
        File dir = tmpDir.newFolder("libs");
        createFile(dir, "unrelated.artifact_1.0.0.jar");

        String result = TCKRunner.jarPathInDir("io.openliberty.jakarta.data.1.1", dir.getAbsolutePath());

        assertNull(result);
    }

    @Test
    public void jarPathInDir_emptyDirReturnsNull() throws IOException {
        File dir = tmpDir.newFolder("empty");

        String result = TCKRunner.jarPathInDir("io.openliberty.jakarta.data.1.1", dir.getAbsolutePath());

        assertNull(result);
    }

    @Test
    public void jarPathInDir_highestOfThreeVersionsReturned() throws IOException {
        File dir = tmpDir.newFolder("libs");
        createFile(dir, "io.openliberty.jakarta.data.1.1_1.0.5.jar");
        createFile(dir, "io.openliberty.jakarta.data.1.1_1.0.99.jar");
        createFile(dir, "io.openliberty.jakarta.data.1.1_1.0.117.jar");

        String result = TCKRunner.jarPathInDir("io.openliberty.jakarta.data.1.1", dir.getAbsolutePath());

        assertEquals("io.openliberty.jakarta.data.1.1_1.0.117.jar", result);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static void createFile(File dir, String name) throws IOException {
        new File(dir, name).createNewFile();
    }
}
