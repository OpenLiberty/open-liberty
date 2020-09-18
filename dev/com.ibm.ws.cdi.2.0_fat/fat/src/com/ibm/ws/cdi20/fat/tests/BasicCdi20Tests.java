/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import beanManagerLookupApp.web.BeanManagerLookupServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import configuratorApp.web.annotatedTypeConfigurator.AnnotatedTypeConfiguratorTest;
import configuratorApp.web.bean.BeanConfiguratorTest;
import configuratorApp.web.beanAttributes.BeanAttributesConfiguratorTest;
import configuratorApp.web.injectionPoint.InjectionPointConfiguratorTest;
import configuratorApp.web.observerMethod.ObserverMethodConfiguratorTest;
import configuratorApp.web.producer.ProducerConfiguratorTest;
import helloworldApp.web.HelloServlet;
import interceptionFactoryApp.web.InterceptionFactoryServlet;
import trimTestApp.web.TrimTestServlet;

/**
 * A collection of tests from different servlets which all use cdi20BasicServer
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class BasicCdi20Tests extends FATServletClient {

    public static final String SERVER_NAME = "cdi20BasicServer";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE8);

    public static final String BEAN_MANAGER_LOOKUP_APP_NAME = "beanManagerLookupApp";
    public static final String CONFIGURATION_APP_NAME = "configuratorApp";
    public static final String INTERCEPTION_FACTORY_APP_NAME = "interceptionFactoryApp";
    public static final String TRIM_TEST_APP_NAME = "trimTestApp";

    @Server(SERVER_NAME)
    @TestServlets({ @TestServlet(servlet = BeanManagerLookupServlet.class, contextRoot = BEAN_MANAGER_LOOKUP_APP_NAME), //LITE
                    @TestServlet(servlet = HelloServlet.class, contextRoot = BEAN_MANAGER_LOOKUP_APP_NAME), //LITE

                    @TestServlet(servlet = BeanConfiguratorTest.class, contextRoot = CONFIGURATION_APP_NAME), //FULL
                    @TestServlet(servlet = AnnotatedTypeConfiguratorTest.class, contextRoot = CONFIGURATION_APP_NAME), //FULL
                    @TestServlet(servlet = ProducerConfiguratorTest.class, contextRoot = CONFIGURATION_APP_NAME), //FULL
                    @TestServlet(servlet = ObserverMethodConfiguratorTest.class, contextRoot = CONFIGURATION_APP_NAME), //FULL
                    @TestServlet(servlet = InjectionPointConfiguratorTest.class, contextRoot = CONFIGURATION_APP_NAME), //FULL
                    @TestServlet(servlet = BeanAttributesConfiguratorTest.class, contextRoot = CONFIGURATION_APP_NAME), //FULL

                    @TestServlet(servlet = InterceptionFactoryServlet.class, contextRoot = INTERCEPTION_FACTORY_APP_NAME), //FULL

                    @TestServlet(servlet = TrimTestServlet.class, contextRoot = TRIM_TEST_APP_NAME) //FULL
    })

    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        
        WebArchive app1 = ShrinkWrap.create(WebArchive.class, BEAN_MANAGER_LOOKUP_APP_NAME + ".war")
                        .addPackages(true, BEAN_MANAGER_LOOKUP_APP_NAME + ".web","helloworldApp.web")
                        .addAsWebInfResource(new File("test-applications/" + BEAN_MANAGER_LOOKUP_APP_NAME + "/resources/index.jsp"));
        
        ShrinkHelper.exportDropinAppToServer(server, app1, DeployOptions.SERVER_ONLY);
        
        if (TestModeFilter.shouldRun(TestMode.FULL)) {

            WebArchive app2 = ShrinkWrap.create(WebArchive.class, CONFIGURATION_APP_NAME+ ".war")
                                        .addPackages(true, CONFIGURATION_APP_NAME+ ".web")
                                        .addAsManifestResource(new File("test-applications/" + CONFIGURATION_APP_NAME + "/resources/META-INF/services/javax.enterprise.inject.spi.Extension")
                                                  , "services/javax.enterprise.inject.spi.Extension")
                                        .addAsWebInfResource(new File("test-applications/" + CONFIGURATION_APP_NAME + "/resources/META-INF/beans.xml"), "beans.xml") // NEEDS TO GO IN WEB-INF in a war
                                        .addAsWebInfResource(new File("test-applications/" + CONFIGURATION_APP_NAME + "/resources/index.jsp"));

            WebArchive app3 = ShrinkWrap.create(WebArchive.class, INTERCEPTION_FACTORY_APP_NAME + ".war")
                                        .addPackages(true, INTERCEPTION_FACTORY_APP_NAME + ".web")
                                        .addAsWebInfResource(new File("test-applications/" + INTERCEPTION_FACTORY_APP_NAME + "/resources/index.jsp"));

            WebArchive app4 = ShrinkWrap.create(WebArchive.class, TRIM_TEST_APP_NAME + ".war")
                                        .addPackages(true, TRIM_TEST_APP_NAME + ".web")
                                        .addAsManifestResource(new File("test-applications/" + TRIM_TEST_APP_NAME + "/resources/META-INF/services/javax.enterprise.inject.spi.Extension")
                                                               , "services/javax.enterprise.inject.spi.Extension")
                                        .addAsWebInfResource(new File("test-applications/" + TRIM_TEST_APP_NAME + "/resources/META-INF/beans.xml"), "beans.xml") // NEEDS TO GO IN WEB-INF in a war
                                        .addAsWebInfResource(new File("test-applications/" + TRIM_TEST_APP_NAME + "/resources/index.jsp"));

            
            ShrinkHelper.exportDropinAppToServer(server, app2, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, app3, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, app4, DeployOptions.SERVER_ONLY);

        }

        server.startServer();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
