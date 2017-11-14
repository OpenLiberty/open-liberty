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
package beanvalidation11.web;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import beanvalidation11.CustomConstraintValidatorFactory;
import beanvalidation11.CustomMessageInterpolator;
import beanvalidation11.CustomParameterNameProvider;
import beanvalidation11.CustomTraversableResolver;
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
     * being configured. The hibernate implementation doesn't handle the null case, so
     * the custom one does a no-op if null is passed in. If the default implementation ever
     * does handle the null case this won't be much of a useful test and another way to test
     * a custom implementation should be used.
     */
    public void testCustomConstraintValidatorFactory() throws Exception {
        ivVFactory.getConstraintValidatorFactory().getInstance(null);
    }

    /**
     * Verify that a our {@link CustomParameterNameProvider} is configured in the ValidatiorFactory
     * as opposed to the default one.
     */
    public void testCustomParameterNameProvider() throws Exception {
        List<String> parameterNames;

        Constructor<CustomMessageInterpolator> constructor = CustomMessageInterpolator.class
                        .getConstructor(String.class,
                                        String.class,
                                        String.class);
        parameterNames = ivVFactory.getParameterNameProvider().getParameterNames(constructor);

        if (!parameterNames.get(0).equals("String_0") ||
            !parameterNames.get(1).equals("String_1") ||
            !parameterNames.get(2).equals("String_2")) {
            throw new Exception("parameter names aren't the ones expected by the" +
                                " custom parameter name provider for constructors: " + parameterNames);
        }

        Method method = CustomMessageInterpolator.class.getMethod("interpolate",
                                                                  String.class,
                                                                  javax.validation.MessageInterpolator.Context.class,
                                                                  Locale.class);
        parameterNames = ivVFactory.getParameterNameProvider().getParameterNames(method);

        if (!parameterNames.get(0).equals("String_0") ||
            !parameterNames.get(1).equals("Context_1") ||
            !parameterNames.get(2).equals("Locale_2")) {
            throw new Exception("parameter names aren't the ones expected by the" +
                                " custom parameter name provider for methods: " + parameterNames);
        }
    }

    /**
     * Test to ensure whether the beanValidation-1.1 is enabled or not. The isBval10 request
     * parameter is passed in from the test runner to indicate if using a 1.1 feature should
     * fail or not.
     */
    public void testDynamicBeanValidationFeatures(HttpServletRequest request,
                                                  HttpServletResponse response) throws Exception {
        boolean isBval10 = Boolean.parseBoolean(request.getParameter("isBval10"));
        if (isBval10) {
            try {
                testCustomParameterNameProvider();
                throw new ValidationException("bval-1.0 should not have been able to invoke the 1.1 api's");
            } catch (NoSuchMethodError e) {
                // expected
            }
        } else {
            testCustomParameterNameProvider();
        }
    }
}