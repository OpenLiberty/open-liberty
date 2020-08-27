/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import javax.crypto.BadPaddingException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import test.UTLocationHelper;
import test.common.SharedOutputManager;

public class LTPAKeyInfoManagerTest {

    private static SharedOutputManager outputMgr;

    private static final String KEYIMPORTFILE_GETS_CREATED = "${server.config.dir}/resources/security/security.token.ltpa.keys.create.txt";
    private static final String KEYIMPORTFILE_NO_EXIST = "${server.config.dir}/resources/security/security.token.ltpa.keys.noexist.txt";
    private static final String KEYIMPORTFILE_INCORRECT_PRIVATEKEY = "${server.config.dir}/resources/security/security.token.ltpa.keys.incorrectprivatekey.txt";
    private static final String KEYIMPORTFILE_NO_SECRETKEY = "${server.config.dir}/resources/security/security.token.ltpa.keys.nosecretkey.txt";
    private static final String KEYIMPORTFILE_NO_PRIVATEKEY = "${server.config.dir}/resources/security/security.token.ltpa.keys.noprivatekey.txt";
    private static final String KEYIMPORTFILE_NO_PUBLICKEY = "${server.config.dir}/resources/security/security.token.ltpa.keys.nopublickey.txt";
    private static final String KEYIMPORTFILE_NO_REALM = "${server.config.dir}/resources/security/security.token.ltpa.keys.norealm.txt";
    private static final String LTPA_KEY_IMPORT_FILE = "${server.config.dir}/resources/security/security.token.ltpa.keys.correct.txt";

    private static final byte[] KEYPASSWORD_CORRECT = "WebAS".getBytes();
    private static final byte[] KEYPASSWORD_INCORRECT = "IncorrectKeyword".getBytes();

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
    public static void tearDownClass() throws MalformedURLException {
        outputMgr.restoreStreams();
    }

    @Test
    public void prepareLTPAKeyInfo_newFile() throws Exception {
        WsLocationAdmin locAdmin = UTLocationHelper.getLocationManager();
        String ltpaKeyFile = "${server.config.dir}/resources/security/ignored";
        WsResource ltpaFile = locAdmin.resolveResource(ltpaKeyFile);
        ltpaFile.delete();
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          ltpaKeyFile,
                                          KEYPASSWORD_CORRECT);

        assertTrue("Expected CWWKS4103I message was not logged",
                   outputMgr.checkForMessages("CWWKS4103I:"));

        assertTrue("Expected CWWKS4104A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4104A:.*resources/security/ignored"));
    }

    @Test
    public void testNoExist() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          KEYIMPORTFILE_GETS_CREATED,
                                          KEYPASSWORD_CORRECT);
        Assert.assertNotNull("Resource does not get created",
                             keyInfoManager.getLTPAKeyFileResource(UTLocationHelper.getLocationManager(),
                                                                   KEYIMPORTFILE_GETS_CREATED));
    }

    @Test
    public void testIncorrectPrivateKey() throws Exception {
        try {
            LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
            keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                              KEYIMPORTFILE_INCORRECT_PRIVATEKEY,
                                              KEYPASSWORD_CORRECT);
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testIncorrectKeyPassword() throws Exception {
        try {
            LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
            keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                              LTPA_KEY_IMPORT_FILE,
                                              KEYPASSWORD_INCORRECT);
        } catch (BadPaddingException e) {
            // Expected
        }
    }

    @Test
    public void testNoSecretKey() throws Exception {
        try {
            LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
            keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                              KEYIMPORTFILE_NO_SECRETKEY,
                                              KEYPASSWORD_CORRECT);
        } catch (IllegalArgumentException e) {
            String expectedMessage = "CWWKS4102E: The system cannot create the LTPA token because the required " + LTPAKeyFileUtility.KEYIMPORT_SECRETKEY + " property is missing.";
            String actualMessage = e.getMessage();
            assertEquals("Exception did not contain expected message",
                         expectedMessage, actualMessage);
            assertTrue("Expected message was not logged",
                       outputMgr.checkForStandardErr(expectedMessage));
        }

    }

    @Test
    public void testNoPrivateKey() throws Exception {
        try {
            LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
            keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                              KEYIMPORTFILE_NO_PRIVATEKEY,
                                              KEYPASSWORD_CORRECT);
        } catch (IllegalArgumentException e) {
            String expectedMessage = "CWWKS4102E: The system cannot create the LTPA token because the required " + LTPAKeyFileUtility.KEYIMPORT_PRIVATEKEY
                                     + " property is missing.";
            String actualMessage = e.getMessage();
            assertEquals("Exception did not contain expected message",
                         expectedMessage, actualMessage);
            assertTrue("Expected message was not logged",
                       outputMgr.checkForStandardErr(expectedMessage));
        }

    }

    @Test
    public void testNoPublicKey() throws Exception {
        try {
            LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
            keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                              KEYIMPORTFILE_NO_PUBLICKEY,
                                              KEYPASSWORD_CORRECT);
        } catch (IllegalArgumentException e) {
            String expectedMessage = "CWWKS4102E: The system cannot create the LTPA token because the required " + LTPAKeyFileUtility.KEYIMPORT_PUBLICKEY + " property is missing.";
            String actualMessage = e.getMessage();
            assertEquals("Exception did not contain expected message",
                         expectedMessage, actualMessage);
            assertTrue("Expected message was not logged",
                       outputMgr.checkForStandardErr(expectedMessage));
        }

    }

    @Test
    public void testNoRealm() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          KEYIMPORTFILE_NO_REALM,
                                          KEYPASSWORD_CORRECT);

        Assert.assertNotNull("Secret key should not be null but was null",
                             keyInfoManager.getSecretKey(KEYIMPORTFILE_NO_REALM));
        Assert.assertNotNull("Private key should not be null but was null",
                             keyInfoManager.getPrivateKey(KEYIMPORTFILE_NO_REALM));
        Assert.assertNotNull("Public key should not be null but was null",
                             keyInfoManager.getPublicKey(KEYIMPORTFILE_NO_REALM));
        Assert.assertNull("Realm should be null but is not",
                          keyInfoManager.getRealm(KEYIMPORTFILE_NO_REALM));
    }

    @Test
    public void testCorrectInformation() throws Exception {

        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          LTPA_KEY_IMPORT_FILE,
                                          KEYPASSWORD_CORRECT);

        // Check the secret key.
        Assert.assertNotNull(keyInfoManager.getSecretKey(LTPA_KEY_IMPORT_FILE));

        // Check the private key.
        Assert.assertNotNull(keyInfoManager.getPrivateKey(LTPA_KEY_IMPORT_FILE));

        // Check the public key.
        Assert.assertNotNull(keyInfoManager.getPublicKey(LTPA_KEY_IMPORT_FILE));

        // Check the realm.
        Assert.assertNotNull(keyInfoManager.getRealm(LTPA_KEY_IMPORT_FILE));
    }

    @Test
    public void testGetLTPAKeyFileResourceExists() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        Assert.assertNotNull("Resource does not exist",
                             keyInfoManager.getLTPAKeyFileResource(UTLocationHelper.getLocationManager(),
                                                                   LTPA_KEY_IMPORT_FILE));
    }

    @Test
    public void testGetLTPAKeyFileResourceNotExists() throws Exception {
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        Assert.assertNull("Resource exists",
                          keyInfoManager.getLTPAKeyFileResource(UTLocationHelper.getLocationManager(),
                                                                KEYIMPORTFILE_NO_EXIST));
    }

    @Test
    public void prepareLTPAKeyInfo_outputdir_newFile() throws Exception {
        WsLocationAdmin locAdmin = UTLocationHelper.getLocationManager();
        String ltpaKeyFile = "${server.output.dir}/resources/security/ignored";
        WsResource ltpaFile = locAdmin.resolveResource(ltpaKeyFile);
        ltpaFile.delete();
        LTPAKeyInfoManager keyInfoManager = new LTPAKeyInfoManager();
        keyInfoManager.prepareLTPAKeyInfo(UTLocationHelper.getLocationManager(),
                                          ltpaKeyFile,
                                          KEYPASSWORD_CORRECT);

        assertTrue("Expected CWWKS4103I message was not logged",
                   outputMgr.checkForMessages("CWWKS4103I:"));

        assertTrue("Expected CWWKS4104A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4104A:.*resources/security/ignored"));
    }

}
