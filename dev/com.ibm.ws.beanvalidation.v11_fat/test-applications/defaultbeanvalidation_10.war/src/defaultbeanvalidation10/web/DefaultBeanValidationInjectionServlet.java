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
package defaultbeanvalidation10.web;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public class DefaultBeanValidationInjectionServlet extends FATServlet {

    @Resource(name = "TestValidatorFactory")
    ValidatorFactory ivVFactory;

    @Resource(name = "TestValidator")
    Validator ivValidator;

    /**
     * Verify that the module ValidatorFactory may be injected and looked up at:
     *
     * java:comp/env/TestValidatorFactory
     */
    public void testDefaultInjectionAndLookupValidatorFactory() throws Exception {
        if (ivVFactory == null) {
            throw new IllegalStateException("Injection of ValidatorFactory never occurred.");
        }
        Context context = new InitialContext();
        ValidatorFactory vfactory = (ValidatorFactory) context.lookup("java:comp/env/TestValidatorFactory");
        if (vfactory == null) {
            throw new IllegalStateException("lookup(java:comp/env/TestValidatorFactory) returned null.");
        }
    }

    /**
     * Verify that the module Validator may be injected and looked up at:
     *
     * java:comp/env/TestValidator
     */
    public void testDefaultInjectionAndLookupValidator() throws Exception {
        if (ivValidator == null) {
            throw new IllegalStateException("Injection of Validator never occurred.");
        }
        Context context = new InitialContext();
        Validator validator = (Validator) context.lookup("java:comp/env/TestValidator");
        if (validator == null) {
            throw new IllegalStateException("lookup(java:comp/env/TestValidator) returned null.");
        }
    }
}