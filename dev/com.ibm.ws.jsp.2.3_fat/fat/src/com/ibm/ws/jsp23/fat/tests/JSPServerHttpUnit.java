/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests to execute on the jspServer that use HttpUnit.
 */

// No need to run against cdi-2.0 since these tests don't use CDI at all.
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSPServerHttpUnit extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(JSPServerTest.class.getName());
    private static final String APP_NAME = "TestJSP2.3";

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("jspServer");

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(SHARED_SERVER.getLibertyServer(),
                                      APP_NAME + ".war",
                                      "com.ibm.ws.jsp23.fat.testjsp23.beans",
                                      "com.ibm.ws.jsp23.fat.testjsp23.interceptors",
                                      "com.ibm.ws.jsp23.fat.testjsp23.listeners",
                                      "com.ibm.ws.jsp23.fat.testjsp23.servlets",
                                      "com.ibm.ws.jsp23.fat.testjsp23.tagHandler");

        SHARED_SERVER.getLibertyServer().addInstalledAppForValidation(APP_NAME);
        SHARED_SERVER.startIfNotStarted();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server, below are the expected exception:
        // SRVE0315E: An exception occurred: java.lang.Throwable: javax.el.MethodNotFoundException:
        //      No matching public static method named [nonStaticMethod] found on class
        // [    com.ibm.ws.jsp23.fat.testjsp23.beans.EL30StaticFieldsAndMethodsBean]
        // SRVE0777E: Exception thrown by application class 'javax.el.StaticFieldELResolver.invoke:128'
        //      SRVE0315E: An exception occurred: java.lang.Throwable: javax.el.MethodNotFoundException:
        //      No matching public static method named [nonStaticMethod] found on
        //      class [com.ibm.ws.jsp23.fat.testjsp23.beans.EL30StaticFieldsAndMethodsBean]
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer("SRVE0315E", "SRVE0777E");
        }
    }

    /**
     * A sample HttpUnit test case for JSP. Just ensure that the basic application is reachable.
     *
     * @throws Exception
     */
    @Test
    public void sampleTest() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestJSP2.3";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/SimpleTestServlet"));
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains("Hello World"));
    }

    /**
     * Test the EL 3.0 static fields and methods functionality
     *
     * @throws Exception
     */
    @Test
    public void testEL30StaticFieldsAndMethods() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestJSP2.3";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/EL30StaticFieldsAndMethodsTests.jsp"));
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);

        assertTrue(response.getText().contains("Boolean.TRUE | true =? true"));
        assertTrue(response.getText().contains("Boolean(true) | true =? true"));
        assertTrue(response.getText().contains("Integer('1000') | 1000 =? 1000"));

        assertTrue(response.getText().contains("EL30StaticFieldsAndMethodsEnum.TEST_ONE | TEST_ONE =? TEST_ONE"));
        assertTrue(response.getText().contains("EL30StaticFieldsAndMethodsBean.staticReference | static reference =? static reference"));
        assertTrue(response.getText().contains("EL30StaticFieldsAndMethodsBean.staticMethod() | static method =? static method"));
        assertTrue(response.getText().contains("EL30StaticFieldsAndMethodsBean.staticMethodParam(\"static method param\") | static method param =? static method param"));
    }

    /**
     * Make sure a MethodNotFoundException is thrown when calling a non static method from EL
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("javax.el.MethodNotFoundException")
    public void testEL30MethodNotFoundException() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestJSP2.3";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/EL30MethodNotFoundException.jsp"));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertTrue(response.getText().contains("javax.el.MethodNotFoundException"));
    }

    /**
     * Make sure a PropertyNotFoundException is thrown when referencing a non-static field from EL
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("javax.el.PropertyNotFoundException")
    @Mode(TestMode.FULL)
    public void testEL30PropertyNotFoundException() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestJSP2.3";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/EL30PropertyNotFoundException.jsp"));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertTrue(response.getText().contains("javax.el.PropertyNotFoundException"));
    }

    /**
     * Make sure a PropertyNotWritableException is thrown when trying to write to a field via EL
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("javax.el.PropertyNotWritableException")
    @Mode(TestMode.FULL)
    public void testEL30PropertyNotWritableException() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestJSP2.3";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/EL30PropertyNotWritableException.jsp"));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertTrue(response.getText().contains("javax.el.PropertyNotWritableException"));
    }

    /**
     * According to the EL 3.0 specification for the assignment operator:
     *
     * If base-a is null, and prop-a is a String,
     * ■ If prop-a is a Lambda parameter, throw a
     * PropertyNotWritableException
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("javax.el.PropertyNotWritableException")
    @Mode(TestMode.FULL)
    public void testEL30AssignmentOperatorException() throws Exception {
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestJSP2.3";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + "/EL30AssignmentOperatorException.jsp"));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertTrue(response.getText().contains("javax.el.PropertyNotWritableException"));
    }

    /**
     * Test EL 3.0 Reserved Words in a JSP
     *
     * The expected JSP exception for Reserved Words is a JspTranslationException
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("javax.el.ELException")
    @AllowedFFDC("java.security.PrivilegedActionException")
    @Mode(TestMode.FULL)
    public void testEL30ReservedWords() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        reservedWordsHelperMethod(wc, "/EL30ReservedWords/and.jsp", "Unable to parse EL function ${test.and}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/or.jsp", "Unable to parse EL function ${test.or}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/not.jsp", "Unable to parse EL function ${test.not}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/eq.jsp", "Unable to parse EL function ${test.eq}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/ne.jsp", "Unable to parse EL function ${test.ne}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/lt.jsp", "Unable to parse EL function ${test.lt}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/gt.jsp", "Unable to parse EL function ${test.gt}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/le.jsp", "Unable to parse EL function ${test.le}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/ge.jsp", "Unable to parse EL function ${test.ge}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/true.jsp", "Unable to parse EL function ${test.true}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/false.jsp", "Unable to parse EL function ${test.false}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/null.jsp", "Unable to parse EL function ${test.null}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/instanceof.jsp", "Unable to parse EL function ${test.instanceof}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/empty.jsp", "Unable to parse EL function ${test.empty}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/div.jsp", "Unable to parse EL function ${test.div}");
        reservedWordsHelperMethod(wc, "/EL30ReservedWords/mod.jsp", "Unable to parse EL function ${test.mod}");

        nonReservedWordsHelperMethod(wc, "/EL30ReservedWords/NonReservedWords.jsp?testNonReservedWord=cat", "Testing \"cat\" non-reserved word. Test Successful");
        nonReservedWordsHelperMethod(wc, "/EL30ReservedWords/NonReservedWords.jsp?testNonReservedWord=T", "Testing \"T\" non-reserved word. Test Successful");
    }

    /**
     * Send a request to the corresponding reserved word in a jsp file (to evaluate them) depending on the path
     *
     * @param wc             WebConversation
     * @param path           Path of the request url
     * @param expectedString Expected String in the response
     * @throws Exception
     */
    private void reservedWordsHelperMethod(WebConversation wc, String path, String expectedString) throws Exception {
        String contextRoot = "/TestJSP2.3";
        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + path));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertTrue(response.getText().contains("com.ibm.ws.jsp.translator.JspTranslationException"));
        assertTrue(response.getText().contains(expectedString));
    }

    /**
     * Send a request to the non-reserved words jsp file in order to evaluate them
     *
     * @param wc             WebConversation
     * @param path           Path of the request url
     * @param expectedString Expected String in the response
     * @throws Exception
     */
    private void nonReservedWordsHelperMethod(WebConversation wc, String path, String expectedString) throws Exception {
        String contextRoot = "/TestJSP2.3";
        WebRequest request = new GetMethodWebRequest(SHARED_SERVER.getServerUrl(true, contextRoot + path));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().contains(expectedString));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

}
