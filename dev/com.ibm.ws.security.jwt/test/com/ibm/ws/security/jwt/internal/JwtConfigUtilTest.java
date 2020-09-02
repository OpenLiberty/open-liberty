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
package com.ibm.ws.security.jwt.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class JwtConfigUtilTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all:io.openliberty.security*=all");

    @Rule
    public final TestName testName = new TestName();

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
        setBeta(false);
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    @Test
    public void test_getSignatureAlgorithm_betaDisabled_noOtherProps() {
        Map<String, Object> props = new HashMap<String, Object>();

        String result = JwtConfigUtil.getSignatureAlgorithm(props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the default signature algorithm as expected.", DEFAULT_SIG_ALG, result);
    }

    @Test
    public void test_getSignatureAlgorithm_betaDisabled_betaAlgorithm() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, "ES256");

        String result = JwtConfigUtil.getSignatureAlgorithm(props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the default signature algorithm as expected.", DEFAULT_SIG_ALG, result);
    }

    @Test
    public void test_getSignatureAlgorithm_betaDisabled_nonBetaAlgorithm() {
        String validNonBetaAlgorithm = "HS256";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, validNonBetaAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", validNonBetaAlgorithm, result);
    }

    @Test
    public void test_getSignatureAlgorithm_betaEnabled_noOtherProps() {
        setBeta(true);
        Map<String, Object> props = new HashMap<String, Object>();

        String result = JwtConfigUtil.getSignatureAlgorithm(props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the default signature algorithm as expected.", DEFAULT_SIG_ALG, result);
    }

    @Test
    public void test_getSignatureAlgorithm_betaEnabled_betaAlgorithm() {
        setBeta(true);
        String validBetaAlgorithm = "ES384";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, validBetaAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", validBetaAlgorithm, result);
    }

    @Test
    public void test_getSignatureAlgorithm_betaEnabled_nonBetaAlgorithm() {
        setBeta(true);
        String validNonBetaAlgorithm = "HS256";
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SIG_ALG_ATTR_NAME, validNonBetaAlgorithm);

        String result = JwtConfigUtil.getSignatureAlgorithm(props, SIG_ALG_ATTR_NAME);
        assertEquals("Did not get the expected signature algorithm.", validNonBetaAlgorithm, result);
    }

    private void setBeta(boolean enable) {
        if (enable) {
            System.setProperty("com.ibm.ws.beta.edition", "true");
        } else {
            System.clearProperty("com.ibm.ws.beta.edition");
        }
    }

}