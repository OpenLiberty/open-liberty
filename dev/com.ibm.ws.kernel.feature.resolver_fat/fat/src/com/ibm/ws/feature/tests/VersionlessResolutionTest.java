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

import org.junit.Assert;
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

    public static final String EXPECTED_PATH = "data/verify/expected.xml";
    public static final String ACTUAL_PATH = "build/verify/actual.xml";
    public static final String REPO_PATH = "build/verify/repo.xml";

    @Test
    public void verifyResolution() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar(VerifyEnv.REPO_PROPERTY_NAME, REPO_PATH);
        server.addEnvVar(VerifyEnv.RESULTS_PROPERTY_NAME, ACTUAL_PATH);

        server.startServer();
        try {
            // EMPTY
        } finally {
            server.stopServer("CWWKF0001E");
        }

        verify(EXPECTED_PATH, ACTUAL_PATH);
    }

    protected void verify(String expectedPath, String actualPath) throws Exception {
        System.out.println("Verifying: Expected [ " + expectedPath + " ]; Actual [ " + actualPath + " ]");

        VerifyData expectedCases = load("Input", expectedPath);
        System.out.println("Expected cases [ " + expectedCases.cases.size() + " ]");

        VerifyData actualCases = load("Actual", actualPath);
        System.out.println("Actual cases [ " + actualCases.cases.size() + " ]");

        Map<String, List<String>> errors = VerifyDelta.compare(expectedCases, actualCases);

        if (errors.isEmpty()) {
            System.out.println("All cases pass");
        } else {
            errors.forEach((String caseName, List<String> caseErrors) -> {
                System.out.println("Case errors [ " + caseName + " ]:");
                for (String caseError : caseErrors) {
                    System.out.println("  [ " + caseError + " ]");
                }
            });
            Assert.fail("Incorrect resolutions detected.");
        }
    }

    protected VerifyData load(String tag, String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            Assert.fail(tag + ": [ " + file.getAbsolutePath() + " ]: does not exist");
        }

        return VerifyXML.read(file);
    }
}
