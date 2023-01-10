/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
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
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for having one EJB implementation class with two different {@code ejb-name}s declared in {@code ejb-jar.xml}.
 */
@RunWith(FATRunner.class)
public class MultipleNamedEJBTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EJB32Server";
    public static final String APP_NAME = "multipleEJBsSingleClass";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs.TestServlet.class, contextRoot = APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive multipleEJBsSingleClass = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs.SimpleEJBImpl.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs.SimpleEJBLocalInterface2.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs.TestServlet.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs.SimpleManagedBean.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.multipleNamedEJBs.SimpleEJBLocalInterface1.class)
                                                       .add(new FileAsset(new File("test-applications/" + APP_NAME + ".war/resources/WEB-INF/ejb-jar.xml")),
                                                            "/WEB-INF/ejb-jar.xml")
                                                       .add(new FileAsset(new File("test-applications/" + APP_NAME + ".war/resources/WEB-INF/beans.xml")),
                                                            "/WEB-INF/beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, multipleEJBsSingleClass, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
