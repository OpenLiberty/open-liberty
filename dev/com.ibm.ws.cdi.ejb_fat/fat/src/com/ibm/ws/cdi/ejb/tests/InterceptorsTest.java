/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.ejb.apps.interceptors.InterceptorsTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Testing that interceptors bound with an @Interceptors (Interceptors spec/not CDI) annotation on an ejb can be resolved
 * even when they have a common type.
 * <p>
 * In this test, although the interceptor classes are eligible to be CDI beans (due to bean-discovery-mode=all), we must not
 * look them up as beans because the classes are named directly in the {@code @Interceptors}
 * annotation.
 * <p>
 * In particular, type-safe resolution for {@code InterceptorSubClass1} would fail because both
 * {@code InterceptorSubClass1} and {@code InterceptorSubClass2} are beans with that type.
 */
@RunWith(FATRunner.class)
public class InterceptorsTest extends FATServletClient {

    public static final String INTERCEPTORS_APP_NAME = "interceptorsApp";
    public static final String SERVER_NAME = "interceptorsServer";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE7, EERepeatActions.EE9);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = InterceptorsTestServlet.class, contextRoot = INTERCEPTORS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive interceptorsApp = ShrinkWrap.create(WebArchive.class, INTERCEPTORS_APP_NAME + ".war")
                                               .addPackages(true, InterceptorsTestServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(interceptorsApp, DiscoveryMode.ALL);

        ShrinkHelper.exportDropinAppToServer(server, interceptorsApp, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
