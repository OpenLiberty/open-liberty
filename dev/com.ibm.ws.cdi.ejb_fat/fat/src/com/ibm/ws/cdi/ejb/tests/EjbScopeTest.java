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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Scope tests for EJBs
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class EjbScopeTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EjbConstructorInjectionServer";
    public static final String EJB_SCOPE_APP_NAME = "ejbScope";
    public static final String MULTIPLE_WAR1_APP_NAME = "multipleWar1";
    public static final String MULTIPLE_WAR2_APP_NAME = "multipleWar2";

    //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.scope.PostConstructScopeServlet.class, contextRoot = EJB_SCOPE_APP_NAME),
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.multipleWar.war1.TestServlet.class, contextRoot = MULTIPLE_WAR1_APP_NAME),
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.multipleWar.war2.TestServlet.class, contextRoot = MULTIPLE_WAR2_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive multipleWarEmbeddedJar = ShrinkWrap.create(JavaArchive.class, "multipleWarEmbeddedJar.jar")
                                                       .addClass(com.ibm.ws.cdi.ejb.apps.multipleWar.embeddedJar.MyEjb.class);

        WebArchive multipleWarOne = ShrinkWrap.create(WebArchive.class, MULTIPLE_WAR1_APP_NAME + ".war")
                                              .addClass(com.ibm.ws.cdi.ejb.apps.multipleWar.war1.TestServlet.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.multipleWar.war1.MyBean.class)
                                              .add(new FileAsset(new File("test-applications/" + MULTIPLE_WAR1_APP_NAME + ".war/resources/WEB-INF/ejb-jar.xml")),
                                                   "/WEB-INF/ejb-jar.xml")
                                              .add(new FileAsset(new File("test-applications/" + MULTIPLE_WAR1_APP_NAME + ".war/resources/WEB-INF/beans.xml")),
                                                   "/WEB-INF/beans.xml")
                                              .addAsLibrary(multipleWarEmbeddedJar);

        WebArchive multipleWarTwo = ShrinkWrap.create(WebArchive.class, MULTIPLE_WAR2_APP_NAME + ".war")
                                              .addClass(com.ibm.ws.cdi.ejb.apps.multipleWar.war2.TestServlet.class)
                                              .addClass(com.ibm.ws.cdi.ejb.apps.multipleWar.war2.MyBean.class)
                                              .add(new FileAsset(new File("test-applications/" + MULTIPLE_WAR2_APP_NAME + ".war/resources/WEB-INF/ejb-jar.xml")),
                                                   "/WEB-INF/ejb-jar.xml")
                                              .add(new FileAsset(new File("test-applications/" + MULTIPLE_WAR2_APP_NAME + ".war/resources/WEB-INF/beans.xml")),
                                                   "/WEB-INF/beans.xml")
                                              .addAsLibrary(multipleWarEmbeddedJar);

        WebArchive ejbScope = ShrinkWrap.create(WebArchive.class, EJB_SCOPE_APP_NAME + ".war")
                                        .addClass(com.ibm.ws.cdi.ejb.apps.scope.PostConstructingStartupBean.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.scope.PostConstructScopeServlet.class)
                                        .addClass(com.ibm.ws.cdi.ejb.apps.scope.RequestScopedBean.class);

        ShrinkHelper.exportDropinAppToServer(server, multipleWarOne, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, multipleWarTwo, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, ejbScope, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
