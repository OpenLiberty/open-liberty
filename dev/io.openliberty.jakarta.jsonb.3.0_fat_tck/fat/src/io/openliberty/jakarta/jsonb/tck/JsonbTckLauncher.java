/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.jakarta.jsonb.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs the whole Jakarta JSON-B TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class JsonbTckLauncher {

    final static Map<String, String> additionalProps = new HashMap<>();

    //This is a standalone test no server needed
    @Server
    public static LibertyServer DONOTSTART;

    @BeforeClass
    public static void setUp() throws Exception {
        int javaSpecVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        // To work around the issue described in issue:
        // https://github.com/eclipse-ee4j/jsonb-api/issues/272
        if (javaSpecVersion >= 13) {
            additionalProps.put("java.locale.providers", "COMPAT");
        }

        // TODO Update if a service release of JSON-B tck is ever released
        additionalProps.put("jakarta.jsonb.tck.groupId", "io.openliberty.jakarta.json.bind");
        additionalProps.put("jakarta.jsonb.tck.version", "3.0.0-13102023");

        // Skip signature testing on Windows
        // So far as I can tell the signature test plugin is not supported on Windows
        // Opened an issue against jsonb tck https://github.com/eclipse-ee4j/jsonb-api/issues/327
        if (System.getProperty("os.name").contains("Windows")) {
            Log.info(JsonbTckLauncher.class, "setUp", "Skipping JSONB Signature Test on Windows");
            additionalProps.put("exclude.tests", "ee.jakarta.tck.json.bind.signaturetest.jsonb.JSONBSigTest.java");
        }
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchJsonb30TCK() throws Exception {

        String bucketName = "io.openliberty.jakarta.jsonb.3.0_fat_tck";
        String testName = this.getClass() + ":launchJsonb30TCK";
        Type type = Type.JAKARTA;
        String specName = "JSON Binding";
        TCKRunner.runTCK(DONOTSTART, bucketName, testName, type, specName, additionalProps);

    }
}