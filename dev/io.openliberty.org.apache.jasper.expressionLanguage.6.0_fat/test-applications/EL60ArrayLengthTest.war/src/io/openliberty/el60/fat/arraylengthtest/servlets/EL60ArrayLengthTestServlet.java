/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.arraylengthtest.servlets;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.el.ELProcessor;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet for Expression Language 6.0 Array length tests.
 */
@WebServlet({ "/EL60ArrayLengthTest" })
public class EL60ArrayLengthTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    ELProcessor elp;

    public EL60ArrayLengthTestServlet() {
        super();

        elp = new ELProcessor();
        elp.defineBean("arrayBean", new ArrayBean());
    }

    /**
     * Test the Expression Language 6.0 Array length property for an Array of
     * non zero size.
     *
     * @throws Exception
     */
    @Test
    public void testEL60ArrayLengthProperty_Non_Zero() throws Exception {
        int arrayLength;
        int expectedLength = 2;

        elp.eval("arrayBean.setTestArray([1,2].toArray())");
        arrayLength = elp.eval("arrayBean.getTestArray().length");
        assertTrue("The testArray length was: " + arrayLength + " but was expected to be: " + expectedLength, arrayLength == expectedLength);
    }

    /**
     * Test the Expression Language 6.0 Array length property for an Array of
     * size 0.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60ArrayLengthProperty_Zero() throws Exception {
        int arrayLength;
        int expectedLength = 0;

        elp.eval("arrayBean.setTestArray([].toArray())");
        arrayLength = elp.eval("arrayBean.getTestArray().length");
        assertTrue("The testArray length was: " + arrayLength + " but was expected to be: " + expectedLength, arrayLength == expectedLength);
    }

    /*
     * Simple bean used to test the Array length property.
     */
    public class ArrayBean {
        int[] testArray;

        public ArrayBean() {
            testArray = new int[0];
        }

        public void setTestArray(int[] testArray) {
            this.testArray = testArray;
        }

        public int[] getTestArray() {
            return this.testArray;
        }

    }
}
