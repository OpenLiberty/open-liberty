/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.config;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class JwtConfigUtilTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all:io.openliberty.security*=all");

    static final String SIG_ALG_ATTR_NAME = "signatureAlgorithm";

    static final String DEFAULT_SIG_ALG = "RS256";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    @Test
    public void test_getSignatureAlgorithm_noOtherProps() {
        Map<String, Object> props = new HashMap<String, Object>();

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the default signature algorithm as expected.", DEFAULT_SIG_ALG, result);

        // No signature algorithm configured should just default and not emit a warning message
        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void test_getSignatureAlgorithm_es384() {
        String esAlgorithm = "ES384";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, esAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", esAlgorithm, result);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void test_getSignatureAlgorithm_hs256() {
        String hsAlgorithm = "HS256";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, hsAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", hsAlgorithm, result);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

}