/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf.container.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jsf.beanval.BeanValTestServlet;

@RunWith(FATRunner.class)
public class JSF22BeanValidationTests extends FATServletClient {

    private static final String MOJARRA_APP = "BeanValidationTests";
    private static final String MYFACES_APP = "BeanValidationTests_MyFaces";

    @Server("jsf.container.2.3_fat.beanval")
    @TestServlets({
                    @TestServlet(servlet = BeanValTestServlet.class, path = MOJARRA_APP + "/BeanValTestServlet"),
                    @TestServlet(servlet = BeanValTestServlet.class, path = MYFACES_APP + "/BeanValTestServlet")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive mojarraApp = ShrinkWrap.create(WebArchive.class, MOJARRA_APP + ".war")
                        .addPackage("jsf.beanval");
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "publish/files/permissions");
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "test-applications/" + MOJARRA_APP + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);
        server.addInstalledAppForValidation(MOJARRA_APP);

        WebArchive myfacesApp = ShrinkWrap.create(WebArchive.class, MYFACES_APP + ".war")
                        .addPackage("jsf.beanval");
        myfacesApp = FATSuite.addMyFaces(myfacesApp);
        myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "publish/files/permissions");
        myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "test-applications/" + MOJARRA_APP + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", myfacesApp);
        server.addInstalledAppForValidation(MYFACES_APP);

        server.startServer();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        server.stopServer();
    }

    @Test
    public void verifyAppProviders() throws Exception {
        server.resetLogMarks();
        server.waitForStringInLogUsingMark("Initializing Mojarra .* for context '/" + MOJARRA_APP + "'");
        server.resetLogMarks();
        server.waitForStringInLogUsingMark("MyFaces Bean Validation support enabled");
    }

    @Test
    public void testValidationBeanTagBinding_Mojarra() throws Exception {
        testValidationBeanTagBinding(MOJARRA_APP);
    }

    @Test
    public void testValidationBeanTagBinding_MyFaces() throws Exception {
        testValidationBeanTagBinding(MYFACES_APP);
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
     */
    private void testValidationBeanTagBinding(String app) throws Exception {
        try (WebClient webClient = new WebClient()) {

            HtmlPage page = (HtmlPage) webClient.getPage(getServerURL() + app + "/BeanValidation.jsf");

            Log.info(getClass(), testName.getMethodName(), "Navigating to: /BeanValidationTests/BeanValidation.jsf");
            Log.info(getClass(), testName.getMethodName(), "Attempting to validate with a string greater than max length");
            HtmlTextInput bindingInputText = (HtmlTextInput) page.getElementById("binding");
            bindingInputText.setValueAttribute("aaa");
            page = doClick(page);

            Assert.assertTrue("Sting greater than max did not cause a validation error: \n\n" + page.asText(),
                              page.getElementById("bindingError").getTextContent().equals("binding: Validation Error: Length is greater than allowable maximum of '2'"));

            Log.info(getClass(), testName.getMethodName(), "Navigating to: /BeanValidationTests/BeanValidation.jsf");
            page = (HtmlPage) webClient.getPage(getServerURL() + app + "/BeanValidation.jsf");

            Log.info(getClass(), testName.getMethodName(), "Attempting to validate with a string of max length");
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

    private static String getServerURL() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + '/';
    }
}
