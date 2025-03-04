/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.tests.anno.caching;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test validates that if you start an application and stop it and replace the application with different content that it works
 */
@RunWith(FATRunner.class)
public class AppReplaceTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("appReplaceTestServer");

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    private EnterpriseArchive getEAR(String packageSuffix) {
        EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, "TestReplace.ear");

        WebArchive warArchive = ShrinkWrap.create(WebArchive.class, "TestReplace.war");
        warArchive.addPackage("com.ibm.ws.tests.anno.servlets." + packageSuffix);

        JavaArchive jarArchive = ShrinkWrap.create(JavaArchive.class, "TestReplaceEJB.jar");

        jarArchive.addPackage("com.ibm.ws.tests.anno.ejbs." + packageSuffix);
        earArchive.addAsModule(warArchive);
        earArchive.addAsModule(jarArchive);
        return earArchive;
    }

    @Test
    public void testEARwithEJBJar() throws Exception {

        EnterpriseArchive earArchive = getEAR("one");

        ShrinkHelper.exportDropinAppToServer(server, earArchive, ShrinkHelper.DeployOptions.OVERWRITE);

        server.addInstalledAppForValidation("TestReplace");
        server.startServer();
        server.removeDropinsApplications("TestReplace.ear");
        assertNotNull(server.waitForStringInLog("CWWKZ0009I:.*TestReplace.*"));

        earArchive = getEAR("two");

        server.setMarkToEndOfLog();

        ShrinkHelper.exportDropinAppToServer(server, earArchive, ShrinkHelper.DeployOptions.OVERWRITE);
        assertNotNull(server.waitForStringInLog("CWWKZ0001I:.*TestReplace.*"));
    }

}
