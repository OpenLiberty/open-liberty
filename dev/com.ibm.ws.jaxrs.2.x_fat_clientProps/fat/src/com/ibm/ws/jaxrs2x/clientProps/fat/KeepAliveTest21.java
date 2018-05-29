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
package com.ibm.ws.jaxrs2x.clientProps.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import jaxrs2x.clientProps.fat.keepAlive.KeepAliveTestServlet;

@RunWith(FATRunner.class)
public class KeepAliveTest21 extends KeepAliveTestBase {

    @Server("jaxrs21.clientProps.fat.keepAlive")
    @TestServlet(servlet = KeepAliveTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        setUp(server);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        afterClass(server);
    }
}