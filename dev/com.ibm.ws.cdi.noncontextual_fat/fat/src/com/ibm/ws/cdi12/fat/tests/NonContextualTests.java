/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi12.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi12.fat.apps.nonContextualInjectionPointWar.NonContextualInjectionPointTestServlet;
import com.ibm.ws.cdi12.fat.apps.nonContextualWar.NonContextualTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class NonContextualTests extends FATServletClient {

    private static final String NON_CONTEXTUAL_APP = "nonContextual";
    private static final String INJECTION_POINT_APP = "nonContextualInjectionPoint";

    @Server("nonContextualServer")
    @TestServlets({ @TestServlet(contextRoot = NON_CONTEXTUAL_APP, servlet = NonContextualTestServlet.class),
                    @TestServlet(contextRoot = INJECTION_POINT_APP, servlet = NonContextualInjectionPointTestServlet.class)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive nonContextual = ShrinkWrap.create(WebArchive.class, NON_CONTEXTUAL_APP + ".war")
                                             .addPackage(NonContextualTestServlet.class.getPackage())
                                             .addAsWebInfResource(NonContextualTestServlet.class.getResource("beans.xml"), "beans.xml");

        WebArchive nonContextualInjectionPoint = ShrinkWrap.create(WebArchive.class, INJECTION_POINT_APP + ".war")
                                                           .addPackage(NonContextualInjectionPointTestServlet.class.getPackage())
                                                           .addAsWebInfResource(NonContextualInjectionPointTestServlet.class.getResource("beans.xml"), "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, nonContextual, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, nonContextualInjectionPoint, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}
