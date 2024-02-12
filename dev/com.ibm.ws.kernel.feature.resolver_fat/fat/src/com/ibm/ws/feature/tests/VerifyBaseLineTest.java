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

import static com.ibm.ws.kernel.feature.resolver.util.VerifyData.VerifyCase;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.kernel.feature.resolver.util.VerifyData;
import com.ibm.ws.kernel.feature.resolver.util.VerifyUtil;
import com.ibm.ws.kernel.feature.resolver.util.VerifyXML;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class VerifyBaseLineTest {

    public static final String SERVER_NAME = "verifyBase";

    public static final String CASES_DATA_PATH = "verify/cases.xml";
    public static final String ACTUAL_OUTPUT_DATA_PATH = "verify/actualOutputs.xml";

    public static final String CASES_PROPERTY_NAME = "feature.verify.cases";
    public static final String ACTUAL_OUTPUT_PROPERTY_NAME = "feature.verify.actual";

    @Test
    public void verifyBaseTest() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.addEnvVar(CASES_PROPERTY_NAME, CASES_DATA_PATH);
        server.addEnvVar(ACTUAL_OUTPUT_PROPERTY_NAME, ACTUAL_OUTPUT_DATA_PATH);

        server.startServer();
        try {
            // EMPTY
        } finally {
            server.stopServer("CWWKF0001E");
        }

        verify(CASES_DATA_PATH, ACTUAL_OUTPUT_DATA_PATH);
    }

    protected void verify(String inputPath, String outputPath) throws Exception {
        System.out.println("Verifying [ " + inputPath + " ] against [ " + outputPath + " ]");

        VerifyData expectedCases = load("Input", inputPath);
        System.out.println("Expected cases [ " + expectedCases.cases.size() + " ]");

        VerifyData actualCases = load("Actual", outputPath);
        System.out.println("Actual cases [ " + actualCases.cases.size() + " ]");

        List<String> errors = VerifyUtil.compare(expectedCases, actualCases);

        if ( errors.isEmpty() ) {
            System.out.println("All cases pass");
        } else {
            for ( String error : errors ) {
                System.out.println("Error [ " + error + " ]");
            }
            Assert.fail("Incorrect resolutions detected.")
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
