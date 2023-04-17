/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token.auth;

import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class PrivateKeyJwtAuthMethodTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private String clientId;
    private final String tokenEndpointUrl = "https://somehost/path/to/token";

    PrivateKeyJwtAuthMethod authMethod;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        clientId = testName.getMethodName();
        authMethod = new PrivateKeyJwtAuthMethod(clientId, tokenEndpointUrl);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_createPrivateKeyJwt() {
        assertNull("Result should be null until this method is implemented.", authMethod.createPrivateKeyJwt());
    }

}
