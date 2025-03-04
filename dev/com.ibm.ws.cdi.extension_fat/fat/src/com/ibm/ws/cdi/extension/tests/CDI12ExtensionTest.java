/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.tests;

import javax.enterprise.inject.spi.Extension;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.CDIVersion;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.cdi.extension.apps.enablementSharedLib.DummyBean;
import com.ibm.ws.cdi.extension.apps.enablementWar.EnablementSharedLibServlet;
import com.ibm.ws.cdi.extension.apps.enablementWar.EnablementTestServlet;
import com.ibm.ws.cdi.extension.apps.helloworld.HelloWorldExtensionBean;
import com.ibm.ws.cdi.extension.apps.helloworld.HelloWorldExtensionTestServlet;
import com.ibm.ws.cdi.extension.apps.invocationContext.BindingExtension;
import com.ibm.ws.cdi.extension.apps.invocationContext.InvocationContextTestServlet;
import com.ibm.ws.cdi.extension.apps.multipleWar.NoBeansTestServlet;
import com.ibm.ws.cdi.extension.apps.multipleWar.ejb.EmbeddedJarMyEjb;
import com.ibm.ws.cdi.extension.apps.multipleWar.war1.WAR1MyBean;
import com.ibm.ws.cdi.extension.apps.multipleWar.war1.WAR1TestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class CDI12ExtensionTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12RuntimeExtensionServer";

    @TestServlets({
                    @TestServlet(contextRoot = "enablement", servlet = EnablementTestServlet.class),
                    @TestServlet(contextRoot = "enablementSharedLib", servlet = EnablementSharedLibServlet.class),
                    @TestServlet(contextRoot = "invocationContext", servlet = InvocationContextTestServlet.class)
    })
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = CDIExtensionRepeatActions.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("Install the user feature bundle... cdi.helloworld.extension");
        CDIExtensionRepeatActions.installUserExtension(server, CDIExtensionRepeatActions.HELLOWORLD_EXTENSION_BUNDLE_ID);
        System.out.println("Install the user feature bundle... cdi.internals");
        CDIExtensionRepeatActions.installSystemFeature(server, CDIExtensionRepeatActions.CDI_INTERNALS_BUNDLE_ID);

        // multipleWar2.ear
        // ----------------

        JavaArchive multipleWarEmbeddedJar = ShrinkWrap.create(JavaArchive.class, "multipleWarEmbeddedJar.jar");
        multipleWarEmbeddedJar.addClass(EmbeddedJarMyEjb.class);
        CDIArchiveHelper.addBeansXML(multipleWarEmbeddedJar, DiscoveryMode.ANNOTATED);

        WebArchive multipleWarNoBeans = ShrinkWrap.create(WebArchive.class, "multipleWarNoBeans.war");
        multipleWarNoBeans.addClass(NoBeansTestServlet.class);

        WebArchive multipleWar = ShrinkWrap.create(WebArchive.class, "multipleWar1.war");
        multipleWar.addClass(WAR1TestServlet.class);
        multipleWar.addClass(WAR1MyBean.class);
        multipleWar.addAsWebInfResource(WAR1TestServlet.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        CDIArchiveHelper.addBeansXML(multipleWar, DiscoveryMode.ANNOTATED);
        multipleWar.addAsLibrary(multipleWarEmbeddedJar);

        EnterpriseArchive multipleWars = ShrinkWrap.create(EnterpriseArchive.class, "multipleWars2.ear");
        multipleWars.setApplicationXML(NoBeansTestServlet.class.getPackage(), "application.xml");
        multipleWars.addAsModule(multipleWar);
        multipleWars.addAsModule(multipleWarNoBeans);

        ShrinkHelper.exportDropinAppToServer(server, multipleWars, DeployOptions.SERVER_ONLY);

        // helloWorldExtension.ear
        // -----------------------

        WebArchive helloWorldExtensionTest = ShrinkWrap.create(WebArchive.class, "helloWorldExtensionTest.war");
        helloWorldExtensionTest.addClass(HelloWorldExtensionTestServlet.class);
        helloWorldExtensionTest.addClass(HelloWorldExtensionBean.class);
        CDIArchiveHelper.addBeansXML(helloWorldExtensionTest, DiscoveryMode.DEFAULT, CDIVersion.CDI11);

        EnterpriseArchive helloWorldExtension = ShrinkWrap.create(EnterpriseArchive.class,
                                                                  "helloWorldExension.ear");
        helloWorldExtension.setApplicationXML(HelloWorldExtensionTestServlet.class.getPackage(), "application.xml");

        helloWorldExtension.addAsModule(helloWorldExtensionTest);
        helloWorldExtension.addAsManifestResource(HelloWorldExtensionTestServlet.class.getPackage(), "permissions.xml", "permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, helloWorldExtension, DeployOptions.SERVER_ONLY);

        // enablement.war
        // --------------

        WebArchive enablementWar = ShrinkWrap.create(WebArchive.class, "enablement.war")
                                             .addClasses(EnablementSharedLibServlet.class, EnablementTestServlet.class);
        CDIArchiveHelper.addBeansXML(enablementWar, DiscoveryMode.ANNOTATED);

        JavaArchive enablementSharedLib = ShrinkWrap.create(JavaArchive.class, "enablementSharedLib.jar")
                                                    .addClass(DummyBean.class);
        CDIArchiveHelper.addBeansXML(enablementSharedLib, DiscoveryMode.ANNOTATED);

        ShrinkHelper.exportAppToServer(server, enablementWar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "", enablementSharedLib, DeployOptions.SERVER_ONLY);

        // invocationContext.war

        WebArchive invocationContextWar = ShrinkWrap.create(WebArchive.class, "invocationContext.war")
                                                    .addPackage(InvocationContextTestServlet.class.getPackage())
                                                    .addAsServiceProvider(Extension.class, BindingExtension.class);
        CDIArchiveHelper.addBeansXML(invocationContextWar, DiscoveryMode.ANNOTATED);

        ShrinkHelper.exportDropinAppToServer(server, invocationContextWar, DeployOptions.SERVER_ONLY);

        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application helloWorldExension started");
    }

    @Test
    public void testHelloWorldExtensionServlet() throws Exception {
        HttpUtils.findStringInUrl(server, "/helloWorldExtension/hello", "Hello World CDI 1.2!");

        Assert.assertFalse("Test for Requested scope destroyed",
                           server.findStringsInLogs("Stop Event request scope is happening").isEmpty());
    }

    @Test
    public void testExtensionLoaded() throws Exception {
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! We are starting the container").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! scanning class").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! scanning class (javax|jakarta).validation.ValidatorFactory").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! scanning class (javax|jakarta).validation.Validator").isEmpty());
        Assert.assertFalse("Test for extension loadded",
                           server.findStringsInLogs("Hello World! We are almost finished with the CDI container boot now...").isEmpty());
    }

    @Test
    public void testCDINotEnabled() throws Exception {

        // Check CDI is enabled in module which has beans
        HttpUtils.findStringInUrl(server, "/multipleWar3", "MyEjb myWar1Bean");
        // Check CDI not enabled in module in same ear which can see no beans, except those in runtime extensions
        HttpUtils.findStringInUrl(server, "/multipleWarNoBeans", "ContextNotActiveException");
    }

    /**
     * Post test processing.
     *
     * @throws Exception
     */
    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Stopping the server.");
        if (server.isStarted()) {
            server.stopServer();
        }
        Log.info(CDI12ExtensionTest.class, METHOD_NAME, "Removing cdi extension test user feature files.");
        CDIExtensionRepeatActions.uninstallUserExtension(server, CDIExtensionRepeatActions.HELLOWORLD_EXTENSION_BUNDLE_ID);
        CDIExtensionRepeatActions.uninstallSystemFeature(server, CDIExtensionRepeatActions.CDI_INTERNALS_BUNDLE_ID);
    }
}
