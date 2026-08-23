/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test duplicates AroundConstructEjbTest as a quick way to test what happens if there is more than one jar containing the EJB
 * but only one of them is an EJB module
 *
 * If all goes well CDI should favour the EJB module when looking for the EJBDescriptor so all works as normal, but since the old
 * code this is testing a fix to was non-deterministic (the order things come out of a set was involved) we spam 30 jars into the
 * ear lib to make the odds of a random false positive low
 */

@RunWith(FATRunner.class)
@Mode(FULL)
public class DuplicateArchiveEjbTest extends FATServletClient {

    public static final String DUPLICATE_APP_NAME = "duplicateJarApp";
    public static final String SERVER_NAME = "cdi12EJB32Server";

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.basic.EjbServlet.class, contextRoot = DUPLICATE_APP_NAME),
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "overloadedApplication.ear");

        WebArchive aroundConstructApp = ShrinkWrap.create(WebArchive.class,
                                                          DUPLICATE_APP_NAME + ".war")
                                                  .addClass(com.ibm.ws.cdi.ejb.apps.basic.EjbServlet.class);

        ear.addAsModules(createEjbLib("ejbModule.jar"), aroundConstructApp);
        for (int i = 0; i < 30; i++) {
            ear.addAsLibraries(createEjbLib("ejbModule" + i + ".jar"));
        }

        ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    private static JavaArchive createEjbLib(String archiveName) {
        return ShrinkWrap.create(JavaArchive.class,
                                 archiveName)
                         .addClass(com.ibm.ws.cdi.ejb.apps.basic.Ejb.class);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWOWB2001E", "CNTR0019E", "SRVE0777E", "SRVE0315E");
        }
    }

}
