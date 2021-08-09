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
package com.ibm.ws.beanvalidation.fat.ejb;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Run ejb module tests on bval-1.1.
 *
 * Test various combinations where an application is packaged with one web module
 * and either one or two ejb modules. The web module does not include a validation.xml
 * and both ejb modules do. This covers what validation.xml is found both by the
 * container and provider and needs to be common between bval-1.0 and bval-1.1.
 */
public abstract class EJBModule_Common extends FATServletClient {

    public static void createAndExportEJBWARs(LibertyServer server) throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchive("EJBModule1EJB.jar", "beanvalidation.ejbmodule.*");
        JavaArchive jar2 = ShrinkHelper.buildJavaArchive("EJBModule2EJB.jar", "beanvalidation.ejbmodule2.ejb");

        if (JakartaEE9Action.isActive()) {
            jar.move("/META-INF/constraints-house_EE9.xml", "/META-INF/constraints-house.xml");
            jar2.move("/META-INF/constraints-house_EE9.xml", "/META-INF/constraints-house.xml");
        }

        WebArchive war = ShrinkHelper.buildDefaultApp("EJBModuleWeb.war", "beanvalidation.web");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "OneEJBModuleApp.ear")
                        .addAsModule(war)
                        .addAsModule(jar);
        ShrinkHelper.addDirectory(ear, "test-applications/OneEJBModuleApp.ear/resources/");
        ShrinkHelper.exportToServer(server, "dropins", ear);

        EnterpriseArchive ear2 = ShrinkWrap.create(EnterpriseArchive.class, "TwoEJBModulesApp.ear")
                        .addAsModule(war)
                        .addAsModule(jar)
                        .addAsModule(jar2);
        ShrinkHelper.addDirectory(ear2, "test-applications/TwoEJBModulesApp.ear/resources/");
        ShrinkHelper.exportToServer(server, "dropins", ear2);
    }

    protected abstract LibertyServer getServer();

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
        testMethod = testMethod.replace("_EE9_FEATURES", "");
        FATServletClient.runTest(getServer(), war + "/" + servlet, testMethod);
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
}
