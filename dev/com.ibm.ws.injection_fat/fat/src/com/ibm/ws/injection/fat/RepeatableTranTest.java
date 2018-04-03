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
package com.ibm.ws.injection.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.injection.repeatable.transaction.web.AdvRepeatableTransactionServlet;
import com.ibm.ws.injection.repeatable.transaction.web.BasicRepeatableTranSynchRegistryServlet;
import com.ibm.ws.injection.repeatable.transaction.web.BasicRepeatableUserTransactionServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test case ensures that repeated @Resource and XML can be declared
 * and inject a resource-env-ref (UserTransaction and
 * TransactionSynchronizationRegistry) into the fields and methods of servlet
 * listeners and filters. It also checks that @Resource can be declared at the
 * class-level of servlet listeners and filters and will create a JNDI resource;
 *
 * To perform the test, a servlet is invoked in the web module with a listener
 * or filter declared in the web.xml. The expected result is that the listener
 * or filter is created and injected an appropriate UserTransaction of
 * TransactionSynchronizationRegistry.
 *
 * @author bmdecker
 *
 */
@RunWith(FATRunner.class)
public class RepeatableTranTest extends FATServletClient {
    @Server("com.ibm.ws.injection.fat.RepeatableTranServer")
    @TestServlets({ @TestServlet(servlet = BasicRepeatableTranSynchRegistryServlet.class, contextRoot = "RepeatableTransactionWeb"),
                    @TestServlet(servlet = BasicRepeatableUserTransactionServlet.class, contextRoot = "RepeatableTransactionWeb"),
                    @TestServlet(servlet = AdvRepeatableTransactionServlet.class, contextRoot = "RepeatableTransactionWeb")
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        WebArchive RepeatableTransactionWeb = ShrinkHelper.buildDefaultApp("RepeatableTransactionWeb.war", "com.ibm.ws.injection.repeatable.transaction.web.");
        EnterpriseArchive RepeatableTransactionTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableTransactionTest.ear");
        RepeatableTransactionTest.addAsModule(RepeatableTransactionWeb);

        ShrinkHelper.exportDropinAppToServer(server, RepeatableTransactionTest);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}