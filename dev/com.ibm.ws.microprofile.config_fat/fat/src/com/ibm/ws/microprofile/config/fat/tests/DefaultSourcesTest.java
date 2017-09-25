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

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.fat.util.SharedServer;

/**
 *
 */
public class DefaultSourcesTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("SimpleConfigSourcesServer");

    public DefaultSourcesTest() {
        super("/defaultSources/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     * Test that a simple config loads from the default locations
     * no provided files so config should just be process environment
     * variables, System.properties and WAS files.
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfig() throws Exception {
        test(testName);
    }

    @Test
    public void defaultsGetBuilderWithDefaults() throws Exception {
        test(testName);
    }

    /**
     * Tests that we can get a builder that will not include the default sources
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetEmptyBuilderNoDefaults() throws Exception {
        test(testName);
    }

    /**
     * Tests that a config source can be loaded from within a jar
     */
    @Test
    public void defaultsGetConfigPathJar() throws Exception {
        test(testName);
    }

    /**
     * Tests that a config source can be loaded from within a war
     */
    @Test
    public void defaultsGetConfigPathWar() throws Exception {
        test(testName);
    }

    /**
     * Tests that a config source can be loaded from all
     * valid places within a ear
     */
    @Test
    public void defaultsGetConfigPathEar() throws Exception {
        test(testName);
    }

    /**
     * Test that the microprofile-config.properties files are sourced ok
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigProperties() throws Exception {
        test(testName);
    }

    /**
     * Test that the WAS server level *.xml, *.properties and *.env files are sourced
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigWasSpecific() throws Exception {
        test(testName);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigPathSysProps() throws Exception {
        test(testName);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigPathProcEnv() throws Exception {
        test(testName);
    }

}
