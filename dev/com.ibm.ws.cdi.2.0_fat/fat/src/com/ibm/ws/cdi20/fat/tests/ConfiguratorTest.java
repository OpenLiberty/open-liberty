/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi20.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import configuratorApp.web.tests.extensions.configurators.annotatedTypeConfigurator.AnnotatedTypeConfiguratorTest;
import configuratorApp.web.tests.extensions.configurators.bean.BeanConfiguratorTest;
import configuratorApp.web.tests.extensions.configurators.beanAttributes.BeanAttributesConfiguratorTest;
import configuratorApp.web.tests.extensions.configurators.injectionPoint.InjectionPointConfiguratorTest;
import configuratorApp.web.tests.extensions.configurators.observerMethod.ObserverMethodConfiguratorTest;
import configuratorApp.web.tests.extensions.configurators.producer.ProducerConfiguratorTest;

/**
 * These tests sniff the configurators described here: http://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#configurators
 */
@RunWith(FATRunner.class)
public class ConfiguratorTest extends FATServletClient {
    public static final String APP_NAME = "configuratorApp";

    @Server("cdi20BasicServer")
    @TestServlets({ @TestServlet(servlet = BeanConfiguratorTest.class, path = APP_NAME + "/beanConfiguratorTest"),
                    @TestServlet(servlet = AnnotatedTypeConfiguratorTest.class, path = APP_NAME + "/annotatedTypeConfiguratorTest"),
                    @TestServlet(servlet = ProducerConfiguratorTest.class, path = APP_NAME + "/producerConfiguratorTest"),
                    @TestServlet(servlet = ObserverMethodConfiguratorTest.class, path = APP_NAME + "/observerMethodConfiguratorTest"),
                    @TestServlet(servlet = InjectionPointConfiguratorTest.class, path = APP_NAME + "/injectionPointConfiguratorTest"),
                    @TestServlet(servlet = BeanAttributesConfiguratorTest.class, path = APP_NAME + "/beanAttributesConfiguratorTest") })
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'configuratorApp.war' once it's written to a file
        // Include the 'configuratorApp.web' package and all of it's java classes and sub-packages
        // Include a simple index.jsp static file in the root of the WebArchive

        WebArchive app1 = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "configuratorApp.web")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + "/resources/META-INF/services/javax.enterprise.inject.spi.Extension"),
                                               "services/javax.enterprise.inject.spi.Extension")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/META-INF/beans.xml"),
                                             "beans.xml") // NEEDS TO GO IN WEB-INF in a war
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/index.jsp"));

        // Write the WebArchive to 'publish/servers/cdi20BasicServer/dropins/configuratorApp.war' and print the contents
        ShrinkHelper.exportDropinAppToServer(server1, app1);
        server1.addInstalledAppForValidation(APP_NAME);
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }
}
