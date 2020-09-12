/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.feature.fat;

import java.io.File;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AlternateFeatureNamesTest {

    private static LibertyServer server;
    private static ServerConfiguration initialCfg;
    private static boolean isClosedLiberty;
    private static String OL_MISSING_FEAT_ERR;
    private static String CL_MISSING_FEAT_ERR;
    private static String ALT_NAME_ERR;
    private static String MISSING_FEAT_ERR;

    private static final Logger logger = Logger.getLogger(AlternateFeatureNamesTest.class.getName());

    @BeforeClass
    public static void staticSetup() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.alternate.names");
        initialCfg = server.getServerConfiguration();
        isClosedLiberty = new File(server.getInstallRoot(), "lib/versions/WebSphereApplicationServer.properties").exists();
        //CWWKF0001E: A feature definition could not be found for {0} ...
        //CWWKF0042E: A feature definition cannot be found for the {} feature. Try running the command, bin/installUtility ...
        //CWWKF0045E: An existing feature definition, {1}, is a possible match for the feature definition {0} which was not found.
        ALT_NAME_ERR = "CWWKF0045E:";
        OL_MISSING_FEAT_ERR = "CWWKF0001E";
        CL_MISSING_FEAT_ERR = "CWWKF0042E";
        MISSING_FEAT_ERR = isClosedLiberty ? CL_MISSING_FEAT_ERR : OL_MISSING_FEAT_ERR;
        Log.info(AlternateFeatureNamesTest.class, "staticSetup", "isClosedLiberty: " + isClosedLiberty);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(MISSING_FEAT_ERR, ALT_NAME_ERR);
    }

    @Before
    public void setUp() throws Exception {
        server.stopServer(MISSING_FEAT_ERR, ALT_NAME_ERR);
    }

    @Test
    public void testAlternateNamesCleanStart() throws Exception {
        server.updateServerConfiguration(initialCfg);
        server.startServer("alternateFeatureNamesTest_console.log");

        Assert.assertNotNull("Expected missing feature error message for jaxrs-3.0",
                             server.waitForStringInLog(MISSING_FEAT_ERR + ".*jaxrs-3.0"));

        Assert.assertNotNull("Expected alternate feature name found error message for restfulWS-3.0",
                             server.waitForStringInLog(ALT_NAME_ERR + ".*restfulWS-3.0.*jaxrs-3.0"));

        //Test live config update
        ServerConfiguration cfg = AlternateFeatureNamesTest.initialCfg.clone();
        cfg.getFeatureManager().getFeatures().add("jca-2.0");
        server.updateServerConfiguration(cfg);

        Assert.assertNotNull("Expected missing feature error message for jca-2.0",
                             server.waitForStringInLog(MISSING_FEAT_ERR + ".*jca-2.0"));

        Assert.assertNotNull("Expected an alternate feature name error message for connectors-2.0",
                             server.waitForStringInLog(ALT_NAME_ERR + ".*connectors-2.0.*jca-2.0"));
    }

    @Test
    public void testAlternateNamesFromCachedFeatures() throws Exception {
        server.updateServerConfiguration(initialCfg);
        ServerConfiguration cfg = AlternateFeatureNamesTest.initialCfg.clone();
        cfg.getFeatureManager().getFeatures().add("jca-2.0");
        server.updateServerConfiguration(cfg);

        server.startServer("alternateFeatureNamesTest_console.log", /* cleanStart */ false);
        Assert.assertNotNull("Expected missing feature error message for jaxrs-3.0",
                             server.waitForStringInLog(MISSING_FEAT_ERR + ".*jaxrs-3.0"));

        Assert.assertNotNull("Expected alternate feature name found error message for restfulWS-3.0",
                             server.waitForStringInLog(ALT_NAME_ERR + ".*restfulWS-3.0.*jaxrs-3.0"));

        Assert.assertNotNull("Expected missing feature error message for jca-2.0",
                             server.waitForStringInLog(MISSING_FEAT_ERR + ".*jca-2.0"));

        Assert.assertNotNull("Expected an alternate feature name error message for connectors-2.0",
                             server.waitForStringInLog(ALT_NAME_ERR + ".*connectors-2.0.*jca-2.0"));

    }
}
