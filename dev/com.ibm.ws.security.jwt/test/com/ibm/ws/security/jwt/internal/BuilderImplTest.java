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

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.jwt.KeyException;

import test.common.SharedOutputManager;

public class BuilderImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all");

    public final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private BuilderImpl builder = null;

    private final PublicKey publicKey = mockery.mock(PublicKey.class);
    private final PrivateKey privateKey = mockery.mock(PrivateKey.class);

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
        builder = new BuilderImpl();
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        System.out.println("Exiting test: " + testName.getMethodName());
    }

    @Test
    public void test_encryptWith_nullKeyManagementAlg() {
        try {
            String keyManagementAlg = null;
            Key keyManagementKey = publicKey;
            String contentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            try {
                builder = (BuilderImpl) builder.encryptWith(keyManagementAlg, keyManagementKey, contentEncryptionAlg);
                fail("Should have thrown a KeyException, but did not.");
            } catch (KeyException e) {
                // TODO
            }
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
