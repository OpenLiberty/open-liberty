/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.policy;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.PolicyExecutorServlet;

@RunWith(FATRunner.class)
public class PolicyExecutorTest extends FATServletClient {

    @Server("PolicyExecutorServer")
    @TestServlet(servlet = PolicyExecutorServlet.class, path = "basicfat/PolicyExecutorServlet")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        server1.copyFileToLibertyInstallRoot("lib/features/", "features/policyExecutorUser-1.0.mf");
        server1.copyFileToLibertyInstallRoot("lib/", "bundles/test.policyexecutor.bundle_fat.jar");

        WebArchive app = ShrinkWrap.create(WebArchive.class, "basicfat.war")//
                        .addPackages(true, "web")//
                        .addAsWebInfResource(new File("test-applications/basicfat/resources/index.jsp"));
        ShrinkHelper.exportAppToServer(server1, app);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWKE1205E:.*PolicyExecutorProvider-testStartTimeout.*"); // some tests intentionally exceed the startTimeout
        server1.deleteFileFromLibertyInstallRoot("lib/features/policyExecutorUser-1.0.mf");
        server1.deleteFileFromLibertyInstallRoot("lib/test.policyexecutor.bundle_fat.jar");
    }
}
