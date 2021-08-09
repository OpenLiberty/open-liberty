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

package bval.v20.multixml.web;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.junit.Test;

import bval.v20.ejb1.web.beans.AValidationXMLTestBean1;
import bval.v20.ejb2.web.beans.AValidationXMLTestBean2;
import bval.v20.multixml.web.beans.AValidationXMLTestBean3;
import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BeanValidationTestServlet")
public class BeanValidationTestServlet extends FATServlet {

    @Inject
    AValidationXMLTestBean1 bean1;

    @Inject
    AValidationXMLTestBean2 bean2;

    /**
     * Test to check if the ValidatorFactory is configured with the provided custom
     * message interpolator specified in the validaiton.xml of the module containing AValidationXMLTestBean1.
     */
    @Test
    public void testCustomMessageInterpolatorFromJar1() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean1 testBean1 = (AValidationXMLTestBean1) ctx.lookup("java:app/MultipleValidationXmlEjb1/AValidationXMLTestBean1");
        testBean1.checkCustomMessageInterpolator();
    }

    /**
     * Test to check if the ValidatorFactory is configured with the provided custom
     * message interpolator specified in the validaiton.xml of the module containing AValidationXMLTestBean2.
     */
    @Test
    public void testCustomMessageInterpolatorFromJar2() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean2 testBean2 = (AValidationXMLTestBean2) ctx.lookup("java:app/MultipleValidationXmlEjb2/AValidationXMLTestBean2");
        testBean2.checkCustomMessageInterpolator();
    }

    /**
     * Test to check if the ValidatorFactory is configured with the provided custom
     * message interpolator specified in the validaiton.xml of the module containing AValidationXMLTestBean3.
     */
    @Test
    public void testCustomMessageInterpolatorFromWar() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean3 testBean3 = (AValidationXMLTestBean3) ctx.lookup("java:app/MultipleValidationXmlWeb/AValidationXMLTestBean3");
        testBean3.checkCustomMessageInterpolator();
    }

    /**
     * Test that @Inject for a ValidatorFactory works for
     * the module containing AValidationXMLTestBean1.
     */
    @Test
    public void testAtInjectValidatorFactoryFromJar1() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean1 testBean1 = (AValidationXMLTestBean1) ctx.lookup("java:app/MultipleValidationXmlEjb1/AValidationXMLTestBean1");
        assertTrue("should have been able to use a ValidatorFactory defined via @Inject in the EJB",
                   testBean1.checkAtInjectValidatorFactory());
    }

    /**
     * Test that @Inject for a ValidatorFactory works for
     * the module containing AValidationXMLTestBean2.
     */
    @Test
    public void testAtInjectValidatorFactoryFromJar2() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean2 testBean2 = (AValidationXMLTestBean2) ctx.lookup("java:app/MultipleValidationXmlEjb2/AValidationXMLTestBean2");
        assertTrue("should have been able to use a ValidatorFactory defined via @Inject in the EJB",
                   testBean2.checkAtInjectValidatorFactory());
    }

    /**
     * Test that @Inject for a ValidatorFactory works for
     * the module containing AValidationXMLTestBean3.
     */
    @Test
    public void testAtInjectValidatorFactoryFromWar() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean3 testBean3 = (AValidationXMLTestBean3) ctx.lookup("java:app/MultipleValidationXmlWeb/AValidationXMLTestBean3");
        assertTrue("should have been able to use a ValidatorFactory defined via @Inject in the EJB",
                   testBean3.checkAtInjectValidatorFactory());
    }

    /**
     * Test that @Resource for a ValidatorFactory works for
     * the module containing AValidationXMLTestBean1.
     */
    @Test
    public void testAtResourceValidatorFactoryFromJar1() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean1 testBean1 = (AValidationXMLTestBean1) ctx.lookup("java:app/MultipleValidationXmlEjb1/AValidationXMLTestBean1");
        assertTrue("should have been able to use a ValidatorFactory defined via @Resource in the EJB",
                   testBean1.checkAtResourceValidatorFactory());
    }

    /**
     * Test that @Resource for a ValidatorFactory works for
     * the module containing AValidationXMLTestBean2.
     */
    @Test
    public void testAtResourceValidatorFactoryFromJar2() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean2 testBean2 = (AValidationXMLTestBean2) ctx.lookup("java:app/MultipleValidationXmlEjb2/AValidationXMLTestBean2");
        assertTrue("should have been able to use a ValidatorFactory defined via @Resource in the EJB",
                   testBean2.checkAtResourceValidatorFactory());
    }

    /**
     * Test that @Resource for a ValidatorFactory works for
     * the module containing AValidationXMLTestBean3.
     */
    @Test
    public void testAtResourceValidatorFactoryFromWar() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean3 testBean3 = (AValidationXMLTestBean3) ctx.lookup("java:app/MultipleValidationXmlWeb/AValidationXMLTestBean3");
        assertTrue("should have been able to use a ValidatorFactory defined via @Resource in the EJB",
                   testBean3.checkAtResourceValidatorFactory());
    }

    /**
     * Test that a JNDI lookup for a ValidatorFactory works for
     * the module containing AValidationXMLTestBean3.
     */
    @Test
    public void testLookupValidatorFactoryFromWar() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean3 testBean3 = (AValidationXMLTestBean3) ctx.lookup("java:app/MultipleValidationXmlWeb/AValidationXMLTestBean3");
        assertTrue("should have been able to use a ValidatorFactory looked up via JNDI in the EJB",
                   testBean3.checkLookupValidatorFactory());
    }

    /**
     * Test that @Inject for a Validator works for
     * the module containing AValidationXMLTestBean1.
     */
    @Test
    public void testAtInjectValidatorFromJar1() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean1 testBean1 = (AValidationXMLTestBean1) ctx.lookup("java:app/MultipleValidationXmlEjb1/AValidationXMLTestBean1");
        assertTrue("should have been able to use a Validator defined via @Inject in the EJB",
                   testBean1.checkAtInjectValidator());
    }

    /**
     * Test that @Inject for a Validator works for
     * the module containing AValidationXMLTestBean2.
     */
    @Test
    public void testAtInjectValidatorFromJar2() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean2 testBean2 = (AValidationXMLTestBean2) ctx.lookup("java:app/MultipleValidationXmlEjb2/AValidationXMLTestBean2");
        assertTrue("should have been able to use a Validator defined via @Inject in the EJB",
                   testBean2.checkAtInjectValidator());
    }

    /**
     * Test that @Inject for a Validator works for
     * the module containing AValidationXMLTestBean3.
     */
    @Test
    public void testAtInjectValidatorFromWar() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean3 testBean3 = (AValidationXMLTestBean3) ctx.lookup("java:app/MultipleValidationXmlWeb/AValidationXMLTestBean3");
        assertTrue("should have been able to use a Validator defined via @Inject in the EJB",
                   testBean3.checkAtInjectValidator());
    }

    /**
     * Test that @Resource for a Validator works for
     * the module containing AValidationXMLTestBean1.
     */
    @Test
    public void testAtResourceValidatorFromJar1() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean1 testBean1 = (AValidationXMLTestBean1) ctx.lookup("java:app/MultipleValidationXmlEjb1/AValidationXMLTestBean1");
        assertTrue("should have been able to use a Validator defined via @Resource in the EJB",
                   testBean1.checkAtResourceValidator());
    }

    /**
     * Test that @Resource for a Validator works for
     * the module containing AValidationXMLTestBean2.
     */
    @Test
    public void testAtResourceValidatorFromJar2() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean2 testBean2 = (AValidationXMLTestBean2) ctx.lookup("java:app/MultipleValidationXmlEjb2/AValidationXMLTestBean2");
        assertTrue("should have been able to use a Validator defined via @Resource in the EJB",
                   testBean2.checkAtResourceValidator());
    }

    /**
     * Test that @Resource for a Validator works for
     * the module containing AValidationXMLTestBean3.
     */
    @Test
    public void testAtResourceValidatorFromWar() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean3 testBean3 = (AValidationXMLTestBean3) ctx.lookup("java:app/MultipleValidationXmlWeb/AValidationXMLTestBean3");
        assertTrue("should have been able to use a Validator defined via @Resource in the EJB",
                   testBean3.checkAtResourceValidator());
    }

    /**
     * Test that a JNDI lookup for a Validator works for
     * the module containing AValidationXMLTestBean3.
     */
    @Test
    public void testLookupValidatorFromWar() throws Exception {
        InitialContext ctx = new InitialContext();
        AValidationXMLTestBean3 testBean3 = (AValidationXMLTestBean3) ctx.lookup("java:app/MultipleValidationXmlWeb/AValidationXMLTestBean3");
        assertTrue("should have been able to use a Validator looked up via JNDI in the EJB",
                   testBean3.checkLookupValidator());
    }

    /**
     * Test method parameter constraints for AValidationXMLTestBean1
     */
    @Test
    @ExpectedFFDC("javax.validation.ConstraintViolationException")
    public void testMethodParmConstraintsFromJar1() throws Exception {
        if (bean1 == null) {
            throw new Exception("CDI didn't inject the bean AValidationXMLTestBean1 into this servlet");
        }

        // Call method with non-null string of length 5. This should pass validation.
        bean1.testMethodParmConstraintEJB1("12345");

        try {
            // Call method with null. This should throw an exception.
            bean1.testMethodParmConstraintEJB1(null);
            throw new Exception("interceptor didn't validate method call properly");
        } catch (EJBException e) {
            // Expected ConstraintViolationException will be wrapped in an EJBException
            Exception causedBy = e.getCausedByException();
            if (!(causedBy instanceof ConstraintViolationException)) {
                throw e;
            }
            Set<ConstraintViolation<?>> cvs = ((ConstraintViolationException) causedBy).getConstraintViolations();
            if (cvs.size() != 1) {
                throw new Exception("interceptor validated method parametersand caught " +
                                    "constraints, but size wasn't 1: " + cvs);
            }

        }
    }

    /**
     * Test method parameter constraints for AValidationXMLTestBean2
     */
    @Test
    @ExpectedFFDC("javax.validation.ConstraintViolationException")
    public void testMethodParmConstraintsFromJar2() throws Exception {
        if (bean2 == null) {
            throw new Exception("CDI didn't inject the bean AValidationXMLTestBean2 into this servlet");
        }

        // Call method with non-null string of length 3. This should pass validation.
        bean2.testMethodParmConstraintEJB2("123");

        try {
            // Call method with null. This should throw an exception.
            bean2.testMethodParmConstraintEJB2(null);
            throw new Exception("interceptor didn't validate method call properly");
        } catch (EJBException e) {
            // Expected ConstraintViolationException will be wrapped in an EJBException
            Exception causedBy = e.getCausedByException();
            if (!(causedBy instanceof ConstraintViolationException)) {
                throw e;
            }
            Set<ConstraintViolation<?>> cvs = ((ConstraintViolationException) causedBy).getConstraintViolations();
            if (cvs.size() != 1) {
                throw new Exception("interceptor validated method parametersand caught " +
                                    "constraints, but size wasn't 1: " + cvs);
            }

        }
    }
}
