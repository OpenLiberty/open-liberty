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
package com.ibm.ws.beanvalidation.fat.tests;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

/**
 * Collection of tests that are applicable starting with Bean Validation 1.1. These
 * tests will run against any bean validation feature starting with beanValidation-1.1
 * with apps written to use 1.1 features.
 */
public class BasicValidation11CommonTest extends BasicValidation10CommonTest {
    /**
     * Verify that the Bean Validation feature is loaded properly and is
     * functional by calling Validation.buildDefaultValidatorFactory.
     */
    @Test
    public void testDefaultBuildDefaultValidatorFactory11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidation");
    }

    /**
     * Verify that the module ValidatorFactory may be looked up at:
     *
     * java:comp/ValidatorFactory
     */
    @Test
    public void testDefaultLookupJavaCompValidatorFactory11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidation");
    }

    /**
     * Verify that the module Validator may be looked up at:
     *
     * java:comp/Validator
     */
    @Test
    public void testDefaultLookupJavaCompValidator11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidation");
    }

    /**
     * Verify that the module ValidatorFactory may be injected and looked up at:
     *
     * java:comp/env/TestValidatorFactory
     */
    @Test
    public void testDefaultInjectionAndLookupValidatorFactory11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidationInjection");
    }

    /**
     * Verify that the module Validator may be injected and looked up at:
     *
     * java:comp/env/TestValidator
     */
    @Test
    public void testDefaultInjectionAndLookupValidator11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidationInjection");
    }

    /**
     * Test validation of a bean with constraints defined in annotations.
     */
    @Test
    public void testDefaultValidatingBeanWithConstraints11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in annotations
     * where validation failures are expected.
     */
    @Test
    public void testDefaultValidatingBeanWithConstraintsToFail11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidation");
    }

    /**
     * Verify that the module ValidatorFactory from validation.xml may be looked up at:
     *
     * java:comp/ValidatorFactory
     */
    @Test
    public void testLookupJavaCompValidatorFactory11() throws Exception {
        run("beanvalidation_11", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml.
     */
    @Test
    public void testValidatingXMLBeanWithConstraints11() throws Exception {
        run("beanvalidation_11", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml
     * where validation failures are expected.
     */
    @Test
    public void testValidatingXMLBeanWithConstraintsToFail11() throws Exception {
        run("beanvalidation_11", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml and annotations.
     */
    @Test
    public void testValidatingMixBeanWithConstraints11() throws Exception {
        run("beanvalidation_11", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml and annotations
     * where validation failures are expected.
     */
    @Test
    public void testValidatingMixBeanWithConstraintsToFail11() throws Exception {
        run("beanvalidation_11", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in annotations, with
     * other beans in the app having xml constraints.
     */
    @Test
    public void testValidatingAnnBeanWithConstraints11() throws Exception {
        run("beanvalidation_11", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in annotations, with
     * other beans in the app having xml constraints, where validation failures
     * are expected.
     */
    @Test
    public void testValidatingAnnBeanWithConstraintsToFail11() throws Exception {
        run("beanvalidation_11", "BeanValidation");
    }

    /**
     * Test that a servlet can inject a ValidatorFactory reference from validation.xml
     * and then look up that same factory in the java:comp/env namespace
     */
    @Test
    public void testInjectionAndLookupValidatorFactory11() throws Exception {
        run("beanvalidation_11", "BeanValidationInjection");
    }

    /**
     * Test that a servlet can inject a Validator reference from validation.xml
     * and then look up that same validator in the java:comp/env namespace
     */
    @Test
    public void testInjectionAndLookupValidator11() throws Exception {
        run("beanvalidation_11", "BeanValidationInjection");
    }

    /**
     * Test that a custom traversable resolver can be specified in validation.xml
     * and can then be used from the ValidatorFactory.
     */
    @Test
    public void testCustomTraversableResolver11() throws Exception {
        run("beanvalidation_11", "BeanValidationInjection");
    }

    /**
     * Test that a custom constraint validator factory can be specified in validation.xml
     * and can then be used from the ValidatorFactory
     */
    @Test
    public void testCustomConstraintValidatorFactory11() throws Exception {
        run("beanvalidation_11", "BeanValidationInjection");
    }

    /**
     * Test that a custom parameter name provider can be specified in validation.xml
     * and can then be used from the ValidatorFactory
     */
    @Test
    public void testCustomParameterNameProvider11() throws Exception {
        //TODO: Remove this assumption when the ValidationConfigurationV20FactoryImpl can be
        // enabled without the CDI bundle
        assumeTrue(bvalVersion < 20);
        run("beanvalidation_11", "BeanValidationInjection");
    }

    /**
     * Test that validation.xml can specify all components directly from the
     * Apache Bval 1.1 bundle.
     */
    @Test
    public void testBuildApacheConfiguredValidatorFactory11() throws Exception {
        assumeTrue(bvalVersion < 20);
        run("ApacheBvalConfig_11", "BeanValidationServlet");
    }

    /**
     * Ensure that the apache bval impl classes have the proper visibility to
     * the application.
     */
    @Test
    public void testApacheBvalImplClassVisibility11() throws Exception {
        assumeTrue(bvalVersion < 20);
        run("ApacheBvalConfig_11", "BeanValidationServlet");
    }

    /**
     * Ensure that a constraint message can use EL in it's text.
     */
    @Test
    public void testELValidationViolationMessage11() throws Exception {
        run("defaultbeanvalidation_11", "DefaultBeanValidation",
            "testELValidationViolationMessage&isELEnabled=true");
    }

}
