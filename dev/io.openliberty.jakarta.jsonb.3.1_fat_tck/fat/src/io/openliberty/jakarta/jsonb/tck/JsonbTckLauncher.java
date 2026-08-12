/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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

import static componenttest.annotation.SkipIfSysProp.OS_IBMI;
import static componenttest.annotation.SkipIfSysProp.OS_ISERIES;
import static componenttest.annotation.SkipIfSysProp.OS_ZOS;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
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
@MinimumJavaLevel(javaLevel = 17)
@MaximumJavaLevel(javaLevel = 21) // TODO Fails on Java 23 due to updates to CLDR https://jdk.java.net/23/release-notes#JDK-8319990
@SkipIfSysProp({ OS_IBMI, OS_ISERIES, OS_ZOS }) // TODO remove once fixed https://github.com/jakartaee/jsonb-api/issues/348
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

        // Enable to run against Snapshot version
//        additionalProps.put("jakarta.jsonb.tck.groupId", "jakarta.json.bind");
//        additionalProps.put("jakarta.jsonb.tck.version", "3.1.0-SNAPSHOT");

        // Skip signature testing on Windows
        // So far as I can tell the signature test plugin is not supported on Windows
        // Opened an issue against jsonb tck https://github.com/eclipse-ee4j/jsonb-api/issues/327
//        if (System.getProperty("os.name").contains("Windows")) {
        if (true) { //TODO replace with windows only once API and TCK are at the same level
            Log.info(JsonbTckLauncher.class, "setUp", "Skipping JSONB Signature Test on Windows");
            additionalProps.put("exclude.tests", "ee.jakarta.tck.json.bind.signaturetest.jsonb.JSONBSigTest.java");
        }

        // Since the signature tests are run in standalone mode (not inside the container)
        // we need to ensure that the temporary file location the signature tests
        // use to read/write files to is accessible to the maven wrapper (.mvnw)
        additionalProps.put("java.io.tmpdir", PrivHelper.getProperty("java.io.tmpdir", "/tmp"));

    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchJsonb31TCK() throws Exception {
        TCKRunner.build(DONOTSTART, Type.JAKARTA, "jsonb")
                        .withPlatformVersion("12")
                        .withAdditionalMvnProps(additionalProps)
                        .runTCK();

    }
}