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

import javax.el.ExpressionFactory;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * This servlet test the coercion rule 1.23.1 which states:
 * To Coerce a Value X to Type Y
 * Test if X is null and Y is not a primitive type and also not a String, (Expected return null)
 * If X is null and Y is not a primitive type and also not a String, return null.
 */
@WebServlet("/EL30CoercionRulesServlet")
public class EL30CoercionRulesServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public EL30CoercionRulesServlet() {
        super();
    }

    /**
     * Test EL 3.0 Coercion Rule 1.23.1
     *
     * Testing Coercion of a Value X to Type Y.
     *
     * @throws Exception
     *             if something goes wrong
     */
    @Test
    public void testCoercionRules() throws Exception {
        ExpressionFactory factory = ExpressionFactory.newInstance();

        // Initialize number Integer to null
        Integer number = null;

        Object result = factory.coerceToType(number, java.lang.Double.class);

        assertTrue("The type was expected to be coerced to null but was not: " + result, result == null);

    }

}
