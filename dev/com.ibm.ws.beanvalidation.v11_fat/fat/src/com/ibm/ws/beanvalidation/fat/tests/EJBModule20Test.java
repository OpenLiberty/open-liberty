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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.beanvalidation.fat.FATSuite;

import componenttest.topology.impl.LibertyServerFactory;

/**
 * Run ejb module tests on bval-2.0.
 *
 * Test various combinations where an application is packaged with one web module
 * and either one or two ejb modules. The web module does not include a validation.xml
 * and both ejb modules do. This covers what validation.xml is found both by the
 * container and provider and needs to be common between bval-1.0 and bval-1.1.
 */
public class EJBModule20Test extends AbstractTest {
    private static final String FOLDER = "dropins";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.beanvalidation.ejb_2.0.fat");

        FATSuite.createAndExportEJBWARs(server);

        server.startServer(EJBModule20Test.class.getSimpleName() + ".log");
    }

    /**
     * Similar to {@link #testLookupValidatorFactoryInServletWithOneEJBModule()}, this
     * test ensures that when there are two ejb modules in the application that under
     * bval-1.1 the lookup still doesn't fail. As documented there, under bval-1.1
     * in this case the ValidatorFactory is built correctly without any incorrect
     * validation.xml because the container now calls ignoreXmlConfiguration before
     * delegating to the provider to create the validator factory.
     */
    @Test
    @Ignore
    //TODO: fix this test - it is supposed to test the beanValidation-1.1 behavior, but
    // it is getting the 1.0 behavior because it's deployment descriptor specifies 1.0
    // and the behavior is determined by the value in the DD.
    // To fix this, one would need to either duplicate the test app and then set the
    // DD to use 1.1 - or find some way to modify the DD version with only one copy
    // of the rest of the app.
    public void testLookupValidatorFactoryInServletWithTwoEJBModules() throws Exception {
        run("TwoEJBModulesWeb", "BeanValidationServlet", "testLookupValidatorFactoryInServlet");
    }

    /**
     * Under both bval-1.0 and bval-1.1, a lookup of a validator factory in the web
     * module will not cause a NamingException due to a ValidatorException when there
     * is only one ejb module in the application.
     *
     * The difference between the two cases is bval-1.0 will find the WRONG validation.xml
     * from the ejb module and use that to configure the ValidatorFactory. The bval-1.1
     * case will simply create the default ValidatorFactory without any configuration.
     */
    @Test
    public void testLookupValidatorFactoryInServletWithOneEJBModule() throws Exception {
        run("OneEJBModuleWeb", "BeanValidationServlet", "testLookupValidatorFactoryInServlet");
    }

    /**
     * TestDescription:
     * This test verifies that the ValidatorFactory is built using the custom
     * specified message interpolator.
     *
     * Only testing this custom component because coverage of all components is
     * contained in the core bean validation test cases. Here we just want to ensure
     * that loading the classes from an ejb module still works.
     */
    @Test
    public void testCheckCustomMessageInterpolator() throws Exception {
        run("OneEJBModuleWeb", "BeanValidationServlet", testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test verifies that it does not always work to directly invoke
     * Validation.buildDefaultValidatorFactory in an EJB. This is because EJB's share
     * classloaders, which means the provider will find multiple META-INF/validation.xml's
     * within the application (one per EJB module). The provider used Classloader.getResource,
     * which is why this doesn't work in this case.
     */
    @Test
    public void testBuildDefaultValidatorFactoryFail() throws Exception {
        run("TwoEJBModulesWeb", "BeanValidationServlet", testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test verifies that in a simple application that only has one EJB module
     * with a validation.xml, Validation.buildDefaultValidatorFactory does indeed
     * work to build a ValidatorFactory instance outside of the server's control.
     */
    @Test
    public void testBuildDefaultValidatorFactory() throws Exception {
        run("OneEJBModuleWeb", "BeanValidationServlet", testName.getMethodName());
    }

    /**
     * TestDescription:
     * This test verifies that a simple application that only has one EJB module
     * with a validation.xml can use Validation.buildDefaultValidatorFactory to
     * get a Validator and use that to validate an EJB.
     */
    @Test
    public void testUseBuildDefaultValidatorFactory() throws Exception {
        run("OneEJBModuleWeb", "BeanValidationServlet", testName.getMethodName());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
