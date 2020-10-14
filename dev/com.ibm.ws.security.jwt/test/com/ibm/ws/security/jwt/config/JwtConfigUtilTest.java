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
import static org.junit.Assert.assertNull;

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

    protected final static String MSG_CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED = "CWWKS6055W";

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
        setBeta(false);
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    @Test
    public void test_getSignatureAlgorithm_betaDisabled_noOtherProps() {
        Map<String, Object> props = new HashMap<String, Object>();

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the default signature algorithm as expected.", DEFAULT_SIG_ALG, result);

        // No signature algorithm configured should just default and not emit a warning message
        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void test_getSignatureAlgorithm_betaDisabled_betaAlgorithm() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, "ES256");

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the default signature algorithm as expected.", DEFAULT_SIG_ALG, result);

        verifyLogMessage(outputMgr, MSG_CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED + ".+" + "ES256");
    }

    @Test
    public void test_getSignatureAlgorithm_betaDisabled_betaAlgorithm_callMethodTwice() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, "ES256");

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the default signature algorithm as expected.", DEFAULT_SIG_ALG, result);

        verifyLogMessage(outputMgr, MSG_CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED + ".+" + "ES256");

        // Second call to the method for the same ID should not issue the warning message
        outputMgr.resetStreams();
        result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void test_getSignatureAlgorithm_betaDisabled_nonBetaAlgorithm() {
        String validNonBetaAlgorithm = "HS256";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, validNonBetaAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", validNonBetaAlgorithm, result);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void test_getSignatureAlgorithm_betaEnabled_noOtherProps() {
        setBeta(true);
        Map<String, Object> props = new HashMap<String, Object>();

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertNull("Should have gotten null as the signature algorithm, but got " + result + ".", result);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void test_getSignatureAlgorithm_betaEnabled_betaAlgorithm() {
        setBeta(true);
        String validBetaAlgorithm = "ES384";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, validBetaAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", validBetaAlgorithm, result);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void test_getSignatureAlgorithm_betaEnabled_nonBetaAlgorithm() {
        setBeta(true);
        String validNonBetaAlgorithm = "HS256";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, validNonBetaAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(testName.getMethodName(), props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", validNonBetaAlgorithm, result);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    private void setBeta(boolean enable) {
        if (enable) {
            System.setProperty("com.ibm.ws.beta.edition", "true");
        } else {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

}