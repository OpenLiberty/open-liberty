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
import com.ibm.ws.microprofile.config.fat.suite.RepeatConfig11EE7;
import com.ibm.ws.microprofile.config.fat.suite.RepeatConfig12EE8;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;

/**
 *
 */
@Mode(TestMode.FULL)
public class DynamicSourcesTest extends AbstractConfigApiTest {

    private final static String testClassName = "DynamicSourcesTest";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(RepeatConfig11EE7.INSTANCE).andWith(RepeatConfig12EE8.INSTANCE);

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("DynamicSourcesServer");

    public DynamicSourcesTest() {
        super("/dynamicSources/");
    }

    @BuildShrinkWrap
    public static Archive buildApp() {
        String APP_NAME = "dynamicSources";

        WebArchive dynamicSources_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.dynamicSources.test")
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"),
                                               "services/org.eclipse.microprofile.config.spi.ConfigSource");

        return dynamicSources_war;
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Are the polling intervals honoured
     *
     * @throws Exception
     */
    @Test
    public void testDynamicTiming() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Do user sources get to change there minds?
     *
     * @throws Exception
     */
    @Test
    public void testDynamicServiceLoaderSources() throws Exception {
        test(testName.getMethodName());
    }

    /**
     * Do user sources get to change their minds?
     *
     * @throws Exception
     */
    @Test
    public void testDynamicUserAddedSources() throws Exception {
        test(testName.getMethodName());
    }
}
