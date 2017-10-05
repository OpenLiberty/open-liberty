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

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.beanvalidation.fat.FATSuite;

import componenttest.topology.impl.LibertyServerFactory;

/**
 * Collection of tests to be run when both cdi-1.1 and beanValidation-1.1 are enabled
 * together. Include all common tests from {@link BasicValidation11CommonTest} to ensure
 * that everything that worked without CDI works with it as well.
 */
public class BeanValidationCDITest extends BasicValidation11CommonTest {
    private static final String FOLDER = "dropins";

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.beanvalidation.cdi.fat");

        FATSuite.createAndExportWARs();

        WebArchive war = ShrinkWrap.create(WebArchive.class, "BeanValidationCDI_11" + ".war");
        war.addPackage("beanvalidation.cdi.beans");
        war.addPackage("beanvalidation.cdi.web");
        war.addPackage("beanvalidation.cdi.validation");
        war.addAsWebInfResource(new File("test-applications/BeanValidationCDI_11.war/resources/WEB-INF/beans.xml"));
        war.addAsDirectory("WEB-INF/classes/META-INF");
        war.addAsResource(new File("test-applications/BeanValidationCDI_11.war/resources/WEB-INF/classes/META-INF/validation.xml"), "META-INF/validation.xml");
        ShrinkHelper.exportToServer(server, FOLDER, war);

        war = ShrinkWrap.create(WebArchive.class, "DefaultBeanValidationCDI_11" + ".war");
        war.addPackage("defaultbeanvalidation.cdi.beans");
        war.addPackage("defaultbeanvalidation.cdi.web");
        war.addPackage("defaultbeanvalidation.cdi.validation");
        war.addAsWebInfResource(new File("test-applications/DefaultBeanValidationCDI_11.war/resources/WEB-INF/beans.xml"));
        ShrinkHelper.exportToServer(server, FOLDER, war);

        server.addInstalledAppForValidation(FATSuite.APACHE_BVAL_CONFIG10);
        server.addInstalledAppForValidation(FATSuite.APACHE_BVAL_CONFIG11);
        server.addInstalledAppForValidation(FATSuite.BEAN_VALIDATION10);
        server.addInstalledAppForValidation(FATSuite.BEAN_VALIDATION11);
        server.addInstalledAppForValidation(FATSuite.DEFAULT_BEAN_VALIDATION10);
        server.addInstalledAppForValidation(FATSuite.DEFAULT_BEAN_VALIDATION11);
        server.addInstalledAppForValidation("BeanValidationCDI_11");
        server.startServer(BeanValidationCDITest.class.getSimpleName() + ".log");
    }

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

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }

        // TODO this needs to be debugged, as currently all @PreDestroy methods
        // created by bean validation get called twice.
//        List<String> destroyedList;
//        destroyedList = server.findStringsInLogs("CustomConstraintValidatorFactory is getting destroyed.");
//        Assert.assertEquals("CustomConstraintValidatorFactory wasn't destroyed once: " + destroyedList,
//                            1, destroyedList.size());
//
//        destroyedList = server.findStringsInLogs("CustomMessageInterpolator is getting destroyed.");
//        Assert.assertEquals("CustomConstraintValidatorFactory wasn't destroyed once: " + destroyedList,
//                            1, destroyedList.size());
//
//        destroyedList = server.findStringsInLogs("TestAnnotationValidator is getting destroyed.");
//        Assert.assertEquals("CustomConstraintValidatorFactory wasn't destroyed once: " + destroyedList,
//                            1, destroyedList.size());

        //Check that server logs are really collected when an application fails to start, if this line is ever re-enabled.
        //Currently they do not get collected when server.stopServer(false) is called.
        //server.postStopServerArchive();
    }
}
