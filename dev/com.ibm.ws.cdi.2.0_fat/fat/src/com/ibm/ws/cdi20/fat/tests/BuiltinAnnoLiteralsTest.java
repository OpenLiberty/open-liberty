/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi20.fat.apps.builtinAnno.BuiltinAnnoServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests verify the use of Built-in annotation literals in CDI2.0 as per http://docs.jboss.org/cdi/spec/2.0/cdi-spec-with-assertions.html#built_in_annotation_literals
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class BuiltinAnnoLiteralsTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi20BuiltinAnnoServer";

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    public static final String APP_NAME = "builtinAnnoLiteralsApp";

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(servlet = BuiltinAnnoServlet.class, contextRoot = APP_NAME) }) //LITE
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackages(true, BuiltinAnnoServlet.class.getPackage());

        app.addAsManifestResource(BuiltinAnnoServlet.class.getPackage(), "permissions.xml", "permissions.xml");
        CDIArchiveHelper.addCDIExtensionService(app, com.ibm.ws.cdi20.fat.apps.builtinAnno.CakeExtension.class);
        CDIArchiveHelper.addBeansXML(app, DiscoveryMode.ALL);

        ShrinkHelper.exportAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
