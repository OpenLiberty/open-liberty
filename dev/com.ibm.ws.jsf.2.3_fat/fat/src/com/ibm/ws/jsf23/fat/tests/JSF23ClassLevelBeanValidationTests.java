/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * JSF 2.3 test cases for the class level bean validation tag f:validateWholeBean.
 */
@RunWith(FATRunner.class)
public class JSF23ClassLevelBeanValidationTests {

    protected static final Class<?> c = JSF23ClassLevelBeanValidationTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIBVServer")
    public static LibertyServer jsf23CDIBVServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIBVServer, "ClassLevelBeanValidation.war", "com.ibm.ws.jsf23.fat.classlevel.bval.beans");

        // Start the server and use the class name so we can find logs easily.
        jsf23CDIBVServer.startServer(JSF23ClassLevelBeanValidationTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIBVServer != null && jsf23CDIBVServer.isStarted()) {
            jsf23CDIBVServer.stopServer();
        }
    }

    /**
     * This test case ensures that the class-level bean validation is called and validates the two input text values.
     * The bean will be copied because it implements Serializable as noted in the validateWholeBean VLDDoc:
     * "If the bean implements Serializable, use that to copy the bean instance."
     * The first part of the test will input two passwords that do NOT match and will check for the validation message displayed.
     * The second part of the test will input two passwords that DO match and will check for the password to be displayed on the page.
     *
     * @throws Exception
     */
    @Test
    public void testValidateWholeBeanSerialized() throws Exception {
        String contextRoot = "ClassLevelBeanValidation";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "validateWholeBeanSerialized.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            HtmlTextInput password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            //Enter passwords that do not match in each input text
            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testUnMatched");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //There should be a validation failure message displayed on the page because the two password fields did not match.
            assertTrue("The class-level bean validate failed, password mismatch message not displayed.", page.asText().contains("Password fields must match"));

            //Enter matching passwords in each input text
            password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testPassword");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the validation code works properly the password will be displayed on the page and the previous validation message will be gone
            assertTrue("The class-level bean validate failed, password not displayed.", page.asText().contains("password1: testPassword"));
            assertFalse("The class-level bean validate failed, The validation message regarding mismatched passwords is displayed.",
                        page.asText().contains("Password fields must match"));
        }
    }

    /**
     * This test case ensures that the class-level bean validation is called and validates the two input text values.
     * The bean will be copied because it implements Cloneable as noted in the validateWholeBean VLDDoc:
     * "Otherwise, if the bean implements Cloneable, clone the bean instance."
     * The first part of the test will input two passwords that do NOT match and will check for the validation message displayed.
     * The second part of the test will input two passwords that DO match and will check for the password to be displayed on the page.
     *
     * @throws Exception
     */
    @Test
    public void testValidateWholeBeanCloneable() throws Exception {
        String contextRoot = "ClassLevelBeanValidation";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "validateWholeBeanCloneable.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            HtmlTextInput password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            //Enter passwords that do not match in each input text
            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testUnMatched");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //There should be a validation failure message displayed on the page because the two password fields did not match.
            assertTrue("The class-level bean validate failed, password mismatch message not displayed.", page.asText().contains("Password fields must match"));

            //Enter matching passwords in each input text
            password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testPassword");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the validation code works properly the password will be displayed on the page and the previous validation message will be gone
            assertTrue("The class-level bean validate failed, password not displayed.", page.asText().contains("password1: testPassword"));
            assertFalse("The class-level bean validate failed, The validation message regarding mismatched passwords is displayed.",
                        page.asText().contains("Password fields must match"));
        }
    }

    /**
     * This test case ensures that the class-level bean validation is called and validates the two input text values.
     * The bean will be copied because it has a copy constructor as noted in the validateWholeBean VLDDoc:
     * "Otherwise, if the bean has a copy constructor, use that to copy the bean instance."
     * The first part of the test will input two passwords that do NOT match and will check for the validation message displayed.
     * The second part of the test will input two passwords that DO match and will check for the password to be displayed on the page.
     *
     * @throws Exception
     */
    @Test
    public void testValidateWholeBeanCopyConstructor() throws Exception {
        String contextRoot = "ClassLevelBeanValidation";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "validateWholeBeanCopyConstructor.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            HtmlTextInput password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            //Enter passwords that do not match in each input text
            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testUnMatched");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //There should be a validation failure message displayed on the page because the two password fields did not match.
            assertTrue("The class-level bean validate failed, password mismatch message not displayed.", page.asText().contains("Password fields must match"));

            //Enter matching passwords in each input text
            password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testPassword");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the validation code works properly the password will be displayed on the page and the previous validation message will be gone
            assertTrue("The class-level bean validate failed, password not displayed.", page.asText().contains("password1: testPassword"));
            assertFalse("The class-level bean validate failed, The validation message regarding mismatched passwords is displayed.",
                        page.asText().contains("Password fields must match"));
        }
    }

    /**
     * The purpose of this test will be based on the statement in the VDLDoc for the validateWholeBean tag, which states:
     * "Invoke the newInstance() method on the bean's Class. If this throws any Exception, swallow it and continue."
     * To accomplish this, the constructor is private instead of public. This will throw an IllegalAccessException, which MyFaces will
     * log, if tracing is enabled, and continue on with the copying of the bean.
     * The bean will be copied because it implements Serializable.
     * The first part of the test will input two passwords that do NOT match and the password "2short" in the password1 field will not pass the field-level validation.
     * The second part of the test will input two passwords that DO match and will check for the password to be displayed on the page.
     * Also, check the trace file to make sure the IllegalAccessException was logged.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testValidateWholeBeanIllegalAccess() throws Exception {
        String contextRoot = "ClassLevelBeanValidation";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "validateWholeBeanIllegalAccess.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            HtmlTextInput password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            //Enter passwords that do not match in each input text
            password1Input.setValueAttribute("2short");
            password2Input.setValueAttribute("testpassword");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //There should be a validation failure message displayed on the page because the two password fields did not match.
            assertTrue("The field-level validation did not display the 'form1:password1: Password must be between 8 and 16 characters long' message.",
                       page.asText().contains("form1:password1: Password must be between 8 and 16 characters long"));

            //Enter matching passwords in each input text
            password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testPassword");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the validation code works properly the password will be displayed on the page and the previous validation message will be gone
            assertTrue("The class-level bean validate failed, password not displayed.", page.asText().contains("password1: testPassword"));
            assertFalse("The class-level bean validate failed, The validation message regarding mismatched passwords is displayed.",
                        page.asText().contains("Password fields must match"));

            /*
             * check the trace file to make sure the IllegalAcessException was logged.
             *
             * Need to use a regular expression because different JDKs format this differently:
             *
             * Class java.lang.IllegalAccessException: Class org.apache.myfaces.component.validate.WholeBeanValidator can not access a member of class
             * com.ibm.ws.jsf23.fat.beans.TestPasswordBeanIllegalAccess with modifiers "private"
             *
             * Class java.lang.IllegalAccessException: Class org/apache/myfaces/component/validate/WholeBeanValidator illegally accessing "private" member of class
             * com/ibm/ws/jsf23/fat/beans/TestPasswordBeanIllegalAccess
             */

            String illegalAccessException = jsf23CDIBVServer
                            .waitForStringInTraceUsingLastOffset("java.lang.IllegalAccessException:.*WholeBeanValidator.*TestPasswordBeanIllegalAccess.*");
            assertTrue("The IllegalAccessException was not logged in the trace file.", illegalAccessException != null);
        }
    }

    /**
     * The purpose of this test will be based on the statement in the VDLDoc for the validateWholeBean tag, which states:
     * "5. If none of these techniques yields a copy, throw FacesException."
     * To accomplish this, the does not implement Serializable or Cloneable and it does not have a copy constructor.
     * MyFaces should log a NoSuchMethodException, if tracing is enabled, and then display a FacesException.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "javax.servlet.ServletException" })
    public void testValidateWholeBeanCopyFailure() throws Exception {
        String contextRoot = "ClassLevelBeanValidation";
        try (WebClient webClient = new WebClient()) {

            // Ensure the test does not fail due to the error condition we are creating
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            jsf23CDIBVServer.addIgnoredErrors(Arrays.asList("SRVE0777E.*"));

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "validateWholeBeanCopyFailure.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            HtmlTextInput password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            password1Input.setValueAttribute("testPassword");
            password2Input.setValueAttribute("testPassword");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the validation code works properly the password will be displayed on the page and the previous validation message will be gone
            assertTrue("The FacesException was not displayed.",
                       page.asText().contains("Cannot create copy for wholeBeanValidator:"));
        }
    }

    /**
     * The purpose of this test will to test the "disabled" attribute on the validateWholeBean.
     * The bean will set it to true. The class-level validation should not occur, therefore allowing two different passwords.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testValidateWholeBeanDisabled() throws Exception {
        String contextRoot = "ClassLevelBeanValidation";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIBVServer, contextRoot, "validateWholeBeanDisabled.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput password1Input = (HtmlTextInput) page.getElementById("form1:password1");
            HtmlTextInput password2Input = (HtmlTextInput) page.getElementById("form1:password2");

            //put two different passwords to test that the validateWholeBean does not validate that they do not match.
            password1Input.setValueAttribute("testPassword1");
            password2Input.setValueAttribute("testPassword2");

            // click the submit button to call the validateWholeBean
            page = page.getElementById("form1:button1").click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            //if the test works properly, the class level validation will not be performed, therefore allowing for two different passwords in the text boxes.
            //We will check that the password from the first text box is displayed, which means the validation didn't occur.
            assertTrue("The password was not displayed.", page.asText().contains("password1: testPassword1"));
            assertFalse("The class-level bean validate was called, The validation message regarding mismatched passwords is displayed when it should not have.",
                        page.asText().contains("Password fields must match"));
        }
    }
}
