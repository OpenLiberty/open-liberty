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
package com.ibm.ws.beanvalidation.fat.cdi;

import org.junit.Test;

import com.ibm.ws.beanvalidation.fat.basic.BasicValidation_Common;

/**
 * Collection of tests to be run when both cdi-1.1 and beanValidation-1.1 are enabled
 * together. Include all common tests from {@link BasicValidation_Common} to ensure
 * that everything that worked without CDI works with it as well.
 */
public abstract class BeanValidationCDI_Common extends BasicValidation_Common {

    /**
     * Test that a servlet can use @Resource to inject a ValidatorFactory that
     * configures a custom MessageInterpolator. This custom component uses @Inject
     * to implement the interface.
     */
    @Test
    public void testCDIInjectionInInterpolatorAtResource11() throws Exception {
        run("BeanValidationCDI_11", "BValAtResourceServlet");
    }

    /**
     * Test that a servlet can use @Inject to inject a ValidatorFactory that
     * configures a custom MessageInterpolator. This custom component uses @Inject
     * to implement the interface.
     *
     * TODO this currently isn't working because the CDI extension that will
     * get enabled when cdi-1.1 and beanValidation-1.1 are both enabled isn't
     * in place yet.
     */
    @Test
    public void testCDIInjectionInInterpolatorAtInject11() throws Exception {
        run("BeanValidationCDI_11", "BValAtInjectServlet");
    }

    /**
     * Test that a servlet can use jndi to lookup a ValidatorFactory that
     * configures a custom MessageInterpolator. This custom component uses @Inject
     * to implement the interface.
     */
    @Test
    public void testCDIInjectionInInterpolatorLookup11() throws Exception {
        run("BeanValidationCDI_11", "BValServlet");
    }

    /**
     * Test that a servlet can use jndi to lookup a ValidatorFactory that
     * configures a custom ConstraintValidatorFactory. This custom component
     * uses @Inject to inject a CDI bean.
     */
    @Test
    public void testCDIInjectionInConstraintValidatorFactoryLookup11() throws Exception {
        run("BeanValidationCDI_11", "BValServlet");
    }

    /**
     * Test that a CDI managed bean can specify method and constructor parameter/return
     * value constraints and the get evaluated automatically.
     */
    @Test
    public void testMethodValidation11() throws Exception {
        run("BeanValidationCDI_11", "BValServlet");
    }

    /**
     * Test that when a custom ConstraintValidatorFactory is not specified in validation.xml,
     * the default provided one by the container creates it's ConstraintValidator instances
     * as CDI managed beans.
     */
    @Test
    public void testConstraintValidatorInjection11() throws Exception {
        run("DefaultBeanValidationCDI_11", "BValInjectionServlet");
    }

    /**
     * Test that bean validation interceptors are not being registered and invoked multiple times.
     * See defect 213484.
     */
    @Test
    public void testInterceptorRegisteredOnlyOnce11() throws Exception {
        run("DefaultBeanValidationCDI_11", "BValInjectionServlet");
    }

    /**
     * Test that @DecimalMax and @DecimalMin correctly implement the inclusive property.
     *
     * Test data is given as a double.
     */
    @Test
    public void testDecimalInclusiveForNumber() throws Exception {
        run("DefaultBeanValidationCDI_11", "BValInjectionServlet", "testDecimalInclusiveForNumber");
    }

    /**
     * Test that @DecimalMax and @DecimalMin correctly implement the inclusive property.
     *
     * Test data is given as a String
     */
    @Test
    public void testDecimalInclusiveForString() throws Exception {
        run("DefaultBeanValidationCDI_11", "BValInjectionServlet", "testDecimalInclusiveForString");
    }
}
