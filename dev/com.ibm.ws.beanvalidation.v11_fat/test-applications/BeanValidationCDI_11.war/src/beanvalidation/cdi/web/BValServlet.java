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
package beanvalidation.cdi.web;

import java.util.Set;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidatorFactory;

import beanvalidation.cdi.beans.TestBean;
import componenttest.app.FATServlet;

/**
 * Servlet that obtains a ValidatorFactory through using jndi lookups.
 */
@WebServlet("/BValServlet")
@SuppressWarnings("serial")
public class BValServlet extends FATServlet {

    @Inject
    TestBean bean;

    /**
     * Test that a ValidatorFactory obtained with a lookup has the custom message
     * interpolator set and that the custom message interpolator was able to inject
     * another bean in this application.
     */
    public void testCDIInjectionInInterpolatorLookup() throws Exception {
        Context context = new InitialContext();
        ValidatorFactory vfactory = (ValidatorFactory) context.lookup("java:comp/ValidatorFactory");
        if (vfactory == null) {
            throw new IllegalStateException("lookup(java:comp/ValidatorFactory) returned null.");
        }

        String interpolatedValue = vfactory.getMessageInterpolator().interpolate(null, null);

        if (!interpolatedValue.equals("something")) {
            throw new Exception("custom interpolator should have returned the value " +
                                "'something', but returned : " + interpolatedValue);
        }

    }

    /**
     * Test that a ValidatorFactory obtained with a lookup has the custom constraint
     * validator factory set and that the custom cvf was able to inject another bean
     * in this application.
     */
    public void testCDIInjectionInConstraintValidatorFactoryLookup() throws Exception {
        Context context = new InitialContext();
        ValidatorFactory vfactory = (ValidatorFactory) context.lookup("java:comp/ValidatorFactory");
        if (vfactory == null) {
            throw new IllegalStateException("lookup(java:comp/ValidatorFactory) returned null.");
        }

        vfactory.getConstraintValidatorFactory().getInstance(null);
    }

    /**
     * Test that method parameter and return value validation works automatically
     * on a CDI managed bean.
     *
     * TODO make this one use just validation.xml
     */
    public void testMethodValidation() throws Exception {
        if (bean == null) {
            throw new Exception("CDI didn't inject the bean TestBean into this servlet");
        }

        bean.testMethodParameterValidation("test string");

        try {
            bean.testMethodParameterValidation(null);
            throw new Exception("interceptor didn't validate method call properly");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 1) {
                throw new Exception("interceptor validated method parametersand caught " +
                                    "constraints, but size wasn't 1: " + cvs);
            }
        }

        bean.testMethodReturnValidation("t");

        try {
            bean.testMethodReturnValidation("large string");
            throw new Exception("interceptor didn't validate method return value properly");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 1) {
                throw new Exception("interceptor validated method return and " +
                                    "caught constraints, but size wasn't 1: " + cvs);
            }
        }
    }

    /**
     * Test that constructor parameter and return value validation works automatically
     * on a CDI managed bean.
     *
     * TODO make this one use just validation.xml
     */
    public void testConstructorValidation() throws Exception {
        // TODO implement test
    }
}
