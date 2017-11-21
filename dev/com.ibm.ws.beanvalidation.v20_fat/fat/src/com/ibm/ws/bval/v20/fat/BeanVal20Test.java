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
package com.ibm.ws.bval.v20.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import bval.v20.cdi.web.BeanValCDIServlet;
import bval.v20.web.BeanVal20TestServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class BeanVal20Test extends FATServletClient {

    public static final String REG_APP = "bvalApp";
    public static final String CDI_APP = "bvalCDIApp";

    @Server("beanval.v20_fat")
    @TestServlets({
                    @TestServlet(servlet = BeanVal20TestServlet.class, contextRoot = REG_APP),
                    @TestServlet(servlet = BeanValCDIServlet.class, contextRoot = CDI_APP)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, REG_APP, "bval.v20.web");
        ShrinkHelper.defaultDropinApp(server, CDI_APP, "bval.v20.cdi.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
