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
package com.ibm.ws.microprofile.config.fat.tests;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@Mode(TestMode.FULL)
public class DynamicSourcesTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("DynamicSourcesServer");

    public DynamicSourcesTest() {
        super("/dynamicSources/");
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
