/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxb.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxb.thirdparty.web.ThirdPartyJAXBFromAppTestServlet;

/**
 * @see jaxb.thirdparty.web.ThirdpartyJAXBFromAppTestServlet for test details.
 */
@RunWith(FATRunner.class)
public class ThirdPartyJAXBFromAppTest extends FATServletClient {

    private static final String APP_NAME = "thirdPartyJaxbApp";

    private static final String SERVER = "jaxb_fat.no-jaxb-feature";

    @Server(SERVER)
    @TestServlet(servlet = ThirdPartyJAXBFromAppTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(APP_NAME, "jaxb.thirdparty.web");

        // Need to copy the specific binaries over for each EE level in to the WAR
        if (JakartaEEAction.isEE10Active()) {
            ShrinkHelper.addDirectory(war, server.pathToAutoFVTTestFiles + "/ee10");

            server.setServerConfigurationFile("server-ee10.xml");

        } else if (JakartaEEAction.isEE9Active()) {

            ShrinkHelper.addDirectory(war, server.pathToAutoFVTTestFiles + "/ee9");

            server.setServerConfigurationFile("server-ee9.xml");

        } else {

            ShrinkHelper.addDirectory(war, server.pathToAutoFVTTestFiles + "/ee8");

        }

        ShrinkHelper.exportAppToServer(server, war);
        server.startServer(RepeatTestFilter.getRepeatActionsAsString() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }
}
