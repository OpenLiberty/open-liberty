/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    // JarVersionComparator.compare()
    // ---------------------------------------------------------------------------

    @Test
    public void jarVersionComparator_value() {
        String high = "io.openliberty.jakarta.data.1.1_1.0.117.jar";
        String low = "io.openliberty.jakarta.data.1.1_1.0.99.jar";

        // 1.0.117 > 1.0.99 → a should sort before b (reversed = negative result for a>b)
        int cmp = TCKRunner.JarVersionComparator.HIGH_TO_LOW.compare(high, low);
        assertTrue("Expected " + high + " to sort before " + low +
                   " but JarVersionComparator.HIGH_TO_LOW returned " + cmp,
                   cmp < 0);

        // 1.0.99 > 1.0.117 → a should sort before b (reversed = negative result for a>b)
        cmp = TCKRunner.JarVersionComparator.LOW_TO_HIGH.compare(high, low);
        assertTrue("Expected " + low + " to sort before " + high +
                   " but JarVersionComparator.LOW_TO_HIGH returned " + cmp,
                   cmp > 0);
    }

    @Test
    public void jarVersionComparator_sort() {
        String high = "io.openliberty.jakarta.data.1.1_1.0.117.jar";
        String low = "io.openliberty.jakarta.data.1.1_1.0.99.jar";

        List<String> jars = Arrays.asList(low, high);

        // After sorting with the comparator, high should be first
        jars.sort(TCKRunner.JarVersionComparator.HIGH_TO_LOW);
        assertEquals(high, jars.get(0));
        assertEquals(low, jars.get(1));

        // After sorting with the comparator, low should be first
        jars.sort(TCKRunner.JarVersionComparator.LOW_TO_HIGH);
        assertEquals(low, jars.get(0));
        assertEquals(high, jars.get(1));
    }

    @Test
    public void jarVersionComparator_equal() {
        String jar = "artifact_1.0.5.jar";
        assertEquals(0, TCKRunner.JarVersionComparator.HIGH_TO_LOW.compare(jar, jar));
        assertEquals(0, TCKRunner.JarVersionComparator.LOW_TO_HIGH.compare(jar, jar));
    }

    @Test
    public void jarVersionComparator_major() {
        String v2 = "artifact_2.0.0.jar";
        String v1 = "artifact_1.9.9.jar";
        List<String> jars = Arrays.asList(v1, v2);

        jars.sort(TCKRunner.JarVersionComparator.HIGH_TO_LOW);
        assertEquals(v2, jars.get(0));
        assertEquals(v1, jars.get(1));

        jars.sort(TCKRunner.JarVersionComparator.LOW_TO_HIGH);
        assertEquals(v1, jars.get(0));
        assertEquals(v2, jars.get(1));
    }

    @Test
    public void jarVersionComparator_exceptionally() {
        String threeParts = "artifact_1.0.0.jar";
        String fourParts = "artifact_1.0.0.0.jar";

        try {
            TCKRunner.JarVersionComparator.HIGH_TO_LOW.compare(threeParts, fourParts);
            fail("Should have thrown IllegalStateException since versions had different number of parts.");
        } catch (IllegalArgumentException e) {
            //pass
        }

        String nonIntPart = "artifact_1.0.a.jar";

        try {
            TCKRunner.JarVersionComparator.HIGH_TO_LOW.compare(threeParts, nonIntPart);
            fail("Should have thrown NumberFormatException when parsing a string which is not an integer.");
        } catch (NumberFormatException e) {
            //pass
        }
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
