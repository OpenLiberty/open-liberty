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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxb.thirdparty.web.ThirdPartyJAXBFromAppTestServlet;

/**
 * @see jaxb.thirdparty.web.ThirdpartyJAXBFromAppTestServlet for test details.
 */
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE10_FEATURES, SkipForRepeat.EE9_FEATURES })
public class ThirdPartyJAXBFromAppTest extends FATServletClient {

    private static final String SERVER = "jaxb_fat.no-jaxb-feature";
    private static final String APP_NAME = "thirdPartyJaxbApp";

    @Server(SERVER)
    @TestServlet(servlet = ThirdPartyJAXBFromAppTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jaxb.thirdparty.web");
        server.startServer(RepeatTestFilter.getRepeatActionsAsString() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }
}
