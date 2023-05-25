/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package tests;

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

@RunWith(FATRunner.class)
public class EJBNewTxRoSTest extends FATServletClient {

    @Server("transaction_EJBRoSServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitNewTxEJBJar1 = ShrinkHelper.buildJavaArchive("InitNewTxEJB1.jar", "ejb.first.");

        EnterpriseArchive InitNewTxApp1 = ShrinkWrap.create(EnterpriseArchive.class, "InitNewTxApp1.ear");
        InitNewTxApp1.addAsModule(InitNewTxEJBJar1);

        ShrinkHelper.exportDropinAppToServer(server, InitNewTxApp1);

        JavaArchive InitNewTxEJBJar2 = ShrinkHelper.buildJavaArchive("InitNewTxEJB2.jar", "ejb.second.");

        EnterpriseArchive InitNewTxApp2 = ShrinkWrap.create(EnterpriseArchive.class, "InitNewTxApp2.ear");
        InitNewTxApp2.addAsModule(InitNewTxEJBJar2);

        ShrinkHelper.exportDropinAppToServer(server, InitNewTxApp2);

        JavaArchive InitNewTxEJBJar3 = ShrinkHelper.buildJavaArchive("InitNewTxEJB3.jar", "ejb.third.");

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
    public void testStartupRoS() throws Exception {
        server.startServer();
    }
}
