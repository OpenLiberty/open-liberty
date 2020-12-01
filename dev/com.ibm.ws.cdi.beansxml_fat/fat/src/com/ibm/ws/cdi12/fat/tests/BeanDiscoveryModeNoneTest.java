/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.net.MalformedURLException;
import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserException;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpRequest;

@RunWith(FATRunner.class)
public class BeanDiscoveryModeNoneTest {

    @Server("cdi12BeanDiscoveryModeNoneServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
         WebArchive beanDiscoveryModeNone = ShrinkWrap.create(WebArchive.class, "beanDiscoveryModeNone.war")
                        .addClass("com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestBean1")
                        .add(new FileAsset(new File("test-applications/beanDiscoveryModeNone.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/beanDiscoveryModeNone.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
         ShrinkHelper.exportDropinAppToServer(server, beanDiscoveryModeNone, DeployOptions.SERVER_ONLY);
         server.startServer();
    }

    @Test
    @ExpectedFFDC({ "javax.servlet.UnavailableException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testcorrectExceptionThrown() throws Exception {
        new HttpRequest(server, "/beanDiscoveryModeNone/TestServlet").expectCode(404).run(String.class);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * Error 404: javax.servlet.UnavailableException: SRVE0319E: For the
             * [com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet] servlet,
             * com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet servlet class
             * was found, but a resource injection failure has occurred. The @Inject
             * java.lang.reflect.Field.testBean1 reference of type com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestBean1
             * for the null component in the beanDiscoveryModeNone.war module of the
             * beanDiscoveryModeNone application cannot be resolved.
             */
            server.stopServer("SRVE0319E", "CWNEN0035E", "CWOWB1008E");
        }
    }
}
