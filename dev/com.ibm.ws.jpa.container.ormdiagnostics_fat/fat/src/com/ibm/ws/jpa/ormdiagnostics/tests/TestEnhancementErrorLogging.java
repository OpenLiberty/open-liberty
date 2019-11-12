/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.ormdiagnostics.tests;

import java.util.List;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jpa.ormdiagnostics.ORMApplicationBuilder;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;
import jpasimple.web.TestJPASimpleServlet;
import junit.framework.Assert;

@RunWith(FATRunner.class)
public class TestEnhancementErrorLogging extends FATServletClient {
    private static final Logger LOG = Logger.getLogger(TestBasicLibertyDump.class.getName());

    public static final String APP_NAME = "jpasimplebadclass";
    public static final String SERVLET = "TestJPASimple";

    @Server("JPABadBytecode")
    @TestServlets({
                    @TestServlet(servlet = TestJPASimpleServlet.class, path = APP_NAME + "/" + SERVLET),
    })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server1,
                                        "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                        "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";");

        LOG.info("Setup : Creating WAR");
        WebArchive war = ORMApplicationBuilder.createWAR("jpasimplebadclass.war", "jpasimple.entity", "jpasimple.web");
        ORMApplicationBuilder.addArchivetoServer(server1, "dropins", war);

        LOG.info("Setup : Starting Server");
        server1.startServer();

        server1.addInstalledAppForValidation("jpasimplebadclass");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W", "CWWKC0044W", "CWWJP9992E");
    }

    @Test
    public void testInvalidFormatClassError() throws Exception {
        Assert.assertTrue(server1.defaultTraceFileExists());

        FATServletClient.runTest(server1, APP_NAME + "/TestJPASimple", "testInvalidFormatClassError");

        String hexText = "Before Class Transform: Bytecode for class empty.classx.BadClass";

        List<String> traceEntry = server1.findStringsInTrace(hexText);
        Assert.assertTrue(traceEntry.size() > 0);
    }

}