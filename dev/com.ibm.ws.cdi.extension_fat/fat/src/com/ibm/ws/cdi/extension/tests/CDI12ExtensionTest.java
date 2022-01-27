/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension.tests;

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
import com.ibm.ws.cdi.extension.apps.helloworld.HelloWorldExtensionBean;
import com.ibm.ws.cdi.extension.apps.helloworld.HelloWorldExtensionTestServlet;
import com.ibm.ws.cdi.extension.apps.multipleWar.NoBeansTestServlet;
import com.ibm.ws.cdi.extension.apps.multipleWar.ejb.EmbeddedJarMyEjb;
import com.ibm.ws.cdi.extension.apps.multipleWar.war1.WAR1MyBean;
import com.ibm.ws.cdi.extension.apps.multipleWar.war1.WAR1TestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
public class CDI12ExtensionTest {

    public static final String SERVER_NAME = "cdi12RuntimeExtensionServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = CDIExtensionRepeatActions.repeat(SERVER_NAME, CDIExtensionRepeatActions.EE7_PLUS, CDIExtensionRepeatActions.EE9_PLUS,
                                                                   CDIExtensionRepeatActions.EE10_PLUS);

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("Install the user feature bundle... cdi.helloworld.extension");
        CDIExtensionRepeatActions.installUserExtension(server, CDIExtensionRepeatActions.HELLOWORLD_EXTENSION_BUNDLE_ID);
        CDIExtensionRepeatActions.installSystemFeature(server, CDIExtensionRepeatActions.CDI_INTERNALS_BUNDLE_ID);

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

        WebArchive helloWorldExtensionTest = ShrinkWrap.create(WebArchive.class, "helloWorldExtensionTest.war");
        helloWorldExtensionTest.addClass(HelloWorldExtensionTestServlet.class);
        helloWorldExtensionTest.addClass(HelloWorldExtensionBean.class);
        CDIArchiveHelper.addBeansXML(helloWorldExtensionTest, DiscoveryMode.ALL, CDIVersion.CDI10);

        EnterpriseArchive helloWorldExension = ShrinkWrap.create(EnterpriseArchive.class,
                                                                 "helloWorldExension.ear");
        helloWorldExension.setApplicationXML(HelloWorldExtensionTestServlet.class.getPackage(), "application.xml");

        helloWorldExension.addAsModule(helloWorldExtensionTest);
        helloWorldExension.addAsManifestResource(HelloWorldExtensionTestServlet.class.getPackage(), "permissions.xml", "permissions.xml");

        /**
         * Install the user feature and the bundle
         */
        ShrinkHelper.exportDropinAppToServer(server, multipleWars, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, helloWorldExension, DeployOptions.SERVER_ONLY);

        server.startServer(true);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application helloWorldExension started");
    }

    @Test
    @SkipForRepeat(CDIExtensionRepeatActions.EE10_PLUS_ID) //currently does not work on EE10, don't know why - https://github.com/OpenLiberty/open-liberty/issues/19900
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

        HttpUtils.findStringInUrl(server, "/multipleWar3", "MyEjb myWar1Bean");
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
