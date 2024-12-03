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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
// import org.junit.Assert;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

//@formatter:off
public class VersionlessTest {

    public static class TestCase {
        public final String description;

        public final String serverName;
        public final String preferredVersions;

        public final String[] expectedResolved;
        public static final String[] NO_FAILURES = null;
        public final String[] expectedFailed;
        public final String[] allowedErrors;

        public static final int UNSPECIFIED_JAVA_LEVEL = -1;
        public static final int JAVA_11 = 11;
        public final int minJavaLevel;

        public TestCase(String description, String serverName,
                        String preferredVersions,
                        String[] expectedResolved, String[] expectedFailed, String[] allowedErrors,
                        int minJavaLevel) {

            this.description = description;
            this.serverName = serverName;
            this.allowedErrors = allowedErrors;
            this.preferredVersions = preferredVersions;
            this.expectedResolved = expectedResolved;
            this.expectedFailed = expectedFailed;
            this.minJavaLevel = minJavaLevel;
        }

        public TestCase(String description, String serverName,
                        String preferredVersions,
                        String[] expectedResolved, String[] expectedFailed, String[] allowedErrors) {

            this(description,
                 serverName,
                 preferredVersions,
                 expectedResolved, expectedFailed, allowedErrors,
                 UNSPECIFIED_JAVA_LEVEL);
        }
    }

    public static int getJavaLevel() {
        return JavaInfo.JAVA_VERSION;
    }

    public static Collection<Object[]> filterData(TestCase[] testCases) {
        List<Object[]> data = new ArrayList<>(testCases.length);

        for (TestCase nextCase : testCases) {
            if ((nextCase.minJavaLevel == TestCase.UNSPECIFIED_JAVA_LEVEL) ||
                (JavaInfo.JAVA_VERSION >= nextCase.minJavaLevel)) {
                data.add(new Object[] { nextCase });
            }
        }

        return data;
    }

    //

    public VersionlessTest(TestCase testCase) {
        this.testCase = testCase;
    }

    private final TestCase testCase;

    public TestCase getTestCase() {
        return testCase;
    }

    //

    public static interface FailableConsumer<T, E extends Exception> {
        void accept(T value) throws E;
    }

    public void withServer(String serverName, String[] allowedErrors, String preferredVersions,
                           String[] expectedResolved,
                           FailableConsumer<LibertyServer, Exception> action) throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        if(!preferredVersions.isEmpty()){
            server.addEnvVar("PREFERRED_PLATFORM_VERSIONS", preferredVersions);
        }

        server.startServer();
        try {
            action.accept(server);
        } finally {
            server.stopServerAlways(allowedErrors);
        }
    }

    public static final String RESOLUTION_MSG = "CWWKF0012I";
    public static final String FAILED_RESOLUTION_MSG = "CWWKF0001E";

    public static final String TEST_MSGS = RESOLUTION_MSG + "|" + FAILED_RESOLUTION_MSG;

    public static void getResolution(LibertyServer server, List<String> resolutionMsgs, List<String> failureMsgs) throws Exception {
        List<String> msgs = server.findStringsInLogs(TEST_MSGS);

        for (String msg : msgs) {
            if (msg.contains(FAILED_RESOLUTION_MSG)) {
                failureMsgs.add(msg);
            } else {
                resolutionMsgs.add(msg);
            }
        }
    }

    public void test(TestCase testCase) throws Exception {
        test(testCase.serverName,
             testCase.allowedErrors,
             testCase.preferredVersions,
             testCase.expectedResolved,
             testCase.expectedFailed);
    }

    public void test(String serverName, String[] allowedErrors, String preferredVersions,
                     String[] expectedResolved, String[] expectedFailed) throws Exception {
        withServer(serverName, allowedErrors, preferredVersions, expectedResolved, (LibertyServer server) -> {

            Set<String> requestedFeatures = server.getServerConfiguration().getFeatureManager().getFeatures();

            System.out.println("Requested features:");
            for (String requested : requestedFeatures) {
                System.out.println("  [ " + requested + " ]");
            }

            System.out.println("Expected successfully resolved features:");
            for (String expected : expectedResolved) {
                System.out.println("  [ " + expected + " ]");
            }

            if ((expectedFailed != null) && (expectedFailed.length > 0)) {
                System.out.println("Expected unsuccessfully resolved features:");
                for (String expected : expectedFailed) {
                    System.out.println("  [ " + expected + " ]");
                }
            }

            List<String> resolutionMsgs = new ArrayList<>();
            List<String> failureMsgs = new ArrayList<>();

            getResolution(server, resolutionMsgs, failureMsgs);

            System.out.println("Resolution:");
            for (String resolutionMsg : resolutionMsgs) {
                System.out.println("  [ " + resolutionMsg + " ]");
            }

            System.out.println("Failed resolutions:");
            for (String failureMsg : failureMsgs) {
                System.out.println("  [ " + failureMsg + " ]");
            }

            List<String> errors = new ArrayList<>();

            if (resolutionMsgs.isEmpty()) {
                errors.add("Missing resolution message [ " + RESOLUTION_MSG + " ]");
            }

            for (String expected : expectedResolved) {
                if (!containsSubstring(resolutionMsgs, expected)) {
                    errors.add("Missing resolution [ " + expected + " ]");
                }
            }

            if ((expectedFailed != null) && (expectedFailed.length > 0)) {
                for (String expected : expectedFailed) {
                    if (!containsSubstring(failureMsgs, expected)) {
                        errors.add("Missing failed resolution [ " + expected + " ]");
                    }
                }
            }

            if (!errors.isEmpty()) {
                System.out.println("Server [ " + serverName + " ] resolution errors:");
                for (String error : errors) {
                    System.out.println("  " + error);
                }

                String failureMsg;
                if (errors.size() < 10) {
                    failureMsg = "Server [ " + serverName + " ] has [ " + errors.size() + " ] resolution errors: [ " + errors + " ]";
                } else {
                    failureMsg = "Server [ " + serverName + " ] has [ " + errors.size() + " ] resolution errors (see logs)";
                }

                Assert.fail(failureMsg);
            }
        });
    }

    private static boolean containsSubstring(List<String> lines, String target) {
        for (String line : lines) {
            if (line.contains(target)) {
                return true;
            }
        }
        return false;
    }
}
//@formatter:on
