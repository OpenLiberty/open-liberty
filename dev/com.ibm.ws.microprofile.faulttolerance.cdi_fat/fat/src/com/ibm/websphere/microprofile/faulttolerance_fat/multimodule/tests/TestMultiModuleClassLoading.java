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
package com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.classloading.DummyServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.classloading.TestServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.suite.FATSuite;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

public class TestMultiModuleClassLoading extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = FATSuite.MULTI_MODULE_SERVER;

    //run against both EE8 and EE7 features
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE7_FEATURES());

    @BeforeClass
    public static void setupApp() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war")
                        .addPackage(TestServlet.class.getPackage())
                        .addAsManifestResource(TestServlet.class.getResource("war-config.properties"), "microprofile-config.properties");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war")
                        .addClass(DummyServlet.class);

        // Need to include two wars to stop us taking shortcuts with the classloader
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "MultiModuleClassLoading.ear")
                        .addAsModule(war1)
                        .addAsModule(war2)
                        .setApplicationXML(TestServlet.class.getResource("application.xml"));

        ShrinkHelper.exportToServer(SHARED_SERVER.getLibertyServer(), "dropins", ear);
        SHARED_SERVER.getLibertyServer().addInstalledAppForValidation("MultiModuleClassLoading");
    }

    @AfterClass
    public static void removeApp() throws Exception {
        SHARED_SERVER.getLibertyServer().deleteFileFromLibertyServerRoot("dropins/MultiModuleClassLoading.ear");
        SHARED_SERVER.getLibertyServer().removeInstalledAppForValidation("MultiModuleClassLoading");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testClassLoading() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        SHARED_SERVER.verifyResponse(browser, "/war1/test", "OK - FallbackB");
    }

}
