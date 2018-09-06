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
package com.ibm.ws.security.common.jwk.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.security.PublicKey;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.common.jwk.impl.JWKProvider;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

import test.common.SharedOutputManager;

public class JWKProviderTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    private final String UTF_8 = "UTF-8";
    private final String RS256 = "RS256";
    private final String HS256 = "HS256";

    private int defaultKeySize = 2048;
    private long defaultRotationTime = 12 * 60 * 60 * 1000;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    public interface MockInterface {
        public JWK mockGenerateJWK();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);
    final JWK jwk = mockery.mock(JWK.class);

    @Rule
    public final TestName testName = new TestName();

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
        mockery.assertIsSatisfied();
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>JWKProvider constructor</li>
     * </ul>
     */
    @Test
    public void testConstructor() {
        try {
            JWKProvider provider = new JWKProvider(-1, null, -1);
            assertEquals("Key size was not the expected length.", defaultKeySize, provider.size);
            assertNull("Algorithm was not null when it should have been. Algorithm was [" + provider.alg + "].", provider.alg);
            assertEquals("Rotation time was not the expected value.", defaultRotationTime, provider.rotationTimeInMilliseconds);
            // No JWKs should have been generated yet
            assertEquals("Number of generated JWKs was not expected value.", 0, provider.jwks.size());

            provider = new JWKProvider(defaultKeySize, RS256, defaultRotationTime);
            assertEquals("Key size was not the expected length.", defaultKeySize, provider.size);
            assertEquals("Did not get expected algorithm.", RS256, provider.alg);
            assertEquals("Rotation time was not the expected value.", defaultRotationTime, provider.rotationTimeInMilliseconds);
            assertEquals("Number of generated JWKs was not expected value.", 0, provider.jwks.size());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link JWKProvider#getJWK}</li>
     * </ul>
     */
    @Test
    public void testGetJWK() {
        try {
            // Bad constructor arguments - null algorithm
            JWKProvider provider = new JWKProvider(-1, null, -1);
            assertEquals("Number of generated JWKs was not expected value.", 0, provider.jwks.size());
            assertNull("Returned JWK was not null when it should not have been.", provider.getJWK());

            provider = new JWKProvider(-1, RS256, -1);
            assertEquals("Number of generated JWKs was not expected value.", 0, provider.jwks.size());
            JSONWebKey jwk = provider.getJWK();
            assertNotNull("Returned JWK was null when it should not have been.", jwk);

            // Golden path constructor arguments
            provider = new JWKProvider(defaultKeySize, RS256, defaultRotationTime);
            assertEquals("Number of generated JWKs was not expected value.", 0, provider.jwks.size());

            // Should generate new JWK
            jwk = provider.getJWK();
            assertNotNull("Returned JWK was null when it should not have been.", jwk);
            assertEquals("JWK's algorithm did not match expected algorithm.", RS256, jwk.getAlgorithm());
            assertNotNull("Public key was null when it should not have been.", jwk.getPublicKey());
            assertNotNull("Private key was null when it should not have been.", jwk.getPrivateKey());
            assertEquals("Number of generated JWKs was not expected value.", 2, provider.jwks.size());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link JWKProvider#rotateKeys()}</li>
     * </ul>
     */
    @Test
    public void testRotateKeys() {
        try {
            JWKProvider provider = new JWKProvider(defaultKeySize, RS256, defaultRotationTime);
            assertEquals("Number of generated JWKs was not expected value.", 0, provider.jwks.size());

            // Should generate new JWK
            JSONWebKey jwk = provider.getJWK();
            assertEquals("Number of generated JWKs was not expected value.", 2, provider.jwks.size());
            PublicKey publicKey = jwk.getPublicKey();

            provider.rotateKeys();

            assertEquals("Number of generated JWKs was not expected value.", 2, provider.jwks.size());
            JSONWebKey newJwk = provider.getJWK();
            assertEquals("Number of generated JWKs was not expected value.", 2, provider.jwks.size());
            PublicKey newPublicKey = newJwk.getPublicKey();

            // Testing equality of public keys should be sufficient to ensure keys are different
            assertFalse("Public keys were equal when they should not have been. Both keys were: " + publicKey, publicKey.toString().equals(newPublicKey.toString()));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }
}
