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

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to execute on the jsf22beanvalServer that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22BeanValidationTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "BeanValidationTests";

    protected static final Class<?> c = JSF22BeanValidationTests.class;

    @Server("jsf22beanvalServer")
    public static LibertyServer jsf22beanvalServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf22beanvalServer, "BeanValidationTests.war", "com.ibm.ws.jsf22.fat.beanvalidation");

        jsf22beanvalServer.startServer(JSF22BeanValidationTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf22beanvalServer != null && jsf22beanvalServer.isStarted()) {
            jsf22beanvalServer.stopServer();
        }
    }

    /**
     * Test whether beanValidation-1.1 is actually enabled when jsf-2.2 is enabled
     * We do this by looking for a message in the logs
     *
     * @throws Exception
     */
    @Test
    public void testBeanValidation11Enabled() throws Exception {
        String msgToSearchFor = "MyFaces Bean Validation support enabled";
        String msgToSearchForMyFaces30 = "MyFaces Core Bean Validation support enabled";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22beanvalServer, contextRoot, "BeanValidation.jsf");
            webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /BeanValidationTests/BeanValidation.jsf");

            String logMessage;
            if (JakartaEE9Action.isActive()) {
                Log.info(c, name.getMethodName(), "Looking for message " + msgToSearchForMyFaces30 + " in the logs");
                logMessage = jsf22beanvalServer.waitForStringInLog(msgToSearchForMyFaces30);
            } else {
                Log.info(c, name.getMethodName(), "Looking for message " + msgToSearchFor + " in the logs");
                logMessage = jsf22beanvalServer.waitForStringInLog(msgToSearchFor);
            }
            Log.info(c, name.getMethodName(), "Message found in the logs : " + logMessage);
            Assert.assertNotNull("Correct message not found", logMessage);
        }
    }

    /**
     * Execute the BeanTagBinding validation test.
     * This test has two states. First it executes an evaluation with a size greater than the max
     * That test is expected to fail
     * The second test is one that test something at the max length. This test is expected to pass
     *
     * The rest of the bean validation tests are run in com.ibm.ws.jsf_fat_jsf22.JSF20BeanValidation
     * This test was moved out of the above bucket because of a message difference between bean validation
     * 1.0 and 1.1
     *
     *
     * @throws Exception
     */
    @Test
    public void testValidationBeanTagBinding() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22beanvalServer, contextRoot, "BeanValidation.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /BeanValidationTests/BeanValidation.jsf");

            Log.info(c, name.getMethodName(), "Attempting to validate with a string greater than max length");
            HtmlTextInput bindingInputText = (HtmlTextInput) page.getElementById("binding");
            bindingInputText.setValueAttribute("aaa");
            page = doClick(page);

            Assert.assertTrue("Sting greater than max did not cause a validation error: \n\n" + page.asText(),
                              page.getElementById("bindingError").getTextContent().equals("binding: Validation Error: Length is greater than allowable maximum of '2'"));

            Log.info(c, name.getMethodName(), "Navigating to: /BeanValidationTests/BeanValidation.jsf");

            page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Attempting to validate with a string of max length");
            bindingInputText = (HtmlTextInput) page.getElementById("binding");
            bindingInputText.setValueAttribute("aa");
            page = doClick(page);

            Assert.assertTrue("Valid input caused a validation error: \n\n" + page.asText(),
                              page.getElementById("success").getTextContent().equals("SUCCESS"));
        }
    }

    private HtmlPage doClick(HtmlPage page) throws Exception {
        HtmlElement button = (HtmlElement) page.getElementById("Validate");

        return button.click();
    }
}
