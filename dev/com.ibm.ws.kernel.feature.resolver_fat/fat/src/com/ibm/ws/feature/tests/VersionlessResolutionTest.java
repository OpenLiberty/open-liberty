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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
// import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    // open-liberty/dev/com.ibm.ws.kernel.feature.resolver_fat/output/libs/autoFVT/publish/verify/expected.xml
    //
    // Actual results:
    //
    // open-liberty/dev/com.ibm.ws.kernel.feature.resolver_fat/build/libs/autoFVT/output/verify/actual.xml
    //
    // When updating the expected results, copy the actual results to the root publish folder.

    public static final String REPO_PATH = "output/verify/repository.xml";

    public static final String SINGLETON_ACTUAL_PATH = "output/verify/singleton_actual.xml";
    public static final String SINGLETON_DURATIONS_PATH = "output/verify/singleton_durations.txt";
    public static final String SINGLETON_EXPECTED_PATH = "publish/verify/singleton_expected.xml";

    public static final String SERVLET_ACTUAL_PATH = "output/verify/servlet_actual.xml";
    public static final String SERVLET_DURATIONS_PATH = "output/verify/servlet_durations.txt";
    public static final String SERVLET_EXPECTED_PATH = "publish/verify/servlet_expected.xml";

    public static File repoFile;
    public static String repoPath;

    public static File singletonActualResultsFile;
    public static String singletonActualResultsPath;
    public static VerifyData singletonActualResults;

    public static File singletonDurationsFile;
    public static String singletonDurationsPath;

    public static File singletonExpectedResultsFile;
    public static String singletonExpectedResultsPath;
    public static VerifyData singletonExpectedResults;

    public static File servletActualResultsFile;
    public static String servletActualResultsPath;
    public static VerifyData servletActualResults;

    public static File servletDurationsFile;
    public static String servletDurationsPath;

    public static File servletExpectedResultsFile;
    public static String servletExpectedResultsPath;
    public static VerifyData servletExpectedResults;

    public static final boolean RUN_SINGLETON = false;
    public static final boolean RUN_SERVLET = true;

    @BeforeClass
    public static void setupXML() throws Exception {
        System.out.println("Results paths:");

        repoFile = new File(REPO_PATH);
        repoPath = repoFile.getAbsolutePath();
        System.out.println("Repo path [ " + repoPath + " ]");

        if (RUN_SINGLETON) {
            singletonActualResultsFile = new File(SINGLETON_ACTUAL_PATH);
            singletonActualResultsPath = singletonActualResultsFile.getAbsolutePath();
            System.out.println("Actual singleton results path [ " + singletonActualResultsPath + " ]");

            singletonDurationsFile = new File(SINGLETON_DURATIONS_PATH);
            singletonDurationsPath = singletonDurationsFile.getAbsolutePath();
            System.out.println("Singleton durations path [ " + singletonDurationsPath + " ]");

            singletonExpectedResultsFile = new File(SINGLETON_EXPECTED_PATH);
            singletonExpectedResultsPath = singletonExpectedResultsFile.getAbsolutePath();
            System.out.println("Expected singleton results path [ " + singletonExpectedResultsPath + " ]");

            if (singletonExpectedResultsFile.exists()) {
                singletonExpectedResults = load("Expected Results", singletonExpectedResultsPath);
                System.out.println("Expected singleton cases [ " + singletonExpectedResults.cases.size() + " ]");
            }
        } else {
            System.out.println("Singleton processing disabled");
        }

        if (RUN_SERVLET) {
            servletActualResultsFile = new File(SERVLET_ACTUAL_PATH);
            servletActualResultsPath = servletActualResultsFile.getAbsolutePath();
            System.out.println("Actual servlet results path [ " + servletActualResultsPath + " ]");

            servletDurationsFile = new File(SERVLET_DURATIONS_PATH);
            servletDurationsPath = servletDurationsFile.getAbsolutePath();
            System.out.println("Servlet durations path [ " + servletDurationsPath + " ]");

            servletExpectedResultsFile = new File(SERVLET_EXPECTED_PATH);
            servletExpectedResultsPath = servletExpectedResultsFile.getAbsolutePath();
            System.out.println("Expected servlet results path [ " + servletExpectedResultsPath + " ]");

            if (servletExpectedResultsFile.exists()) {
                servletExpectedResults = load("Expected Results", servletExpectedResultsPath);
                System.out.println("Expected servlet cases [ " + servletExpectedResults.cases.size() + " ]");
            }
        } else {
            System.out.println("Servlet processing disabled");
        }
    }

    @Test
    public void verifyResolution() throws Exception {
        ensureDirectory(repoFile, repoPath);
        if (RUN_SINGLETON) {
            ensureDirectory(singletonActualResultsFile, singletonActualResultsPath);
            ensureDirectory(singletonDurationsFile, singletonDurationsPath);
        }
        if (RUN_SINGLETON) {
            ensureDirectory(servletActualResultsFile, servletActualResultsPath);
            ensureDirectory(servletDurationsFile, servletDurationsPath);
        }

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar(VerifyEnv.REPO_PROPERTY_NAME, repoPath);
        if (RUN_SINGLETON) {
            server.addEnvVar(VerifyEnv.RESULTS_SINGLETON_PROPERTY_NAME, singletonActualResultsPath);
            server.addEnvVar(VerifyEnv.DURATIONS_SINGLETON_PROPERTY_NAME, singletonDurationsPath);
        }
        if (RUN_SERVLET) {
            server.addEnvVar(VerifyEnv.RESULTS_SERVLET_PROPERTY_NAME, servletActualResultsPath);
            server.addEnvVar(VerifyEnv.DURATIONS_SERVLET_PROPERTY_NAME, servletDurationsPath);
        }

        server.startServer();
        try {
            // EMPTY
        } finally {
            server.stopServerAlways(ALLOWED_ERRORS);
        }

        List<String> singletonFailures = new ArrayList<>();
        Map<String, List<String>> singletonErrors = Collections.emptyMap();

        if (RUN_SINGLETON) {
            if (!singletonDurationsFile.exists()) {
                singletonFailures.add("No singleton durations [ " + singletonDurationsPath + " ]");
            }
            if (!singletonActualResultsFile.exists()) {
                singletonFailures.add("No singleton actual results [ " + singletonActualResultsPath + " ]");
            } else {
                if (singletonExpectedResults == null) {
                    singletonFailures.add("No singleton expected results [ " + singletonExpectedResultsPath + " ]");
                } else {
                    singletonActualResults = load("Actual", singletonActualResultsPath);
                    System.out.println("Actual singleton cases [ " + singletonActualResults.cases.size() + " ]");

                    System.out.println("Expected [ " + singletonExpectedResultsPath + " ]; Actual [ " + singletonActualResultsPath + " ]");
                    singletonErrors = VerifyDelta.compare(singletonExpectedResults, singletonActualResults, !VerifyDelta.UPDATED_USED_KERNEL);
                }
            }
        }

        List<String> servletFailures = new ArrayList<>();
        Map<String, List<String>> servletErrors = Collections.emptyMap();

        if (RUN_SERVLET) {
            if (!servletDurationsFile.exists()) {
                servletFailures.add("No servlet durations [ " + servletDurationsPath + " ]");
            }
            if (!servletActualResultsFile.exists()) {
                servletFailures.add("No servlet actual results [ " + servletActualResultsPath + " ]");
            } else {
                if (servletExpectedResults == null) {
                    servletFailures.add("No servlet expected results [ " + servletExpectedResultsPath + " ]");
                } else {
                    servletActualResults = load("Actual", servletActualResultsPath);
                    System.out.println("Actual servlet cases [ " + servletActualResults.cases.size() + " ]");

                    System.out.println("Expected [ " + servletExpectedResultsPath + " ]; Actual [ " + servletActualResultsPath + " ]");
                    servletErrors = VerifyDelta.compare(servletExpectedResults, servletActualResults, !VerifyDelta.UPDATED_USED_KERNEL);
                }
            }
        }

        if (displayErrors("Singleton generation errors", singletonFailures) ||
            displayErrors("Singleton case errors", singletonErrors) ||
            displayErrors("Servlet generation errors", servletFailures) ||
            displayErrors("Servlet case errors", servletErrors)) {

            Assert.fail("Resolution failure");

        } else {
            System.out.println("Resolution success");
        }
    }

    protected boolean displayErrors(String title, List<String> errors) {
        if (errors.isEmpty()) {
            return false;
        }
        System.out.println(title);
        for (String error : errors) {
            System.out.println("  " + error);
        }
        return true;
    }

    protected boolean displayErrors(String title, Map<String, List<String>> errors) {
        if (errors.isEmpty()) {
            return false;
        }

        System.out.println(title);
        errors.forEach((String caseName, List<String> caseErrors) -> {
            System.out.println("Case errors [ " + caseName + " ]:");
            for (String caseError : caseErrors) {
                System.out.println("  [ " + caseError + " ]");
            }
        });

        return true;
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
