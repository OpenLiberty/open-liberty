/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.validation.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.openapi.fat.FATSuite;

import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@SuiteClasses({
    OpenAPIValidationTestOne.class,
    OpenAPIValidationTestTwo.class,
    OpenAPIValidationTestThree.class,
    OpenAPIValidationTestFour.class,
    OpenAPIValidationTestFive.class,
    OpenAPIValidationTestSix.class
})
@RunWith(Suite.class)
public class ValidationSuite {

    private static final String SERVER_NAME = "validationServer";

    static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME, ValidationSuite.class);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKO1650E", "CWWKO1651W");
    }

    public static void deployApp(WebArchive archive) throws Exception {
        ShrinkHelper.exportDropinAppToServer(server, archive, DeployOptions.SERVER_ONLY);
    }

    public static void removeApp(String archiveName) throws Exception {
        String appName = archiveName.substring(0, archiveName.lastIndexOf("."));
        server.deleteFileFromLibertyServerRoot("dropins/" + archiveName); // Deletes the application file
        server.removeInstalledAppForValidation(appName); // Ensures that the application stops
    }

}
