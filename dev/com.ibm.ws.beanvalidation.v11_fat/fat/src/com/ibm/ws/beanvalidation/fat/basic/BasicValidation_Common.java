/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.fat.basic;

import static com.ibm.websphere.simplicity.ShrinkHelper.buildDefaultApp;
import static com.ibm.websphere.simplicity.ShrinkHelper.defaultDropinApp;
import static com.ibm.websphere.simplicity.ShrinkHelper.exportToServer;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Collection of tests that are applicable starting with Bean Validation 1.1. These
 * tests will run against any bean validation feature starting with beanValidation-1.1
 * with apps written to use 1.1 features.
 */
public abstract class BasicValidation_Common extends FATServletClient {

    protected static int bvalVersion;

    public static void createAndExportCommonWARs(LibertyServer server) throws Exception {
        defaultDropinApp(server, "defaultbeanvalidation_10.war", "defaultbeanvalidation10.web.*");
        defaultDropinApp(server, "defaultbeanvalidation_11.war", "defaultbeanvalidation11.web.*");
        WebArchive beanvalidation_10War = buildDefaultApp("beanvalidation_10.war", "beanvalidation10.*");
        WebArchive beanvalidation_11War = buildDefaultApp("beanvalidation_11.war", "beanvalidation11.*");

        Set<String> features = server.getServerConfiguration().getFeatureManager().getFeatures();
        if (!(features.contains("beanValidation-1.0") || features.contains("beanValidation-1.1") || features.contains("beanValidation-2.0"))) {
            beanvalidation_10War.move("/WEB-INF/constraints-house_EE9.xml", "/WEB-INF/constraints-house.xml");
            beanvalidation_11War.move("/WEB-INF/constraints-house_EE9.xml", "/WEB-INF/constraints-house.xml");
        }

        exportToServer(server, "dropins", beanvalidation_10War);
        exportToServer(server, "dropins", beanvalidation_11War);
    }

    public static void createAndExportApacheWARs(LibertyServer server) throws Exception {
        defaultDropinApp(server, "ApacheBvalConfig_10.war", "beanvalidation.apachebvalconfig10.web");
        defaultDropinApp(server, "ApacheBvalConfig_11.war", "beanvalidation.apachebvalconfig11.web");
    }

    public static void createAndExportCDIWARs(LibertyServer server) throws Exception {
        defaultDropinApp(server, "BeanValidationCDI_11" + ".war", "beanvalidation.cdi.*");
        defaultDropinApp(server, "DefaultBeanValidationCDI_11" + ".war", "defaultbeanvalidation.cdi.*");
    }

    public abstract LibertyServer getServer();

    protected void run(String war, String servlet) throws Exception {
        String originalTestName = testName.getMethodName();
        originalTestName = originalTestName.replace("_EE9_FEATURES", "");
        String servletTest = originalTestName.substring(0, originalTestName.length() - 2);
        run(war, servlet, servletTest);
    }

    /**
     * Run a test by connecting to a url that is put together with the context-root
     * being the war, the servlet and test method in the web application.
     */
    protected void run(String war, String servlet, String testMethod) throws Exception {
        FATServletClient.runTest(getServer(), war + "/" + servlet, testMethod);
    }

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
    @Test
    public void testBuildApacheConfiguredValidatorFactory10() throws Exception {
        assumeTrue(bvalVersion < 20);
        run("ApacheBvalConfig_10", "BeanValidationServlet",
            "testBuildApacheConfiguredValidatorFactory&isFeature11=" + (bvalVersion == 11));
    }

    /**
     * Ensure that the apache bval impl classes have the proper visibility to
     * the application.
     */
    @Test
    public void testApacheBvalImplClassVisibility10() throws Exception {
        assumeTrue(bvalVersion < 20);
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
        List<String> errMsgs = getServer().findStringsInLogsAndTrace("CWWKF0033E");
        assertTrue(errMsgs.isEmpty());

        //CWWKE0702E: Could not resolve module:
        errMsgs = getServer().findStringsInLogsAndTrace("CWWKE0702E");
        assertTrue(errMsgs.isEmpty());
    }

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
