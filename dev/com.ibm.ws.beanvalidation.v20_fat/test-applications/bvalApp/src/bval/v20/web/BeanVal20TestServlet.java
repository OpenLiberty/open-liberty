/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package bval.v20.web;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/TestServletA")
public class BeanVal20TestServlet extends FATServlet {

    @Resource(name = "TestValidatorFactory")
    ValidatorFactory ivVFactory;

    @Resource(name = "TestValidator")
    Validator ivValidator;

    /**
     * Verify that the module ValidatorFactory may be injected and looked up at:
     * java:comp/env/TestValidatorFactory
     */
    @Test
    public void testDefaultInjectionAndLookupValidatorFactory() throws Exception {
        assertNotNull("Injection of ValidatorFactory never occurred.", ivVFactory);

        assertNotNull(InitialContext.doLookup("java:comp/env/TestValidatorFactory"));
    }

    /**
     * Verify that the module Validator may be injected and looked up at:
     * java:comp/env/TestValidator
     */
    @Test
    public void testDefaultInjectionAndLookupValidator() throws Exception {
        assertNotNull("Injection of Validator never occurred.", ivValidator);

        assertNotNull(InitialContext.doLookup("java:comp/env/TestValidator"));
    }
}
