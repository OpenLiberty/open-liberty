/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el.fat.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * All EL 3.0 tests with all applicable server features enabled.
 *
 * Tests that just need to drive a simple request using our WebBrowser object can be placed in this class.
 *
 * If a test needs httpunit it should more than likely be placed in the ELServerHttpUnit test class.
 */
@MinimumJavaLevel(javaLevel = 7)
public class ELServerTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(ELServerTest.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("elServer");

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    /**
     * Sample test
     *
     * @throws Exception
     *             if something goes horribly wrong
     */
    @Test
    public void testServlet() throws Exception {
        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestEL3.0/SimpleTestServlet", "Hello World");
    }

    /**
     * Test the EL 3.0 reserved words to ensure they are correctly restricted.
     *
     * @throws Exception
     *             if something goes horribly wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL30ReservedWords() throws Exception {
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=and",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.and}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=or",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.or}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=not",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.not}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=eq",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.eq}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=ne",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.ne}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=lt",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.lt}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=gt",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.gt}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=le",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.le}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=ge",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.ge}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=true",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.true}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=false",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.false}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=null",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.null}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=instanceof",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.instanceof}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=empty",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.empty}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=div",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.div}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=mod",
                            "Test Successful. Correct exception was thrown: javax.el.ELException: Failed to parse the expression [${test.mod}]");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=cat",
                            "Testing \"cat\" non-reserved word. Test Successful");
        this.verifyResponse("/TestEL3.0/EL30ReservedWordsServlet?testReservedWord=T",
                            "Testing \"T\" non-reserved word. Test Successful");
    }

    /**
     * Test EL 3.0 Operations on List Collection Object
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void testListCollectionObjectOperations() throws Exception {
        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=filter",
                            "Filter: [4, 3, 5, 3]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=map",
                            "Map: [3, 6, 5, 4, 7, 5, 3]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=flatMap",
                            "FlatMap: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=distinct",
                            "Distinct: [1, 4, 3, 2, 5]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=sorted",
                            "Sorted in Decreasing: [5, 4, 3, 3, 2, 1, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=forEach",
                            "ForEach: 1432531");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=peek",
                            "Debug Peek: 144322531 Peek: [4, 2]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=iterator",
                            "Iterator: 1 4 3 2 5 3 1");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=limit",
                            "Limit: [1, 4, 3]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=substream",
                            "Substream: [3, 2]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=toArray",
                            "ToArray: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=toList",
                            "ToList: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=reduce",
                            "Reduce: 5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=max",
                            "Max: 5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=min",
                            "Min: 1");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=average",
                            "Average: 2.7142857142857144");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=sum",
                            "Sum: 19");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=count",
                            "Count: 7");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=anyMatch",
                            "AnyMatch: true");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=allMatch",
                            "AllMatch: false");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=noneMatch",
                            "NoneMatch: true");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testListCollectionOperations=findFirst",
                            "FindFirst: 1");
    }

    /**
     * Test EL 3.0 Operations on Set Collection Object
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSetCollectionObjectOperations() throws Exception {
        // Use the SharedServer to verify a response.

        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=filter",
                            "Filter: ", "3", "5", "4");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=map",
                            "Map: ", "5", "4", "3", "7", "6");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=flatMap",
                            "FlatMap: ", "2", "3", "1", "4", "1", "3", "5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=distinct",
                            "Distinct: ", "3", "2", "1", "5", "4");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=sorted",
                            "Sorted in Decreasing: [5, 4, 3, 2, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=forEach",
                            "ForEach: ", "3", "2", "1", "5", "4");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=peek",
                            "Debug Peek: ", "3", "2", "2", "1", "5", "4", "4", "Peek: [2, 4]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=iterator",
                            "Iterator: ", "3", "2", "1", "5", "4");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=limit",
                            "Limit: ", "3", "2", "1");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=substream",
                            "Substream: [3, 4]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=toArray",
                            "ToArray: ", "3", "2", "1", "5", "4");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=toList",
                            "ToList: ", "3", "2", "1", "5", "4");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=reduce",
                            "Reduce: 5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=max",
                            "Max: 5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=min",
                            "Min: 1");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=average",
                            "Average: 3.0");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=sum",
                            "Sum: 15");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=count",
                            "Count: 5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=anyMatch",
                            "AnyMatch: true");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=allMatch",
                            "AllMatch: false");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=noneMatch",
                            "NoneMatch: true");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testSetCollectionOperations=findFirst",
                            "FindFirst:", "1");
    }

    /**
     * Test EL 3.0 Operations on Map Collection Object
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMapCollectionObjectOperations() throws Exception {
        // Use the SharedServer to verify a response.
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=filter",
                            "Filter: [4, 3, 5, 3]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=map",
                            "Map: [3, 6, 5, 4, 7, 5, 3]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=flatMap",
                            "FlatMap: [1, 4, 3, 2, 5, 3, 1, 1, 4, 3, 2, 5, 3, 1, 1, 4, 3, 2, 5, 3, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=distinct",
                            "Distinct: [1, 4, 3, 2, 5]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=sorted",
                            "Sorted in Decreasing: [5, 4, 3, 3, 2, 1, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=forEach",
                            "ForEach: 1432531");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=peek",
                            "Debug Peek: 144322531 Peek: [4, 2]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=iterator",
                            "Iterator: 1 4 3 2 5 3 1");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=limit",
                            "Limit: [1, 4, 3]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=substream",
                            "Substream: [3, 2]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=toArray",
                            "ToArray: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=toList",
                            "ToList: [1, 4, 3, 2, 5, 3, 1]");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=reduce",
                            "Reduce: 5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=max",
                            "Max: 5");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=min",
                            "Min: 1");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=average",
                            "Average: 2.7142857142857144");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=sum",
                            "Sum: 19");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=count",
                            "Count: 7");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=anyMatch",
                            "AnyMatch: true");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=allMatch",
                            "AllMatch: false");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=noneMatch",
                            "NoneMatch: true");
        this.verifyResponse("/TestEL3.0/EL30CollectionObjectOperationsServlet?testMapCollectionOperations=findFirst",
                            "FindFirst: 1");
    }

    /**
     * Test EL 3.0 invocations of Method Expressions
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    public void testMethodExpressionInvocations() throws Exception {
        WebBrowser browser = this.createWebBrowserForTestCase();
        this.verifyResponse(browser, "/TestEL3.0/EL30InvocationMethodExpressionsServlet",
                            new String[] { "Get Parent Name Using Value Expression (Expected: \"John Smith Sr.\"): John Smith Sr.",
                                           "Get Child Name Using Value Expression (Expected: \"John Smith Jr.\"): John Smith Jr.",
                                           "Get Object Representation Using Value Expression: toString method of object with current parent name John Smith Sr.",
                                           "Get Parent Name Using Method Expression (Expected: \"Steven Johnson Sr.\"): Steven Johnson Sr.",
                                           "Get Child Name Using Method Expression (Expected: \"Steven Johnson Jr.\"): Steven Johnson Jr.",
                                           "Get Object Representation Using Method Expression: toString method of object with current parent name Steven Johnson Sr."
                            });
    }

    /**
     * Test EL 3.0 Operator Precedence
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void testOperatorPrecedence() throws Exception {
        WebBrowser browser = this.createWebBrowserForTestCase();
        this.verifyResponse(browser, "/TestEL3.0/EL30OperatorPrecedenceServlet",
                            new String[] { "<b>Test 1:</b> EL 3.0 [] and . operators left-to-right (Expected:true): true",
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
                                           "<b>Test 26:</b> EL 3.0 Assignment (=) and Semi-colon (;) operators with lambda operator (->) (Expected:11): 11"
                            });
    }

    /**
     * Test EL 3.0 Coercion Rule 1.23.1
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCoercionRules() throws Exception {
        WebBrowser browser = this.createWebBrowserForTestCase();
        this.verifyResponse(browser, "/TestEL3.0/EL30CoercionRulesServlet",
                            new String[] { "Testing Coercion of a Value X to Type Y.",
                                           "Test if X is null and Y is not a primitive type and also not a String, (Expected return null): null"
                            });
    }

    /**
     * Test EL 3.0 static fields and methods functionality
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    public void testEL30StaticFieldsAndMethods() throws Exception {

        this.verifyResponse(getStaticFieldsAndMethodsURI("Boolean.TRUE",
                                                         "true"),
                            "Test successful");
        // Invoke a constructor with an argument (spec 1.22)
        this.verifyResponse(getStaticFieldsAndMethodsURI("Boolean(true)",
                                                         "true"),
                            "Test successful");
        // Reference a static field (spec 1.22)
        this.verifyResponse(getStaticFieldsAndMethodsURI("Integer.MAX_VALUE",
                                                         "positive%20int"),
                            "Test successful");
        // Reference a static method (spec 1.22)
        this.verifyResponse(getStaticFieldsAndMethodsURI("System.currentTimeMillis()",
                                                         "positive%20long"),
                            "Test successful");
        // Invoke a constructor with an argument (spec 1.22.3)
        this.verifyResponse(getStaticFieldsAndMethodsURI("Integer('1000')",
                                                         "1000"),
                            "Test successful");
        // Try to modify a static field (spec 1.22.1 #2)
        this.verifyResponse(getStaticFieldsAndMethodsURI("Integer.MAX_VALUE%20=%201",
                                                         "javax.el.PropertyNotWritableException"),
                            "Test successful");
        // Try to reference a field whose class hasn't been imported (spec 1.22.1 #3)
        this.verifyResponse(getStaticFieldsAndMethodsURI("java.math.RoundingMode.CEILING",
                                                         "javax.el.PropertyNotFoundException"),
                            "Test successful");

        // Test beans.EL30StaticFieldsAndMethodsBean and beans.EL30StaticFieldsAndMethodsEnum classes

        // Reference a static field on custom enum
        this.verifyResponse(getStaticFieldsAndMethodsURI("EL30StaticFieldsAndMethodsEnum.TEST_ONE",
                                                         "TEST_ONE"),
                            "Test successful");
        // Reference a static field on a custom class
        this.verifyResponse(getStaticFieldsAndMethodsURI("EL30StaticFieldsAndMethodsBean.staticReference",
                                                         "static%20reference"),
                            "Test successful");
        // Call a zero parameter custom method
        this.verifyResponse(getStaticFieldsAndMethodsURI("EL30StaticFieldsAndMethodsBean.staticMethod()",
                                                         "static%20method"),
                            "Test successful");
        // Call a one parameter custom method
        this.verifyResponse(getStaticFieldsAndMethodsURI("EL30StaticFieldsAndMethodsBean.staticMethodParam('static%20method%20param')",
                                                         "static%20method%20param"),
                            "Test successful");
        //Try to reference a non-static field
        this.verifyResponse(getStaticFieldsAndMethodsURI("EL30StaticFieldsAndMethodsBean.nonStaticReference",
                                                         "javax.el.PropertyNotFoundException"),
                            "Test successful");
        // Try to reference a non-static method
        this.verifyResponse(getStaticFieldsAndMethodsURI("EL30StaticFieldsAndMethodsBean.nonStaticMethod()",
                                                         "javax.el.MethodNotFoundException"),
                            "Test successful");
    }

    /*
     * Generate URIs for the testEL30StaticFieldsAndMethods servlet
     */
    private String getStaticFieldsAndMethodsURI(String expression, String expected) {
        return "/TestEL3.0/EL30StaticFieldsAndMethodsServlet?testExpression="
               + expression + "&expectedResult=" + expected;
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
