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
package bval.v20.cdi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.ClockProvider;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.xml.bind.ValidationException;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BeanValCDIServlet")
public class BeanValCDIServlet extends FATServlet {

    @Inject
    BeanValCDIBean bean;

    @Resource
    ValidatorFactory injectValidatorFactory;

    @Inject
    ValidatorFactory injectValFact;

    @Inject
    Validator injectVal;

    @Test
    public void testInjectVF() throws Exception {
        assertNotNull(injectValFact);
    }

    @Test
    public void testInjectVal() throws Exception {
        assertNotNull(injectVal);
    }

    @Test
    public void testConstraintValidatorInjection() throws Exception {
        Validator validator = this.injectValidatorFactory.getValidator();
        Set<ConstraintViolation<BeanValCDIBean>> violations = validator.validate(this.bean, new Class[0]);
        if (!violations.isEmpty()) {
            StringBuffer msg = new StringBuffer();
            for (ConstraintViolation<BeanValCDIBean> cv : violations) {
                msg.append("\n\t" + cv.toString());
            }
            throw new ValidationException("validating produced constraint violations: " + msg);
        }
    }

    @Test
    public void testInterceptorRegisteredOnlyOnce() throws Exception {
        TestAnnotationValidator.isValidCounter = 0;
        this.bean.validateMethod("Inside testInterceptorRegisteredOnlyOnce test.");
        if (TestAnnotationValidator.isValidCounter != 1) {
            throw new Exception("Interceptor was not invoked the correct number of times.  It should only be invoked once, but was invoked "
                                + TestAnnotationValidator.isValidCounter + " times.");
        }
    }

    @Test
    public void testDecimalInclusiveForNumber() throws Exception {
        if (this.bean == null) {
            throw new Exception("CDI didn't inject the bean BeanValCDIBean into this servlet");
        }
        this.bean.testDecimalInclusiveValidationForNumber(9.9D, 1.1D, 10.0D, 1.0D);
        try {
            this.bean.testDecimalInclusiveValidationForNumber(10.0D, 1.0D, 10.1D, 0.9D);
            throw new Exception("Decimal inclusive property isn't working properly");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 4) {
                int i = 0;
                for (ConstraintViolation<?> cv : cvs) {
                    System.out.println("Constraint violation " + ++i + ":");
                    System.out.println(cv.getMessage());
                }
                throw new Exception("interceptor validated method parameters and caught constraint violations, but size wasn't 4.");
            }
        }
    }

    @Test
    public void testDecimalInclusiveForString() throws Exception {
        if (this.bean == null) {
            throw new Exception("CDI didn't inject the bean BeanValCDIBean into this servlet");
        }
        this.bean.testDecimalInclusiveValidationForString("9.9", "1.1", "10", "1");
        try {
            this.bean.testDecimalInclusiveValidationForString("10", "1", "10.1", ".9");
            throw new Exception("Decimal inclusive property isn't working properly");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 4) {
                int i = 0;
                for (ConstraintViolation<?> cv : cvs) {
                    System.out.println("Constraint violation " + ++i + ":");
                    System.out.println(cv.getMessage());
                }
                throw new Exception("interceptor validated method parameters and caught constraint violations, but size wasn't 4.");
            }
        }
    }

    @Test
    public void testCustomClockProvider() throws Exception {
        ClockProvider cp = injectValFact.getClockProvider();
        System.out.println("Got ClockProvider: " + cp);
        assertEquals(TestClockProvider.class, cp.getClass());

        // Verify that the custom clock provider is CDI-enabled
        cp.getClock();
    }
}
