/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxb.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxb.web.JAXBContextTestServlet;

/**
 *
 */

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
@SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "JAXRS", "JAXB-2.3", SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
public class ThirdPartyJAXBContextTest extends FATServletClient {

    private static final String APP_NAME = "jaxbContextThirdPartyApp";

    @Server("com.ibm.ws.jaxb.ContextThirdPartyServer")
    @TestServlet(servlet = JAXBContextTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jaxb.web", "jaxb.web.dataobjects");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}