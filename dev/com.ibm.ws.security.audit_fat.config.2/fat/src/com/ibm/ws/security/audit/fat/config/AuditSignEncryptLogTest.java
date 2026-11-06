/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.audit.fat.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.AuditFileStream;
import com.ibm.ws.security.audit.fat.common.tooling.AuditMessageConstants;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.TestServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test Description:
 *
 * This test verifies that when auditFileHandler values for sign and encrypt are false, there is no signing and encryption.
 * The test verifies when sign and encrypt are true that various configuration values for the auditFileHandler signing and
 * encryption
 * parameters result in audit log records with headers for signing and encryption. The emphasis is on positive tests for
 * combinations of signing and
 * encryption config parameters along with the events, and compact. Testing of the maxFileSize, maxFiles, and logDirectory are
 * included in
 * the BasicAuditLoggingTest. Negative (error) tests are included in AuditSignEncryptErrorLogTest.
 *
 * Note: Tests below do a reconfigServer with restartServer=true so the server is restarted.
 * The dynamic reconfiguration with signing and encryption will be covered in the audit_fat.features test.
 * 
 * The keystores used in this test are copied over from the /com.ibm.ws.security.audit_fat.common.tooling/securitykeys directory.
 *
 * 1. AuditEncryptionKeyStore.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 2048, signature algorithm SHA256wRSA
 * auditencryption
 * auditsigning
 * Signer Certificates / Public Key (extracted from personal certs)
 * enccert
 * signcert
 *
 * 2. AuditEncryptionPublicKeyKeyStore.jks
 * Signer Certificates / Public Key
 * encrsigner
 *
 * 3. AuditEncryptionKeyStore2.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 256, signature algorithm SHA256wECDSA
 * encryptioncert
 * Signer Certificate / Public Key (extracted from personal certs)
 * enccert2
 *
 * 4. AuditSigningKeyStore.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 2048, signature algorithm SHA256wRSA
 * auditsigning
 * Signer Certificates / Public Key (extracted from personal certs)
 * signcert
 *
 * 5. AuditSigningKeyStore2.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 256, signature algorithm SHA256wECDSA
 * auditsigning2
 * Signer Certificate / Public Key (extracted from personal certs)
 * signcert2
 *
 * 6. key.jks (liberty default keystore - pre created to avoid creation delay during server start)
 * Personal Certificates/ Private Key
 * default
 *
 * 7. AuditCertPasswordKeystore.jks
 * Personal certificate/ Private key
 * passprotect (cert is protected by password "mypass" which is configured in the keystore configuration)
 *
 */

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditSignEncryptLogTest extends AuditCommonTest {

    public static TestServer myTestServer = new TestServer("com.ibm.ws.security.audit.fat.securelog", "audit_encrypt_goodSignerSettings.xml");
    private static String USER1 = "user1";
    private static String USER1PWD = "user1pwd";
    private static Boolean RESTART_SERVER = true;
    private static int DEFAULT_DELAY_SECONDS = 3;

    protected static final String CONFIG_ENCRYPT_TRUE_PUBLICKEY_GOODSETTINGS = "audit_encrypt_PublicKey_AlgSHA256RSA.xml";
    protected static final String CONFIG_ENCRYPT_TRUE_PRIVATEKEY_GOODSETTINGS = "audit_encrypt_PrivateKey_AlgSHA256RSA.xml";
    protected static final String CONFIG_ENCRYPT_TRUE_GOODSETTINGS_ALGSHA256WECDSA = "audit_encrypt_AlgSHA256ECDSA.xml";
    protected static final String CONFIG_ENCRYPT_TRUE_GOODSIGNERSETTINGS = "audit_encrypt_goodSignerSettings.xml";
    protected static final String CONFIG_SIGN_ENCRYPT_FALSE = "audit_signFalse_encryptFalse.xml";
    protected static final String CONFIG_SIGN_ENCRYPT_FALSE_KEYSTORES_SPECIFIED = "audit_signFalse_encryptFalse_withKS.xml";
    protected static final String CONFIG_SIGN_TRUE_GOODSETTINGS = "audit_sign_AlgSHA256RSA.xml";
    protected static final String CONFIG_SIGN_TRUE_GOODSETTINGS_ALGSHA256WECDSA = "audit_sign_AlgSHA256ECDSA.xml";
    protected static final String CONFIG_SIGN_PUBLIC_SIGNING_ALIAS = "audit_signTrue_publicSigningAlias.xml";
    protected static final String CONFIG_SIGN_ENCRYPT_TRUE_DIFF_KEYSTORE_DIFF_ALGS = "audit_encryptSign_diffKS_diffAlgs.xml";
    protected static final String CONFIG_SIGN_ENCRYPT_TRUE_SAME_KEYSTORE_DIFF_CERTS = "audit_encryptSign_sameKS_diffCerts.xml";
    protected static final String CONFIG_SIGN_ENCRYPT_TRUE_SAME_KEYSTORE_SAME_CERT = "audit_encryptSign_sameKS_sameCert.xml";
    protected static final String CONFIG_SIGN_ENCRYPT_TRUE_DEFAULT_KEYSTORE_DEFAULT_CERT = "audit_encryptSign_defaultKS_defaultCert.xml";
    protected static final String CONFIG_SIGN_ENCRYPT_PASSWORD_PROTECT_CERT_KEYSTORE = "audit_encryptSign_certPasswordKS.xml";
    protected static final String DEFAULT_GENERATED_KEY_PATH = "resources/security/key.jks";

    protected static Class<?> logClass = AuditSignEncryptLogTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;

    static List<String> checkApps = new ArrayList<String>();
    static List<String> startMsgs = new ArrayList<String>();
    static List<String> startFailedMsgs = new ArrayList<String>();
    static List<String> startFailedHandlerMsgs = new ArrayList<String>();
    static List<String> startMsgsWithDefaultKS = new ArrayList<String>();

    public AuditSignEncryptLogTest() {
        super(myTestServer.getServer(), logClass);
    }

    @Rule
    public TestName name = new TestName();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(logClass, "setUp", "Prep for test");
        urlBase = "http://" + myTestServer.getServer().getHostname() + ":" + myTestServer.getHttpDefaultPort();
        auditLogFile = myTestServer.getServer().getLogsRoot() + DEFAULT_AUDIT_LOG;

        checkApps.add(AUDIT_BASICAUTH_SERVLET);

        // Messages expected for start of audit with good server config
        startMsgs.add(AuditMessageConstants.CWWKF0012I_AUDIT_FEATURE_INSTALLED);
        startMsgs.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY);
        startMsgs.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);
        startMsgs.add(AuditMessageConstants.CWWKS0008I_SECURITY_SERVICE_READY);

        myTestServer.startServer(CONFIG_SIGN_ENCRYPT_FALSE, "AuditSignEncryptLogTest", checkApps, startMsgs, Constants.JUNIT_REPORTING, null);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myTestServer.stopServer();
        myTestServer.getServer().deleteFileFromLibertyServerRoot(DEFAULT_GENERATED_KEY_PATH);
    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() throws Exception {
        httpclient.getConnectionManager().shutdown();

    }

    /**
     * Test description:
     * When audit-1.0 feature is configured with auditFileHandler sign=false and encrypt=false and
     * no keystores or certificates, then the audit log is not signed or encrypted.
     * Config:
     * - sign=false, encrypt=false and no keystores or certs configured
     *
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     *
     * Expected results:
     * - Log file contains expected SECURITY_AUTHN event in JSON format which can be parsed by AuditAsserts tooling.
     */
    @Test
    public void testConfig_SignFalse_EncryptFalse_logHasJsonFormat() throws Exception {
        // 1. Reconfigure the server with auditFileHandler without signing and encryption
        myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_FALSE, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2.  Set up audit stream to capture the entire audit file contents
        AuditFileStream allEvents = new AuditFileStream(auditLogFile);
        AuditAsserts asserts = new AuditAsserts(logClass, allEvents);

        // 3. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 4. Verify that AUTHN events is found in the log
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }

    /**
     * Test description:
     * When audit-1.0 feature is configured with auditFileHandler sign=false and encrypt=false and
     * no keystores or certificates, then the audit log is not signed or encrypted.
     * Config:
     * - sign=false, encrypt=false with keystores and aliases specified
     * - compact=true
     *
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     *
     * Expected results:
     * - Log file contains expected SECURITY_AUTHN event in JSON format which can be parsed by AuditAsserts tooling.
     */
    @Test
    public void testConfig_SignFalse_EncryptFalse_withKeystoreAlias_logHasJsonFormat() throws Exception {
        // 1. Reconfigure the server with auditFileHandler without signing and encryption
        myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_FALSE_KEYSTORES_SPECIFIED, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2.  Set up audit stream to capture the entire audit file contents
        AuditFileStream allEvents = new AuditFileStream(auditLogFile);
        AuditAsserts asserts = new AuditAsserts(logClass, allEvents);

        // 3. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 4. Verify that AUTHN events is found in the log
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }

    /**
     * Test description:
     * When audit-1.0 feature is configured with auditFileHandler and encrypt=true with a valid keystore
     * which contains both the public and private key, then the audit log can be encrypted using the public key.
     * 
     * The keystore has Signer certificate/public key (enccert) with key size 2048, Version X509V3, and signature algorithm
     * SHA256withRSA
     *
     * Config:
     * - encrypt with public key (signer certificate) specified in config
     * - signing defaults to false since no config parameters are specified for signing
     * - keystore password is not encoded
     * - events filtering is included
     *
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     *
     * Expected results:
     * - Log file contains header with Encryption Information for encrypted public key, certificate ailas, keystore and encryption
     * certificate.
     * - Log file does not contain a header for Signing Information.
     * - The audit log contains 3 records after the servlet is accessed.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfigEncryptLog_WithKeystorePublicKey_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to encrypt the log
        myTestServer.reconfigServer(CONFIG_ENCRYPT_TRUE_PUBLICKEY_GOODSETTINGS, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
        delay(DEFAULT_DELAY_SECONDS);

        // 3. Verify the expected values are found in the header of the audit log for encryption and there is no section for signing
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS));
        assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_RSA));
        assertNull("Signing section found when none was expected", readSecureLogHeader(auditLogFile, SIGNING_SECTION));

        // 4. Verify count of audit records includes 2 AUDIT MGMT and 1 SUCCESSFUL AUTHN
        Map<String, Integer> counts = countSections(auditLogFile, "auditRecord");
        assertEquals("Expected 3 auditRecords in the log.  Instead found: " + counts.get(AUDIT_RECORD).toString(), 3, counts.get(AUDIT_RECORD).intValue());
    }

    /**
     * Test description:
     * When audit-1.0 feature is configured with auditFileHandler and encrypt=true with a valid keystore
     * which contains both the public and private key, then the audit log can be encrypted using the private key (personal cert).
     *
     * The keystore has Personal certificate/private key (auditencryption) with key size 2048, Version X509V3, and signature
     * algorithm SHA256withRSA
     *
     * Config:
     * - encrypt with public key (signer certificate) specified in config
     * - signing is set to false so signing certificate and alias settings should be ignored
     * - keystore password is encoded
     * - no event filtering
     *
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     *
     * Expected results:
     * - Log file contains header with Encryption Information for encrypted public key, certificate ailas, keystore and encryption
     * certificate.
     * - Log file does not contain a header for Signing Information.
     */
    @Test
    public void testConfigEncryptLog_WithKeystorePrivateKey_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to encrypt the log
        myTestServer.reconfigServer(CONFIG_ENCRYPT_TRUE_PRIVATEKEY_GOODSETTINGS, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 3. Verify the expected values are found in the header of the audit log for encryption
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS));
        assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_RSA));
        assertNull("Signing section found when none was expected", readSecureLogHeader(auditLogFile, SIGNING_SECTION));

    }

    /**
     * Test description:
     * When audit-1.0 feature is configured with auditFileHandler and sign=true with a valid keystore
     * which contains both the public and private key, then the audit log can be signed using personal cert (private key).
     *
     * The keystore has Personal certificate/private key (auditsigning) with key size 2048, Version X509V3, and signature
     * algorithm SHA256withRSA
     *
     * Config:
     * - sign with personal certificate specified in signing config - SHA256 RSA
     * - encryptKeyStore and encryptAlias are specified but should be ignored since encrypt defaults to false.
     * - compact - false
     *
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     *
     * Expected results:
     * - Log file contains header with Signing Information for signing private key, certificate ailas, keystore and signing
     * certificate.
     * - Log file does not contain a header for Encryption Information.
     * - Log contains 3 audit records - 2 AUDIT_MGMT + 1 SUCCESSFUL AUTHN
     */
    @Test
    public void testConfigSignLog_KeystoreAlgSHA256wRSA_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to sign the log
        myTestServer.reconfigServer(CONFIG_SIGN_TRUE_GOODSETTINGS, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
        delay(DEFAULT_DELAY_SECONDS);

        // 3. Verify the expected values are found in the header of the audit log for signing and no header is found for encryption.
        List<String> header = readSecureLogHeader(auditLogFile, SIGNING_SECTION);
        Map<String, String> found = parseValues(header, HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS, HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
        assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
        assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS));
        assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE));
        assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));
        assertNull("Encryption section found when none was expected", readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION));

        // 4. Verify count of audit records includes 2 AUDIT MGMT and 1 SUCCESSFUL AUTHN
        Map<String, Integer> counts = countSections(auditLogFile, "auditRecord");
        assertEquals("Expected 3 auditRecords in the log.  Instead found: " + counts.get(AUDIT_RECORD).toString(), 3, counts.get(AUDIT_RECORD).intValue());

    }

    /**
     * Test description:
     * When audit-1.0 feature is configured with auditFileHandler and sign=true with a valid keystore
     * which contains both the public and private keys then the audit log can be signed when the Personal Cert alias is specified.
     *
     * The keystore has personal certificate/private key (encryptioncert) with key size 2048, Version X509V3, and signature
     * algorithm SHA256withECDSA
     *
     * Config:
     * - sign with personal certificate specified in signing config - SHA256 ECDSA
     * - compact=true should have no effect on this log format
     *
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     *
     * Expected results:
     * - Log file contains header with Signing Information for signing private key, certificate ailas, keystore and signing
     * certificate.
     * - Log file does not contain a header for Encryption Information.
     * - Log contains 3 audit records after servlet access - 2 AUDIT_MGMT + 1 SUCCESSFUL AUTHN
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfigSignLog_KeystoreAlgSHA256wECDSA_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to sign the log
        myTestServer.reconfigServer(CONFIG_SIGN_TRUE_GOODSETTINGS_ALGSHA256WECDSA, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
        delay(DEFAULT_DELAY_SECONDS);

        // 3. Verify the expected values are found in the header of the audit log for signing but not for encryption
        List<String> header = readSecureLogHeader(auditLogFile, SIGNING_SECTION);
        Map<String, String> found = parseValues(header, HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS, HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
        assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
        assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS2 + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS2));
        assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE2 + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE2));
        assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_EC + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_EC));
        assertNull("Encryption section found when none was expected", readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION));

        // 4. Verify count of audit records includes 2 AUDIT MGMT and 1 SUCCESSFUL AUTHN
        Map<String, Integer> counts = countSections(auditLogFile, "auditRecord");
        assertEquals("Expected 3 auditRecords in the log.  Instead found: " + counts.get(AUDIT_RECORD).toString(), 3, counts.get(AUDIT_RECORD).intValue());
    }

    /**
     * Test description:
     * When audit-1.0 feature is specified with auditFileHandler and encrypt=true and valid keystore
     * which contains only the Signer certificate/public key, then the audit log can be encrypted using public key.
     *
     * Keystore contains Signer certificate (encrsigner) extacted from the Personal Certificate.
     *
     * - Start the server and access a servlet to generate audit records in the log.
     * Expected results:
     * - Log file contains header for Encryption information
     * - Log file does not contain a header for Signing Information.
     */
    @Test
    public void testConfigEncryptLog_KeystorePublicKeyOnly_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to encrypt the log
        myTestServer.reconfigServer(CONFIG_ENCRYPT_TRUE_GOODSIGNERSETTINGS, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 3. Verify the expected values are found in the header of the audit log for encryption
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);

        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS_ONLY + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS_ONLY));
        assertTrue("Expected encryption keystore of " + ENCRYPTION_PUBLICKEY_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_PUBLICKEY_KEYSTORE));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_RSA));
        assertNull("Signing section found when none was expected", readSecureLogHeader(auditLogFile, SIGNING_SECTION));
    }

    /**
     * Test description:
     * When audit-1.0 feature is specified with auditFileHandler and encrypt=true and valid keystore
     * which contains both the public and private key, is encrypted with SHA256 ECDSA, then the audit log can be encrypted using
     * the public key.
     * The keystore has certificate with key size 2048, Version X509V3, and signature algorithm SHA256 with ECDSA
     *
     * - Start the server and access a servlet to generate audit records in the log.
     * Expected results:
     * - Log file contains header for Encryption information
     * - Log file does not contains header for Signing information
     */
    @Test
    public void testConfigEncryptLog_KeystoreAlgSHA256wECDSA_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to encrypt the log
        myTestServer.reconfigServer(CONFIG_ENCRYPT_TRUE_GOODSETTINGS_ALGSHA256WECDSA, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 3. Verify the expected values are found in the header of the audit log for encryption
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS2 + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS2));
        assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE2 + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE2));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_EC + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_EC));
        assertNull("Signing section found when none was expected", readSecureLogHeader(auditLogFile, SIGNING_SECTION));
    }

    /**
     * Test description:
     * When audit-1.0 feature is configured with auditFileHandler and sign=true and encrypt=true with a different
     * keystore and different algorithm for signing and encryption.
     *
     * The signing keystore has Personal certificate/ private key (auditsigning) with key size 2048, Version X509V3, and signature
     * algorithm SHA256withRSA
     * The encryption keystore has Signer certificate/public key (enccert2) with key size 2048, Version X509V3, and signature
     * algorithm SHA256withECDSA
     * 
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     *
     * Expected results:
     * - Log file contains header with Signing Information and Encryption Information.
     * - Log contains 3 audit records - 2 AUDIT_MGMT + 1 SUCCESSFUL AUTHN
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfigSignEncryptLog_differentKeystoresAndAlgs_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to sign and encrypt the log
        myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_TRUE_DIFF_KEYSTORE_DIFF_ALGS, name.getMethodName(), startMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
        delay(DEFAULT_DELAY_SECONDS);

        // 3. Verify the expected values are found in the header of the audit log for signing
        List<String> header = readSecureLogHeader(auditLogFile, SIGNING_SECTION);
        Map<String, String> found = parseValues(header, HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS, HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
        assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
        assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS));
        assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE));
        assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));

        // 4. Verify the expected values are found in the header of the audit log for encryption
        header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS2 + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS2));
        assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE2 + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE2));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_EC + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_EC));

        // 5. Verify count of audit records includes 2 AUDIT MGMT and 1 SUCCESSFUL AUTHN
        Map<String, Integer> counts = countSections(auditLogFile, "auditRecord");
        assertEquals("Expected 3 auditRecords in the log.  Instead found: " + counts.get(AUDIT_RECORD).toString(), 3, counts.get(AUDIT_RECORD).intValue());
    }

    /**
     * Test description: When audit-1.0 feature is configured with auditFileHandler and sign=true and encrypt=true with the same
     * keystore
     * and different certificates for signing and encryption and compact true.
     * 
     * The keystore has signing certificate alias (auditsigning) with key size 2048, Version X509V3, and signature algorithm
     * SHA256withRSA The keystore
     * has encryption certificate alias (enccert) with key size 2048, Version X509V3, and signature algorithm SHA256withRSA
     * 
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     * 
     * Expected results: - Log file contains header with Signing Information and Encryption Information.
     * - Log contains 3 audit records - 2 AUDIT_MGMT + 1 SUCCESSFUL AUTHN
     */
    @Test
    public void testConfigSignEncryptLog_sameKeystore_diffCerts_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to sign and encrypt the log
        // the log
        myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_TRUE_SAME_KEYSTORE_DIFF_CERTS, name.getMethodName(), startMsgs,
                RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a
        // successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
                USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
        delay(DEFAULT_DELAY_SECONDS);

        // 3. Verify the expected values are found in the header of the audit log for encryption
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS));
        assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_RSA));

        // 4. Verify the expected values are found in the header of the audit log for signing
        header = readSecureLogHeader(auditLogFile, SIGNING_SECTION);
        found = parseValues(header, HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS, HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
        assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
        assertTrue("Expected signing cert alias of " + ENCRYPTION_KEYSTORE_PRIVATE_SIGNING_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(ENCRYPTION_KEYSTORE_PRIVATE_SIGNING_CERT_ALIAS));
        assertTrue("Expected signing keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(ENCRYPTION_KEYSTORE));
        assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));

        // 4. Verify count of audit records includes 2 AUDIT MGMT and 1 SUCCESSFUL AUTHN
        Map<String, Integer> counts = countSections(auditLogFile, "auditRecord");
        assertEquals("Expected 3 auditRecords in the log.  Instead found: " + counts.get(AUDIT_RECORD).toString(), 3, counts.get(AUDIT_RECORD).intValue());
    }

    /**
     * Test description: When audit-1.0 feature is configured with auditFileHandler and sign=true and encrypt=true with the same
     * keystore
     * and the same certificate used for signing with its private key and encrypting with public key.
     * 
     * The keystore has Personal signing certificate alias (auditencryption) with key size 2048, Version X509V3, and signature
     * algorithm SHA256withRSA
     * The keystore has Signer encryption certificate alias (enccert) with key size 2048, Version X509V3, and signature algorithm
     * SHA256withRSA
     * 
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     * 
     * Expected results: - Log file contains header with Signing Information and Encryption Information.
     */
    @Test
    public void testConfigSignEncryptLog_sameKeystore_sameCert_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to sign and encrypt the log
        // the log
        myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_TRUE_SAME_KEYSTORE_SAME_CERT, name.getMethodName(), startMsgs,
                RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a
        // successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 3. Verify the expected values are found in the header of the audit log for encryption
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + ENCRYPTION_KEYSTORE_PRIVATE_SIGNING_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_KEYSTORE_PRIVATE_SIGNING_CERT_ALIAS));
        assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE));
        assertTrue("Expected encryption certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));

        // 4. Verify the expected values are found in the header of the audit log for signing
        header = readSecureLogHeader(auditLogFile, SIGNING_SECTION);
        found = parseValues(header, HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS, HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
        assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
        assertTrue("Expected signing cert alias of " + ENCRYPTION_KEYSTORE_PRIVATE_SIGNING_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(ENCRYPTION_KEYSTORE_PRIVATE_SIGNING_CERT_ALIAS));
        assertTrue("Expected signing keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(ENCRYPTION_KEYSTORE));
        assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));
    }

    /**
     * Test description: When audit-1.0 feature is configured with auditFileHandler and sign=true and encrypt=true with the
     * Liberty
     * default keystore and the default certificate used for signing with its private key and encrypting with public key.
     * 
     * The keystore has Personal certificate alias (default) used for signing and encryption.
     * 
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     * 
     * Expected results: - Log file contains header with Signing Information and Encryption Information.
     */
    @Test
    public void testConfigSignEncryptLog_defautKeystore_defaultCert_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to sign and  encrypt the log
        myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_TRUE_DEFAULT_KEYSTORE_DEFAULT_CERT, name.getMethodName(), startMsgs,
                RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 3. Verify the expected values are found in the header of the audit log for encryption
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + DEFAULT_KEYSTORE_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(DEFAULT_KEYSTORE_CERT_ALIAS));
        assertTrue("Expected encryption keystore of " + DEFAULT_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(DEFAULT_KEYSTORE));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));

        // 4. Verify the expected values are found in the header of the audit log for signing
        header = readSecureLogHeader(auditLogFile, SIGNING_SECTION);
        found = parseValues(header, HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS, HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
        assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
        assertTrue("Expected signing cert alias of " + DEFAULT_KEYSTORE_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(DEFAULT_KEYSTORE_CERT_ALIAS));
        assertTrue("Expected signing keystore of " + DEFAULT_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(DEFAULT_KEYSTORE));
        assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));
    }

    /**
     * Test description: When audit-1.0 feature is configured with auditFileHandler and sign=true and encrypt=true with a
     * keystore which contains a personal certificate that is password protected.
     * 
     * The keystore has Personal certificate alias (passprotect) used for signing and encryption.
     * 
     * Start the server and access a servlet to generate audit records in the log for successful AUDIT MGMT and AUTHN events.
     * 
     * Expected results: - Log file contains header with Signing Information and Encryption Information.
     * - The audit log contains 3 records after the servlet is accessed.
     */
    @Test
    public void testConfigSignEncryptLog_sameKeystore_passwordProtectedCert_headerContainsExpectedValues() throws Exception {
        // 1. Reconfigure the server with auditFileHandler specifying to sign the log
        myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_PASSWORD_PROTECT_CERT_KEYSTORE, name.getMethodName(), startMsgs,
                RESTART_SERVER, Constants.JUNIT_REPORTING);

        // 2. Access the servlet with good userId and password to generate a successful AUTH event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI, USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
        delay(DEFAULT_DELAY_SECONDS);

        // 3. Verify the expected values are found in the header of the audit log for encryption
        List<String> header = readSecureLogHeader(auditLogFile, ENCRYPTION_SECTION);
        Map<String, String> found = parseValues(header, HEADER_ENCRYPTED_SHARED_KEY, HEADER_ENCRYPTION_CERT_ALIAS, HEADER_ENCRYPTION_KEYSTORE, HEADER_ENCRYPTION_CERTIFICATE);
        assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
        assertTrue("Expected encryption cert alias of " + PASSWORD_PROTECTED_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(PASSWORD_PROTECTED_CERT_ALIAS));
        assertTrue("Expected encryption keystore of " + PASSWORD_PROTECTED_CERT_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(PASSWORD_PROTECTED_CERT_KEYSTORE));
        assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));

        // 4. Verify the expected values are found in the header of the audit log for signing
        header = readSecureLogHeader(auditLogFile, SIGNING_SECTION);
        found = parseValues(header, HEADER_SIGNING_SHARED_KEY, HEADER_SIGNING_CERT_ALIAS, HEADER_SIGNING_KEYSTORE, HEADER_SIGNING_CERTIFICATE);
        assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
        assertTrue("Expected signing cert alias of " + PASSWORD_PROTECTED_CERT_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains("passprotect"));
        assertTrue("Expected signing keystore of " + PASSWORD_PROTECTED_CERT_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(PASSWORD_PROTECTED_CERT_KEYSTORE));
        assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));

        // 5. Verify count of audit records includes 2 AUDIT MGMT and 1 SUCCESSFUL AUTHN
        Map<String, Integer> counts = countSections(auditLogFile, "auditRecord");
        assertEquals("Expected 3 auditRecords in the log.  Instead found: " + counts.get(AUDIT_RECORD).toString(), 3, counts.get(AUDIT_RECORD).intValue());
    }

}
