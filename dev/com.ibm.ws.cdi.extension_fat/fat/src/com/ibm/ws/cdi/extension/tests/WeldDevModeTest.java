/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.cdi.extension.apps.multipleWar.NoBeansTestServlet;
import com.ibm.ws.cdi.extension.apps.multipleWar.ejb.EmbeddedJarMyEjb;
import com.ibm.ws.cdi.extension.apps.multipleWar.war1.WAR1MyBean;
import com.ibm.ws.cdi.extension.apps.multipleWar.war1.WAR1TestServlet;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the runtime extension to function correctly
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WeldDevModeTest {

    public static final String SERVER_NAME = "weldDevModeServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    //Does not repeat against CDI 4.0 where dev mode is not supported
    @ClassRule
    public static RepeatTests r = CDIExtensionRepeatActions.repeat(SERVER_NAME, CDIExtensionRepeatActions.EE9_PLUS, CDIExtensionRepeatActions.EE7_PLUS);

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

        ShrinkHelper.exportDropinAppToServer(server, multipleWars, DeployOptions.SERVER_ONLY);

        server.startServer(true);
    }

    /**
     * Post test processing.
     *
     * @throws Exception
     */
    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.info(WeldDevModeTest.class, METHOD_NAME, "Stopping the server.");
        if (server.isStarted()) {
            server.stopServer();
        }
        Log.info(WeldDevModeTest.class, METHOD_NAME, "Removing cdi extension test user feature files.");
        CDIExtensionRepeatActions.uninstallUserExtension(server, CDIExtensionRepeatActions.HELLOWORLD_EXTENSION_BUNDLE_ID);
        CDIExtensionRepeatActions.uninstallSystemFeature(server, CDIExtensionRepeatActions.CDI_INTERNALS_BUNDLE_ID);
    }

    @Test
    public void testExtensionLoaded() throws Exception {
        assertStringsInLogs("WELD-000411: Observer method \\[BackedAnnotatedMethod\\] public org.jboss.weld.probe.ProbeExtension.processAnnotatedTypes");
        assertStringsInLogs("Weld Development Mode: ENABLED");

        assertProbeProcessedBDA("multipleWars2#org.jboss.weld.probe.ProbeExtension.additionalClasses");
        assertProbeProcessedBDA("multipleWars2#multipleWar1.war");
        assertProbeProcessedBDA("multipleWars2#multipleWar1.war#WEB_INF_LIB#WEB-INF/lib/multipleWarEmbeddedJar.jar");
        assertProbeProcessedBDA("multipleWars2#multipleWarNoBeans.war");
        //Where the BDA name varies only slightly in EE9, a wildcard is sufficient to check for them
        assertProbeProcessedBDA("multipleWars2#cdi.helloworld.extension.*_1.additionalClasses");
        assertProbeProcessedBDA("multipleWars2#io.openliberty.transaction.internal.cdi.*_1.additionalClasses");
        //Ignoring BDAs which are part of internal components where the names have significantly changed between EE8 and EE9, e.g. Bean Validation and JSF
    }

    private static void assertStringsInLogs(String msg) throws Exception {
        Assert.assertFalse("Log Message Not Found: " + msg, server.findStringsInLogs(msg).isEmpty());
    }

    private static void assertProbeProcessedBDA(String bda) throws Exception {
        assertStringsInLogs("PROBE-000002: Processing bean deployment archive: " + bda);
    }
}
