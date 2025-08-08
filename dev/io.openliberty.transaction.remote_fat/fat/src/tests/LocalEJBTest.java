/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import localClient.LocalEJBClient;

@RunWith(FATRunner.class)
public class LocalEJBTest extends EJBTest {

    @Server("RemoteEJBClient")
    @TestServlets({
                    @TestServlet(servlet = LocalEJBClient.class, contextRoot = CLIENT_APP_NAME),
    })
    public static LibertyServer client;

    @BeforeClass
    public static void setUp() throws Exception {
        final JavaArchive TestBeanEJBJar = ShrinkHelper.buildJavaArchive("TestBeanEJB.jar", "ejb", "shared");
        final EnterpriseArchive TestBeanApp = ShrinkWrap.create(EnterpriseArchive.class, "TestBeanApp.ear");
        TestBeanApp.addAsModule(TestBeanEJBJar);
        ShrinkHelper.exportDropinAppToServer(client, TestBeanApp, DeployOptions.SERVER_ONLY);

        ShrinkHelper.defaultApp(client, CLIENT_APP_NAME, "client", "localClient", "shared");

        FATUtils.startServers(client);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        EJBTest.afterClass(client);
    }
}