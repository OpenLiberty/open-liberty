/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(FULL)
public class EjbConstructorInjectionTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EjbConstructorInjectionServer";
    public static final String EJB_CONSTRUCTOR_INJECTION_APP_NAME = "ejbConstructorInjection";

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.constructorInjection.Servlet.class, contextRoot = EJB_CONSTRUCTOR_INJECTION_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive ejbConstructorInjection = ShrinkWrap.create(WebArchive.class, EJB_CONSTRUCTOR_INJECTION_APP_NAME + ".war")
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.Servlet.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.BeanTwo.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.BeanThree.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.MyQualifier.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.MyForthQualifier.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.MyThirdQualifier.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.Iface.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.BeanFourWhichIsEJB.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.MySecondQualifier.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.BeanOne.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.BeanEJB.class)
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.constructorInjection.StaticState.class)
                                                       .add(new FileAsset(new File("test-applications/" + EJB_CONSTRUCTOR_INJECTION_APP_NAME
                                                                                   + ".war/resources/META-INF/permissions.xml")),
                                                            "/META-INF/permissions.xml")
                                                       .add(new FileAsset(new File("test-applications/" + EJB_CONSTRUCTOR_INJECTION_APP_NAME + ".war/resources/WEB-INF/web.xml")),
                                                            "/WEB-INF/web.xml");

        ShrinkHelper.exportDropinAppToServer(server, ejbConstructorInjection, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
