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

import static org.junit.Assert.assertFalse;

import java.net.URL;
import java.util.List;

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
 * jsfTestServer2
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22ViewPoolingTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22ViewPooling";

    protected static final Class<?> c = JSFCompELTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfTestServer2, "JSF22ViewPooling.war");

        jsfTestServer2.startServer(JSF22ViewPoolingTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            //Ignore expected exception
            jsfTestServer2.stopServer();
        }
    }

    /**
     * Check to make sure that no NullPointerException is thrown in ViewPoolProcessor.pushPartialView()
     * when View Pooling is enabled and oamEnableViewPool is set to false in a specific resource
     *
     * @throws Exception
     */
    @Test
    public void JSF22ViewPooling_TestViewPoolingDisabled() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "JSF22ViewPooling_Disabled.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22ViewPooling_Disabled.xhtml did not render properly.");
            }

            String msgToSearchFor1 = "java.lang.NullPointerException";
            String msgToSearchFor2 = "ViewPoolProcessor.pushPartialView";

            List<String> msg1 = jsfTestServer2.findStringsInLogs(msgToSearchFor1);
            List<String> msg2 = jsfTestServer2.findStringsInLogs(msgToSearchFor2);

            assertFalse("NullPointerException and ViewPoolProcessor.pushPartialView found in logs", !msg1.isEmpty() && !msg2.isEmpty());
        }
    }
}
