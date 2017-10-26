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
package com.ibm.ws.jpa;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jpa21bootstrap.web.TestJPA21BootstrapServlet;

@RunWith(FATRunner.class)
public class JPA21BootstrapTest extends FATServletClient {
    public static final String APP_NAME = "jpa21bootstrap";
    public static final String SERVLET = "TestJPA21Bootstrap";

    @Server("JPA21FATServer")
    @TestServlet(servlet = TestJPA21BootstrapServlet.class, path = APP_NAME + "/" + SERVLET)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server1, APP_NAME, "jpa21bootstrap.web", "jpa21bootstrap.entity");
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W");
    }

}
