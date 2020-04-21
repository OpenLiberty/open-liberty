/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsp23.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jspServer that use HttpUnit.
 */

// No need to run against cdi-2.0 since these tests don't use CDI at all.
@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSPServerHttpUnit {
    private static final Logger LOG = Logger.getLogger(JSPServerHttpUnit.class.getName());
    private static final String JSP23_APP_NAME = "TestJSP2.3";
    private static final String PI44611_APP_NAME = "PI44611";
    private static final String PI59436_APP_NAME = "PI59436";

    @Server("jspServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server,
                                      JSP23_APP_NAME + ".war",
                                      "com.ibm.ws.jsp23.fat.testjsp23.beans",
                                      "com.ibm.ws.jsp23.fat.testjsp23.interceptors",
                                      "com.ibm.ws.jsp23.fat.testjsp23.listeners",
                                      "com.ibm.ws.jsp23.fat.testjsp23.servlets",
                                      "com.ibm.ws.jsp23.fat.testjsp23.tagHandler");

        ShrinkHelper.defaultDropinApp(server, PI44611_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, PI59436_APP_NAME + ".war");

        server.startServer(JSPServerHttpUnit.class.getSimpleName() + ".log");
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
        // SRVE8094W and SRVE8115W...Response already committed...
        //      Caused by testEL30ReservedWords();
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0315E", "SRVE0777E", "SRVE8094W", "SRVE8115W");
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
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "SimpleTestServlet");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: Hello World", response.getText().contains("Hello World"));
    }

    /**
     * Test the EL 3.0 static fields and methods functionality
     *
     * @throws Exception
     */
    @Test
    public void testEL30StaticFieldsAndMethods() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "EL30StaticFieldsAndMethodsTests.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        assertTrue("The response did not contain: Boolean.TRUE | true =? true", response.getText().contains("Boolean.TRUE | true =? true"));
        assertTrue("The response did not contain: Boolean(true) | true =? true", response.getText().contains("Boolean(true) | true =? true"));
        assertTrue("The response did not contain: Integer('1000') | 1000 =? 1000", response.getText().contains("Integer('1000') | 1000 =? 1000"));

        assertTrue("The response did not contain: EL30StaticFieldsAndMethodsEnum.TEST_ONE | TEST_ONE =? TEST_ONE",
                   response.getText().contains("EL30StaticFieldsAndMethodsEnum.TEST_ONE | TEST_ONE =? TEST_ONE"));

        assertTrue("The response did not contain: EL30StaticFieldsAndMethodsBean.staticReference | static reference =? static reference",
                   response.getText().contains("EL30StaticFieldsAndMethodsBean.staticReference | static reference =? static reference"));

        assertTrue("The response did not contain: EL30StaticFieldsAndMethodsBean.staticMethod() | static method =? static method",
                   response.getText().contains("EL30StaticFieldsAndMethodsBean.staticMethod() | static method =? static method"));

        assertTrue("The response did not contain: EL30StaticFieldsAndMethodsBean.staticMethodParam(\\\"static method param\\\") | static method param =? static method param",
                   response.getText().contains("EL30StaticFieldsAndMethodsBean.staticMethodParam(\"static method param\") | static method param =? static method param"));
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
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "EL30MethodNotFoundException.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());

        assertTrue("The response did not contain: javax.el.MethodNotFoundException", response.getText().contains("javax.el.MethodNotFoundException"));
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
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "EL30PropertyNotFoundException.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());
        assertTrue("The response did not contain: javax.el.PropertyNotFoundException", response.getText().contains("javax.el.PropertyNotFoundException"));
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
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "EL30PropertyNotWritableException.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());
        assertTrue("The response did not contain: javax.el.PropertyNotWritableException", response.getText().contains("javax.el.PropertyNotWritableException"));
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
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "EL30AssignmentOperatorException.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());
        assertTrue("The response did not contain: javax.el.PropertyNotWritableException", response.getText().contains("javax.el.PropertyNotWritableException"));
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

        reservedWordsHelperMethod(wc, "EL30ReservedWords/and.jsp", "Unable to parse EL function ${test.and}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/or.jsp", "Unable to parse EL function ${test.or}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/not.jsp", "Unable to parse EL function ${test.not}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/eq.jsp", "Unable to parse EL function ${test.eq}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/ne.jsp", "Unable to parse EL function ${test.ne}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/lt.jsp", "Unable to parse EL function ${test.lt}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/gt.jsp", "Unable to parse EL function ${test.gt}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/le.jsp", "Unable to parse EL function ${test.le}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/ge.jsp", "Unable to parse EL function ${test.ge}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/true.jsp", "Unable to parse EL function ${test.true}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/false.jsp", "Unable to parse EL function ${test.false}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/null.jsp", "Unable to parse EL function ${test.null}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/instanceof.jsp", "Unable to parse EL function ${test.instanceof}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/empty.jsp", "Unable to parse EL function ${test.empty}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/div.jsp", "Unable to parse EL function ${test.div}");
        reservedWordsHelperMethod(wc, "EL30ReservedWords/mod.jsp", "Unable to parse EL function ${test.mod}");

        nonReservedWordsHelperMethod(wc, "EL30ReservedWords/NonReservedWords.jsp?testNonReservedWord=cat", "Testing \"cat\" non-reserved word. Test Successful");
        nonReservedWordsHelperMethod(wc, "EL30ReservedWords/NonReservedWords.jsp?testNonReservedWord=T", "Testing \"T\" non-reserved word. Test Successful");
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
        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, path));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());
        assertTrue("The response did not contain: com.ibm.ws.jsp.translator.JspTranslationException",
                   response.getText().contains("com.ibm.ws.jsp.translator.JspTranslationException"));
        assertTrue("The response did not contain: " + expectedString, response.getText().contains(expectedString));
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
        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, path));
        WebResponse response = wc.getResponse(request);

        LOG.info("Response: " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());
        assertTrue("The response did not contain: " + expectedString, response.getText().contains(expectedString));
    }

    /**
     * Sample test
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testServlet() throws Exception {
        this.verifyStringInResponse(JSP23_APP_NAME, "SimpleTestServlet", "Hello World");
    }

    /**
     * Test the existing EL 2.2 operators in the context of a JSP.
     *
     * @throws Exception
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testEL22Operators() throws Exception {
        // Each entry in the array is an expected output in the response
        String[] expectedInResponse = {
                                        "<b>Test 1:</b> EL 2.2 Multiplication Operator (Expected:16): 16",
                                        "<b>Test 2:</b> EL 2.2 Addition Operator (+) (Expected:5): 5",
                                        "<b>Test 3:</b> EL 2.2 Subtraction Operator (-) (Expected:1): 1",
                                        "<b>Test 4:</b> EL 2.2 Division Operator (/) (Expected:8.0): 8.0",
                                        "<b>Test 5:</b> EL 2.2 Division Operator (div) (Expected:8.0): 8.0",
                                        "<b>Test 6:</b> EL 2.2 Remainder Operator (%) (Expected:1): 1",
                                        "<b>Test 7:</b> EL 2.2 Remainder Operator (mod) (Expected:1): 1",
                                        "<b>Test 8:</b> EL 2.2 Relational Operator (==) (Expected: true): true",
                                        "<b>Test 9:</b> EL 2.2 Relational Operator (eq) (Expected: false): false",
                                        "<b>Test 10:</b> EL 2.2 Relational Operator (!=) (Expected: true): true",
                                        "<b>Test 11:</b> EL 2.2 Relational Operator (ne) (Expected: false): false",
                                        "<b>Test 12:</b> EL 2.2 Relational Operator (<) (Expected: true): true",
                                        "<b>Test 13:</b> EL 2.2 Relational Operator (lt) (Expected: false): false",
                                        "<b>Test 14:</b> EL 2.2 Relational Operator (>) (Expected: false): false",
                                        "<b>Test 15:</b> EL 2.2 Relational Operator (gt) (Expected: true): true",
                                        "<b>Test 16:</b> EL 2.2 Relational Operator (<=) (Expected: true): true",
                                        "<b>Test 17:</b> EL 2.2 Relational Operator (le) (Expected: false): false",
                                        "<b>Test 18:</b> EL 2.2 Relational Operator (le) (Expected: true): true",
                                        "<b>Test 19:</b> EL 2.2 Relational Operator (>=) (Expected: true): true",
                                        "<b>Test 20:</b> EL 2.2 Relational Operator (ge) (Expected: false): false",
                                        "<b>Test 21:</b> EL 2.2 Relational Operator (ge) (Expected: true): true",
                                        "<b>Test 22:</b> EL 2.2 Logical Operator (&&) (Expected: false): false",
                                        "<b>Test 23:</b> EL 2.2 Logical Operator (&&) (Expected: true): true",
                                        "<b>Test 24:</b> EL 2.2 Logical Operator (and) (Expected: false): false",
                                        "<b>Test 25:</b> EL 2.2 Logical Operator (||) (Expected: true): true",
                                        "<b>Test 26:</b> EL 2.2 Logical Operator (||) (Expected: false): false",
                                        "<b>Test 27:</b> EL 2.2 Logical Operator (or) (Expected: true): true",
                                        "<b>Test 28:</b> EL 2.2 Logical Operator (!) (Expected: false): false",
                                        "<b>Test 29:</b> EL 2.2 Logical Operator (!) (Expected: true): true",
                                        "<b>Test 30:</b> EL 2.2 Logical Operator (not) (Expected: true): true",
                                        "<b>Test 31:</b> EL 2.2 Empty Operator (empty) (Expected: true): true",
                                        "<b>Test 32:</b> EL 2.2 Empty Operator (empty) (Expected: false): false",
                                        "<b>Test 33:</b> EL 2.2 Conditional Operator (A?B:C) (Expected: 2): 2",
                                        "<b>Test 34:</b> EL 2.2 Conditional Operator (A?B:C) (Expected: 3): 3" };

        this.verifyStringsInResponse(JSP23_APP_NAME, "EL22Operators.jsp", expectedInResponse);
    }

    /**
     * Test the new EL 3.0 operators in the context of a JSP.
     *
     * @throws Exception
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testEL30Operators() throws Exception {
        // Each entry in the array is an expected output in the response
        String[] expectedInResponse = {
                                        "<b>Test 1:</b> EL 3.0 String Concatenation Operator (+=) with literals (Expected: xy): xy",
                                        "<b>Test 2:</b> EL 3.0 String Concatenation Operator (+=) with variables (Expected: 12): 12",
                                        "<b>Test 3:</b> EL 3.0 String Concatenation Operator with literals and multiple concatenations (Expected: xyz): xyz",
                                        "<b>Test 4:</b> EL 3.0 String Concatenation Operator with literals and single quotes  (Expected: xyz): xyz",
                                        "<b>Test 5:</b> EL 3.0 String Concatenation Operator with literals and mixed quotes  (Expected: xyz): xyz",
                                        "<b>Test 6:</b> EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: \"x\"yz): \"x\"yz",
                                        "<b>Test 7:</b> EL 3.0 String Concatenation Operator with literals and escape characters  (Expected: 'x'yz): 'x'yz",
                                        "<b>Test 8:</b> EL 3.0 Assignment Operator (=) (Expected:3): 3",
                                        "<b>Test 9:</b> EL 3.0 Assignment Operator (=) (Expected:8): 8",
                                        "<b>Test 10:</b> EL 3.0 Assignment Operator (Expected:3): 3",
                                        "<b>Test 11:</b> EL 3.0 Semi-colon Operator (Expected:8): 8" };

        this.verifyStringsInResponse(JSP23_APP_NAME, "EL30Operators.jsp?testString1=1&testString2=2", expectedInResponse);
    }

    /**
     * Test the new EL 3.0 LambdaExpressions in the context of a JSP.
     *
     * @throws Exception
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testEL30LambdaExpressions() throws Exception {
        // Each entry in the array is an expected output in the response
        String[] expectedInResponse = {
                                        "<b>LambdaParam_EL3.0_Test 1:</b> --> 9",
                                        "<b>RejectExtraLambdaParam_EL3.0_Test 2:</b> --> 5",
                                        "<b>MultipleLambdaParams_EL3.0_Test 3:</b> --> 17",
                                        "<b>CatchExeptionOnLessParam_EL3.0_Test 4:</b> --> Pass another argument. Only [1] arguments were provided for a lambda expression that requires at least [2]",
                                        "<b>AssignedLambdaExp_EL3.0_Test 5:</b> --> 9",
                                        "<b>NoParam_EL3.0_Test 6:</b> --> 64",
                                        "<b>OptionalParenthesis_EL3.0_Test 7:</b> --> 64",
                                        "<b>PrintFromBody_EL3.0_Test 8:</b> -->",
                                        "<b>ParameterCocerceToString_EL3.0_Test 9:</b> --> -1",
                                        "<b>ParameterCocerceToInt_EL3.0_Test 10:</b> --> 0",
                                        "<b>InvokeFunctionIndirect_EL3.0_Test 11:</b> --> 11",
                                        "<b>InvokeFunctionIndirect2_EL3.0_Test 12:</b> --> 120",
                                        "<b>PassedAsArgumentToMethod_EL3.0_Test 13:</b> -->  Charlie NAME MATCHES: Charlie;",
                                        "<b>Nested1_EL3.0_Test 14:</b> --> 12",
                                        "<b>Nested2_EL3.0_Test 15:</b> --> 2468"
        };

        this.verifyStringsInResponse(JSP23_APP_NAME, "EL30Lambda.jsp", expectedInResponse);
    }

    /**
     * Test Servlet 3.1 request/response API
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testServlet31RequestResponse() throws Exception {
        String[] expectedInResponse = { "JSP to test Servlet 3.1 Request and Response",
                                        "Testing BASIC_AUTH static field from HttpServletRequest (Expected: BASIC): BASIC",
                                        "Testing request.getParameterNames method (Expected: [firstName, lastName]): [lastName, firstName]",
                                        "Testing request.getParameter method (Expected: John): John",
                                        "Testing request.getParameter method (Expected: Smith): Smith",
                                        "Testing request.getQueryString method (Expected: firstName=John&lastName=Smith): firstName=John&lastName=Smith",
                                        "Testing request.getContextPath method (Expected: /TestJSP2.3): /TestJSP2.3",
                                        "Testing request.getRequestURI method (Expected: /TestJSP2.3/Servlet31RequestResponseTest.jsp): /TestJSP2.3/Servlet31RequestResponseTest.jsp",
                                        "Testing request.getMethod method (Expected: GET): GET",
                                        "Testing request.getContentLengthLong method (Expected: -1): -1",
                                        "Testing request.getProtocol method (Expected: HTTP/1.1): HTTP/1.1",
                                        "Testing SC_NOT_FOUND static field from HttpServletResponse (Expected: 404): 404",
                                        "Testing response.getStatus method (Expected: 200): 200",
                                        "Testing response.getBufferSize method (Expected: 4096): 4096",
                                        "Testing response.getCharacterEncoding method (Expected: ISO-8859-1): ISO-8859-1",
                                        "Testing response.getContentType method (Expected: text/html; charset=ISO-8859-1): text/html; charset=ISO-8859-1",
                                        "Testing response.containsHeader method (Expected: true): true",
                                        "Testing response.isCommitted method (Expected: false): false" };

        this.verifyStringsInResponse(JSP23_APP_NAME, "Servlet31RequestResponseTest.jsp?firstName=John&lastName=Smith", expectedInResponse);
    }

    /**
     * Test EL 3.0 invocations of Method Expressions in a JSP
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testMethodExpressionInvocations() throws Exception {
        String[] expectedInResponse = { "Get Parent Name Using Value Expression (Expected: \"John Smith Sr.\"): John Smith Sr.",
                                        "Get Child Name Using Value Expression (Expected: \"John Smith Jr.\"): John Smith Jr.",
                                        "Get Object Representation Using Value Expression: toString method of object with current parent name John Smith Sr.",
                                        "Get Parent Name Using Method Expression (Expected: \"Steven Johnson Sr.\"): Steven Johnson Sr.",
                                        "Get Child Name Using Method Expression (Expected: \"Steven Johnson Jr.\"): Steven Johnson Jr.",
                                        "Get Object Representation Using Method Expression: toString method of object with current parent name Steven Johnson Sr." };

        this.verifyStringsInResponse(JSP23_APP_NAME, "EL30InvocationMethodExpressions.jsp", expectedInResponse);
    }

    /**
     * Test EL 3.0 Operator Precedence in a JSP
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    @Mode(TestMode.FULL)
    public void testOperatorPrecedence() throws Exception {
        String[] expectedInResponse = { "<b>Test 1:</b> EL 3.0 [] and . operators left-to-right (Expected:true): true",
                                        "<b>Test 2:</b> EL 3.0 [] and . operators left-to-right (Expected:true): true",
                                        "<b>Test 3:</b> EL 3.0 Parenthesis Operator with - (unary) (Expected:-14): -14",
                                        "<b>Test 4:</b> EL 3.0 Parenthesis Operator with - (unary) (Expected:-10): -10",
                                        "<b>Test 5:</b> EL 3.0 not ! empty operators left-to-right (Expected:true): true",
                                        "<b>Test 6:</b> EL 3.0 Parenthesis Operator with not ! empty operators (Expected:true): true",
                                        "<b>Test 7:</b> EL 3.0 Parenthesis Operator with not ! empty operators (Expected:false): false",
                                        "<b>Test 8:</b> EL 3.0 * / div % mod operators left-to-right (Expected:1.0): 1.0",
                                        "<b>Test 9:</b> EL 3.0 Parenthesis Operator with * / div % mod operators (Expected:16.0): 16.0",
                                        "<b>Test 10:</b> EL 3.0 + - operators left-to-right (Expected:5): 5",
                                        "<b>Test 11:</b> EL 3.0 + - * / div operators (Expected:31.0): 31.0",
                                        "<b>Test 12:</b> EL 3.0 Parenthesis Operator with + - * / div operators (Expected:45.0): 45.0",
                                        "<b>Test 13:</b> EL 3.0 String Concatenation Operator (+=) and + operator (Expected:3abc): 3abc",
                                        "<b>Test 14:</b> EL 3.0 < > <= >= lt gt le ge relational operators left-to-right (Expected:true): true",
                                        "<b>Test 15:</b> EL 3.0 < > relational operators with + - operators (Expected:false): false",
                                        "<b>Test 16:</b> EL 3.0 == != eq ne relational operators left-to-right (Expected:true): true",
                                        "<b>Test 17:</b> EL 3.0 == and <= relational operators (Expected:true): true",
                                        "<b>Test 18:</b> EL 3.0 != and > relational operators (Expected:false): false",
                                        "<b>Test 19:</b> EL 3.0 && and || logical operators (Expected:true): true",
                                        "<b>Test 20:</b> EL 3.0 and or logical operators (Expected:true): true",
                                        "<b>Test 21:</b> EL 3.0 ? and : conditional operators (Expected:2): 2",
                                        "<b>Test 22:</b> EL 3.0 ? and : conditional operators (Expected:3): 3",
                                        "<b>Test 23:</b> EL 3.0 -> (lambda) operator (Expected:60): 60",
                                        "<b>Test 24:</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with concatenation operator (+=) (Expected:13): 13",
                                        "<b>Test 25:</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:5): 5",
                                        "<b>Test 26:</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:11): 11" };

        this.verifyStringsInResponse(JSP23_APP_NAME, "EL30OperatorPrecedences.jsp", expectedInResponse);
    }

    /**
     * Test EL 3.0 Coercion Rules in a JSP
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testEL30CoercionRules() throws Exception {
        String[] expectedInResponse = { "Testing Coercion of a Value X to Type Y.",
                                        "Test if X is null and Y is not a primitive type and also not a String, return null (Expected:true): true" };

        this.verifyStringsInResponse(JSP23_APP_NAME, "EL30CoercionRules.jsp", expectedInResponse);
    }

    /**
     * Test EL 3.0 List Operations on Collection Objects in a JSP
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    @Mode(TestMode.FULL)
    public void testEL30ListCollectionObjectOperations() throws Exception {
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=filter",
                                    "Filter: [4, 3, 5, 3]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=map",
                                    "Map: [3, 6, 5, 4, 7, 5, 3]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=flatMap",
                                    "FlatMap: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=distinct",
                                    "Distinct: [1, 4, 3, 2, 5]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=sorted",
                                    "Sorted in Decreasing: [5, 4, 3, 3, 2, 1, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=forEach",
                                    "ForEach: 1432531");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=peek",
                                    "Debug Peek: 144322531[4, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=iterator",
                                    "Iterator: 1 4 3 2 5 3 1");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=limit",
                                    "Limit: [1, 4, 3]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=substream",
                                    "Substream: [3, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=toArray",
                                    "ToArray: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=toList",
                                    "ToList: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=reduce",
                                    "Reduce: 5");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=max",
                                    "Max: 5");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=min",
                                    "Min: 1");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=average",
                                    "Average: 2.7142857142857144");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=sum",
                                    "Sum: 19");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=count",
                                    "Count: 7");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=anyMatch",
                                    "AnyMatch: true");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=allMatch",
                                    "AllMatch: false");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=noneMatch",
                                    "NoneMatch: true");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testListCollectionOperations=findFirst",
                                    "FindFirst: 1");
    }

    /**
     * Test EL 3.0 Set Operations on Collection Objects in a JSP
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testEL30SetCollectionObjectOperations() throws Exception {
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=filter",
                                    "Filter: [4, 3, 5]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=map",
                                    "Map: [3, 6, 5, 7, 4]");
        this.verifyStringInResponse(JSP23_APP_NAME, "/EL30CollectionObjectOperations.jsp?testSetCollectionOperations=flatMap",
                                    "FlatMap: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=distinct",
                                    "Distinct: [1, 4, 3, 5, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=sorted",
                                    "Sorted in Decreasing: [5, 4, 3, 2, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=forEach",
                                    "ForEach: 14352");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=peek",
                                    "Debug Peek: 1443522[4, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=iterator",
                                    "Iterator: 1 4 3 5 2");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=limit",
                                    "Limit: [1, 4, 3]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=substream",
                                    "Substream: [3, 5]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=toArray",
                                    "ToArray: [1, 4, 3, 5, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=toList",
                                    "ToList: [1, 4, 3, 5, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=reduce",
                                    "Reduce: 5");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=max",
                                    "Max: 5");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=min",
                                    "Min: 1");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=average",
                                    "Average: 3.0");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=sum",
                                    "Sum: 15");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=count",
                                    "Count: 5");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=anyMatch",
                                    "AnyMatch: true");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=allMatch",
                                    "AllMatch: false");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=noneMatch",
                                    "NoneMatch: true");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testSetCollectionOperations=findFirst",
                                    "FindFirst: 1");
    }

    /**
     * Test EL 3.0 Map Operations on Collection Objects in a JSP
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    @Mode(TestMode.FULL)
    public void testEL30MapCollectionObjectOperations() throws Exception {
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=filter",
                                    "Filter: [4, 3, 5, 3]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=map",
                                    "Map: [3, 6, 5, 4, 7, 5, 3]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=flatMap",
                                    "FlatMap: [1, 4, 3, 2, 5, 3, 1, 1, 4, 3, 2, 5, 3, 1, 1, 4, 3, 2, 5, 3, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=distinct",
                                    "Distinct: [1, 4, 3, 2, 5]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=sorted",
                                    "Sorted in Decreasing: [5, 4, 3, 3, 2, 1, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=forEach",
                                    "ForEach: 1432531");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=peek",
                                    "Debug Peek: 144322531[4, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=iterator",
                                    "Iterator: 1 4 3 2 5 3 1");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=limit",
                                    "Limit: [1, 4, 3]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=substream",
                                    "Substream: [3, 2]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=toArray",
                                    "ToArray: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=toList",
                                    "ToList: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=reduce",
                                    "Reduce: 5");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=max",
                                    "Max: 5");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=min",
                                    "Min: 1");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=average",
                                    "Average: 2.7142857142857144");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=sum",
                                    "Sum: 19");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=count",
                                    "Count: 7");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=anyMatch",
                                    "AnyMatch: true");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=allMatch",
                                    "AllMatch: false");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=noneMatch",
                                    "NoneMatch: true");
        this.verifyStringInResponse(JSP23_APP_NAME, "EL30CollectionObjectOperations.jsp?testMapCollectionOperations=findFirst",
                                    "FindFirst: 1");
    }

    /**
     * Test JSP 2.3 Resolution of Variables and their Properties
     *
     * @throws Exception
     *                       if something goes wrong
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Test
    public void testJSP23ResolutionVariableProperties() throws Exception {
        String[] expectedInResponse = { "class org.apache.el.stream.StreamELResolverImpl",
                                        "class javax.el.StaticFieldELResolver",
                                        "class javax.el.MapELResolver",
                                        "class javax.el.ResourceBundleELResolver",
                                        "class javax.el.ListELResolver",
                                        "class javax.el.ArrayELResolver",
                                        "class javax.el.BeanELResolver",
                                        "The order and number of ELResolvers from the CompositeELResolver are correct!",
                                        "Testing StaticFieldELResolver with Boolean.TRUE (Expected: true): true",
                                        "Testing StaticFieldELResolver with Integer.parseInt (Expected: 86): 86",
                                        "Testing StreamELResolver with distinct method (Expected: [1, 4, 3, 2, 5]): [1, 4, 3, 2, 5]",
                                        "Testing StreamELResolver with filter method (Expected: [4, 3, 5, 3]): [4, 3, 5, 3]" };

        this.verifyStringsInResponse(JSP23_APP_NAME, "ResolutionVariablesPropertiesServlet", expectedInResponse);
    }

    /**
     * Test Tag Handlers with CDI.
     *
     * @throws Exception
     */
    @Test
    public void testTag1() throws Exception {
        // Each entry in the array is an expected output in the response
        String[] expectedInResponse = {
                                        "<b>Test 1:</b> Test Start",
                                        "<b>Test 2:</b> Message: DependentBean Hit SessionBean Hit RequestBean Hit ...constructor injection OK ...interceptor OK",
        };

        this.verifyStringsInResponse(JSP23_APP_NAME, "Tag1.jsp", expectedInResponse);
    }

    /**
     * Test Tag Handlers with CDI Method Injection.
     *
     * @throws Exception
     */
    @Test
    public void testTag2() throws Exception {
        // Each entry in the array is an expected output in the response
        String[] expectedInResponse = {
                                        "<b>Test 1:</b> Test Start",
                                        "<b>Test 2:</b> Message: BeanCounters are OK"
        };

        this.verifyStringsInResponse(JSP23_APP_NAME, "Tag2.jsp", expectedInResponse);
    }

    /**
     * Test the tag library event listeners injected using CDI.
     * Constructor injection.
     *
     * @throws Exception
     */
    @Test
    public void testTagLibraryEventListenerCI() throws Exception {
        final int iterations = 5;

        DefaultHttpClient client = new DefaultHttpClient();
        DefaultHttpClient client2 = new DefaultHttpClient(); //Used to test @SessionScoped

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "TagLibraryEventListenerCI.jsp?increment=true");
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);
        HttpResponse response;

        for (int i = 1; i <= iterations; i++) {
            String[] expectedInResponse = {
                                            "<li>TestConstructorInjection index: " + i + "</li>",
                                            "<li>TestConstructorInjectionRequest index: " + 1 + "</li>",
                                            "<li>TestConstructorInjectionApplication index: " + i + "</li>",
                                            "<li>TestConstructorInjectionSession index: " + i + "</li>"
            };
            response = client.execute(getMethod);
            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getStatusLine().getStatusCode());

            String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
            LOG.info("Response content: " + content);
            org.apache.http.util.EntityUtils.consume(response.getEntity());

            for (String expectedResponse : expectedInResponse) {
                assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
            }
        }

        //Testing if a new TestConstructorInjectionSession was injected when another session is used
        String[] expectedInResponse = {
                                        "<li>TestConstructorInjection index: " + (iterations + 1) + "</li>",
                                        "<li>TestConstructorInjectionRequest index: " + 1 + "</li>",
                                        "<li>TestConstructorInjectionApplication index: " + (iterations + 1) + "</li>",
                                        "<li>TestConstructorInjectionSession index: " + 1 + "</li>"
        };

        response = client2.execute(getMethod);
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getStatusLine().getStatusCode());

        String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
        LOG.info("Response content: " + content);
        org.apache.http.util.EntityUtils.consume(response.getEntity());

        for (String expectedResponse : expectedInResponse) {
            assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
        }
    }

    /**
     * Test the tag library event listeners injected using CDI.
     * Field injection.
     *
     * @throws Exception
     */
    @Test
    public void testTagLibraryEventListenerFI() throws Exception {
        final int iterations = 5;

        DefaultHttpClient client = new DefaultHttpClient();
        DefaultHttpClient client2 = new DefaultHttpClient(); //Used to test @SessionScoped

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "TagLibraryEventListenerFI.jsp?increment=true");
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);
        HttpResponse response;

        for (int i = 1; i <= iterations; i++) {
            String[] expectedInResponse = {
                                            "<li>TestFieldInjection index: " + i + "</li>",
                                            "<li>TestFieldInjectionRequest index: " + 1 + "</li>",
                                            "<li>TestFieldInjectionApplication index: " + i + "</li>",
                                            "<li>TestFieldInjectionSession index: " + i + "</li>"
            };

            response = client.execute(getMethod);
            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getStatusLine().getStatusCode());

            String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
            LOG.info("Response content: " + content);
            org.apache.http.util.EntityUtils.consume(response.getEntity());

            for (String expectedResponse : expectedInResponse) {
                assertTrue("The expected response String was not found" + expectedResponse, content.contains(expectedResponse));
            }
        }

        //Testing if a new TestFieldInjectionSession was injected when another session is used
        String[] expectedInResponse = {
                                        "<li>TestFieldInjection index: " + (iterations + 1) + "</li>",
                                        "<li>TestFieldInjectionRequest index: " + 1 + "</li>",
                                        "<li>TestFieldInjectionApplication index: " + (iterations + 1) + "</li>",
                                        "<li>TestFieldInjectionSession index: " + 1 + "</li>"
        };

        response = client2.execute(getMethod);
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getStatusLine().getStatusCode());

        String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
        LOG.info("Response content: " + content);
        org.apache.http.util.EntityUtils.consume(response.getEntity());

        for (String expectedResponse : expectedInResponse) {
            assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
        }
    }

    /**
     * Test the tag library event listeners injected using CDI.
     * Method injection.
     *
     * @throws Exception
     */
    @Test
    public void testTagLibraryEventListenerMI() throws Exception {
        final int iterations = 5;

        DefaultHttpClient client = new DefaultHttpClient();
        DefaultHttpClient client2 = new DefaultHttpClient(); //Used to test @SessionScoped

        String url = JSPUtils.createHttpUrlString(server, JSP23_APP_NAME, "TagLibraryEventListenerMI.jsp?increment=true");
        LOG.info("url: " + url);

        HttpGet getMethod = new HttpGet(url);
        HttpResponse response;

        for (int i = 1; i <= iterations; i++) {
            String[] expectedInResponse = {
                                            "<li>TestMethodInjection index: " + i + "</li>",
                                            "<li>TestMethodInjectionRequest index: " + 1 + "</li>",
                                            "<li>TestMethodInjectionApplication index: " + i + "</li>",
                                            "<li>TestMethodInjectionSession index: " + i + "</li>"
            };

            response = client.execute(getMethod);
            assertEquals("Expected " + 200 + " status code was not returned!",
                         200, response.getStatusLine().getStatusCode());

            String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
            LOG.info("Response content: " + content);
            org.apache.http.util.EntityUtils.consume(response.getEntity());

            for (String expectedResponse : expectedInResponse) {
                assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
            }
        }

        //Testing if a new TestMethodInjectionSession was injected when another session is used
        String[] expectedInResponse = {
                                        "<li>TestMethodInjection index: " + (iterations + 1) + "</li>",
                                        "<li>TestMethodInjectionRequest index: " + 1 + "</li>",
                                        "<li>TestMethodInjectionApplication index: " + (iterations + 1) + "</li>",
                                        "<li>TestMethodInjectionSession index: " + 1 + "</li>"
        };

        response = client2.execute(getMethod);
        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getStatusLine().getStatusCode());

        String content = org.apache.http.util.EntityUtils.toString(response.getEntity());
        LOG.info("Response content: " + content);
        org.apache.http.util.EntityUtils.consume(response.getEntity());

        for (String expectedResponse : expectedInResponse) {
            assertTrue("The response did not contain: " + expectedResponse, content.contains(expectedResponse));
        }
    }

    /**
     * This test makes a request to a jsp page and expects no exceptions,
     * and the text "Test passed!" to be output after a session invalidation.
     *
     * @throws Exception
     */

    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Mode(TestMode.FULL)
    @Test
    public void testPI44611() throws Exception {
        this.verifyStringInResponse(PI44611_APP_NAME, "PI44611.jsp", "Test passed!");
    }

    /**
     * This test makes a request to a jsp page and expects no NullPointerExceptions,
     * and the text "Test passed." to be output.
     *
     * @throws Exception
     */
    // No need to run against cdi-2.0 since this test does not use CDI at all.
    @SkipForRepeat("CDI-2.0")
    @Mode(TestMode.FULL)
    @Test
    public void testPI59436() throws Exception {
        this.verifyStringInResponse(PI59436_APP_NAME, "PI59436.jsp", "Test passed.");
    }

    private void verifyStringsInResponse(String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, contextRoot, path));
        WebResponse response = wc.getResponse(request);
        LOG.info("Response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        String responseText = response.getText();

        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseText.contains(expectedResponse));
        }
    }

    private void verifyStringInResponse(String contextRoot, String path, String expectedResponseString) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, contextRoot, path));
        WebResponse response = wc.getResponse(request);
        LOG.info("Response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        String responseText = response.getText();

        assertTrue("The response did not contain: " + expectedResponseString, responseText.contains(expectedResponseString));
    }
}
