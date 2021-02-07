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
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8_FULL;
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
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test to ensure that we correctly discover and fire events for types and beans which are EJBs
 */
@RunWith(FATRunner.class)
public class EjbDiscoveryTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EjbDiscoveryServer";
    public static final String EJB_DISCOVERY_APP_NAME = "ejbDiscovery";

    //chosen this one test to run on EE8 as well
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE8_FULL, EE7_FULL);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.ejbdiscovery.servlet.DiscoveryServlet.class, contextRoot = EJB_DISCOVERY_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive ejbDiscoveryNone = ShrinkWrap.create(JavaArchive.class,
                                                         EJB_DISCOVERY_APP_NAME + "None.jar")
                                                 .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.none.UndiscoveredStatelessBean.class)
                                                 .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.none.StatelessLocal.class)
                                                 .add(new FileAsset(new File("test-applications/" + EJB_DISCOVERY_APP_NAME + "None.jar/resources/WEB-INF/beans.xml")),
                                                      "/META-INF/beans.xml");

        WebArchive ejbDiscovery = ShrinkWrap.create(WebArchive.class, EJB_DISCOVERY_APP_NAME + ".war")
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.extension.DiscoveryExtension.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.servlet.DiscoveryServlet.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonDdBean.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessBean.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.interfaces.StatelessLocal.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.interfaces.StatelessDdLocal.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessDdBean.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulDdBean.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonBean.class)
                                            .addClass(com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulBean.class)
                                            .add(new FileAsset(new File("test-applications/" + EJB_DISCOVERY_APP_NAME + ".war/resources/WEB-INF/ejb-jar.xml")),
                                                 "/WEB-INF/ejb-jar.xml")
                                            .add(new FileAsset(new File("test-applications/" + EJB_DISCOVERY_APP_NAME + ".war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                                            .add(new FileAsset(new File("test-applications/" + EJB_DISCOVERY_APP_NAME
                                                                        + ".war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                                 "/META-INF/services/javax.enterprise.inject.spi.Extension")
                                            .addAsLibrary(ejbDiscoveryNone);

        ShrinkHelper.exportDropinAppToServer(server, ejbDiscovery, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }
}
