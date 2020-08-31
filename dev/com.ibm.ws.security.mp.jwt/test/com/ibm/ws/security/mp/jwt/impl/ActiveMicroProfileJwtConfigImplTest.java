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
package com.ibm.ws.security.mp.jwt.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.config.MpConstants;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class ActiveMicroProfileJwtConfigImplTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all:io.openliberty.security*=all");

    private final MicroProfileJwtConfig config = mockery.mock(MicroProfileJwtConfig.class);

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
    public void test_getAudiences_noAudiencesConfigured() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        mockery.checking(new Expectations() {
            {
                allowing(config).getAudiences();
                will(returnValue(null));
            }
        });
        List<String> result = activeConfig.getAudiences();
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getAudiences_audiencesInServerConfig_emptyList() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        final List<String> serverAudiences = new ArrayList<String>();
        mockery.checking(new Expectations() {
            {
                allowing(config).getAudiences();
                will(returnValue(serverAudiences));
            }
        });
        List<String> result = activeConfig.getAudiences();
        assertEquals("Returned audiences did not match configured value.", serverAudiences, result);
    }

    @Test
    public void test_getAudiences_audiencesInServerConfig_nonEmptyList() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        final List<String> serverAudiences = new ArrayList<String>();
        serverAudiences.add("1");
        serverAudiences.add("2 2");
        serverAudiences.add(" three ");
        mockery.checking(new Expectations() {
            {
                allowing(config).getAudiences();
                will(returnValue(serverAudiences));
            }
        });
        List<String> result = activeConfig.getAudiences();
        assertEquals("Returned audiences did not match configured value.", serverAudiences, result);
    }

    @Test
    public void test_getAudiences_audiencesInMpConfig() {
        String audience = "http://www.example.com";
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.VERIFY_AUDIENCES, audience);
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        mockery.checking(new Expectations() {
            {
                allowing(config).getAudiences();
                will(returnValue(null));
            }
        });

        List<String> result = activeConfig.getAudiences();
        assertEquals("Did not get the expected number of audiences. Got: " + result, 1, result.size());
        assertTrue("List of audiences did not contain [" + audience + "]. Audiences were: " + result, result.contains(audience));
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_noMpConfigProps() {
        Map<String, String> props = new HashMap<String, String>();
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        List<String> result = activeConfig.getAudiencesFromMpConfigProps();
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_missingAudiences() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.ISSUER, "blah");
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        List<String> result = activeConfig.getAudiencesFromMpConfigProps();
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_singleAudience() {
        String audience = "my audience";
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.VERIFY_AUDIENCES, audience);
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        List<String> result = activeConfig.getAudiencesFromMpConfigProps();
        assertNotNull("Should have gotten a non-null list of audiences.", result);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 1, result.size());
        assertTrue("List of audiences did not contain [" + audience + "]. Audiences were: " + result, result.contains(audience));
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_multipleAudiences() {
        String audience1 = "aud1";
        String audience2 = " aud 2";
        String audience3 = "aud 3 ";
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.VERIFY_AUDIENCES, audience1 + "," + audience2 + "," + audience3);
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        List<String> result = activeConfig.getAudiencesFromMpConfigProps();
        assertNotNull("Should have gotten a non-null list of audiences.", result);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 3, result.size());
        assertTrue("List of audiences did not contain [" + audience1 + "]. Audiences were: " + result, result.contains(audience1));
        assertTrue("List of audiences did not contain [" + audience2 + "]. Audiences were: " + result, result.contains(audience2));
        assertTrue("List of audiences did not contain [" + audience3 + "]. Audiences were: " + result, result.contains(audience3));
    }

    @Test
    public void test_getSignatureAlgorithm_sigAlgInServerConfig() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());
        final String serverConfigAlg = "RS512";
        mockery.checking(new Expectations() {
            {
                one(config).getSignatureAlgorithm();
                will(returnValue(serverConfigAlg));
            }
        });
        String result = activeConfig.getSignatureAlgorithm();
        assertEquals("Did not get expected signature algorithm value.", serverConfigAlg, result);
    }

    @Test
    public void test_getSignatureAlgorithm_sigAlgMissingFromServerConfigAndMpConfig() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());
        mockery.checking(new Expectations() {
            {
                one(config).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = activeConfig.getSignatureAlgorithm();
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getSignatureAlgorithm_mpConfigPropsUnsupportedAlg() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.PUBLIC_KEY_ALG, "NO256");
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        mockery.checking(new Expectations() {
            {
                one(config).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = activeConfig.getSignatureAlgorithm();
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getSignatureAlgorithm_mpConfigPropsSupportedAlg() {
        String mpConfigSigAlg = "HS512";
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.PUBLIC_KEY_ALG, mpConfigSigAlg);
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        mockery.checking(new Expectations() {
            {
                one(config).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = activeConfig.getSignatureAlgorithm();
        assertEquals("Did not get expected signature algorithm value.", mpConfigSigAlg, result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps_noConfigProps() {
        Map<String, String> props = new HashMap<String, String>();
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        String result = activeConfig.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps_unsupportedAlgorithm() {
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.PUBLIC_KEY_ALG, "unknown algorithm");
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        String result = activeConfig.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps() {
        String knownAlgorithm = "ES384";
        Map<String, String> props = new HashMap<String, String>();
        props.put(MpConstants.PUBLIC_KEY_ALG, knownAlgorithm);
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, props);

        String result = activeConfig.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected signature algorithm value.", knownAlgorithm, result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_nullAlg() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        String sigAlg = null;
        boolean result = activeConfig.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_emptyAlg() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        String sigAlg = "";
        boolean result = activeConfig.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_rs2560() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        String sigAlg = "RS2560";
        boolean result = activeConfig.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_rs256() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        String sigAlg = "RS256";
        boolean result = activeConfig.isSupportedSignatureAlgorithm(sigAlg);
        assertTrue("Input algorithm [" + sigAlg + "] should have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_rs1024() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        String sigAlg = "RS1024";
        boolean result = activeConfig.isSupportedSignatureAlgorithm(sigAlg);
        assertFalse("Input algorithm [" + sigAlg + "] should not have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_hs384() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        String sigAlg = "HS384";
        boolean result = activeConfig.isSupportedSignatureAlgorithm(sigAlg);
        assertTrue("Input algorithm [" + sigAlg + "] should have been considered supported.", result);
    }

    @Test
    public void test_isSupportedSignatureAlgorithm_es512() {
        ActiveMicroProfileJwtConfigImpl activeConfig = new ActiveMicroProfileJwtConfigImpl(config, new HashMap<String, String>());

        String sigAlg = "ES512";
        boolean result = activeConfig.isSupportedSignatureAlgorithm(sigAlg);
        assertTrue("Input algorithm [" + sigAlg + "] should have been considered supported.", result);
    }

}
