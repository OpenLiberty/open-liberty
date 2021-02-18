/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test Method parameter injection.
 * <p>
 * We had an issue where method parameters were sometimes injected in the wrong order.
 * <p>
 * This test tests method parameter injection on a servlet, an EJB and a CDI bean.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class InjectParameterTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EJB32Server";
    public static final String INJECTED_PARAMS_APP_NAME = "injectParameters";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.injectparameters.TestEjbServlet.class, contextRoot = INJECTED_PARAMS_APP_NAME),
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.injectparameters.TestServlet.class, contextRoot = INJECTED_PARAMS_APP_NAME),
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.injectparameters.TestCdiBeanServlet.class, contextRoot = INJECTED_PARAMS_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive injectParameters = ShrinkWrap.create(WebArchive.class, INJECTED_PARAMS_APP_NAME + ".war")
                                                .addClass(com.ibm.ws.cdi.ejb.apps.injectparameters.TestEjb.class)
                                                .addClass(com.ibm.ws.cdi.ejb.apps.injectparameters.TestEjbServlet.class)
                                                .addClass(com.ibm.ws.cdi.ejb.apps.injectparameters.TestServlet.class)
                                                .addClass(com.ibm.ws.cdi.ejb.apps.injectparameters.TestProducer.class)
                                                .addClass(com.ibm.ws.cdi.ejb.apps.injectparameters.TestResources.class)
                                                .addClass(com.ibm.ws.cdi.ejb.apps.injectparameters.TestCdiBean.class)
                                                .addClass(com.ibm.ws.cdi.ejb.apps.injectparameters.TestCdiBeanServlet.class);

        ShrinkHelper.exportDropinAppToServer(server, injectParameters, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
