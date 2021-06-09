/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.ws.el30.fat.varargstest.EL30VarargsMethodMatchingTestBean;

import componenttest.annotation.SkipForRepeat;
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

        // Add the bean to the ELProcessor
        elp.defineBean("testBean", testBean);
 

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testSingleEnum() throws Exception {

        getMethodExpression("testBean.testMethod(testBean.enum1)", "(IEnum enum1)");

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testSingleString() throws Exception {

        getMethodExpression("testBean.testMethod('string1')", "(String param1)");

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testString_VarargsString() throws Exception {

        getMethodExpression("testBean.testMethod('string1','string2','string3')", "(String param1, String... param2)");

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testString_String() throws Exception {

        getMethodExpression("testBean.testMethod('string1','string2')", "(String param1, String param2)");

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testEnum_VarargsEnum() throws Exception {
        
        getMethodExpression("testBean.testMethod(testBean.enum1,testBean.enum1,testBean.enum1)", "(IEnum enum1, IEnum... enum2)");

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testString_VarargsEnum() throws Exception {
        
        getMethodExpression("testBean.testMethod('string1',testBean.enum1)", "(String param1, IEnum... param2)");

    }

    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testVarargsInt() throws Exception {
        
        getMethodExpression("testBean.testMethod(testBean.number)", "(int... param1)");

    }

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
