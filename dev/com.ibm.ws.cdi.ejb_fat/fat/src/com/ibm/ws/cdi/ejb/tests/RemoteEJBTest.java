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
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test requires the ejb-3.2 feature. I started with it merged into EjbTimerTest, but
 * that test depends on ejbLite-3.2, and/or there's something funny about the way it uses
 * SHARED_SERVER... either way, EjbTimerTest hard to add new tests to.
 */
@RunWith(FATRunner.class)
public class RemoteEJBTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12EJB32FullServer";
    public static final String REMOTE_EJB_APP_NAME = "remoteEJB";

    // Only run on EE7. Skip this test for EE8 & EE9 features (cdi-2.0 & cdi-3.0) because Weld tightened up its EJB checks and we get the following error:
    // WELD-000088: Observer method must be static or local business method:  [EnhancedAnnotatedMethodImpl] public com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver.observeRemote(@Observes EJBEvent)
    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE7_FULL);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.cdi.ejb.apps.remoteEJB.RemoteEJBServlet.class, contextRoot = REMOTE_EJB_APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive ebjMisc = ShrinkWrap.create(WebArchive.class, REMOTE_EJB_APP_NAME + ".war")
                                       .addClass(com.ibm.ws.cdi.ejb.apps.remoteEJB.RemoteEJBServlet.class)
                                       .addClass(com.ibm.ws.cdi.ejb.apps.remoteEJB.TestObserver.class)
                                       .addClass(com.ibm.ws.cdi.ejb.apps.remoteEJB.RemoteInterface.class)
                                       .addClass(com.ibm.ws.cdi.ejb.apps.remoteEJB.EJBEvent.class)
                                       .add(new FileAsset(new File("test-applications/" + REMOTE_EJB_APP_NAME + ".war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, ebjMisc, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
