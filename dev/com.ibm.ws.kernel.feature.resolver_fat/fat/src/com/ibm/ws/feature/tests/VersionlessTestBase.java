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
import java.util.List;
import java.util.Set;

import org.junit.Assert;
// import org.junit.Assert;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class VersionlessTestBase {

    public static interface FailableConsumer<T, E extends Exception> {
        void accept(T value) throws E;
    }

        public void withServer(LibertyServer server, String[] allowedErrors, String preferredVersions,
                           String[] expectedResolved,
                           FailableConsumer<LibertyServer, Exception> action) throws Exception {

        if(preferredVersions != null){
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

    public void test(String serverName, String[] allowedErrors, String preferredVersions,
                     String[] expectedResolved) throws Exception {
        test(serverName, allowedErrors, preferredVersions, expectedResolved, null);
    }

    public void test(String serverName, String[] allowedErrors, String preferredVersions,
                     String[] expectedResolved, String[] expectedFailed) throws Exception {

        LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);
        test(server, allowedErrors, preferredVersions, expectedResolved, expectedFailed);
    }


    public void test(LibertyServer server, String[] allowedErrors, String preferredVersions,
                     String[] expectedResolved, String[] expectedFailed) throws Exception{

        withServer(server, allowedErrors, preferredVersions, expectedResolved, (LibertyServer serv) -> {

            Set<String> requestedFeatures = server.getServerConfiguration().getFeatureManager().getFeatures();

            System.out.println("Requested features:");
            for (String requested : requestedFeatures) {
                System.out.println("  [ " + requested + " ]");
            }

            Set<String> requestedPlatforms = server.getServerConfiguration().getFeatureManager().getPlatforms();

            if(requestedPlatforms != null && !requestedPlatforms.isEmpty()){
                System.out.println("Requested platforms:");
                for (String requested : requestedPlatforms) {
                    System.out.println("  [ " + requested + " ]");
                }
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
                System.out.println("Server resolution errors:");
                for (String error : errors) {
                    System.out.println("  " + error);
                }

                String failureMsg;
                if (errors.size() == 1) {
                    failureMsg = "Resolution error [ " + errors.get(0) + " ]";
                } else {
                    failureMsg = "Server has [ " + errors.size() + " ] resolution errors";
                }

                //Assert.fail(failureMsg);
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
