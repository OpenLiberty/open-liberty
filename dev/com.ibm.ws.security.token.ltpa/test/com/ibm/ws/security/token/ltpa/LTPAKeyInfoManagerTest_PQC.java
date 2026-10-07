/*******************************************************************************
 * Copyright (c) 2007, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.UTLocationHelper;
import test.common.SharedOutputManager;

/**
 * Unit tests for PQC (Post-Quantum Cryptography) functionality in LTPAKeyInfoManager.
 * Tests ML-DSA key loading, caching, and retrieval.
 */
public class LTPAKeyInfoManagerTest_PQC {

    private static SharedOutputManager outputMgr;

    private static final String KEYIMPORTFILE_NO_EXIST = "${server.config.dir}/resources/security/security.token.ltpa.keys.noexist.txt";
    private static final String LTPA_KEY_IMPORT_FILE = "${server.config.dir}/resources/security/security.token.ltpa.keys.correct.txt";

    private static final byte[] KEYPASSWORD_CORRECT = "WebAS".getBytes();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownClass() {
        outputMgr.restoreStreams();
    }

    /**
     * Test loading LTPA key file with PQC ML-DSA keys.
     * Verifies that ML-DSA keys are properly loaded and cached.
     */
    @Test
    public void testLoadPQCKeys() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        String pqcKeyFile = "${server.config.dir}/resources/security/security.token.ltpa.keys.pqc.txt";
        
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          pqcKeyFile,
                                          KEYPASSWORD_CORRECT, null, false);
        
        // Verify classical keys are loaded
        Assert.assertNotNull("Secret key should not be null", 
                            keyInfoManager.getSecretKey(pqcKeyFile));
        Assert.assertNotNull("Private key should not be null", 
                            keyInfoManager.getPrivateKey(pqcKeyFile));
        Assert.assertNotNull("Public key should not be null", 
                            keyInfoManager.getPublicKey(pqcKeyFile));
        
        // Verify PQC ML-DSA keys are loaded
        Assert.assertNotNull("ML-DSA private key should not be null", 
                            keyInfoManager.getMLDSAPrivateKey(pqcKeyFile));
        Assert.assertNotNull("ML-DSA public key should not be null", 
                            keyInfoManager.getMLDSAPublicKey(pqcKeyFile));
    }

    /**
     * Test retrieving ML-DSA keys when they don't exist in the key file.
     * Should return null for missing PQC keys.
     */
    @Test
    public void testGetMLDSAKeysNotPresent() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        
        // Load a classical LTPA key file without PQC keys
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          LTPA_KEY_IMPORT_FILE,
                                          KEYPASSWORD_CORRECT, null, false);
        
        // Verify classical keys are loaded
        Assert.assertNotNull("Secret key should not be null", 
                            keyInfoManager.getSecretKey(LTPA_KEY_IMPORT_FILE));
        
        // Verify PQC keys return null when not present
        Assert.assertNull("ML-DSA private key should be null when not in file", 
                         keyInfoManager.getMLDSAPrivateKey(LTPA_KEY_IMPORT_FILE));
        Assert.assertNull("ML-DSA public key should be null when not in file", 
                         keyInfoManager.getMLDSAPublicKey(LTPA_KEY_IMPORT_FILE));
    }

    /**
     * Test retrieving ML-DSA keys for a non-existent key file.
     * Should return null.
     */
    @Test
    public void testGetMLDSAKeysForNonExistentFile() {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        
        // Try to get PQC keys for a file that was never loaded
        Assert.assertNull("ML-DSA private key should be null for non-existent file", 
                         keyInfoManager.getMLDSAPrivateKey(KEYIMPORTFILE_NO_EXIST));
        Assert.assertNull("ML-DSA public key should be null for non-existent file", 
                         keyInfoManager.getMLDSAPublicKey(KEYIMPORTFILE_NO_EXIST));
    }

    /**
     * Test that PQC keys are properly cached and can be retrieved multiple times.
     */
    @Test
    public void testPQCKeyCaching() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        String pqcKeyFile = "${server.config.dir}/resources/security/security.token.ltpa.keys.pqc.txt";
        
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          pqcKeyFile,
                                          KEYPASSWORD_CORRECT, null, false);
        
        // Get PQC keys first time
        byte[] mldsaPrivateKey1 = keyInfoManager.getMLDSAPrivateKey(pqcKeyFile);
        byte[] mldsaPublicKey1 = keyInfoManager.getMLDSAPublicKey(pqcKeyFile);
        
        Assert.assertNotNull("ML-DSA private key should not be null", mldsaPrivateKey1);
        Assert.assertNotNull("ML-DSA public key should not be null", mldsaPublicKey1);
        
        // Get PQC keys second time - should return same cached instances
        byte[] mldsaPrivateKey2 = keyInfoManager.getMLDSAPrivateKey(pqcKeyFile);
        byte[] mldsaPublicKey2 = keyInfoManager.getMLDSAPublicKey(pqcKeyFile);
        
        Assert.assertSame("ML-DSA private key should be cached", mldsaPrivateKey1, mldsaPrivateKey2);
        Assert.assertSame("ML-DSA public key should be cached", mldsaPublicKey1, mldsaPublicKey2);
    }

    /**
     * Test loading hybrid mode LTPA key file with both classical and PQC keys.
     */
    @Test
    public void testLoadHybridModeKeys() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        String hybridKeyFile = "${server.config.dir}/resources/security/security.token.ltpa.keys.hybrid.txt";
        
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          hybridKeyFile,
                                          KEYPASSWORD_CORRECT, null, false);
        
        // Verify all classical keys are present
        Assert.assertNotNull("Secret key should not be null in hybrid mode", 
                            keyInfoManager.getSecretKey(hybridKeyFile));
        Assert.assertNotNull("Private key should not be null in hybrid mode", 
                            keyInfoManager.getPrivateKey(hybridKeyFile));
        Assert.assertNotNull("Public key should not be null in hybrid mode", 
                            keyInfoManager.getPublicKey(hybridKeyFile));
        
        // Verify all PQC keys are present
        Assert.assertNotNull("ML-DSA private key should not be null in hybrid mode", 
                            keyInfoManager.getMLDSAPrivateKey(hybridKeyFile));
        Assert.assertNotNull("ML-DSA public key should not be null in hybrid mode", 
                            keyInfoManager.getMLDSAPublicKey(hybridKeyFile));
    }

}
