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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import xa.servlets.XAServlet;

@RunWith(FATRunner.class)
public class XADBTranlogTest extends XATest {

    public static final String APP_NAME = "xa";
    public static final String SERVLET_NAME = APP_NAME + "/XAServlet";

    @TestServlet(servlet = XAServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Log.info(XADBTranlogTest.class, "setup", "In BeforeClass of XADBTranlogTest");
        server = LibertyServerFactory.getLibertyServer("transaction_xaDB");
        setup(server);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Log.info(XADBTranlogTest.class, "tearDown", "In AfterClass of XADBTranlogTest");
        tearDown(server);
    }
}
