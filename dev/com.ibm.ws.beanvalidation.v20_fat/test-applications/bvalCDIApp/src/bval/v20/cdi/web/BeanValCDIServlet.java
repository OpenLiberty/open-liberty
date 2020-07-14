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
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.Year;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.ClockProvider;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.junit.Test;

import bval.v20.cdi.web.MethodReturnValidatedBean.NestedBean;
import componenttest.app.FATServlet;
import junit.framework.Assert;

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

    @Inject
    MethodReturnValidatedBean methodReturnBean;

    @Test
    public void testBasicMethodValidation() {
        assertViolation(() -> methodReturnBean.email("bogus@bogus.com"), Email.class);
        assertViolation(() -> methodReturnBean.future(Instant.now().minusSeconds(60)), Future.class);
        assertViolation(() -> methodReturnBean.max100(101), Max.class);
        assertViolation(() -> methodReturnBean.min5(4), Min.class);
        assertViolation(() -> methodReturnBean.negative(1), Negative.class);
        assertViolation(() -> methodReturnBean.negativeOrZero(1), NegativeOrZero.class);
        assertViolation(() -> methodReturnBean.notNull(null), NotNull.class);
        assertViolation(() -> methodReturnBean.past(Year.of(2222)), Past.class);
        assertViolation(() -> methodReturnBean.positive((short) -1), Positive.class);
        assertViolation(() -> methodReturnBean.positiveOrZero(-1), PositiveOrZero.class);
        assertViolation(() -> methodReturnBean.size3to5("ab"), Size.class);
        NestedBean b = methodReturnBean.new NestedBean();
        b.positive = -1;
        assertViolation(() -> methodReturnBean.validBean(b), Positive.class);;
    }

    private void assertViolation(Supplier<?> function, Class<?> expectedViolationClass) {
        String expectedViolation = expectedViolationClass.getCanonicalName();
        try {
            function.get();
            Assert.fail("Expected to get a constraint violation on " + expectedViolation);
        } catch (ConstraintViolationException e) {
            assertEquals("Did not fid expected number of constraints", 1, e.getConstraintViolations().size());
            assertTrue("Did not find expected constraint " + expectedViolation + " in " + e.getConstraintViolations(),
                       e.getConstraintViolations()
                                       .stream()
                                       .anyMatch(v -> v.getConstraintDescriptor()
                                                       .getAnnotation()
                                                       .toString()
                                                       .contains(expectedViolation)));
            System.out.println("Found expected constraint violation " + expectedViolation);
        }
    }

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
