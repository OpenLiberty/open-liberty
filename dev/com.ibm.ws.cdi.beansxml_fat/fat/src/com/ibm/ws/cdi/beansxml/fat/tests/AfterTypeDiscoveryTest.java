/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery.AfterTypeExtension;
import com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery.AfterTypeServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AfterTypeDiscoveryTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12AfterTypeDiscoveryServer";

    public static final String AFTER_TYPE_DISCOVERY_APP_NAME = "afterTypeDiscoveryApp";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE11, EERepeatActions.EE9, EERepeatActions.EE7); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AfterTypeServlet.class, contextRoot = AFTER_TYPE_DISCOVERY_APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive afterTypeDiscoveryApp = ShrinkWrap.create(WebArchive.class, AFTER_TYPE_DISCOVERY_APP_NAME + ".war");
        afterTypeDiscoveryApp.addPackage(AfterTypeServlet.class.getPackage());
        afterTypeDiscoveryApp.addAsManifestResource(AfterTypeServlet.class.getPackage(), "permissions.xml", "permissions.xml");
        CDIArchiveHelper.addBeansXML(afterTypeDiscoveryApp, AfterTypeServlet.class.getPackage());
        CDIArchiveHelper.addCDIExtensionService(afterTypeDiscoveryApp, AfterTypeExtension.class);

        ShrinkHelper.exportDropinAppToServer(server, afterTypeDiscoveryApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E");
    }

}
