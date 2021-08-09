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
package beanvalidation10.web;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import beanvalidation10.CustomConstraintValidatorFactory;
import beanvalidation10.CustomTraversableResolver;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public class BeanValidationInjectionServlet extends FATServlet {

    @Resource(name = "TestValidatorFactory")
    ValidatorFactory ivVFactory;

    @Resource(name = "TestValidator")
    Validator ivValidator;

    /**
     * Verify that the module ValidatorFactory may be injected and looked up at:
     *
     * java:comp/env/TestValidatorFactory
     */
    public void testInjectionAndLookupValidatorFactory() throws Exception {
        testInjection();
        testLookup();
    }

    /**
     * Verify that the module ValidatorFactory may be injected.
     */
    public void testInjectionValidatorFactory() throws Exception {
        testInjection();
    }

    /**
     * Verify that the module ValidatorFactory reference can be looked up.
     */
    public void testLookupValidatorFactory(HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        boolean isJNDIEnabled = Boolean.parseBoolean(request.getParameter("isJNDIEnabled"));
        if (isJNDIEnabled) {
            testLookup();
        } else {
            try {
                testLookup();
                throw new IllegalArgumentException("testLookup should have thrown a NamingException " +
                                                   "because jndi-1.0 isn't enabled");
            } catch (NamingException e) {
                // expected
            }
        }
    }

    private void testInjection() {
        if (ivVFactory == null) {
            throw new IllegalStateException("Injection of ValidatorFactory never occurred.");
        }
    }

    private void testLookup() throws NamingException {
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
    public void testInjectionAndLookupValidator() throws Exception {
        if (ivValidator == null) {
            throw new IllegalStateException("Injection of Validator never occurred.");
        }
        Context context = new InitialContext();
        Validator validator = (Validator) context.lookup("java:comp/env/TestValidator");
        if (validator == null) {
            throw new IllegalStateException("lookup(java:comp/env/TestValidator) returned null.");
        }
    }

    /**
     * Verify that a our {@link CustomTraversableResolver} is configured in the ValidatiorFactory
     * as opposed to the default one.
     */
    public void testCustomTraversableResolver() throws Exception {
        boolean returnValue;

        returnValue = ivVFactory.getTraversableResolver().isCascadable("cascadable", null, null, null, null);
        if (!returnValue) {
            throw new IllegalStateException("custom TraversableResolver.isCascadable returned [false]" +
                                            " when it should have been [true]");
        }

        returnValue = ivVFactory.getTraversableResolver().isCascadable("non-cascadable", null, null, null, null);
        if (returnValue) {
            throw new IllegalStateException("custom TraversableResolver.isCascadable returned [true]" +
                                            " when it should have been [false]");
        }

        returnValue = ivVFactory.getTraversableResolver().isReachable("reachable", null, null, null, null);
        if (!returnValue) {
            throw new IllegalStateException("custom TraversableResolver.isReachable returned [false]" +
                                            " when it should have been [true]");
        }

        returnValue = ivVFactory.getTraversableResolver().isReachable("non-reachable", null, null, null, null);
        if (returnValue) {
            throw new IllegalStateException("custom TraversableResolver.isReachable returned [true]" +
                                            " when it should have been [false]");
        }
    }

    /**
     * This is a crude way to ensure that our {@link CustomConstraintValidatorFactory} is
     * being configured. The apache implementation doesn't handle the null case, so
     * the custom one does a no-op if null is passed in. If the default implementation ever
     * does handle the null case this won't be much of a useful test and another way to test
     * a custom implementation should be used.
     */
    public void testCustomConstraintValidatorFactory() throws Exception {
        ivVFactory.getConstraintValidatorFactory().getInstance(null);
    }
}