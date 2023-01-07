/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.cdi12.fat.cdiinjectintoapp.CDIInjectIntoAppTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@SkipForRepeat({"EE9_FEATURES", "EE10_FEATURES"}) // skip because cdi injection in Application subclasses require the @ApplicationPath annotation in our EE9 implementation
public class CDIInjectIntoAppTest extends FATServletClient {

    public static final String APP_NAME = "cdiinjectintoapp";
    public static final String SERVER_NAME = APP_NAME;

    @Server(SERVER_NAME)
    @TestServlet(servlet = CDIInjectIntoAppTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.jaxrs20.cdi12.fat.cdiinjectintoapp");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}