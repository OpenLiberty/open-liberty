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

import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import bval.v20.ejb1.web.beans.AValidationXMLTestBean1;
import bval.v20.ejb2.web.beans.AValidationXMLTestBean2;
import bval.v20.web.beans.AValidationXMLTestBean3;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BeanValidationTestServlet")
public class BeanValidationTestServlet extends FATServlet {

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
}
