/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to execute on the jsfTestServer2 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22LocalizationTesterTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22LocalizationTester";

    protected static final Class<?> c = JSF22LocalizationTesterTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive JSF22LocalizationTesterWar = ShrinkHelper.buildDefaultApp("JSF22LocalizationTester.war", "com.ibm.ws.jsf22.fat.localbean.*");

        JSF22LocalizationTesterWar.addAsResource(new File("test-applications/JSF22LocalizationTester.war/src/com/ibm/ws/jsf22/fat/localprops/messages.properties"),
                                                 "com/ibm/ws/jsf22/fat/localprops/messages.properties");
        JSF22LocalizationTesterWar.addAsResource(new File("test-applications/JSF22LocalizationTester.war/src/com/ibm/ws/jsf22/fat/localprops/messages_zh_CN.properties"),
                                                 "com/ibm/ws/jsf22/fat/localprops/messages_zh_CN.properties");
        JSF22LocalizationTesterWar.addAsResource(new File("test-applications/JSF22LocalizationTester.war/src/com/ibm/ws/jsf22/fat/localprops/resources_zh_CN.properties"),
                                                 "com/ibm/ws/jsf22/fat/localprops/resources_zh_CN.properties");
        JSF22LocalizationTesterWar.addAsResource(new File("test-applications/JSF22LocalizationTester.war/src/com/ibm/ws/jsf22/fat/localprops/resources.properties"),
                                                 "com/ibm/ws/jsf22/fat/localprops/resources.properties");

        ShrinkHelper.exportDropinAppToServer(jsfTestServer2, JSF22LocalizationTesterWar);

        jsfTestServer2.startServer(JSF22LocalizationTesterTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }
    }

    /**
     * Check to make sure that a transient view renders with the correct viewstate value
     *
     * @throws Exception
     */
    @Test
    public void JSF22LocalizationTester_TestLocalAndGlobalResources() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "default.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22LocalizationTester_TestLocalAndGlobalResources.xhtml did not render properly.");
            }

            assertTrue(page.asText().contains("Testing"));
        }
    }

    /**
     * Set the Accept-Language header to zh_CN but make sure that english is displayed.
     * According to the Jira below, the ResourceManager previously only checked the header while
     * calculating the locale.
     * http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1065
     *
     * @throws Exception
     */
    @Test
    public void JSF22LocalizationTester_TestCalculateLocale() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.addRequestHeader("Accept-Language", "zh_CN");

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "default.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22LocalizationTester_TestCalculateLocale, default.xhtml did not render properly.");
            }

            assertTrue(page.asText().contains("Happy learning JSF 2.2"));
        }
    }
}
