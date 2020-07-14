/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.fat;

import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.injection.jpa.web.AdvJPAPersistenceServlet;
import com.ibm.ws.injection.jpa.web.BasicJPAPersistenceContextServlet;
import com.ibm.ws.injection.jpa.web.BasicJPAPersistenceUnitServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test case ensures that @Resource and XML can be declared and inject a
 * persistence-unit-ref and persistence-context-ref into the fields and methods
 * of servlet listeners and filters. It also checks that @PersistenceContext and @PersistenceUnit
 * can be declared at the class-level of servlet listeners and filters and will
 * create a JNDI resource;
 *
 * To perform the test, a servlet is invoked in the web module with a listener
 * or filter declared in the web.xml. The expected result is that the listener
 * or filter is created and injected an appropriate EntityManagerFactory or
 * EntityManager.
 *
 * @author jnowosa
 *
 */
@RunWith(FATRunner.class)
public class JPATest extends FATServletClient {
    @Server("com.ibm.ws.injection.fat.JPAServer")
    @TestServlets({ @TestServlet(servlet = BasicJPAPersistenceContextServlet.class, contextRoot = "JPAInjectionWeb"),
                    @TestServlet(servlet = BasicJPAPersistenceUnitServlet.class, contextRoot = "JPAInjectionWeb"),
                    @TestServlet(servlet = AdvJPAPersistenceServlet.class, contextRoot = "JPAInjectionWeb")
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.injection.fat.JPAServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.injection.fat.JPAServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        WebArchive JPAInjectionWeb = ShrinkHelper.buildDefaultApp("JPAInjectionWeb.war", "com.ibm.ws.injection.jpa.web.");
        EnterpriseArchive JPAInjectionTest = ShrinkWrap.create(EnterpriseArchive.class, "JPAInjectionTest.ear");
        JPAInjectionTest.addAsModule(JPAInjectionWeb);

        ShrinkHelper.exportDropinAppToServer(server, JPAInjectionTest);

        // When repeated with EE 8 features, jpa-2.2 requires jdbc-4.2 instead of jdbc-4.1
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("jpa-2.2") && features.remove("jdbc-4.1"))
            features.add("jdbc-4.2");
        server.updateServerConfiguration(config);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWJP9991W");
        }
    }
}