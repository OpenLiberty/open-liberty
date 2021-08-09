/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class UserApiConfigImplTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    final String userApi = "myUserApi";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** constructors **************************************/

    @Test
    public void init_nullUserApi() {
        try {
            UserApiConfigImpl apiConfig = new UserApiConfigImpl(null);
            assertNull("API was not null as expected but was [" + apiConfig.getApi() + "].", apiConfig.getApi());
            assertEquals("API method did not match expected value.", Constants.client_secret_basic, apiConfig.getMethod());
            assertNull("API parameter was not null as expected but was [" + apiConfig.getParameter() + "].", apiConfig.getParameter());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void init_withUserApi() {
        try {
            UserApiConfigImpl apiConfig = new UserApiConfigImpl(userApi);
            assertEquals("API string did not match expected value.", userApi, apiConfig.getApi());
            assertEquals("API method did not match expected value.", Constants.client_secret_basic, apiConfig.getMethod());
            assertNull("API parameter was not null as expected but was [" + apiConfig.getParameter() + "].", apiConfig.getParameter());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void init_nullArgs() {
        try {
            UserApiConfigImpl apiConfig = new UserApiConfigImpl(null, null);
            assertNull("API was not null as expected but was [" + apiConfig.getApi() + "].", apiConfig.getApi());
            assertNull("API method was not null as expected but was [" + apiConfig.getMethod() + "].", apiConfig.getMethod());
            assertNull("API parameter was not null as expected but was [" + apiConfig.getParameter() + "].", apiConfig.getParameter());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

}
