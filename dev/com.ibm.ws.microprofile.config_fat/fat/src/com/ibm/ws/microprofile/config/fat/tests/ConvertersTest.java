/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.microprofile.config.fat.suite.RepeatConfig11EE8;
import com.ibm.ws.microprofile.config.fat.suite.RepeatConfig12EE8;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.ExpectedFFDC;
import componenttest.rules.repeater.RepeatTests;

/**
 *
 */
public class ConvertersTest extends AbstractConfigApiTest {

    private final static String testClassName = "ConvertersTest";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(RepeatConfig11EE8.INSTANCE).andWith(RepeatConfig12EE8.INSTANCE);

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("ConvertersServer");

    @BuildShrinkWrap
    public static Archive buildApp() {
        String APP_NAME = "converters";

        WebArchive converters_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.converters.test")
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter"),
                                               "services/org.eclipse.microprofile.config.spi.Converter");

        return converters_war;
    }

    public ConvertersTest() {
        super("/converters/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Test that a simple converter can be registered for a user type
     *
     * @throws Exception
     */
    @Test
    public void testConverters() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test support for duplicate converters
     *
     * @throws Exception
     */
    @Test
    public void testMultipleSameTypeConverters() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test support for different type and subclass converters
     *
     * @throws Exception
     */
    @Test
    public void testConverterSubclass() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test support for discovered converters
     *
     * @throws Exception
     */
    @Test
    public void testDiscoveredConverters() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Test what happens when a converter raises an exception
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ws.microprofile.config.interfaces.ConversionException" })
    public void testConverterExceptions() throws Exception {
        test(testName.getMethodName());
    }

}
