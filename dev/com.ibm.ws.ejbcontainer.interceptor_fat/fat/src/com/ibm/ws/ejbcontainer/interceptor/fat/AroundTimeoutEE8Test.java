/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.ee8.web.AroundTimeoutEE8Servlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class AroundTimeoutEE8Test extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.interceptor.fat.AroundTimeoutEE8Server")
    @TestServlets({ @TestServlet(servlet = AroundTimeoutEE8Servlet.class, contextRoot = "AroundTimeoutEE8Web")
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive AroundTimeoutEE8EJB = ShrinkHelper.buildJavaArchive("AroundTimeoutEE8EJB.jar", "com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ee8.ejb.");
        WebArchive AroundTimeoutEE8Web = ShrinkHelper.buildDefaultApp("AroundTimeoutEE8Web.war", "com.ibm.ws.ejbcontainer.interceptor.aroundTimeout.ee8.web.");
        EnterpriseArchive AroundTimeoutEE8Test = ShrinkWrap.create(EnterpriseArchive.class, "AroundTimeoutEE8Test.ear");
        AroundTimeoutEE8Test.addAsModule(AroundTimeoutEE8EJB).addAsModule(AroundTimeoutEE8Web);

        ShrinkHelper.exportDropinAppToServer(server, AroundTimeoutEE8Test);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}