/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import transactionscopedtest.TransactionScopedTestServlet;

@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class TransactionScopedObserversTest extends FATServletClient {

    public static final String APP_NAME = "transactionscoped";
    public static final String SECOND_APP_NAME = "transactionscopedtwo";

    @Server("com.ibm.ws.transaction_cdi")
    @TestServlets({ @TestServlet(servlet = TransactionScopedTestServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = TransactionScopedTestServlet.class, contextRoot = SECOND_APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "transactionscopedtest.*");
        //Default app uses the app name to find resource files, since I'm just duplicating an app I'll manually deploy it.
        WebArchive appTwo = ShrinkWrap.create(WebArchive.class, SECOND_APP_NAME + ".war")
                        .addPackage("transactionscopedtest")
                        .add(new FileAsset(new File("test-applications/transactionscoped/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        ShrinkHelper.exportAppToServer(server, appTwo);
        server.addInstalledAppForValidation(SECOND_APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
