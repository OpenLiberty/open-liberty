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

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Collection of tests that are applicable starting with Bean Validation 1.0. These
 * tests will run against any bean validation feature but with apps written to use
 * 1.0 features.
 */
public class BasicValidation10CommonTest extends AbstractTest {

    protected boolean isUsingBval11;

    /**
     * Verify that the Bean Validation feature is loaded properly and is
     * functional by calling Validation.buildDefaultValidatorFactory.
     */
    @Test
    public void testDefaultBuildDefaultValidatorFactory10() throws Exception {
        run("defaultbeanvalidation_10", "DefaultBeanValidation");
    }

    /**
     * Verify that the module ValidatorFactory may be looked up at:
     *
     * java:comp/ValidatorFactory
     */
    @Test
    public void testDefaultLookupJavaCompValidatorFactory10() throws Exception {
        run("defaultbeanvalidation_10", "DefaultBeanValidation");
    }

    /**
     * Verify that the module Validator may be looked up at:
     *
     * java:comp/Validator
     */
    @Test
    public void testDefaultLookupJavaCompValidator10() throws Exception {
        run("defaultbeanvalidation_10", "DefaultBeanValidation");
    }

    /**
     * Verify that the module ValidatorFactory may be injected and looked up at:
     *
     * java:comp/env/TestValidatorFactory
     */
    @Test
    public void testDefaultInjectionAndLookupValidatorFactory10() throws Exception {
        run("defaultbeanvalidation_10", "DefaultBeanValidationInjection");
    }

    /**
     * Verify that the module Validator may be injected and looked up at:
     *
     * java:comp/env/TestValidator
     */
    @Test
    public void testDefaultInjectionAndLookupValidator10() throws Exception {
        run("defaultbeanvalidation_10", "DefaultBeanValidationInjection");
    }

    /**
     * Test validation of a bean with constraints defined in annotations.
     */
    @Test
    public void testDefaultValidatingBeanWithConstraints10() throws Exception {
        run("defaultbeanvalidation_10", "DefaultBeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in annotations
     * where validation failures are expected.
     */
    @Test
    public void testDefaultValidatingBeanWithConstraintsToFail10() throws Exception {
        run("defaultbeanvalidation_10", "DefaultBeanValidation");
    }

    /**
     * Verify that the module ValidatorFactory from validation.xml may be looked up at:
     *
     * java:comp/ValidatorFactory
     */
    @Test
    public void testLookupJavaCompValidatorFactory10() throws Exception {
        run("beanvalidation_10", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml.
     */
    @Test
    public void testValidatingXMLBeanWithConstraints10() throws Exception {
        run("beanvalidation_10", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml
     * where validation failures are expected.
     */
    @Test
    public void testValidatingXMLBeanWithConstraintsToFail10() throws Exception {
        run("beanvalidation_10", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml and annotations.
     */
    @Test
    public void testValidatingMixBeanWithConstraints10() throws Exception {
        run("beanvalidation_10", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml and annotations
     * where validation failures are expected.
     */
    @Test
    public void testValidatingMixBeanWithConstraintsToFail10() throws Exception {
        run("beanvalidation_10", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in annotations, with
     * other beans in the app having xml constraints.
     */
    @Test
    public void testValidatingAnnBeanWithConstraints10() throws Exception {
        run("beanvalidation_10", "BeanValidation");
    }

    /**
     * Test validation of a bean with constraints defined in annotations, with
     * other beans in the app having xml constraints, where validation failures
     * are expected.
     */
    @Test
    public void testValidatingAnnBeanWithConstraintsToFail10() throws Exception {
        run("beanvalidation_10", "BeanValidation");
    }

    /**
     * Test that a servlet can inject a ValidatorFactory reference from validation.xml
     * and then look up that same factory in the java:comp/env namespace
     */
    @Test
    public void testInjectionAndLookupValidatorFactory10() throws Exception {
        run("beanvalidation_10", "BeanValidationInjection");
    }

    /**
     * Test that a servlet can inject a Validator reference from validation.xml
     * and then look up that same validator in the java:comp/env namespace
     */
    @Test
    public void testInjectionAndLookupValidator10() throws Exception {
        run("beanvalidation_10", "BeanValidationInjection");
    }

    /**
     * Test that a custom traversable resolver can be specified in validation.xml
     * and can then be used from the ValidatorFactory.
     */
    @Test
    public void testCustomTraversableResolver10() throws Exception {
        run("beanvalidation_10", "BeanValidationInjection");
    }

    /**
     * Test that a custom constraint validator factory can be specified in validation.xml
     * and can then be used from the ValidatorFactory
     */
    @Test
    public void testCustomConstraintValidatorFactory10() throws Exception {
        run("beanvalidation_10", "BeanValidationInjection");
    }

    /**
     * Test that validation.xml can specify all components directly from the
     * Apache Bval 1.0 bundle.
     */
    //@Test TODO disable for bval 2.0
    public void testBuildApacheConfiguredValidatorFactory10() throws Exception {
        run("ApacheBvalConfig_10", "BeanValidationServlet",
            "testBuildApacheConfiguredValidatorFactory&isFeature11=" + isUsingBval11);
    }

    /**
     * Ensure that the apache bval impl classes have the proper visibility to
     * the application.
     */
    //@Test TODO disable for bval 2.0
    public void testApacheBvalImplClassVisibility10() throws Exception {
        run("ApacheBvalConfig_10", "BeanValidationServlet");
    }

    /**
     * Ensure that there were no feature errors during server startup
     * Error currently searched for:
     * CWWKF0033E
     * CWWKE0702E
     */
    @Test
    public void testCheckLogsForErrors() throws Exception {
        //CWWKF0033E: The singleton features com.ibm.websphere.appserver.javaeeCompatible-6.0 and com.ibm.websphere.appserver.javaeeCompatible-7.0
        //cannot be loaded at the same time.  The configured features <featureA> and <featureB> include one or more features
        //that cause the conflict. Your configuration is not supported; update server.xml to remove incompatible features.
        List<String> errMsgs = server.findStringsInLogsAndTrace("CWWKF0033E");
        assertTrue(errMsgs.isEmpty());

        //CWWKE0702E: Could not resolve module:
        errMsgs = server.findStringsInLogsAndTrace("CWWKE0702E");
        assertTrue(errMsgs.isEmpty());
    }
}
