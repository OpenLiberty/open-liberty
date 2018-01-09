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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.Year;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.validation.ConstraintViolation;
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

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/TestServletA")
public class BeanVal20TestServlet extends FATServlet {

    @Resource(name = "TestValidatorFactory")
    ValidatorFactory ivVFactory;

    @Resource(name = "TestValidator")
    Validator ivValidator;

    @Test
    public void testBasicConstraints() throws Exception {
        FieldValidatedBean b = new FieldValidatedBean();
        b.notNull = null;
        assertViolations(ivValidator.validate(b), NotNull.class);

        b = new FieldValidatedBean();
        b.email = "bob@bogus.com";
        assertViolations(ivValidator.validate(b), Email.class);

        b = new FieldValidatedBean();
        b.future = Instant.now().minusSeconds(5);
        assertViolations(ivValidator.validate(b), Future.class);

        b = new FieldValidatedBean();
        b.max100 = 101;
        assertViolations(ivValidator.validate(b), Max.class);

        b = new FieldValidatedBean();
        b.min5 = 3;
        assertViolations(ivValidator.validate(b), Min.class);

        b = new FieldValidatedBean();
        b.negative = 1;
        assertViolations(ivValidator.validate(b), Negative.class);

        b = new FieldValidatedBean();
        b.negativeOrZero = 1;
        assertViolations(ivValidator.validate(b), NegativeOrZero.class);

        b = new FieldValidatedBean();
        b.notNull = null;
        assertViolations(ivValidator.validate(b), NotNull.class);

        b = new FieldValidatedBean();
        b.past = Year.of(2222);
        assertViolations(ivValidator.validate(b), Past.class);

        b = new FieldValidatedBean();
        b.positive = -1;
        assertViolations(ivValidator.validate(b), Positive.class);

        b = new FieldValidatedBean();
        b.positiveOrZero = -1;
        assertViolations(ivValidator.validate(b), PositiveOrZero.class);

        b = new FieldValidatedBean();
        b.size3to5 = "ab";
        assertViolations(ivValidator.validate(b), Size.class);

        b = new FieldValidatedBean();
        b.size3to5 = "abcdefg";
        b.past = Year.of(2222);
        assertViolations(ivValidator.validate(b), Size.class, Past.class);

        b = new FieldValidatedBean();
        b.validBean.positive = -1;
        assertViolations(ivValidator.validate(b), Positive.class);
    }

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

    private void assertViolations(Set<ConstraintViolation<FieldValidatedBean>> violations, Class<?>... constraintTypes) {
        assertEquals(constraintTypes.length, violations.size());

        Set<String> foundConstraints = new HashSet<>();
        for (ConstraintViolation<FieldValidatedBean> v : violations) {
            String constraintAnno = v.getConstraintDescriptor().getAnnotation().toString();
            System.out.println("Found constraint violation '" + v.getMessage() + "' from annotation " + constraintAnno);
            foundConstraints.add(constraintAnno);
        }

        for (Class<?> expectedConstraint : constraintTypes)
            assertTrue("Did not find expected constraint " + expectedConstraint.getCanonicalName() + " in " + foundConstraints,
                       foundConstraints.stream().anyMatch(s -> s.contains(expectedConstraint.getCanonicalName())));
    }
}
