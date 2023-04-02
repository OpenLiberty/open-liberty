/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.microprofile.rest.client.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import mpRestClientFT.retry.ClassRetryClient;
import mpRestClientFT.retry.RetryClient;
import mpRestClientFT.retry.RetryTestServlet;

/**
 * This test should only be run with mpRestClient-1.1 and above.
 * Note that mpRestClient-1.1 can work with Java EE 7.0 (MP 1.4) and EE 8 (MP 2.0).
 */
@RunWith(FATRunner.class)
public class RetryTest extends FATServletClient {

    final static String SERVER_NAME = "mpRestClientFT.retry";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME,
                                                             MicroProfileActions.MP22, // 1.2
                                                             MicroProfileActions.MP30, // 1.3
                                                             MicroProfileActions.MP33, // 1.4
                                                             MicroProfileActions.MP40, // 2.0
                                                             MicroProfileActions.MP50, // 3.0 + EE9
                                                             MicroProfileActions.MP60);// 3.0 + EE10
    
    private static final String appName = "retryApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = RetryTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(appName, SERVER_NAME);
        PropertiesAsset mpConfig = new PropertiesAsset();
        mpConfig.addProperty(RetryClient.class.getName() + "/mp-rest/uri",
                             "http://localhost:" + server.getHttpDefaultPort() + "/retryApp");
        mpConfig.addProperty(ClassRetryClient.class.getName() + "/mp-rest/uri",
                             "http://localhost:" + server.getHttpDefaultPort() + "/retryApp");
        war.addAsWebInfResource(mpConfig, "classes/META-INF/microprofile-config.properties");
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer(true, false, false, "CWWKW1002W");
    }
}
