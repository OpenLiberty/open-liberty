/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

// import org.junit.Assert;

import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyDelta;
import com.ibm.ws.kernel.feature.internal.util.VerifyEnv;
import com.ibm.ws.kernel.feature.internal.util.VerifyXML;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class VersionlessResolutionTest {

    public static final String SERVER_NAME = "verify";

    public static final String[] ALLOWED_ERRORS = { "CWWKF0001E", "CWWKF0048E" };

    // Expected results:
    //
    // open-liberty/dev/com.ibm.ws.kernel.feature.resolver_fat/publish/verify/expected.xml
    // -- as --
    // open-liberty/dev/com.ibm.ws.kernel.feature.resolver_fat/build/libs/autoFVT/publish/verify/expected.xml
    //
    // Actual results:
    //
    // open-liberty/dev/com.ibm.ws.kernel.feature.resolver_fat/build/libs/autoFVT/build/verify/actual.xml
    //
    // When updating the expected results, copy the actual results to the root publish folder.

    public static final String REPO_PATH = "build/verify/repo.xml";

    public static final String ACTUAL_PATH = "build/verify/actual.xml";
    public static final String DURATIONS_PATH = "build/verify/durations.txt";

    public static final String EXPECTED_PATH = "publish/verify/expected.xml";

    public static File repoFile;
    public static String repoPath;

    public static File actualResultsFile;
    public static String actualResultsPath;
    public static File durationsFile;
    public static String durationsPath;
    public static VerifyData actualResults;

    public static File expectedResultsFile;
    public static String expectedResultsPath;
    public static VerifyData expectedResults;

    @BeforeClass
    public static void setupXML() throws Exception {
        repoFile = new File(REPO_PATH);
        repoPath = repoFile.getAbsolutePath();
        System.out.println("Verifying: Repo path [ " + repoPath + " ]");

        actualResultsFile = new File(ACTUAL_PATH);
        actualResultsPath = actualResultsFile.getAbsolutePath();
        System.out.println("Verifying: Actual results path [ " + actualResultsPath + " ]");

        durationsFile = new File(DURATIONS_PATH);
        durationsPath = durationsFile.getAbsolutePath();
        System.out.println("Verifying: Durations path [ " + durationsPath + " ]");

        expectedResultsFile = new File(EXPECTED_PATH);
        expectedResultsPath = expectedResultsFile.getAbsolutePath();
        System.out.println("Verifying: Expected results path [ " + expectedResultsPath + " ]");

        if (expectedResultsFile.exists()) {
            expectedResults = load("Expected Results", expectedResultsPath);
            System.out.println("Expected cases [ " + expectedResults.cases.size() + " ]");
        }
    }

    @Test
    public void verifyExpected() throws Exception {
        if (expectedResults == null) {
            Assert.fail("No expected [ " + expectedResultsPath + " ]");

        } else {
            Map<String, List<String>> errors = VerifyDelta.compare(expectedResults, expectedResults, !VerifyDelta.UPDATED_USED_KERNEL);
            if (!errors.isEmpty()) {
                String firstError = displayErrors("Expected vs Expected", errors);
                Assert.fail("Base comparison failure: First error: [ " + firstError + " ]");
            }
        }
    }

    @Test
    public void verifyResolution() throws Exception {
        ensureDirectory(repoFile, repoPath);
        ensureDirectory(actualResultsFile, actualResultsPath);
        ensureDirectory(durationsFile, durationsPath);

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar(VerifyEnv.REPO_PROPERTY_NAME, repoPath);
        server.addEnvVar(VerifyEnv.RESULTS_PROPERTY_NAME, actualResultsPath);
        server.addEnvVar(VerifyEnv.DURATIONS_PROPERTY_NAME, durationsPath);

        server.startServer();
        try {
            // EMPTY
        } finally {
            server.stopServerAlways(ALLOWED_ERRORS);
        }

        if (!actualResultsFile.exists()) {
            Assert.fail("No actual results [ " + actualResultsPath + " ]");
        }
        if (!durationsFile.exists()) {
            Assert.fail("No durations [ " + durationsPath + " ]");
        }

        if (expectedResults == null) {
            Assert.fail("No expected results [ " + expectedResultsPath + " ]");
        } else {
            actualResults = load("Actual", actualResultsPath);
            System.out.println("Actual cases [ " + actualResults.cases.size() + " ]");

            verify();
        }
    }

    protected void verify() throws Exception {
        System.out.println("Verifying: Expected [ " + expectedResultsPath + " ]; Actual [ " + actualResultsPath + " ]");

        Map<String, List<String>> errors = VerifyDelta.compare(expectedResults, actualResults, !VerifyDelta.UPDATED_USED_KERNEL);

        if (errors.isEmpty()) {
            System.out.println("All cases pass");
        } else {
            String firstError = displayErrors("Expected vs Actual", errors);
            Assert.fail("Incorrect resolutions detected: First error: [ " + firstError + " ].");
        }
    }

    protected String displayErrors(String title, Map<String, List<String>> errors) {
        final AtomicReference<String> firstError = new AtomicReference<>();

        System.out.println(title);
        errors.forEach((String caseName, List<String> caseErrors) -> {
            System.out.println("Case errors [ " + caseName + " ]:");
            for (String caseError : caseErrors) {
                System.out.println("  [ " + caseError + " ]");

                if (firstError.get() == null) {
                    firstError.set(caseName + " : " + caseError);
                }
            }
        });

        return firstError.get();
    }

    protected static VerifyData load(String tag, String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            Assert.fail(tag + ": [ " + file.getAbsolutePath() + " ]: does not exist");
            return null;
        } else {
            return VerifyXML.read(file);
        }
    }

    private static void ensureDirectory(File targetFile, String targetPath) {
        File parentFile = targetFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
            if (!parentFile.exists()) {
                Assert.fail("Target parent could not be created [ " + targetPath + " ]");
            }
        }
        if (!parentFile.isDirectory()) {
            Assert.fail("Target parent is not a directory [ " + targetPath + " ]");
        }
    }
}
