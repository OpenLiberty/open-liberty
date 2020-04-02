/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.auth.TokenCreationFailedException;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.ltpa.TokenFactory;

import test.UTLocationHelper;
import test.common.SharedOutputManager;

/**
 *
 */
public class LTPAToken2FactoryTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String KEYIMPORTFILE_CORRECT = "${server.config.dir}/resources/security/security.token.ltpa.keys.correct.txt";
    private static final byte[] KEYPASSWORD_CORRECT = "WebAS".getBytes();
    private static final String REALM = "TestRealm";
    private static final String decodedSharedKey = "Three can keep a secret when two are no longer there";
    private static final String encodedSharedKey = Base64Coder.base64Encode(decodedSharedKey);
    private LTPAPrivateKey ltpaPrivateKey;
    private LTPAPublicKey ltpaPublicKey;
    private Map<String, Object> tokenFactoryMap;
    private TokenFactory tokenFactory;

    @Before
    public void setUp() throws Exception {
        setupLTPAKeys();
        tokenFactoryMap = createTestTokenFactoryMap();
        tokenFactory = createInitializedTokenFactory(tokenFactoryMap);
    }

    private void setupLTPAKeys() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          KEYIMPORTFILE_CORRECT,
                                          KEYPASSWORD_CORRECT,
                                          REALM);
        ltpaPrivateKey = new LTPAPrivateKey(keyInfoManager.getPrivateKey(KEYIMPORTFILE_CORRECT));
        ltpaPublicKey = new LTPAPublicKey(keyInfoManager.getPublicKey(KEYIMPORTFILE_CORRECT));
    }

    private Map<String, Object> createTestTokenFactoryMap() {
        long expectedExpirationLimit = 120;
        Map<String, Object> tokenFactoryMap = new HashMap<String, Object>();
        tokenFactoryMap.put("expiration", expectedExpirationLimit);
        tokenFactoryMap.put("ltpa_shared_key", encodedSharedKey.getBytes());
        tokenFactoryMap.put("ltpa_public_key", ltpaPublicKey);
        tokenFactoryMap.put("ltpa_private_key", ltpaPrivateKey);

        return tokenFactoryMap;
    }

    private LTPAToken2Factory createInitializedTokenFactory(Map<String, Object> tokenFactoryMap) {
        LTPAToken2Factory tokenFactory = new LTPAToken2Factory();
        tokenFactory.initialize(tokenFactoryMap);
        return tokenFactory;
    }

    @Test
    public void testConstructor() {
        TokenFactory tokenFactory = new LTPAToken2Factory();
        assertNotNull("There must be a token factory.", tokenFactory);
    }

    @Test
    public void testInitializeSetsExpirationLimit() throws Exception {
        long expectedExpirationLimit = (Long) tokenFactoryMap.get("expiration");
        Field expirationInMinutesField = LTPAToken2Factory.class.getDeclaredField("expirationInMinutes");
        expirationInMinutesField.setAccessible(true);
        long actualExpirationInMinutes = expirationInMinutesField.getLong(tokenFactory);

        assertEquals("The expiration in minutes must be equals to the expected expiration limit.", expectedExpirationLimit, actualExpirationInMinutes);
    }

    @Test
    public void testInitializeSetsSharedKey() throws Exception {
        byte[] expectedSharedKey = (byte[]) tokenFactoryMap.get("ltpa_shared_key");
        Field sharedKeyField = LTPAToken2Factory.class.getDeclaredField("sharedKey");
        sharedKeyField.setAccessible(true);
        byte[] actualSharedKey = (byte[]) sharedKeyField.get(tokenFactory);

        assertEquals("The shared key must be equals to the expected shared key.", expectedSharedKey, actualSharedKey);
    }

    @Test
    public void testInitializeSetsPublicKey() throws Exception {
        LTPAPublicKey expectedPublicKey = (LTPAPublicKey) tokenFactoryMap.get("ltpa_public_key");
        Field publicKeyField = LTPAToken2Factory.class.getDeclaredField("publicKey");
        publicKeyField.setAccessible(true);
        LTPAPublicKey actualPublicKey = (LTPAPublicKey) publicKeyField.get(tokenFactory);

        assertEquals("The public key must be equals to the expected public key.", expectedPublicKey, actualPublicKey);
    }

    @Test
    public void testInitializeSetsPrivateKey() throws Exception {
        LTPAPrivateKey expectedPrivateKey = (LTPAPrivateKey) tokenFactoryMap.get("ltpa_private_key");
        Field publicKeyField = LTPAToken2Factory.class.getDeclaredField("privateKey");
        publicKeyField.setAccessible(true);
        LTPAPrivateKey actualPrivateKey = (LTPAPrivateKey) publicKeyField.get(tokenFactory);

        assertEquals("The private key must be equals to the expected private key.", expectedPrivateKey, actualPrivateKey);
    }

    @Test
    public void testCreateToken() throws Exception {
        Map<String, Object> tokenData = createBasicLTPA2TokenData();
        Token token = tokenFactory.createToken(tokenData);
        assertNotNull("There must be a token.", token);
    }

    @Test
    public void testCreateTokenThrowsExceptionWhenNoUniqueIdIsProvided() throws Throwable {
        try {
            Map<String, Object> tokenData = new HashMap<String, Object>();
            tokenFactory.createToken(tokenData);
        } catch (TokenCreationFailedException e) {
            String expectedMessage = "CWWKS4101E: There is no unique ID with which to create the token.";
            String actualMessage = e.getMessage();
            assertEquals("Exception did not contain expected message",
                         expectedMessage, actualMessage);
            assertTrue("Expected message was not logged",
                       outputMgr.checkForStandardErr(expectedMessage));
        }
    }

    @Test
    public void testValidateTokenBytes() throws Exception {
        Map<String, Object> tokenData = createBasicLTPA2TokenData();
        Token token = tokenFactory.createToken(tokenData);
        byte[] tokenBytes = token.getBytes();
        Token validatedToken = tokenFactory.validateTokenBytes(tokenBytes);
        assertNotNull("There must be a validated token.", validatedToken);
    }

    private Map<String, Object> createBasicLTPA2TokenData() {
        Map<String, Object> tokenData = new HashMap<String, Object>();
        tokenData.put("unique_id", "user:BasicRealm/user1");
        return tokenData;
    }
}
