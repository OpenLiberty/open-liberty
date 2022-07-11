/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.web.TimeoutServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class TimeoutTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/TimeoutServlet";

    @Server("com.ibm.ws.transaction.timeout")
    @TestServlet(servlet = TimeoutServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.web.*");

        server.setServerStartTimeout(300000);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0017W");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

}
