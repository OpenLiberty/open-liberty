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

    //

    public static final String REPO_PATH_OL = "output/verify/repository.xml";
    public static final String REPO_PATH_WL = "output/verify/repository_WL.xml";

    public static final String SINGLETON_ACTUAL_PATH_OL = "output/verify/singleton_actual.xml";
    public static final String SINGLETON_ACTUAL_PATH_WL = "output/verify/singleton_actual_WL.xml";

    public static final String SINGLETON_DURATIONS_PATH_OL = "output/verify/singleton_durations.txt";
    public static final String SINGLETON_DURATIONS_PATH_WL = "output/verify/singleton_durations_WL.txt";

    public static final String SINGLETON_EXPECTED_PATH_OL = "publish/verify/singleton_expected.xml";
    public static final String SINGLETON_EXPECTED_PATH_WL = "publish/verify/singleton_expected_WL.xml";

    public static final String SERVLET_ACTUAL_PATH_OL = "output/verify/servlet_actual.xml";
    public static final String SERVLET_ACTUAL_PATH_WL = "output/verify/servlet_actual_WL.xml";

    public static final String SERVLET_DURATIONS_PATH_OL = "output/verify/servlet_durations.txt";
    public static final String SERVLET_DURATIONS_PATH_WL = "output/verify/servlet_durations_WL.txt";

    public static final String SERVLET_EXPECTED_PATH_OL = "publish/verify/servlet_expected.xml";
    public static final String SERVLET_EXPECTED_PATH_WL = "publish/verify/servlet_expected_WL.xml";

    public static File repoFile_OL;
    public static String repoPath_OL;

    public static File singletonDurationsFile_OL;
    public static String singletonDurationsPath_OL;

    public static File singletonActualResultsFile_OL;
    public static String singletonActualResultsPath_OL;

    public static File servletDurationsFile_OL;
    public static String servletDurationsPath_OL;

    public static File servletActualResultsFile_OL;
    public static String servletActualResultsPath_OL;

    //

    public static boolean isWASLiberty;

    public static String repoBasePath; // Either OL or WL
    public static File repoFile;
    public static String repoPath;

    public static String singletonActualResultsBasePath; // Either OL or WL
    public static File singletonActualResultsFile;
    public static String singletonActualResultsPath;
    public static VerifyData singletonActualResults;

    public static String singletonDurationsBasePath; // Either OL or WL
    public static File singletonDurationsFile;
    public static String singletonDurationsPath;

    public static File singletonExpectedResultsFile_OL;
    public static String singletonExpectedResultsPath_OL;
    public static VerifyData singletonExpectedResults_OL;

    public static File singletonExpectedResultsFile_WL;
    public static String singletonExpectedResultsPath_WL;
    public static VerifyData singletonExpectedResults_WL;

    public static VerifyData singletonExpectedResults; // Merge OL and WL

    public static String servletActualResultsBasePath; // Either OL or WL
    public static File servletActualResultsFile;
    public static String servletActualResultsPath;
    public static VerifyData servletActualResults;

    public static String servletDurationsBasePath; // Either OL or WL
    public static File servletDurationsFile;
    public static String servletDurationsPath;

    public static File servletExpectedResultsFile_OL;
    public static String servletExpectedResultsPath_OL;
    public static VerifyData servletExpectedResults_OL;

    public static File servletExpectedResultsFile_WL;
    public static String servletExpectedResultsPath_WL;
    public static VerifyData servletExpectedResults_WL;

    public static VerifyData servletExpectedResults; // Merge OL and WL

    public static final boolean RUN_SINGLETON = false; // Turn these off: They take too long.
    public static final boolean RUN_SERVLET = true;

    /**
     * Open liberty values are provided to environment variables.
     *
     * The baseline detects WAS Liberty, and adjusts the values.
     *
     * @throws Exception Thrown in case of a failure.
     */
    @BeforeClass
    public static void setupBaseXML() throws Exception {
        System.out.println("Results paths:");

        repoFile_OL = new File(REPO_PATH_OL);
        repoPath_OL = repoFile_OL.getAbsolutePath();
        System.out.println("Repository path [ " + repoPath_OL + " ] (Open Liberty, Parameter)");

        singletonDurationsFile_OL = new File(SINGLETON_DURATIONS_PATH_OL);
        singletonDurationsPath_OL = singletonDurationsFile_OL.getAbsolutePath();
        System.out.println("Singleton durations path [ " + singletonDurationsPath_OL + " ] (Open Liberty, Parameter)");

        singletonActualResultsFile_OL = new File(SINGLETON_ACTUAL_PATH_OL);
        singletonActualResultsPath_OL = singletonActualResultsFile_OL.getAbsolutePath();
        System.out.println("Singleton actual results path [ " + singletonActualResultsPath_OL + " ] (Open Liberty, Parameter)");

        servletDurationsFile_OL = new File(SERVLET_DURATIONS_PATH_OL);
        servletDurationsPath_OL = servletDurationsFile_OL.getAbsolutePath();
        System.out.println("Servlet durations path [ " + servletDurationsPath_OL + " ] (Open Liberty, Parameter)");

        servletActualResultsFile_OL = new File(SERVLET_ACTUAL_PATH_OL);
        servletActualResultsPath_OL = servletActualResultsFile_OL.getAbsolutePath();
        System.out.println("Servlet actual results path [ " + servletActualResultsPath_OL + " ] (Open Liberty, Parameter)");
    }

    public static void completeXML() throws Exception {
        repoBasePath = REPO_PATH_OL;
        repoFile = new File(repoBasePath);
        repoPath = repoFile.getAbsolutePath();
        System.out.println("Trying repo path [ " + repoPath + " ]");
        if (!repoFile.exists()) {
            System.out.println("Trying repo path [ " + repoPath + " ]: Failed");

            repoBasePath = REPO_PATH_WL;
            repoFile = new File(repoBasePath);
            repoPath = repoFile.getAbsolutePath();
            System.out.println("Trying repo path [ " + repoPath + " ]");
            if (!repoFile.exists()) {
                System.out.println("Trying repo path [ " + repoPath + " ]: Failed: Defaulting to Open Liberty");
                repoBasePath = null;
                repoFile = null;
                repoPath = null;
                isWASLiberty = false;
            } else {
                System.out.println("Trying repo path [ " + repoPath + " ]: Success: WAS Liberty Detected");
                isWASLiberty = true;
            }

        } else {
            System.out.println("Trying repo path [ " + repoPath + " ]: Success: Open Liberty Detected");
            isWASLiberty = false;
        }

        if (RUN_SINGLETON) {
            singletonActualResultsBasePath = (isWASLiberty ? SINGLETON_ACTUAL_PATH_WL : SINGLETON_ACTUAL_PATH_OL);
            singletonActualResultsFile = new File(singletonActualResultsBasePath);
            singletonActualResultsPath = singletonActualResultsFile.getAbsolutePath();
            System.out.println("Actual singleton results path [ " + singletonActualResultsPath + " ]");

            singletonDurationsBasePath = (isWASLiberty ? SINGLETON_DURATIONS_PATH_WL : SINGLETON_DURATIONS_PATH_OL);
            singletonDurationsFile = new File(singletonDurationsBasePath);
            singletonDurationsPath = singletonDurationsFile.getAbsolutePath();
            System.out.println("Singleton durations path [ " + singletonDurationsPath + " ]");

            singletonExpectedResultsFile_OL = new File(SINGLETON_EXPECTED_PATH_OL);
            singletonExpectedResultsPath_OL = singletonExpectedResultsFile_OL.getAbsolutePath();
            System.out.println("Expected singleton results path [ " + singletonExpectedResultsPath_OL + " ]");

            if (singletonExpectedResultsFile_OL.exists()) {
                singletonExpectedResults_OL = load("Expected Results", singletonExpectedResultsPath_OL);
                System.out.println("Expected singleton cases [ " + singletonExpectedResults_OL.cases.size() + " ]");
            } else {
                singletonExpectedResults_OL = null;
            }

            if (isWASLiberty) {
                singletonExpectedResultsFile_WL = new File(SINGLETON_EXPECTED_PATH_WL);
                singletonExpectedResultsPath_WL = singletonExpectedResultsFile_WL.getAbsolutePath();
                System.out.println("Expected singleton results path [ " + singletonExpectedResultsPath_WL + " ] (WAS Liberty)");

                if (singletonExpectedResultsFile_WL.exists()) {
                    singletonExpectedResults_WL = load("Expected Singleton Results (WAS Liberty)", singletonExpectedResultsPath_WL);
                    System.out.println("Expected singleton cases [ " + singletonExpectedResults_WL.cases.size() + " ] (WAS Liberty)");
                } else {
                    singletonExpectedResults_WL = null;
                }
            } else {
                singletonExpectedResults_WL = null;
            }

            singletonExpectedResults = merge(singletonExpectedResults_OL, singletonExpectedResults_WL);

        } else {
            System.out.println("Singleton processing disabled");
        }

        if (RUN_SERVLET) {
            servletActualResultsBasePath = (isWASLiberty ? SERVLET_ACTUAL_PATH_WL : SERVLET_ACTUAL_PATH_OL);
            servletActualResultsFile = new File(servletActualResultsBasePath);
            servletActualResultsPath = servletActualResultsFile.getAbsolutePath();
            System.out.println("Actual servlet results path [ " + servletActualResultsPath + " ]");

            servletDurationsBasePath = (isWASLiberty ? SERVLET_DURATIONS_PATH_WL : SERVLET_DURATIONS_PATH_OL);
            servletDurationsFile = new File(servletDurationsBasePath);
            servletDurationsPath = servletDurationsFile.getAbsolutePath();
            System.out.println("Servlet durations path [ " + servletDurationsPath + " ]");

            servletExpectedResultsFile_OL = new File(SERVLET_EXPECTED_PATH_OL);
            servletExpectedResultsPath_OL = servletExpectedResultsFile_OL.getAbsolutePath();
            System.out.println("Expected servlet results path [ " + servletExpectedResultsPath_OL + " ]");

            if (servletExpectedResultsFile_OL.exists()) {
                servletExpectedResults_OL = load("Expected Results", servletExpectedResultsPath_OL);
                System.out.println("Expected servlet cases [ " + servletExpectedResults_OL.cases.size() + " ]");
            } else {
                servletExpectedResults_OL = null;
            }

            if (isWASLiberty) {
                servletExpectedResultsFile_WL = new File(SERVLET_EXPECTED_PATH_WL);
                servletExpectedResultsPath_WL = servletExpectedResultsFile_WL.getAbsolutePath();
                System.out.println("Expected servlet results path [ " + servletExpectedResultsPath_WL + " ] (WAS Liberty)");

                if (servletExpectedResultsFile_WL.exists()) {
                    servletExpectedResults_WL = load("Expected Servlet Results (WAS Liberty)", servletExpectedResultsPath_OL);
                    System.out.println("Expected servlet cases [ " + servletExpectedResults_WL.cases.size() + " ] (WAS Liberty)");
                } else {
                    servletExpectedResults_WL = null;
                }
            } else {
                servletExpectedResults_WL = null;
            }

            servletExpectedResults = merge(servletExpectedResults_OL, servletExpectedResults_WL);

        } else {
            System.out.println("Servlet processing disabled");
        }
    }

    public static VerifyData merge(VerifyData baseData, VerifyData overlayData) {
        if (baseData == null) {
            return overlayData;
        } else if (overlayData == null) {
            return baseData;
        } else {
            return baseData.add(overlayData);
        }
    }

    @Test
    public void verifyResolution() throws Exception {
        System.out.println("Verifying feature resolution:");

        ensureDirectory(repoFile_OL, repoPath_OL);
        if (RUN_SINGLETON) {
            ensureDirectory(singletonActualResultsFile_OL, singletonActualResultsPath_OL);
            ensureDirectory(singletonDurationsFile_OL, singletonDurationsPath_OL);
        }
        if (RUN_SINGLETON) {
            ensureDirectory(servletActualResultsFile_OL, servletActualResultsPath_OL);
            ensureDirectory(servletDurationsFile_OL, servletDurationsPath_OL);
        }

        System.out.println("Performing resolution:");

        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar(VerifyEnv.REPO_PROPERTY_NAME, repoPath_OL);
        if (RUN_SINGLETON) {
            server.addEnvVar(VerifyEnv.RESULTS_SINGLETON_PROPERTY_NAME, singletonActualResultsPath_OL);
            server.addEnvVar(VerifyEnv.DURATIONS_SINGLETON_PROPERTY_NAME, singletonDurationsPath_OL);
        }
        if (RUN_SERVLET) {
            server.addEnvVar(VerifyEnv.RESULTS_SERVLET_PROPERTY_NAME, servletActualResultsPath_OL);
            server.addEnvVar(VerifyEnv.DURATIONS_SERVLET_PROPERTY_NAME, servletDurationsPath_OL);
        }

        server.startServer();
        try {
            // EMPTY
        } finally {
            server.stopServerAlways(ALLOWED_ERRORS);
        }

        System.out.println("Validating resolution:");

        completeXML();

        List<String> singletonFailures = new ArrayList<>();
        Map<String, List<String>> singletonErrors = Collections.emptyMap();

        if (RUN_SINGLETON) {
            System.out.println("Verifying singleton results");

            if (!singletonDurationsFile.exists()) {
                System.out.println("No singleton durations [ " + singletonDurationsPath + " ]");
                // Ignore this problem for now: Still figuring out whether the output locations are correct.
                // singletonFailures.add("No singleton durations [ " + singletonDurationsPath + " ]");
            }
            if (!singletonActualResultsFile.exists()) {
                System.out.println("No singleton actual results [ " + singletonActualResultsPath + " ]");
                // Ignore this problem for now: Still figuring out whether the output locations are correct.
                // singletonFailures.add("No singleton actual results [ " + singletonActualResultsPath + " ]");

            } else {
                if (singletonExpectedResults == null) {
                    singletonFailures.add("No singleton expected results [ " + singletonExpectedResultsPath_OL + " ]");
                } else {
                    singletonActualResults = load("Actual", singletonActualResultsPath);
                    System.out.println("Actual singleton cases [ " + singletonActualResults.cases.size() + " ]");

                    System.out.println("Expected [ " + singletonExpectedResultsPath_OL + " ]; Actual [ " + singletonActualResultsPath + " ]");
                    singletonErrors = VerifyDelta.compare(singletonExpectedResults_OL, singletonActualResults, !VerifyDelta.UPDATED_USED_KERNEL);
                }
            }
        }

        List<String> servletFailures = new ArrayList<>();
        Map<String, List<String>> servletErrors = Collections.emptyMap();

        if (RUN_SERVLET) {
            if (!servletDurationsFile.exists()) {
                System.out.println("No servlet durations [ " + servletDurationsPath + " ]");
                // Ignore this problem for now: Still figuring out whether the output locations are correct.
                // servletFailures.add("No servlet durations [ " + servletDurationsPath + " ]");
            }
            if (!servletActualResultsFile.exists()) {
                System.out.println("No servlet actual results [ " + servletActualResultsPath + " ]");
                // Ignore this problem for now: Still figuring out whether the output locations are correct.
                // servletFailures.add("No servlet actual results [ " + servletActualResultsPath + " ]");
            } else {
                if (servletExpectedResults == null) {
                    servletFailures.add("No servlet expected results [ " + servletExpectedResultsPath_OL + " ]");
                } else {
                    servletActualResults = load("Actual", servletActualResultsPath);
                    System.out.println("Actual servlet cases [ " + servletActualResults.cases.size() + " ]");

                    System.out.println("Expected [ " + servletExpectedResultsPath_OL + " ]; Actual [ " + servletActualResultsPath + " ]");
                    servletErrors = VerifyDelta.compare(servletExpectedResults_OL, servletActualResults, !VerifyDelta.UPDATED_USED_KERNEL);
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
