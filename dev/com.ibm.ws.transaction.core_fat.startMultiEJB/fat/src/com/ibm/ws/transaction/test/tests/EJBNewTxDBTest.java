/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.transaction.test.tests;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
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
public class EJBNewTxDBTest extends FATServletClient {

    @Server("com.ibm.ws.transaction_EJBDBServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitNewTxEJBJar1 = ShrinkHelper.buildJavaArchive("InitNewTxEJB1.jar", "com.ibm.ws.transaction.ejb.first.");

        EnterpriseArchive InitNewTxApp1 = ShrinkWrap.create(EnterpriseArchive.class, "InitNewTxApp1.ear");
        InitNewTxApp1.addAsModule(InitNewTxEJBJar1);

        ShrinkHelper.exportDropinAppToServer(server, InitNewTxApp1);

        JavaArchive InitNewTxEJBJar2 = ShrinkHelper.buildJavaArchive("InitNewTxEJB2.jar", "com.ibm.ws.transaction.ejb.second.");

        EnterpriseArchive InitNewTxApp2 = ShrinkWrap.create(EnterpriseArchive.class, "InitNewTxApp2.ear");
        InitNewTxApp2.addAsModule(InitNewTxEJBJar2);

        ShrinkHelper.exportDropinAppToServer(server, InitNewTxApp2);

        JavaArchive InitNewTxEJBJar3 = ShrinkHelper.buildJavaArchive("InitNewTxEJB3.jar", "com.ibm.ws.transaction.ejb.third.");

        EnterpriseArchive InitNewTxApp3 = ShrinkWrap.create(EnterpriseArchive.class, "InitNewTxApp3.ear");
        InitNewTxApp3.addAsModule(InitNewTxEJBJar3);

        ShrinkHelper.exportDropinAppToServer(server, InitNewTxApp3);
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

    /**
     * Test enlistment in transactions.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    @Test
    public void testStartupDB() throws Exception {
        server.startServer();
    }
}
