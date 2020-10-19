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
import static org.junit.Assert.fail;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.security.jwt.KeyException;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class BuilderImplTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all");

    static final String MSG_UNSUPPORTED_KEY_MANAGEMENT_ALGORITHM = "CWWKS6056E";
    static final String MSG_UNSUPPORTED_CONTENT_ENCRYPTION_ALGORITHM = "CWWKS6057E";
    static final String MSG_KEY_MANAGEMENT_KEY_MISSING = "CWWKS6058W";

    private BuilderImpl builder = null;

    private final PublicKey publicKey = mockery.mock(PublicKey.class);
    private final PrivateKey privateKey = mockery.mock(PrivateKey.class);

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
        builder = new BuilderImpl();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_encryptWith_nullKeyManagementAlg() {
        try {
            String keyManagementAlg = null;
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
            String setAlg = builder.getKeyManagementAlg();
            assertEquals("Key management algorithm was not set to the expected default value.", BuilderImpl.DEFAULT_KEY_MANAGEMENT_ALGORITHM, setAlg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_encryptWith_unknownKeyManagementAlg() {
        try {
            String keyManagementAlg = "does not exist";
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
            String setAlg = builder.getKeyManagementAlg();
            assertEquals("Key management algorithm did not equal expected value.", keyManagementAlg, setAlg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_encryptWith_nonDefaltKeyManagementAlg() {
        try {
            String keyManagementAlg = KeyManagementAlgorithmIdentifiers.ECDH_ES_A128KW;
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
            String setAlg = builder.getKeyManagementAlg();
            assertEquals("Key management algorithm did not equal expected value.", keyManagementAlg, setAlg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_encryptWith_nullKeyManagementKey() {
        try {
            String keyManagementAlg = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            Key keyManagementKey = null;
            String contentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            try {
                builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
                fail("Should have thrown a KeyException, but did not.");
            } catch (KeyException e) {
                verifyException(e, MSG_KEY_MANAGEMENT_KEY_MISSING);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_encryptWith_nullContentEncryptionAlg() {
        try {
            String keyManagementAlg = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = null;
            builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
            String setAlg = builder.getContentEncryptionAlg();
            assertEquals("Content encryption algorithm was not set to the expected default value.", BuilderImpl.DEFAULT_CONTENT_ENCRYPTION_ALGORITHM, setAlg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_encryptWith_unknownContentEncryptionAlg() {
        try {
            String keyManagementAlg = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = "some random string";
            builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
            String setAlg = builder.getContentEncryptionAlg();
            assertEquals("Content encryption algorithm did not equal expected value.", contentEncryptionAlg, setAlg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_encryptWith_nonDefaultContentEncryptionAlg() {
        try {
            String keyManagementAlg = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_192_CBC_HMAC_SHA_384;
            builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
            String setAlg = builder.getContentEncryptionAlg();
            assertEquals("Content encryption algorithm did not equal expected value.", contentEncryptionAlg, setAlg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_encryptWith_goldenPath() {
        try {
            String keyManagementAlg = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);

            assertEquals("Did not get the correct key management algorithm.", keyManagementAlg, builder.getKeyManagementAlg());
            assertEquals("Did not get the correct key management key.", keyManagementKey, builder.getKeyManagementKey());
            assertEquals("Did not get the correct content encryption algorithm.", contentEncryptionAlg, builder.getContentEncryptionAlg());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
