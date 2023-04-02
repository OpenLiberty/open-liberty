/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.thirdparty.tests;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.thirdparty.apps.deltaspikeWar.DeltaspikeTestServlet;
import com.ibm.ws.cdi.thirdparty.apps.deltaspikeWar.RequestScopedNumberProvider;
import com.ibm.ws.cdi.thirdparty.apps.entityListenersEarLibJar.EntityBListener;
import com.ibm.ws.cdi.thirdparty.apps.entityListenersWar.model.EntityA;
import com.ibm.ws.cdi.thirdparty.apps.entityListenersWar.web.EntityListenersTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class ThirdPartyTests extends FATServletClient {

    @Server("cdiThirdPartyServer")
    @TestServlet(contextRoot = "deltaspikeTest", servlet = DeltaspikeTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive entityListenersEarLib = ShrinkWrap.create(JavaArchive.class, "entityListenersEarLib.jar")
                                                      .addPackage(EntityBListener.class.getPackage());

        WebArchive entityListenersWar = ShrinkWrap.create(WebArchive.class, "entityListeners.war")
                                                  .addPackage(EntityListenersTestServlet.class.getPackage())
                                                  .addPackage(EntityA.class.getPackage())
                                                  .addAsResource("com/ibm/ws/cdi/thirdparty/apps/entityListenersWar/persistence.xml", "META-INF/persistence.xml")
                                                  .addAsResource("com/ibm/ws/cdi/thirdparty/apps/entityListenersWar/jpaorm.xml", "META-INF/jpaorm.xml");

        EnterpriseArchive entityListenersEar = ShrinkWrap.create(EnterpriseArchive.class, "entityListeners.ear")
                                                         .addAsModule(entityListenersWar)
                                                         .addAsLibrary(entityListenersEarLib)
                                                         .addAsManifestResource("com/ibm/ws/cdi/thirdparty/apps/entityListenersWar/permissions.xml", "permissions.xml");

        WebArchive deltaspikeTest = ShrinkWrap.create(WebArchive.class, "deltaspikeTest.war")
                                              .addPackage(RequestScopedNumberProvider.class.getPackage())
                                              .addAsWebInfResource(RequestScopedNumberProvider.class.getResource("beans.xml"), "beans.xml")
                                              .addAsWebInfResource(RequestScopedNumberProvider.class.getResource("quartz-config.xml"), "quartz-config.xml")
                                              .addAsWebInfResource(RequestScopedNumberProvider.class.getResource("web.xml"), "web.xml")
                                              .addAsManifestResource(RequestScopedNumberProvider.class.getResource("permissions.xml"), "permissions.xml")
                                              .addAsLibraries(new File("lib/deltaspike").listFiles());

        ShrinkHelper.exportAppToServer(server, entityListenersEar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, deltaspikeTest, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W");
    }

    @Test
    public void testInjectWorksInsideEntityListeners() throws Exception {
        String body = HttpUtils.getHttpResponseAsString(server, "/entityListeners/EntityListenersTestServlet"); // we just poke the server to make sure everything runs.

        List<String> matching = server.findStringsInLogsAndTraceUsingMark("testInjectWorksInsideEntityListeners passed!");
        assertThat("Did not find the test passed string in logs.", matching, not(empty()));
    }

}
