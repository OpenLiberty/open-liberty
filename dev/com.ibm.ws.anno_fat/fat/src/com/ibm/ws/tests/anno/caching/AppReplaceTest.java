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
 * Validate update of an EAR by updating a WAR and an EJB jar.
 *
 * Update the WAR by changing the fully qualified name of the single
 * servlet provided by the WAR.
 *
 * Update the EJB jar by changing the fully qualified name of the single
 * EJB provided by the EJB jar.
 *
 * Specifically, replace the class <code>com.ibm.ws.tests.anno.servlets.one.TestReplaceServlet</code>
 * with  <code>com.ibm.ws.tests.anno.servlets.two.TestReplaceServlet</code>,
 * and replace the class <code>com.ibm.ws.tests.anno.ejbs.one.TestReplaceEJB</code>
 * with <code>com.ibm.ws.tests.anno.ejbs.two.TestReplaceEJB</code>.
 */
@RunWith(FATRunner.class)
public class AppReplaceTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("appReplaceTestServer");

    @AfterClass
    public static void tearDown() throws Exception {
        if ( server.isStarted() ) {
            server.stopServer();
        }
    }

    /**
     * Create an EAR which contains "TestReplace.war" and "TestReplaceEJB.jar".
     * Conditionally include classes in the WAR and EJB jar according to the
     * specified package suffix.
     * 
     * @param packageSuffix The suffix of the qualified package name which is
     *    included in the packaged WAR and EJB jar files.
     *
     * @return The packaged enterprise application archive, EAR file.
     */
    private EnterpriseArchive getEAR(String packageSuffix) {
        WebArchive warArchive = ShrinkWrap.create(WebArchive.class, "TestReplace.war");
        warArchive.addPackage("com.ibm.ws.tests.anno.servlets." + packageSuffix);

        JavaArchive jarArchive = ShrinkWrap.create(JavaArchive.class, "TestReplaceEJB.jar");
        jarArchive.addPackage("com.ibm.ws.tests.anno.ejbs." + packageSuffix);

        EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, "TestReplace.ear");
        earArchive.addAsModule(warArchive);
        earArchive.addAsModule(jarArchive);

        return earArchive;
    }

    /**
     * Test that the cache is updated for the replacement of an enterprise application.
     * 
     * Update the application by changing the qualified name of the servlet and EJB in
     * the application.
     */
    @Test
    public void testEARwithEJBJar() throws Exception {
        EnterpriseArchive earArchiveOne = getEAR("one");

        ShrinkHelper.exportDropinAppToServer(server, earArchiveOne, ShrinkHelper.DeployOptions.OVERWRITE);
        server.addInstalledAppForValidation("TestReplace");

        server.startServer();
        assertNotNull( server.waitForStringInLog("CWWKZ0001I:.*TestReplace.*") );
        server.setMarkToEndOfLog();

        server.removeDropinsApplications("TestReplace.ear");
        assertNotNull( server.waitForStringInLog("CWWKZ0009I:.*TestReplace.*") );
        server.setMarkToEndOfLog();

        EnterpriseArchive earArchiveTwo = getEAR("two");
        ShrinkHelper.exportDropinAppToServer(server, earArchiveTwo, ShrinkHelper.DeployOptions.OVERWRITE);
        assertNotNull( server.waitForStringInLog("CWWKZ0001I:.*TestReplace.*") );
        server.setMarkToEndOfLog();
    }
}
