/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el30.fat.servlets;

import static org.junit.Assert.assertTrue;

import javax.el.ELException;
import javax.el.ELProcessor;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.el30.fat.beans.EL30ReservedWordsTestBean;

import componenttest.app.FATServlet;

/**
 * This servlet tests all the EL 3.0 reserved words depending on the parameter that it is being passed.
 */
@WebServlet("/EL30ReservedWordsServlet")
public class EL30ReservedWordsServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    ELProcessor elp;

    /**
     * Constructor
     */
    public EL30ReservedWordsServlet() {
        super();

        // Create an instance of the ReservedWordsTestBean
        elp = new ELProcessor();
        EL30ReservedWordsTestBean test = new EL30ReservedWordsTestBean();
        test.setCat("Testing \"cat\" non-reserved word. Test Successful");
        test.setT("Testing \"T\" non-reserved word. Test Successful");

        // Define the bean within the ELProcessor object
        elp.defineBean("test", test);
    }

    @Test
    public void testReservedWordAnd() throws Exception {
        evaluateReservedWord(elp, "test.and", "Failed to parse the expression [${test.and}]");
    }

    @Test
    public void testReservedWordOr() throws Exception {
        evaluateReservedWord(elp, "test.or", "Failed to parse the expression [${test.or}]");
    }

    @Test
    public void testReservedWordNot() throws Exception {
        evaluateReservedWord(elp, "test.not", "Failed to parse the expression [${test.not}]");
    }

    @Test
    public void testReservedWordEq() throws Exception {
        evaluateReservedWord(elp, "test.eq", "Failed to parse the expression [${test.eq}]");
    }

    @Test
    public void testReservedWordNe() throws Exception {
        evaluateReservedWord(elp, "test.ne", "Failed to parse the expression [${test.ne}]");
    }

    @Test
    public void testReservedWordLt() throws Exception {
        evaluateReservedWord(elp, "test.lt", "Failed to parse the expression [${test.lt}]");
    }

    @Test
    public void testReservedWordGt() throws Exception {
        evaluateReservedWord(elp, "test.gt", "Failed to parse the expression [${test.gt}]");
    }

    @Test
    public void testReservedWordLe() throws Exception {
        evaluateReservedWord(elp, "test.le", "Failed to parse the expression [${test.le}]");
    }

    @Test
    public void testReservedWordGe() throws Exception {
        evaluateReservedWord(elp, "test.ge", "Failed to parse the expression [${test.ge}]");
    }

    @Test
    public void testReservedWordTrue() throws Exception {
        evaluateReservedWord(elp, "test.true", "Failed to parse the expression [${test.true}]");
    }

    @Test
    public void testReservedWordFalse() throws Exception {
        evaluateReservedWord(elp, "test.false", "Failed to parse the expression [${test.false}]");
    }

    @Test
    public void testReservedWordNull() throws Exception {
        evaluateReservedWord(elp, "test.null", "Failed to parse the expression [${test.null}]");
    }

    @Test
    public void testReservedWordInstanceOf() throws Exception {
        evaluateReservedWord(elp, "test.instanceof", "Failed to parse the expression [${test.instanceof}]");
    }

    @Test
    public void testReservedWordEmpty() throws Exception {
        evaluateReservedWord(elp, "test.empty", "Failed to parse the expression [${test.empty}]");
    }

    @Test
    public void testReservedWordDiv() throws Exception {
        evaluateReservedWord(elp, "test.div", "Failed to parse the expression [${test.div}]");
    }

    @Test
    public void testReservedWordMod() throws Exception {
        evaluateReservedWord(elp, "test.mod", "Failed to parse the expression [${test.mod}]");
    }

    @Test
    public void testReservedWordCat() throws Exception {
        evaluateNonReservedWord(elp, "test.cat", "Testing \"cat\" non-reserved word. Test Successful");
    }

    @Test
    public void testReservedWordT() throws Exception {
        evaluateNonReservedWord(elp, "test.t", "Testing \"T\" non-reserved word. Test Successful");
    }

    /**
     * Helper method to evaluate EL3.0 reserved words and print the response
     *
     * @param expression Expression to be evaluated.
     */
    private void evaluateReservedWord(ELProcessor elp, String expression, String expectedMessage) {
        try {
            elp.eval(expression);
        } catch (Exception e) {
            assertTrue("The expected ELException was not thrown: " + e.toString(), e instanceof ELException);
            assertTrue("The ELException did not contain the expected message: " + e.getMessage(),
                       e.getMessage().equals(expectedMessage));
        }
    }

    /**
     * Helper method to evaluate the non reserved words and print the response
     *
     * @param expression Expression to be evaluated.
     */
    private void evaluateNonReservedWord(ELProcessor elp, String expression, String expectedValue) {
        String result = elp.eval(expression).toString();

        assertTrue("The expression did not evaluate to: " + expectedValue + " but was: " + result, result.equals(expectedValue));

    }
}
