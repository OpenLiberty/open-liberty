/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.el30.fat.varargstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.el.ELProcessor;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * This servlet tests method matching when varargs are used
 */
@WebServlet("/EL30VarargsMethodMatchingServlet")
public class EL30VarargsMethodMatchingServlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    ELProcessor elp;

    /**
     * Constructor
     */
    public EL30VarargsMethodMatchingServlet() {
        super();

        elp = new ELProcessor();

        // Create an instance of the EL30VarargsMethodMatchingTestBean bean
        EL30VarargsMethodMatchingTestBean testBean = new EL30VarargsMethodMatchingTestBean();
        Falcon falcon = new Falcon();
        Bird bird = new Bird();
        Integer number = new Integer(1);
        IEnum enum1 = EnumBean.ENUM1;

        // Add the beans to the ELProcessor
        elp.defineBean("testBean", testBean);
        elp.defineBean("falcon", falcon);
        elp.defineBean("bird", bird);
        elp.defineBean("number", number);
        elp.defineBean("enum1", enum1);

    }

    @Test
    public void testSingleEnum() throws Exception {

        getMethodExpression("testBean.testMethod(enum1)", "(IEnum enum1)");

    }

    @Test
    public void testSingleString() throws Exception {

        getMethodExpression("testBean.testMethod('string1')", "(String param1)");

    }

    @Test
    public void testWithNoArguments() throws Exception {

        getMethodExpression("testBean.testMethod()", "(int... param1)");

    }

    @Test
    public void testString_VarargsString() throws Exception {

        getMethodExpression("testBean.testMethod('string1', 'string2', 'string3')", "(String param1, String... param2)");

    }

    @Test
    public void testString_String() throws Exception {

        getMethodExpression("testBean.testMethod('string1', 'string2')", "(String param1, String param2)");

    }

    @Test
    public void testEnum_VarargsEnum() throws Exception {

        getMethodExpression("testBean.testMethod(enum1, enum1, enum1)", "(IEnum enum1, IEnum... enum2)");

    }

    @Test
    public void testString_VarargsMultipleEnum() throws Exception {

        getMethodExpression("testBean.testMethod('string1', enum1, enum1)", "(String param1, IEnum... param2)");

    }

    @Test
    public void selectMethodWithNoVarargs() throws Exception {

        getMethodExpression("testBean.chirp(falcon)", "(Bird bird1)");

    }

    @Test
    public void testString_VarargsBird() throws Exception {

        getMethodExpression("testBean.chirp('string1', bird, bird)", "(String string1, Bird... bird2)");

    }

    @Test
    public void selectMethodWithNullVarargs() throws Exception {
        // Test for verifying null varargs or varargs with null values to verify functionality
        // See https://github.com/openliberty/open-liberty/issues/20460
        getMethodExpression("testBean.chirp('string1', null)", "(String string1, Bird... bird2)");
        getMethodExpression("testBean.chirp('string1', null, bird)", "(String string1, Bird... bird2)");
        getMethodExpression("testBean.chirp('string1', bird, null)", "(String string1, Bird... bird2)");

    }

    // Limitions of the varags selection are below -- tests currently fail
    // See Mark's July 29th comment here: https://bz.apache.org/bugzilla/show_bug.cgi?id=65358

    // No varargs methods are always prefered over varargs. Method with (String param1), through coercion,  is selected here instead.
    // @Test
    // @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    // public void testVarargsInt() throws Exception {
    //     getMethodExpression("testBean.testMethod(number)", "(int... param1)");
    // }

    // Failure related to coercion, I think?
    // @Test
    // @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    // public void testString_VarargsEnum() throws Exception {
    //     getMethodExpression("testBean.testMethod('string1', enum1)", "(String param1, IEnum... param2)");
    // }

    //  Not exactly related to varargs, but it fails nonetheless. -- MethodNotFoundException "Unable to find unambiguous method"
    //  Util.java in the Tomcat EL code has a comment explaining this exception:
    //      "If multiple methods have the same matching number of parameters - the match is ambiguous so throw an exception"
    // Test will likely never work since EL cant' differeniate between chirp(String string1, Bird... bird2) and chirp(String string1, Falcon... bird2)
    // but it remains here moreso for documenation
    // @Test
    // public void testString_VarargsFalcon() throws Exception {
    //     getMethodExpression("testBean.chirp('string1', falcon, falcon)", "chirp(String string1, Bird... bird2)");
    // }
    /**
     * Helper method to get value using Method Expressions
     *
     * @param expression Expression to be evaluated
     * @return the result of the evaluated expression
     */
    private void getMethodExpression(String expression, String expectedResult) throws Exception {
        String result;
        Object obj = elp.eval(expression);
        assertNotNull(obj);
        result = obj.toString();

        assertEquals("The expression did not evaluate to: " + expectedResult + " but was: " + result, expectedResult, result);
    }

}
