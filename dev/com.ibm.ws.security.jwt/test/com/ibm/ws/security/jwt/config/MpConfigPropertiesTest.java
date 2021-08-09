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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class MpConfigPropertiesTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all");

    private MpConfigProperties props = null;

    private final JwtConsumerConfig jwtConsumerConfig = mockery.mock(JwtConsumerConfig.class);

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
        props = new MpConfigProperties();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_getConfiguredAudiences_noAudiencesConfigured() {
        mockery.checking(new Expectations() {
            {
                allowing(jwtConsumerConfig).getAudiences();
                will(returnValue(null));
            }
        });
        List<String> result = props.getConfiguredAudiences(jwtConsumerConfig);
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getConfiguredAudiences_audiencesInServerConfig_emptyList() {
        final List<String> serverAudiences = new ArrayList<String>();
        mockery.checking(new Expectations() {
            {
                allowing(jwtConsumerConfig).getAudiences();
                will(returnValue(serverAudiences));
            }
        });
        List<String> result = props.getConfiguredAudiences(jwtConsumerConfig);
        assertEquals("Returned audiences did not match configured value.", serverAudiences, result);
    }

    @Test
    public void test_getConfiguredAudiences_audiencesInServerConfig_nonEmptyList() {
        final List<String> serverAudiences = new ArrayList<String>();
        serverAudiences.add("1");
        serverAudiences.add("2 2");
        serverAudiences.add(" three ");
        mockery.checking(new Expectations() {
            {
                allowing(jwtConsumerConfig).getAudiences();
                will(returnValue(serverAudiences));
            }
        });
        List<String> result = props.getConfiguredAudiences(jwtConsumerConfig);
        assertEquals("Returned audiences did not match configured value.", serverAudiences, result);
    }

    @Test
    public void test_getConfiguredAudiences_audiencesInMpConfig() {
        String audience = "http://www.example.com";
        props.put(MpConfigProperties.VERIFY_AUDIENCES, audience);
        mockery.checking(new Expectations() {
            {
                allowing(jwtConsumerConfig).getAudiences();
                will(returnValue(null));
            }
        });

        List<String> result = props.getConfiguredAudiences(jwtConsumerConfig);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 1, result.size());
        assertTrue("List of audiences did not contain [" + audience + "]. Audiences were: " + result, result.contains(audience));
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_noMpConfigProps() {
        List<String> result = props.getAudiencesFromMpConfigProps();
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_missingAudiences() {
        props.put(MpConfigProperties.ISSUER, "blah");

        List<String> result = props.getAudiencesFromMpConfigProps();
        assertNull("Should not have gotten any audiences, but got " + result, result);
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_singleAudience() {
        String audience = "my audience";
        props.put(MpConfigProperties.VERIFY_AUDIENCES, audience);

        List<String> result = props.getAudiencesFromMpConfigProps();
        assertNotNull("Should have gotten a non-null list of audiences.", result);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 1, result.size());
        assertTrue("List of audiences did not contain [" + audience + "]. Audiences were: " + result, result.contains(audience));
    }

    @Test
    public void test_getAudiencesFromMpConfigProps_multipleAudiences() {
        String audience1 = "aud1";
        String audience2 = " aud 2";
        String audience3 = "aud 3 ";
        props.put(MpConfigProperties.VERIFY_AUDIENCES, audience1 + "," + audience2 + "," + audience3);

        List<String> result = props.getAudiencesFromMpConfigProps();
        assertNotNull("Should have gotten a non-null list of audiences.", result);
        assertEquals("Did not get the expected number of audiences. Got: " + result, 3, result.size());
        assertTrue("List of audiences did not contain [" + audience1 + "]. Audiences were: " + result, result.contains(audience1));
        assertTrue("List of audiences did not contain [" + audience2 + "]. Audiences were: " + result, result.contains(audience2));
        assertTrue("List of audiences did not contain [" + audience3 + "]. Audiences were: " + result, result.contains(audience3));
    }

    @Test
    public void test_getConfiguredSignatureAlgorithm_sigAlgInServerConfig() {
        final String serverConfigAlg = "RS512";
        mockery.checking(new Expectations() {
            {
                one(jwtConsumerConfig).getSignatureAlgorithm();
                will(returnValue(serverConfigAlg));
            }
        });
        String result = props.getConfiguredSignatureAlgorithm(jwtConsumerConfig);
        assertEquals("Did not get expected signature algorithm value.", serverConfigAlg, result);
    }

    @Test
    public void test_getConfiguredSignatureAlgorithm_sigAlgMissingFromServerConfigAndMpConfig() {
        mockery.checking(new Expectations() {
            {
                one(jwtConsumerConfig).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = props.getConfiguredSignatureAlgorithm(jwtConsumerConfig);
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getConfiguredSignatureAlgorithm_mpConfigPropsUnsupportedAlg() {
        props.put(MpConfigProperties.PUBLIC_KEY_ALG, "NO256");

        mockery.checking(new Expectations() {
            {
                one(jwtConsumerConfig).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = props.getConfiguredSignatureAlgorithm(jwtConsumerConfig);
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getConfiguredSignatureAlgorithm_mpConfigPropsSupportedAlg() {
        String mpConfigSigAlg = "HS512";
        props.put(MpConfigProperties.PUBLIC_KEY_ALG, mpConfigSigAlg);

        mockery.checking(new Expectations() {
            {
                one(jwtConsumerConfig).getSignatureAlgorithm();
                will(returnValue(null));
            }
        });
        String result = props.getConfiguredSignatureAlgorithm(jwtConsumerConfig);
        assertEquals("Did not get expected signature algorithm value.", mpConfigSigAlg, result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps_noConfigProps() {
        String result = props.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps_unsupportedAlgorithm() {
        props.put(MpConfigProperties.PUBLIC_KEY_ALG, "unknown algorithm");

        String result = props.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected default signature algorithm value.", "RS256", result);
    }

    @Test
    public void test_getSignatureAlgorithmFromMpConfigProps() {
        String knownAlgorithm = "ES384";
        props.put(MpConfigProperties.PUBLIC_KEY_ALG, knownAlgorithm);

        String result = props.getSignatureAlgorithmFromMpConfigProps();
        assertEquals("Did not get expected signature algorithm value.", knownAlgorithm, result);
    }

}
