/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.tests.spec20.tests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class ValidateJPAFeatureTest extends JPAFATServletClient {

    @Server("JPA20Server")
    public static LibertyServer server;

    private static long timestart = 0;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, AbstractFATSuite.JAXB_PERMS);
        bannerStart(ValidateJPAFeatureTest.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        server.addEnvVar("repeat_phase", AbstractFATSuite.repeatPhase);

        server.startServer();
    }

    @Test
    public void testJPAFeatureMatchesRun() throws Exception {
        Assert.assertNotNull("AbstractFATSuite.repeatPhase is not set as expected", AbstractFATSuite.repeatPhase);

        List<String> installedFeaturesRaw = server.findStringsInLogs("CWWKF0012I: .*");
        if (installedFeaturesRaw == null || installedFeaturesRaw.size() == 0)
            return;
        Set<String> installedFeatures = new HashSet<String>();
        for (String f : installedFeaturesRaw)
            for (String installedFeature : f.substring(0, f.lastIndexOf(']')).substring(f.lastIndexOf('[') + 1).split(","))
                installedFeatures.add(installedFeature.trim().toLowerCase());
        System.out.println("installedFeatures: " + installedFeatures);

        if (AbstractFATSuite.repeatPhase.contains("20")) {
            Assert.assertTrue("Expected jpa-2.0 feature to be installed", installedFeatures.contains("jpa-2.0"));
        } else if (AbstractFATSuite.repeatPhase.contains("21")) {
            Assert.assertTrue("Expected jpa-2.1 or jpaContainer-2.1 feature to be installed",
                              (installedFeatures.contains("jpa-2.1") || installedFeatures.contains("jpacontainer-2.1")));
        } else if (AbstractFATSuite.repeatPhase.contains("22")) {
            Assert.assertTrue("Expected jpa-2.2 or jpaContainer-2.2 feature to be installed",
                              (installedFeatures.contains("jpa-2.2") || installedFeatures.contains("jpacontainer-2.2")));
        } else if (AbstractFATSuite.repeatPhase.contains("30")) {
            Assert.assertTrue("Expected persistence-3.0 or persistenceContainer-3.0 feature to be installed",
                              (installedFeatures.contains("persistence-3.0") || installedFeatures.contains("persistencecontainer-3.0")));
        } else if (AbstractFATSuite.repeatPhase.contains("31")) {
            Assert.assertTrue("Expected persistence-3.1 or persistenceContainer-3.1 feature to be installed",
                              (installedFeatures.contains("persistence-3.1") || installedFeatures.contains("persistencecontainer-3.1")));
        } else if (AbstractFATSuite.repeatPhase.contains("32")) {
            Assert.assertTrue("Expected persistence-3.2 or persistenceContainer-3.2 feature to be installed",
                              (installedFeatures.contains("persistence-3.2") || installedFeatures.contains("persistencecontainer-3.2")));
        } else {
            Assert.fail("Unexpected AbstractFATSuite.repeatPhase found: " + AbstractFATSuite.repeatPhase);
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            bannerEnd(ValidateJPAFeatureTest.class, timestart);
        }
    }
}
