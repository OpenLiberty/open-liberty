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
package defaultbeanvalidation.cdi.web;

import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import componenttest.app.FATServlet;
import defaultbeanvalidation.cdi.beans.TestBean;
import defaultbeanvalidation.cdi.validation.TestAnnotationValidator;

@WebServlet("/BValInjectionServlet")
@SuppressWarnings("serial")
public class BValAtInjectionServlet extends FATServlet {

    @Inject
    TestBean bean;

    @Resource
    ValidatorFactory injectValidatorFactory;

    /**
     * Test that a ConstraintValidator is created as a CDI managed bean when
     * a custom constraint validator factory is not specified and the application
     * is CDI enabled.
     */
    public void testConstraintValidatorInjection() throws Exception {
        Validator validator = injectValidatorFactory.getValidator();
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            StringBuffer msg = new StringBuffer();
            for (ConstraintViolation<TestBean> cv : violations) {
                msg.append("\n\t" + cv.toString());
            }

            throw new ValidationException("validating produced constraint violations: " + msg);
        }
    }

    /**
     * Test that bean validation interceptors are not being registered and invoked multiple times.
     * See defect 213484.
     */
    public void testInterceptorRegisteredOnlyOnce() throws Exception {
        TestAnnotationValidator.isValidCounter = 0;
        bean.validateMethod("Inside testInterceptorRegisteredOnlyOnce test.");
        if (TestAnnotationValidator.isValidCounter != 1) {
            throw new Exception("Interceptor was not invoked the correct number of times.  It should only be invoked once, but was invoked "
                                + TestAnnotationValidator.isValidCounter
                                + " times.");
        }
    }

    /**
     * Test that @DecimalMax and @DecimalMin correctly implement the inclusive property.
     */
    public void testDecimalInclusiveForNumber() throws Exception {
        if (bean == null) {
            throw new Exception("CDI didn't inject the bean TestBean into this servlet");
        }

        bean.testDecimalInclusiveValidationForNumber(9.9, 1.1, 10, 1);

        try {
            bean.testDecimalInclusiveValidationForNumber(10, 1, 10.1, .9);
            throw new Exception("Decimal inclusive property isn't working properly");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 4) {
                int i = 0;
                for (ConstraintViolation<?> cv : cvs) {
                    System.out.println("Constraint violation " + ++i + ":");
                    System.out.println(cv.getMessage());
                }
                throw new Exception("interceptor validated method parameters and " +
                                    "caught constraint violations, but size wasn't 4.");
            }
        }
    }

    /**
     * Test that @DecimalMax and @DecimalMin correctly implement the inclusive property.
     */
    public void testDecimalInclusiveForString() throws Exception {
        if (bean == null) {
            throw new Exception("CDI didn't inject the bean TestBean into this servlet");
        }
        bean.testDecimalInclusiveValidationForString("9.9", "1.1", "10", "1");

        try {
            bean.testDecimalInclusiveValidationForString("10", "1", "10.1", ".9");
            throw new Exception("Decimal inclusive property isn't working properly");
        } catch (ConstraintViolationException e) {
            Set<ConstraintViolation<?>> cvs = e.getConstraintViolations();
            if (cvs.size() != 4) {
                int i = 0;
                for (ConstraintViolation<?> cv : cvs) {
                    System.out.println("Constraint violation " + ++i + ":");
                    System.out.println(cv.getMessage());
                }
                throw new Exception("interceptor validated method parameters and " +
                                    "caught constraint violations, but size wasn't 4.");
            }
        }
    }
}