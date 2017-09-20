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

import componenttest.annotation.ExpectedFFDC;

/**
 *
 */
public class ClassLoadersTest extends AbstractConfigApiTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("ClassLoadersServer");

    public ClassLoadersTest() {
        super("/classLoaders/");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Rule
    public TestName testName = new TestName();

    /**
     *
     * @throws Exception
     */
    @Test
    public void testUserClassLoaders() throws Exception {
        test(testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.util.ServiceConfigurationError" })
    public void testUserLoaderErrors() throws Exception {
        test(testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testMultiUrlResources() throws Exception {
        test(testName.getMethodName());
    }

}
