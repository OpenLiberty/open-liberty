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
package com.ibm.ws.security.common.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECField;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class KeyAlgorithmCheckerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.common.*=all:com.ibm.ws.security.common.*=all");

    private final PublicKey publicKey = mockery.mock(PublicKey.class);
    private final PrivateKey privateKey = mockery.mock(PrivateKey.class);
    private final RSAPublicKey rsaPublicKey = mockery.mock(RSAPublicKey.class);
    private final RSAPrivateKey rsaPrivateKey = mockery.mock(RSAPrivateKey.class);
    private final ECKey ecKey = mockery.mock(ECKey.class);
    private final ECPublicKey ecPublicKey = mockery.mock(ECPublicKey.class);
    private final ECPrivateKey ecPrivateKey = mockery.mock(ECPrivateKey.class);
    private final ECParameterSpec ecParameterSpec = mockery.mock(ECParameterSpec.class);
    private final EllipticCurve ellipticCurve = mockery.mock(EllipticCurve.class);
    private final ECField ecField = mockery.mock(ECField.class);

    KeyAlgorithmChecker checker;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        checker = new KeyAlgorithmChecker();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_isHSAlgorithm_nullAlgorithm() {
        String algorithm = null;
        boolean result = checker.isHSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an HS algorithm.", result);
    }

    @Test
    public void test_isHSAlgorithm_emptyString() {
        String algorithm = "";
        boolean result = checker.isHSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an HS algorithm.", result);
    }

    @Test
    public void test_isHSAlgorithm_superStringPrefix() {
        String algorithm = "DHS256";
        boolean result = checker.isHSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an HS algorithm.", result);
    }

    @Test
    public void test_isHSAlgorithm_superStringSuffix() {
        String algorithm = "HS256A";
        boolean result = checker.isHSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an HS algorithm.", result);
    }

    @Test
    public void test_isHSAlgorithm_HS256() {
        String algorithm = "HS256";
        boolean result = checker.isHSAlgorithm(algorithm);
        assertTrue("Algorithm [" + algorithm + "] was not considered an HS algorithm.", result);
    }

    @Test
    public void test_isHSAlgorithm_HS10240() {
        String algorithm = "HS10240";
        boolean result = checker.isHSAlgorithm(algorithm);
        assertTrue("Algorithm [" + algorithm + "] was not considered an HS algorithm.", result);
    }

    @Test
    public void test_isPublicKeyValidType_nullKey() {
        Key key = null;
        String algorithm = "RS256";
        boolean result = checker.isPublicKeyValidType(key, algorithm);
        assertTrue("Call with null Key should have been considered valid; up to the caller to decide what to do with null inputs.", result);
    }

    @Test
    public void test_isPublicKeyValidType_nullAlgorithm() {
        Key key = rsaPublicKey;
        String algorithm = null;
        boolean result = checker.isPublicKeyValidType(key, algorithm);
        assertTrue("Call with null algorithm should have been considered valid; up to the caller to decide what to do with null inputs.", result);
    }

    @Test
    public void test_isPublicKeyValidType_rsaAlgorithm_nonRsaKey() {
        Key key = ecPublicKey;
        String algorithm = "RS256";
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
            }
        });
        boolean result = checker.isPublicKeyValidType(key, algorithm);
        assertFalse("Key " + key + " should not have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPublicKeyValidType_rsaAlgorithm_rsaKey() {
        Key key = rsaPublicKey;
        String algorithm = "RS256";
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isPublicKeyValidType(key, algorithm);
        assertTrue("Key " + key + " should have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPublicKeyValidType_ecAlgorithm_nonEcKey() {
        Key key = rsaPublicKey;
        String algorithm = "ES256";
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isPublicKeyValidType(key, algorithm);
        assertFalse("Key " + key + " should not have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPublicKeyValidType_ecAlgorithm_ecKey() {
        Key key = ecPublicKey;
        String algorithm = "ES256";
        final int fieldSize = 256;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
                one(ecPublicKey).getParams();
                will(returnValue(ecParameterSpec));
                one(ecParameterSpec).getCurve();
                will(returnValue(ellipticCurve));
                one(ellipticCurve).getField();
                will(returnValue(ecField));
                one(ecField).getFieldSize();
                will(returnValue(fieldSize));
            }
        });
        boolean result = checker.isPublicKeyValidType(key, algorithm);
        assertTrue("Key " + key + " should have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPublicKeyValidType_unknownAlgorithm() {
        Key key = publicKey;
        String algorithm = "HS256";
        boolean result = checker.isPublicKeyValidType(key, algorithm);
        assertFalse("Key " + key + " should not have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isValidRSAPublicKey_nonRsaAlgorithm() {
        Key key = rsaPublicKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("DSA"));
            }
        });
        boolean result = checker.isValidRSAPublicKey(key);
        assertFalse("Key with DSA algorithm should not have been considered a valid RSA public key.", result);
    }

    @Test
    public void test_isValidRSAPublicKey_genericPublicKey() {
        Key key = publicKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isValidRSAPublicKey(key);
        assertFalse("Generic PublicKey type should not have been considered a valid RSA public key.", result);
    }

    @Test
    public void test_isValidRSAPublicKey_rsaPublicKey() {
        Key key = rsaPublicKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isValidRSAPublicKey(key);
        assertTrue("Key " + key + " should have been considered a valid RSA public key.", result);
    }

    @Test
    public void test_isValidECPublicKey_ES256_nonECKey() {
        String supportedSigAlg = "ES256";
        Key key = rsaPublicKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isValidECPublicKey(supportedSigAlg, key);
        assertFalse("Key with RSA algorithm should not have been considered a valid EC public key.", result);
    }

    @Test
    public void test_isValidECPublicKey_ES256_publicKey() {
        String supportedSigAlg = "ES256";
        Key key = publicKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
            }
        });
        boolean result = checker.isValidECPublicKey(supportedSigAlg, key);
        assertFalse("Key " + key + " should not have been considered a valid EC public key.", result);
    }

    @Test
    public void test_isValidECPublicKey_ES512() {
        String supportedSigAlg = "ES512";
        Key key = ecPublicKey;
        // Field size for 512 algorithm should actually be 521
        final int fieldSize = 521;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
                one(ecPublicKey).getParams();
                will(returnValue(ecParameterSpec));
                one(ecParameterSpec).getCurve();
                will(returnValue(ellipticCurve));
                one(ellipticCurve).getField();
                will(returnValue(ecField));
                one(ecField).getFieldSize();
                will(returnValue(fieldSize));
            }
        });
        boolean result = checker.isValidECPublicKey(supportedSigAlg, key);
        assertTrue("Key with field size " + fieldSize + " should have been considered a valid " + supportedSigAlg + " public key.", result);
    }

    @Test
    public void test_isPrivateKeyValidType_nullKey() {
        Key key = null;
        String algorithm = "RS256";
        boolean result = checker.isPrivateKeyValidType(key, algorithm);
        assertTrue("Call with null Key should have been considered valid; up to the caller to decide what to do with null inputs.", result);
    }

    @Test
    public void test_isPrivateKeyValidType_nullAlgorithm() {
        Key key = rsaPrivateKey;
        String algorithm = null;
        boolean result = checker.isPrivateKeyValidType(key, algorithm);
        assertTrue("Call with null algorithm should have been considered valid; up to the caller to decide what to do with null inputs.", result);
    }

    @Test
    public void test_isPrivateKeyValidType_rsaAlgorithm_nonRsaKey() {
        Key key = ecPrivateKey;
        String algorithm = "RS256";
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
            }
        });
        boolean result = checker.isPrivateKeyValidType(key, algorithm);
        assertFalse("Key " + key + " should not have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPrivateKeyValidType_rsaAlgorithm_rsaKey() {
        Key key = rsaPrivateKey;
        String algorithm = "RS256";
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isPrivateKeyValidType(key, algorithm);
        assertTrue("Key " + key + " should have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPrivateKeyValidType_ecAlgorithm_nonEcKey() {
        Key key = rsaPrivateKey;
        String algorithm = "ES256";
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isPrivateKeyValidType(key, algorithm);
        assertFalse("Key " + key + " should not have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPrivateKeyValidType_ecAlgorithm_ecKey() {
        Key key = ecPrivateKey;
        String algorithm = "ES256";
        final int fieldSize = 256;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
                one(ecPrivateKey).getParams();
                will(returnValue(ecParameterSpec));
                one(ecParameterSpec).getCurve();
                will(returnValue(ellipticCurve));
                one(ellipticCurve).getField();
                will(returnValue(ecField));
                one(ecField).getFieldSize();
                will(returnValue(fieldSize));
            }
        });
        boolean result = checker.isPrivateKeyValidType(key, algorithm);
        assertTrue("Key " + key + " should have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isPrivateKeyValidType_unknownAlgorithm() {
        Key key = privateKey;
        String algorithm = "HS256";
        boolean result = checker.isPrivateKeyValidType(key, algorithm);
        assertFalse("Key " + key + " should not have been considered valid for algorithm " + algorithm + ".", result);
    }

    @Test
    public void test_isRSAlgorithm_nullAlgorithm() {
        String algorithm = null;
        boolean result = checker.isRSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an RS algorithm.", result);
    }

    @Test
    public void test_isRSAlgorithm_emptyString() {
        String algorithm = "";
        boolean result = checker.isRSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an RS algorithm.", result);
    }

    @Test
    public void test_isRSAlgorithm_superStringPrefix() {
        String algorithm = "DRS256";
        boolean result = checker.isRSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an RS algorithm.", result);
    }

    @Test
    public void test_isRSAlgorithm_superStringSuffix() {
        String algorithm = "RS256A";
        boolean result = checker.isRSAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an RS algorithm.", result);
    }

    @Test
    public void test_isRSAlgorithm_RS256() {
        String algorithm = "RS256";
        boolean result = checker.isRSAlgorithm(algorithm);
        assertTrue("Algorithm [" + algorithm + "] was not considered an RS algorithm.", result);
    }

    @Test
    public void test_isRSAlgorithm_RS10240() {
        String algorithm = "RS10240";
        boolean result = checker.isRSAlgorithm(algorithm);
        assertTrue("Algorithm [" + algorithm + "] was not considered an RS algorithm.", result);
    }

    @Test
    public void test_isValidRSAPrivateKey_nonRsaAlgorithm() {
        Key key = rsaPrivateKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("DSA"));
            }
        });
        boolean result = checker.isValidRSAPrivateKey(key);
        assertFalse("Key with DSA algorithm should not have been considered a valid RSA private key.", result);
    }

    @Test
    public void test_isValidRSAPrivateKey_genericPrivateKey() {
        Key key = privateKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isValidRSAPrivateKey(key);
        assertFalse("Generic PrivateKey type should not have been considered a valid RSA private key.", result);
    }

    @Test
    public void test_isValidRSAPrivateKey_rsaPrivateKey() {
        Key key = rsaPrivateKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isValidRSAPrivateKey(key);
        assertTrue("Key " + key + " should have been considered a valid RSA private key.", result);
    }

    @Test
    public void test_isESAlgorithm_nullAlgorithm() {
        String algorithm = null;
        boolean result = checker.isESAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an ES algorithm.", result);
    }

    @Test
    public void test_isESAlgorithm_emptyString() {
        String algorithm = "";
        boolean result = checker.isESAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an ES algorithm.", result);
    }

    @Test
    public void test_isESAlgorithm_superStringPrefix() {
        String algorithm = "DES256";
        boolean result = checker.isESAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an ES algorithm.", result);
    }

    @Test
    public void test_isESAlgorithm_superStringSuffix() {
        String algorithm = "ES256A";
        boolean result = checker.isESAlgorithm(algorithm);
        assertFalse("Algorithm [" + algorithm + "] was considered an ES algorithm.", result);
    }

    @Test
    public void test_isESAlgorithm_ES256() {
        String algorithm = "ES256";
        boolean result = checker.isESAlgorithm(algorithm);
        assertTrue("Algorithm [" + algorithm + "] was not considered an ES algorithm.", result);
    }

    @Test
    public void test_isESAlgorithm_ES10240() {
        String algorithm = "ES10240";
        boolean result = checker.isESAlgorithm(algorithm);
        assertTrue("Algorithm [" + algorithm + "] was not considered an ES algorithm.", result);
    }

    @Test
    public void test_isValidECPrivateKey_ES256_nonECKey() {
        String supportedSigAlg = "ES256";
        Key key = rsaPrivateKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("RSA"));
            }
        });
        boolean result = checker.isValidECPrivateKey(supportedSigAlg, key);
        assertFalse("Key with RSA algorithm should not have been considered a valid EC private key.", result);
    }

    @Test
    public void test_isValidECPrivateKey_ES256_publicKey() {
        String supportedSigAlg = "ES256";
        Key key = privateKey;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
            }
        });
        boolean result = checker.isValidECPrivateKey(supportedSigAlg, key);
        assertFalse("Key " + key + " should not have been considered a valid EC private key.", result);
    }

    @Test
    public void test_isValidECPrivateKey_ES384() {
        String supportedSigAlg = "ES384";
        Key key = ecPrivateKey;
        final int fieldSize = 384;
        mockery.checking(new Expectations() {
            {
                one(key).getAlgorithm();
                will(returnValue("EC"));
                one(ecPrivateKey).getParams();
                will(returnValue(ecParameterSpec));
                one(ecParameterSpec).getCurve();
                will(returnValue(ellipticCurve));
                one(ellipticCurve).getField();
                will(returnValue(ecField));
                one(ecField).getFieldSize();
                will(returnValue(fieldSize));
            }
        });
        boolean result = checker.isValidECPrivateKey(supportedSigAlg, key);
        assertTrue("Key with field size " + fieldSize + " should have been considered a valid " + supportedSigAlg + " private key.", result);
    }

    @Test
    public void test_isValidECKeyParameters_ES256_wrongFieldSize() {
        String supportedSigAlg = "ES256";
        ECKey key = ecKey;
        final int fieldSize = 128;
        mockery.checking(new Expectations() {
            {
                one(ecKey).getParams();
                will(returnValue(ecParameterSpec));
                one(ecParameterSpec).getCurve();
                will(returnValue(ellipticCurve));
                one(ellipticCurve).getField();
                will(returnValue(ecField));
                one(ecField).getFieldSize();
                will(returnValue(fieldSize));
            }
        });
        boolean result = checker.isValidECKeyParameters(supportedSigAlg, key);
        assertFalse("Key with field size " + fieldSize + " should not have been considered a valid " + supportedSigAlg + " private key.", result);
    }

    @Test
    public void test_isValidECKeyParameters_ES256_correctFieldSize() {
        String supportedSigAlg = "ES256";
        ECKey key = ecKey;
        final int fieldSize = 256;
        mockery.checking(new Expectations() {
            {
                one(ecKey).getParams();
                will(returnValue(ecParameterSpec));
                one(ecParameterSpec).getCurve();
                will(returnValue(ellipticCurve));
                one(ellipticCurve).getField();
                will(returnValue(ecField));
                one(ecField).getFieldSize();
                will(returnValue(fieldSize));
            }
        });
        boolean result = checker.isValidECKeyParameters(supportedSigAlg, key);
        assertTrue("Key with field size " + fieldSize + " should have been considered a valid " + supportedSigAlg + " private key.", result);
    }

    @Test
    public void test_isValidECKeyParameters_ES512_correctFieldSize() {
        String supportedSigAlg = "ES512";
        ECKey key = ecKey;
        // Field size for 512 algorithm should actually be 521
        final int fieldSize = 521;
        mockery.checking(new Expectations() {
            {
                one(ecKey).getParams();
                will(returnValue(ecParameterSpec));
                one(ecParameterSpec).getCurve();
                will(returnValue(ellipticCurve));
                one(ellipticCurve).getField();
                will(returnValue(ecField));
                one(ecField).getFieldSize();
                will(returnValue(fieldSize));
            }
        });
        boolean result = checker.isValidECKeyParameters(supportedSigAlg, key);
        assertTrue("Key with field size " + fieldSize + " should have been considered a valid " + supportedSigAlg + " private key.", result);
    }

    @Test
    public void test_getHashSizeFromAlgorithm_emptyAlgString() {
        String supportedSigAlg = "";
        int result = checker.getHashSizeFromAlgorithm(supportedSigAlg);
        assertEquals("Did not get the expected hash size from the algorithm string [" + supportedSigAlg + "].", KeyAlgorithmChecker.UNKNOWN_HASH_SIZE, result);
    }

    @Test
    public void test_getHashSizeFromAlgorithm_randomAlgString() {
        String supportedSigAlg = "Be afraid. Be very afraid.";
        int result = checker.getHashSizeFromAlgorithm(supportedSigAlg);
        assertEquals("Did not get the expected hash size from the algorithm string [" + supportedSigAlg + "].", KeyAlgorithmChecker.UNKNOWN_HASH_SIZE, result);
    }

    @Test
    public void test_getHashSizeFromAlgorithm_algStringWithShortHashSize() {
        String supportedSigAlg = "RS32";
        int result = checker.getHashSizeFromAlgorithm(supportedSigAlg);
        assertEquals("Did not get the expected hash size from the algorithm string [" + supportedSigAlg + "].", KeyAlgorithmChecker.UNKNOWN_HASH_SIZE, result);
    }

    @Test
    public void test_getHashSizeFromAlgorithm_algTypeNotValid() {
        String supportedSigAlg = "BLAH256";
        int result = checker.getHashSizeFromAlgorithm(supportedSigAlg);
        assertEquals("Did not get the expected hash size from the algorithm string [" + supportedSigAlg + "].", KeyAlgorithmChecker.UNKNOWN_HASH_SIZE, result);
    }

    @Test
    public void test_getHashSizeFromAlgorithm_smallValidHashSize() {
        int supportedAlgSize = 384;
        String supportedSigAlg = "ES" + supportedAlgSize;
        int result = checker.getHashSizeFromAlgorithm(supportedSigAlg);
        assertEquals("Did not get the expected hash size from the algorithm string [" + supportedSigAlg + "].", supportedAlgSize, result);
    }

    @Test
    public void test_getHashSizeFromAlgorithm_largeValidHashSize() {
        int supportedAlgSize = 4096;
        String supportedSigAlg = "RS" + supportedAlgSize;
        int result = checker.getHashSizeFromAlgorithm(supportedSigAlg);
        assertEquals("Did not get the expected hash size from the algorithm string [" + supportedSigAlg + "].", supportedAlgSize, result);
    }

}
