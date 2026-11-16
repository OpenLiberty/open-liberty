package com.ibm.ws.security.audit.reader.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditFileStream;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * This test verifies that the auditUtility auditReader command functions to decrypt encrypted audit logs,
 * unsign signed audit logs and both unsign and decrypt logs which are both signed and encrypted. It covers the supported
 * keystore types of JKS, PCKS12 and JECKS. The test covers positive as well as negative causes.
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
 * 2. AuditEncryptionKeyStore2.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 256, signature algorithm SHA256wECDSA
 * encryptioncert
 * Signer Certificate / Public Key (extracted from personal certs)
 * enccert2
 *
 * 3. AuditSigningKeyStore.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 2048, signature algorithm SHA256wRSA
 * auditsigning
 * Signer Certificates / Public Key (extracted from personal certs)
 * signcert
 *
 * 4. AuditSigningKeyStore2.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 256, signature algorithm SHA256wECDSA
 * auditsigning2
 * Signer Certificate / Public Key (extracted from personal certs)
 * signcert2
 *
 *
 * 5. AuditCertPasswordKeystore.jks
 * Personal certificate/ Private key
 * passprotect (cert is protected by password "mypass" which is configured in the keystore configuration)
 *
 * 6. PKCS12Keystore
 * Personal certificate (version X509 v3, size 2048, signature algorithm SHA256wRSA
 * selfsign
 *
 * 7. JCEKSKeystore
 * Personal certificate (version X509 v3, size 2048, signature algorithm SHA256wRSA
 * selfsign
 *
 * The audit logs used in this test are pre-generated from running tests in FAT project
 * com.ibm.ws.security.audit_fat.config/AuditSignEncryptLogTest and then renaming the audit.log file which is generated. The
 * logs are then copied to this project under publish/servers/com.ibm.ws.security.audit.reader.fat/auditlogs
 *
 * - certPwdProtectedAuditLog - from test testConfigSignEncryptLog_sameKeystore_passwordProtectedCert_headerContainsExpectedValues
 * - compactSignedAuditLog - testConfigSignLog_KeystoreAlgSHA256wECDSA_headerContainsExpectedValues
 * - encAuditLog - from test testConfigEncryptLog_WithKeystorePrivateKey_headerContainsExpectedValues
 * - encAuditDiffPathLog - edited version of encAuditLog to specify log path that does not exist for the server
 * ..build.image/wlp/usr/servers/com.ibm.ws.security.audit.reader.fat.different
 * - encSignedAuditLog - from test testConfigSignEncryptLog_differentKeystoresAndAlgs_headerContainsExpectedValues
 * - signedAuditLog - from test testConfigSignLog_KeystoreAlgSHA256wRSA_headerContainsExpectedValues
 * - signJCEKSEncryptPKCS12Audit.log - from running
 * testConfigSignEncryptLog_sameKeystore_passwordProtectedCert_headerContainsExpectedValues but with changes to
 * use JCEKSKeystore for signing and PKCS12Keystore for encryption. Run the test with the config file changed to
 * audit_encryptPkcs12SignJceksKS.xml
 * - regularJSONAudit.log -from running testConfig_SignFalse_EncryptFalse_logHasJsonFormat
 *
 *
 * Since these tests are run on different OS platforms, the template logs are used to replace the keystore path with
 * the path to the keystores on that platform so they can be used in the tests where the keystore location defaults to that in the
 * audit log file (rather than being specified on the command line)
 *
 * - signedTemplateAuditLog - edited version of signedAuditLog with keystore specified as
 * <signingKeyStore>replaceThisStringWithSigningKS</signingKeyStore>
 *
 * - encSignedTemplateAudit.log - edited version of encSignedAuditLog with keystores specified as
 * <encryptionKeyStore>replaceThisStringWithEncKS</encryptionKeyStore> and
 * <signingKeyStore>replaceThisStringWithSigningKS</signingKeyStore>
 *
 * - encTemplateAudit.log - edited version of encAuditLog with keystore specified as
 * <encryptionKeyStore>replaceThisStringWithEncKS</encryptionKeyStore>
 *
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditReaderUtilityTest {

    private static final Class<?> thisClass = AuditReaderUtilityTest.class;
    private static final String SLASH = System.getProperty("file.separator");
    private static LibertyServer server;
    private static Machine machine;
    private static Properties env;
    private static String installRoot;
    private static String serverRoot;

    private static final String CMD_AUDIT_UTILITY = "/bin/auditUtility";
    private static final String CMD_AUDIT_READER = "auditReader";

    private static final String PARM_AUDIT_FILE_LOCATION = "--auditFileLocation=";
    private static final String PARM_OUTPUT_FILE_LOCATION = "--outputFileLocation=";
    private static final String PARM_ENCRYPTED = "--encrypted=";
    private static final String PARM_SIGNED = "--signed=";
    private static final String PARM_ENCRYPTION_KEYSTORE_PASSWORD = "--encKeyStorePassword=";
    private static final String PARM_SIGNING_KEYSTORE_PASSWORD = "--signingKeyStorePassword=";
    private static final String PARM_ENCRYPTION_KEYSTORE_LOCATION = "--encKeyStoreLocation=";
    private static final String PARM_SIGNING_KEYSTORE_LOCATION = "--signingKeyStoreLocation=";
    private static final String PARM_ENCRYPTION_KEYSTORE_TYPE = "--encKeyStoreType=";
    private static final String PARM_SIGNING_KEYSTORE_TYPE = "--signingKeyStoreType=";
    private static final String PARM_UNKNOWN = "--unknownParm=";

    private static final int RETURN_CODE_SUCCESS = 0;
    private static final int RETURN_CODE_FAILED = 1;

    private static String REGULAR_JSON_LOG = "/auditlogs/regularJSONAudit.log";
    private static String ENCRYPTED_LOG = "/auditlogs/encAudit.log";
    private static String ENCRYPTED_MODIFIED_DATA_FIRST_RECORD_LOG = "/auditlogs/encAuditModifiedDataFirstRecord.log";
    private static String ENCRYPTED_MODIFIED_DATA_THIRD_RECORD_LOG = "/auditlogs/encAuditModifiedDataThirdRecord.log";
    private static String ENCRYPTED_TEMPLATE_LOG = "/auditlogs/encTemplateAudit.log";
    private static String ENCRYPTED_DEFAULTKS_LOG = "/auditlogs/encDefaultKSAudit.log";
    private static String ENCRYPTED_DIFFPATH_LOG = "/auditlogs/encAuditDiffPath.log";
    private static String SIGNED_LOG = "/auditlogs/signedAuditLog";
    private static String SIGNED_MODIFIED_DATA_FIRST_RECORD_LOG = "/auditlogs/signedAuditModifiedDataFirstRecordLog";
    private static String SIGNED_MODIFIED_DATA_THIRD_RECORD_LOG = "/auditlogs/signedAuditModifiedDataThirdRecordLog";
    private static String SIGNED_TEMPLATE_LOG = "/auditlogs/signedTemplateAuditLog";
    private static String SIGNED_DEFAULTKS_LOG = "/auditlogs/signedDefaultKSAuditLog";
    private static String SIGNED_COMPACT_LOG = "/auditlogs/compactSignedAudit.log";
    private static String ENCRYPTED_SIGNED_LOG = "/auditlogs/encSignedAudit.log";
    private static String ENCRYPTED_SIGNED_MODIFIED_DATA_FIRST_RECORD_LOG = "/auditlogs/encSignedAuditModifiedDataFirstRecord.log";
    private static String ENCRYPTED_SIGNED_MODIFIED_DATA_THIRD_RECORD_LOG = "/auditlogs/encSignedAuditModifiedDataThirdRecord.log";
    private static String ENCRYPTED_SIGNED_DEFAULTKS_LOG = "/auditlogs/encSignedDefaultKSAudit.log";
    private static String ENCRYPTED_SIGNED_TEMPLATE_LOG = "/auditlogs/encSignedTemplateAudit.log";
    private static String ENCRYPTEDPKCS12_SIGNEDJCEKS_LOG = "/auditlogs/signJCEKSEncryptPKCS12Audit.log";
    private static String CERT_PROTECTED_LOG = "/auditlogs/certPwdProtectedAudit.log";
    private static String NONEXISTING_LOG = "/auditlogs/notExistLog.log";
    private static String NONEXISTING_DIR = "/nonExistDir/notExistLog.log";

    private static String REGULAR_JSON_OUTPUT = "/auditout/regularJSONAudit.out";
    private static String REGULAR_JSON_OUTPUT2 = "/auditout/regularJSONAudit2.out";
    private static String DECRYPTED_OUTPUT = "/auditout/encAudit.out";
    private static String DECRYPTED_MODIFIED_DATA_THIRD_RECORD_OUTPUT = "/auditout/encAuditModifiedDataThirdRecord.out";
    private static String DECRYPTED_DEFAULTKS_OUTPUT = "/auditout/encDefaultKSAudit.out";
    private static String DECRYPTED_DIFFPATH_OUTPUT = "/auditout/encDiffPathAudit.out";
    private static String DECRYPTED_OUTPUT2 = "/auditout/encAudit2.out";
    private static String DECRYPTED_NOSIGN_OUTPUT = "/auditout/encNoSignAudit.out";
    private static String UNSIGNED_OUTPUT = "/auditout/signedAuditOut";
    private static String UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT = "/auditout/signedAuditModifiedDataThirdRecordOut";
    private static String UNSIGNED_DEFAULTKS_OUTPUT = "/auditout/signedDefaultKSAuditOut";
    private static String DECRYPTED_UNSIGNED_OUTPUT = "/auditout/encSignedAudit.out";
    private static String DECRYPTED_UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT = "/auditout/encSignedAuditModifiedDataThirdRecord.out";
    private static String DECRYPTED_UNSIGNED_DEFAULTKS_OUTPUT = "/auditout/encSignedDefaultKSAudit.out";
    private static String DECRYPTPKCS12_UNSIGNJCEKS_OUTPUT = "/auditout/signJCEKSEncryptPKCS12Audit.out";
    private static String UNSIGNED_COMPACT_OUTPUT = "/auditout/compactSignedAudit.out";
    private static String CERT_PROTECTED_OUTPUT = "/auditout/certPwdProtectedAudit.out";
    private static String ERROR_OUTPUT = "/auditout/shouldNotExist.out";
    private static String PREEXISTING_OUTPUT = "/auditout/preExisting.out";
    private static String READONLY_OUTPUT = "/auditout/readOnly.out";

    private static String ENCRYPTION_KEYSTORE = "/resources/security/AuditEncryptionKeyStore.jks";
    private static String ENCRYPTION_KEYSTORE2 = "/resources/security/AuditEncryptionKeyStore2.jks";
    private static String SIGNING_KEYSTORE = "/resources/security/AuditSigningKeyStore.jks";
    private static String SIGNING_KEYSTORE2 = "/resources/security/AuditSigningKeyStore2.jks";
    private static String CERTPWD_KEYSTORE = "/resources/security/AuditCertPasswordKeystore.jks";
    private static String ENCRYPTION_PKCS12_KEYSTORE = "/resources/security/PKCS12Keystore";
    private static String SIGNING_JCEKS_KEYSTORE = "/resources/security/JCEKSKeystore";
    private static String TYPE_JKS = "JKS";
    private static String TYPE_CMS = "CMS";
    private static String TYPE_PKCS11 = "PKCS11";
    private static String TYPE_JCEKS = "JCEKS";
    private static String TYPE_PKCS12 = "PKCS12";
    private static String KEYSTORE_PASSWORD = "Liberty";

    private static int SUCCESS_RC = 0;
    private static int FAILURE_RC = 1;

    @Rule
    public static TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        final String method = "setUp";
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.audit.reader.fat");
        installRoot = server.getInstallRoot();
        serverRoot = server.getServerRoot();
        server.setKeysAndJVMOptsForFips();
        env = new Properties();
        machine = server.getMachine();

        createAuditLogWithDefaultKSLocation(ENCRYPTION_KEYSTORE2, SIGNING_KEYSTORE, ENCRYPTED_SIGNED_TEMPLATE_LOG, ENCRYPTED_SIGNED_DEFAULTKS_LOG);
        createAuditLogWithDefaultKSLocation(ENCRYPTION_KEYSTORE, null, ENCRYPTED_TEMPLATE_LOG, ENCRYPTED_DEFAULTKS_LOG);
        createAuditLogWithDefaultKSLocation(null, SIGNING_KEYSTORE, SIGNED_TEMPLATE_LOG, SIGNED_DEFAULTKS_LOG);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO0221E"); //Ignore TCP Channel defaultHttpEndpoint initialization error message
        }
    }

    /**
     * SUCCESSFUL tests for auditReader with encrypted and/or signed audit logs.
     */

    /**
     * Test description:
     * Running auditUtility help command results in Usage.
     *
     */
    @Test
    public void testAuditUtility_Help_returnsUsage() throws Exception {
        ProgramOutput po = machine.execute(installRoot + "/bin/auditUtility",
                new String[] {
                        "auditUtility",
                        "help"
                },
                installRoot,
                env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility help command:" + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout());
        assertEquals("auditUtility help auditReader did not result in expected return code.", SUCCESS_RC, po.getReturnCode());
        assertTrue("The expected usage message was not received.",
                po.getStdout().contains("Usage:"));

    }

    /**
     * Test description:
     * Running auditUtility with an unknown command results in Usage.
     *
     */
    @Test
    public void testAuditUtility_UnknownCommand_returnsUsage() throws Exception {
        ProgramOutput po = machine.execute(installRoot + "/bin/auditUtility",
                new String[] {
                        "auditUtility",
                        "unknownCmd"
                },
                installRoot,
                env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:" + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout());
        assertEquals("auditUtility help auditReader did not result in expected return code.", SUCCESS_RC, po.getReturnCode());
        assertTrue("The expected usage message was not received.",
                po.getStdout().contains("Usage:"));

    }

    /**
     * Test description:
     * Running auditUtility help for auditReader results in Usage.
     *
     */
    @Test
    public void testAuditUtility_AuditReaderHelp_returnsUsage() throws Exception {
        ProgramOutput po = machine.execute(installRoot + "/bin/auditUtility",
                new String[] {
                        "help",
                        "auditReader"
                },
                installRoot,
                env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:" + po.getCommand());
        Log.info(thisClass, name.getMethodName(), "Result:\n" + po.getStdout());
        assertEquals("auditUtility help auditReader did not result in expected return code.", SUCCESS_RC, po.getReturnCode());
        assertTrue("The expected usage message was not received.",
                po.getStdout().contains("Usage:"));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully decrypt an encrypted audit log when required encryption parameters
     * are specfied along with a keystore location which differs from that in the audit log.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_Decrypt_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully decrypt an encrypted audit log when required encryption parameters
     * are specified for type and password, but the keystore location defaults to that in the audit log header.
     */
    @Test
    public void testAuditReader_Decrypt_KSLocationDefaults_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_DEFAULTKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_DEFAULTKS_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_DEFAULTKS_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully decrypt an encrypted audit log when required encryption parameters
     * are specified for type and password, but the keystore location on the command differs from that in the log header.
     */
    @Test
    public void testAuditReader_Decrypt_SpecifiedKSPathDiffersFromLogHeader_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_DIFFPATH_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_DIFFPATH_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_DIFFPATH_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully unsign and decrypt a signed and encrypted audit log when required
     * parameters
     * are specified for type and password, but the keystore location defaults to that in the audit log.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_DecryptUnsign_KSLocationDefaultsToLogHeader_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_DEFAULTKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_UNSIGNED_DEFAULTKS_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_UNSIGNED_DEFAULTKS_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command overwrites an output file if that file exists on the file system and is not read-only.
     */
    @Test
    public void testAuditReader_Decrypt_OverwritesExistingOutputFile_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + PREEXISTING_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + PREEXISTING_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully decrypt an encrypted audit log when required encryption parameters
     * are specified along with signed=false.
     */
    @Test
    public void testAuditReader_Decrypt_signFalse_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_NOSIGN_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_SIGNED + "false",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_NOSIGN_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully unsign a signed audit log when required signing parameters
     * are specified with signed=true and encrypted defaulting to false.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_Unsign_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + UNSIGNED_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + UNSIGNED_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully unsign a signed audit log when required signing parameters
     * are specified with signed=true, encrypted=false and keystore location defaulting to that in the audit log header.
     */
    @Test
    public void testAuditReader_Unsign_DefaultKSLocationFromLogHeader_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_DEFAULTKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + UNSIGNED_DEFAULTKS_OUTPUT,
                PARM_ENCRYPTED + "false",
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + UNSIGNED_DEFAULTKS_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully unsign and decrypt an audit log using a keystore location
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_UnsignDecrypt_KSLocationsSpecified_URLHostnameFormat_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_UNSIGNED_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_UNSIGNED_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully unsign and decrypt an audit log when the keystore type differs
     * for the signing and encryption -- unsign with PKCS12 and decrypt with JCEKS.
     */
    @Test
    public void testAuditReader_UnsignJCEKS_DecryptPKCS12_NoKSExtension_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTEDPKCS12_SIGNEDJCEKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTPKCS12_UNSIGNJCEKS_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_JCEKS_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JCEKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_PKCS12_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_PKCS12,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTPKCS12_UNSIGNJCEKS_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully unsign a signed audit log which was generated using compact=true
     * in the config file.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_UnsignCompactLog_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_COMPACT_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + UNSIGNED_COMPACT_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE2,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(serverRoot + UNSIGNED_COMPACT_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command can be used to successfully unsign and decrypt an audit log where the same keystore is used for
     * signing and encryption and the certificate in the keystore is protected with a password. This certificate password is not
     * required on
     * the auditReader command to unsign/decrypt the audit log.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_SameKS_CertPasswordKeystore_Success() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + CERT_PROTECTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + CERT_PROTECTED_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + CERTPWD_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + CERTPWD_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());

        AuditFileStream allEvents = new AuditFileStream(installRoot + "/usr/servers/com.ibm.ws.security.audit.reader.fat" + CERT_PROTECTED_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }

    /**
     * Test description:
     * The auditReader command when issued with signed and encrypted parms on a JSON formated log file (not signed or encrypted),
     * generates an output file
     * which is the same as the input audit log. Note: This test just verifies that the file exists because there is a one byte
     * difference which prevents a file compare from
     * matching.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_LogNotSignedOrEncrypted_WithUnsignDecryptParms_Success() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + REGULAR_JSON_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + REGULAR_JSON_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + CERTPWD_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + CERTPWD_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());
        assertTrue("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(REGULAR_JSON_OUTPUT));
    }

    /**
     * Test description:
     * The auditReader command when issued without signed and encrypted parms on a JSON formated log file (not signed or
     * encrypted), generates an output file
     * which is same as the input file. Note: This test just verifies that the file exists because there is a one byte difference
     * which prevents a file compare from
     * matching.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_LogNotSignedOrEncrypted_WithoutUnsignDecryptParms_Success() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + REGULAR_JSON_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + REGULAR_JSON_OUTPUT2,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", SUCCESS_RC, po.getReturnCode());
        assertTrue("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(REGULAR_JSON_OUTPUT2));

    }

    /**
     * ERROR tests for auditReader with encrypted audit logs.
     */

    /**
     * Test description:
     * An error return code is returned when the auditReader tries to decrypt an encrypted log
     * but the encrypted record cannot be decrypted. Since decryption fails on the first audit record,
     * no output file is produced.
     */
    @Test
    public void testAuditReader_Decrypt_DecryptionFailedFirstRecord_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_MODIFIED_DATA_FIRST_RECORD_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());

        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("The decryption of the auditRecord failed at index [0]."));
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
    }

    /**
     * Test description:
     * An error return code is returned when the auditReader tries to decrypt an encrypted log
     * but the encrypted record cannot be decrypted. Since validation fails on the third audit record,
     * an output file is produced up to the third record (contains the first and second record).
     */
    @Test
    public void testAuditReader_Decrypt_DecryptionFailedThirdRecord_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_MODIFIED_DATA_THIRD_RECORD_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_MODIFIED_DATA_THIRD_RECORD_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());

        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("The decryption of the auditRecord failed at index [2]."));
        assertTrue("Audit Reader output file was not produced.", server.fileExistsInLibertyServerRoot(DECRYPTED_MODIFIED_DATA_THIRD_RECORD_OUTPUT));

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_MODIFIED_DATA_THIRD_RECORD_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        asserts.assertNotFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }

    /**
     * Test description:
     * The auditReader command with a read-only outputFileLocation results in error return code and usage.
     * Note: In some cases this generates and NPE because the FileWriter indicates the file is writable but we cannot
     * open the FileWriter for the file.
     */
    @Test
    public void testAuditReader_Decrypt_ReadOnlyOutputFile_Error() throws Exception {
        File ro = new File(serverRoot + READONLY_OUTPUT);
        ro.setWritable(false);
        Log.info(thisClass, name.getMethodName(), "Is writable file: " + ro.canWrite());
        // File.setWritable is an unreliable JDK method call, and may leave the file as writable after executing.  If
        // the file is writeable, then do not execute the test.   File.setReadOnly() is also similarly unreliable.
        if (!ro.canWrite()) {
            String[] params = new String[] {
                    CMD_AUDIT_READER,
                    PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                    PARM_OUTPUT_FILE_LOCATION + serverRoot + READONLY_OUTPUT,
                    PARM_ENCRYPTED + "true",
                    PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                    PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                    PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
            };
            ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

            Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
            Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
            Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
            assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());        	
        }     	

    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but is missing the required encryption parameters.
     */
    @Test
    public void testAuditReader_Decrypt_MissingEncryptionParms_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("encAudit.log, is encrypted but the --encrypted argument was either not specified or was specified and set to false."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but is missing the required audit file location.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_Decrypt_MissingAuditFileLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_AUDIT_FILE_LOCATION,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: A value is missing for the --auditFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but is missing the required output file location (and keystore defaults to value in the audit log header).
     */
    @Test
    public void testAuditReader_Decrypt_MissingOutputFileLocationParameter_noKS_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --outputFileLocation argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * with all encryption parameters but missing outputFileLocation value.
     */
    @Test
    public void testAuditReader_Decrypt_MissingOutputFileLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: A value is missing for the --outputFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but is missing the required encryption keystore type (keystore defaults to location in audit log header)
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_Decrypt_KSLocationDefaults_MissingKSTypeParameter_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_DEFAULTKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --encKeyStoreType argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but is missing the required keystore password parameter.
     */
    @Test
    public void testAuditReader_Decrypt_KSLocationDefaults_MissingPasswordParam_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_DEFAULTKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,

        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: The --encKeyStorePassword argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but with an non-existing audit log file name.
     */
    @Test
    public void testAuditReader_Decrypt_NonExistingAuditFileLogName_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + NONEXISTING_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("notExistLog.log does not exist."));

    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but with non-existing directory for the audit log file.
     */
    @Test
    public void testAuditReader_Decrypt_NonExistingAuditFileLogDirectory_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + NONEXISTING_DIR,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("/nonExistDir/notExistLog.log does not exist."));

    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but with an empty audit file location value.
     */
    @Test
    public void testAuditReader_Decrypt_EmptyAuditFileLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + "",
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: A value is missing for the --auditFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but is missing the required audit file location parameter.
     */
    @Test
    public void testAuditReader_Decrypt_MissingAuditFileLocationParameter_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("The --auditFileLocation argument is missing"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but is missing the required output file location value.
     */
    @Test
    public void testAuditReader_Decrypt_MissingOutputLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + "",
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: A value is missing for the --outputFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but with bad value for encrypted parameter.
     */
    @Test
    public void testAuditReader_Decrypt_BadValueEncryptedParm_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "yes",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: The yes value is invalid for the --encrypted argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but with non-existing directory and no filename for the output file location.
     */
    @Test
    public void testAuditReader_Decrypt_NonExistingDirOutputLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + "/nonExistingDir/",
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("does not exist"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but non-existing directory and non-existing file name for output location value.
     */
    @Test
    public void testAuditReader_Decrypt_NonExistingDirAndFileOutputLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + "/nonExistingDir/nonExistingFile",
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected exception message was not received.", po.getStdout().contains("java.io.IOException"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but missing the output location parameter.
     */
    @Test
    public void testAuditReader_Decrypt_MissingOutputLocationParameter_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected exception message was not received.", po.getStdout().contains("Error: The --outputFileLocation argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but with a bad keystore password.
     */
    @Test
    public void testAuditReader_Decrypt_BadPassword_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + "badPwd",
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected exception was not received.", po.getStdout().contains("Error: java.io.IOException"));
        assertTrue("The expected error message was not received.", po.getStdout().contains("The password may have been incorrectly specified"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but with missing required keystore password value.
     */
    @Test
    public void testAuditReader_Decrypt_MissingPasswordValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + "",
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: A value is missing for the --encKeyStorePassword argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but missing the keystore password parameter.
     */
    @Test
    public void testAuditReader_Decrypt_MissingPassowrdParameter_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: The --encKeyStorePassword argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for an encrypted log
     * but without the required URL format for the encryption keystore.
     */
    //@Test
    public void testAuditReader_Decrypt_EncryptKeystoreNotURLFormat_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("the --encKeyStoreLocation argument is not in fully-qualified file URL format."));
    }

    /**
     * ERROR tests for auditReader and unsigning
     */

    /**
     * Test description:
     * An error return code is returned when the auditReader tries to read a signed log
     * but the signature does not match the audit record's data. Since validation fails on the first audit record,
     * no output file is produced.
     */
    @Test
    public void testAuditReader_Unsign_SignatureVerificationFailedFirstRecord_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_MODIFIED_DATA_FIRST_RECORD_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());

        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("The validation of the auditRecord signature failed at index [0]."));
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
    }

    /**
     * Test description:
     * An error return code is returned when the auditReader tries to read a signed log
     * but the signature does not match the audit record's data. Since validation fails on the third audit record,
     * an output file is produced up to the third record (contains the first and second record).
     */
    @Test
    public void testAuditReader_Unsign_SignatureVerificationFailedThirdRecord_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_MODIFIED_DATA_THIRD_RECORD_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());

        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("The validation of the auditRecord signature failed at index [2]."));
        assertTrue("Audit Reader output file was not produced.", server.fileExistsInLibertyServerRoot(UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT));

        AuditFileStream allEvents = new AuditFileStream(serverRoot + UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        asserts.assertNotFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }

    /**
     * Test description:
     * An error return code is returned when the auditReader is issued for a signed log
     * but with read-only output file.
     *
     * Note: on some operating systems this may result in a Null pointer exception because the FileWrite indicates
     * the file is writable but when we try to open the file and NPE is generated.
     */
    @Test
    public void testAuditReader_Unsign_ReadOnlyOutputFile_Error() throws Exception {
        File ro = new File(serverRoot + READONLY_OUTPUT);
        ro.setReadOnly();
        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command for file set to read-only. ");
        // File.setWritable is an unreliable JDK method call, and may leave the file as writable after executing.  If
        // the file is writeable, then do not execute the test.   File.setReadOnly() is also similarly unreliable.
        if (!ro.canWrite()) {
            String[] params = new String[] {
                    CMD_AUDIT_READER,
                    PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_LOG,
                    PARM_OUTPUT_FILE_LOCATION + serverRoot + READONLY_OUTPUT,
                    PARM_SIGNED + "true",
                    PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                    PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                    PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
            };
            ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

            Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
            Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
            Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
            assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        }
       	
    }
        
 
    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * with encryption parameters but missing all of the required signing parameters.
     *
     */
    @Test
    public void testAuditReader_Unsign_WithEncryptParms_SignedFalseWithNoParms_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_LOG,
                PARM_ENCRYPTED + "true",
                PARM_SIGNED + "false",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("signedAuditLog, is signed but the --signed argument was either not specified or was specifed and set to false."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the required audit file location value.
     */
    @Test
    public void testAuditReader_Unsign_MissingAuditFileLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_AUDIT_FILE_LOCATION,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: A value is missing for the --auditFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the required output file location parameter (keystore defaults to that in log header).
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_Unsign_MissingOutputFileLocationParameter_noKS_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --outputFileLocation argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the required output file location value.
     */
    @Test
    public void testAuditReader_Unsign_MissingOutputFileLocationValue_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: A value is missing for the --outputFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with audit log file and output file specified as the same audit file name. The audit file should not
     * be overwritten or deleted.
     */
    @Test
    public void testAuditReader_Unsign_OutputFileLocationEqualsAuditFileLocation_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("Audit Reader file was deleted when it should not have been.", server.fileExistsInLibertyServerRoot(ENCRYPTED_LOG));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Both paths must be unique."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the keystore type parameter.
     */
    @Test
    public void testAuditReader_Unsign_KSLocationDefaults_MissingKSTypeParameter_Error() throws Exception {
        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_DEFAULTKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --signingKeyStoreType argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the keystore password parameter.
     */
    @Test
    public void testAuditReader_Unsign_KSLocationDefaults_MissingPasswordParam_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_DEFAULTKS_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,

        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: The --signingKeyStorePassword argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with non-existing audit log file.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_Unsign_NonExistingAuditFileLogName_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + NONEXISTING_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("notExistLog.log does not exist."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with non-existing audit file directory.
     */
    @Test
    public void testAuditReader_Unsign_NonExistingAuditFileLogDirectory_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + NONEXISTING_DIR,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("nonExistDir/notExistLog.log does not exist."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with empty value for audit file location.
     */
    @Test
    public void testAuditReader_Unsign_EmptyAuditFileLocationValue_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + "",
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: A value is missing for the --auditFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the valuefor the signed parameter.
     */
    @Test
    public void testAuditReader_Unsign_MissingValueForSignedParam_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: A value is missing for the --signed argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the required audit file location parameter.
     */
    @Test
    public void testAuditReader_Unsign_MissingAuditFileLocationParameter_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("The --auditFileLocation argument is missing"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the output location value.
     */
    @Test
    public void testAuditReader_Unsign_MissingOutputLocationValue_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + "",
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: A value is missing for the --outputFileLocation argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with non-existing output location value.
     */
    @Test
    public void testAuditReader_Unsign_NonExistingDirOutputLocationValue_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + "/nonExistingDir/",
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("nonExistingDir/ does not exist."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with non-existing directory and output file location value.
     */
    @Test
    public void testAuditReader_Unsign_NonExistingDirAndFileOutputLocationValue_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + "/nonExistingDir/nonExistingFile",
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected exception message was not received.", po.getStdout().contains("Error: java.io.IOException"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the required output location parameter.
     */
    @Test
    public void testAuditReader_Unsign_MissingOutputLocationParameter_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected exception message was not received.", po.getStdout().contains("Error: The --outputFileLocation argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with bad keystore password value.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_Unsign_BadPassword_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + "*",
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected exception was not received.", po.getStdout().contains("Error: java.io.IOException"));
        assertTrue("The expected error message was not received.", po.getStdout().contains("The password may have been incorrectly specified"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the keystore password value.
     */
    @Test
    public void testAuditReader_Unsign_MissingPasswordValue_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: A value is missing for the --signingKeyStorePassword argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but missing the required keystore password parameter.
     */
    @Test
    public void testAuditReader_Unsign_MissingPasswordParameter_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: The --signingKeyStorePassword argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed log
     * but with signing keystore not in the URL format.
     */
    //@Test
    public void testAuditReader_Unsign_SignedKeystoreNotURLFormat_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("the --signingKeyStoreLocation argument is not in fully-qualified file URL format."));
    }

    /**
     * ERROR cases for auditReader with both unsign and decrypt
     */

    /**
     * Test description:
     * An error return code is returned when the auditReader tries to read a signed and encrypted log
     * but the signature does not match the audit record's data. Since validation fails on the first audit record,
     * no output file is produced.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_SignatureVerificationFailedFirstRecord_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_MODIFIED_DATA_FIRST_RECORD_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());

        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("The validation of the auditRecord signature failed at index [0]."));
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
    }

    /**
     * Test description:
     * An error return code is returned when the auditReader tries to read a signed and encrypted log
     * but the signature does not match the audit record's data. Since validation fails on the third audit record,
     * an output file is produced up to the third record (contains the first and second record).
     */
    @Test
    public void testAuditReader_UnsignDecrypt_SignatureVerificationFailedThirdRecord_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_MODIFIED_DATA_THIRD_RECORD_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());

        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.", po.getStdout().contains("The validation of the auditRecord signature failed at index [2]."));
        assertTrue("Audit Reader output file was not produced.", server.fileExistsInLibertyServerRoot(DECRYPTED_UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT));

        AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_UNSIGNED_MODIFIED_DATA_THIRD_RECORD_OUTPUT);
        AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
        asserts.assertFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/start",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        asserts.assertNotFoundInData(
                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                        AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                        AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                        AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditHandler:AuditFileHandler",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),

                asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
                        AuditEvent.OBSERVER_NAME + "=AuditService",
                        AuditEvent.TARGET_TYPEURI + "=service/audit/stop",
                        AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }


    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with bad signing keystore type.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_BadSigningKeyStoreType_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_PKCS11,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The PKCS11 keystore type value for the --signingKeyStoreType argument is not supported."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with empty required encryption keystore type.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_EmptyEncryptionKeyStoreTypeValue_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + "JKS",
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + "",
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: A value is missing for the --encKeyStoreType argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but missing the required signing keystore type parameter.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_MissingSigningKeyStoreTypeParameter_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --signingKeyStoreType argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with bad encryption keystore type.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_BadEncKeyStoreType_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_PKCS11,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The PKCS11 keystore type value for the --encKeyStoreType argument is not supported."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but missing required encryption keystore type.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_MissingEncKeyStoreTypeParameter_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --encKeyStoreType argument is missing."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but missing a value for the required encryption keystore type.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_MissingEncKeyStoreTypeValue_Error() throws Exception {

        Log.info(thisClass, name.getMethodName(), "Running auditUtility auditReader command");
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + "",
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: A value is missing for the --encKeyStoreType argument."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with an extra unknown parameter.
     */
    @Test
    public void testAuditReader_Decrypt_extraUnknownParm_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_UNKNOWN + "unknown",

        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected successful return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.", po.getStdout().contains("Error: The --unknownParm argument is invalid."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with bad value for the keystore type.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_JunkKeyStoreType_Error() throws Exception {
        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + "JUNK",
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + "JUNK",
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: java.lang.Exception: Exception was raised while trying to get an instance of the keystore with the specified keystore type and provider type. JUNK not found"));

    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with the original audit input file name used for audit file and output file location. The original file should
     * not be deleted.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_SameAuditFileLocationAndOutputFile_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("Audit log file was deleted when it should not have been", server.fileExistsInLibertyServerRoot(ENCRYPTED_SIGNED_LOG));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Both paths must be unique."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with the wrong keystore files used for signing and encryption keystores (keystores are swapped).
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_UnsignDecrypt_WrongKeystoresForUnsignDecrypt_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("The auditsigning certificate alias does not exist"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but missing the required encryption parameters.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_UnsignDecrypt_MissingEncryptionParms_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("is encrypted and signed, but either the --signed or --encrypted argument was either not specified or was specified and set to false."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but missing the required encrypted and signed parameters.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_SignedFalse_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "false",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("is encrypted and signed, but either the --signed or --encrypted argument was either not specified or was specified and set to false."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with non-existing audit file location.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_BadAuditFileLocation_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + "/nonExisting.log",
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("nonExisting.log does not exist."));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but missing the required output file location.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_MissingOutputFileLocation_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_SIGNED + "false",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --outputFileLocation argument is missing"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but with bad keystore password for signing keystore.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuditReader_UnsignDecrypt_BadUnsignKeystorePassword_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + "BadPassword",
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,
                PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Keystore was tampered with, or password was incorrect"));
    }

    /**
     * Test description:
     * An error return code and error message is returned when the auditReader is issued for a signed and encrypted log
     * but missing the required keystore password parameter for the encryption keystore.
     */
    @Test
    public void testAuditReader_UnsignDecrypt_MissingDecryptKeystorePassword_Error() throws Exception {

        String[] params = new String[] {
                CMD_AUDIT_READER,
                PARM_AUDIT_FILE_LOCATION + serverRoot + ENCRYPTED_SIGNED_LOG,
                PARM_OUTPUT_FILE_LOCATION + serverRoot + ERROR_OUTPUT,
                PARM_SIGNED + "true",
                PARM_SIGNING_KEYSTORE_LOCATION + serverRoot + SIGNING_KEYSTORE,
                PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
                PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
                PARM_ENCRYPTED + "true",
                PARM_ENCRYPTION_KEYSTORE_LOCATION + serverRoot + ENCRYPTION_KEYSTORE2,
                PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS,

        };
        ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

        Log.info(thisClass, name.getMethodName(), "Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
        Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
        Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
        assertEquals("auditUtility auditReader command did not result in expected failure return code.", FAILURE_RC, po.getReturnCode());
        assertFalse("Audit Reader output file was produced when none was expected", server.fileExistsInLibertyServerRoot(ERROR_OUTPUT));
        assertTrue("The expected error message was not received.",
                po.getStdout().contains("Error: The --encKeyStorePassword argument is missing."));
    }

    private static void createAuditLogWithDefaultKSLocation(String encrKS, String signKS, String logTemplate, String logResult) {
        try {
            Log.info(thisClass, name.getMethodName(), "Creating audit log " + serverRoot + logResult + " from template log " + serverRoot + logTemplate);
            String searchEncKS = "replaceThisStringWithEncKS";
            String searchSignKS = "replaceThisStringWithSigningKS";
            Path logPath = Paths.get(serverRoot + logTemplate);
            Path outPath = Paths.get(serverRoot + logResult);
            Charset charset = StandardCharsets.UTF_8;
            String logContents = new String(Files.readAllBytes(logPath), charset);
            if (encrKS != null) {
                logContents = logContents.replaceAll(searchEncKS, serverRoot + encrKS);
            }
            if (signKS != null) {
                logContents = logContents.replaceAll(searchSignKS, serverRoot + signKS);
            }
            Files.write(outPath, logContents.getBytes(charset));
            if (Files.exists(outPath)) {
                Log.info(thisClass, name.getMethodName(), "Successfully audit log " + serverRoot + logResult + "with keystore entry: " + encrKS + " " + signKS);
            }
        } catch (Exception e) {
            Log.info(thisClass, name.getMethodName(), "Unable to update template log to create keystore location for this OS. ");
        }
    }

    boolean sameContent(Path file1, Path file2) throws IOException {
        final long size = Files.size(file1);
        if (size != Files.size(file2)) {
            Log.info(thisClass, name.getMethodName(), "File sizes are not equal. ");
            return false;
        }

        if (size < 4096) {
            Log.info(thisClass, name.getMethodName(),
                    "File byte compare for below 4096 does not show equal. " + Arrays.equals(Files.readAllBytes(file1), Files.readAllBytes(file2)));
            return Arrays.equals(Files.readAllBytes(file1), Files.readAllBytes(file2));
        }

        try (InputStream is1 = Files.newInputStream(file1);
                InputStream is2 = Files.newInputStream(file2)) {
            // Compare byte-by-byte.
            // Note that this can be sped up drastically by reading large chunks
            // (e.g. 16 KBs) but care must be taken as InputStream.read(byte[])
            // does not neccessarily read a whole array!
            int data;
            while ((data = is1.read()) != -1) {
                if (data != is2.read()) {
                    return false;
                }
            }
        }

        return true;
    }

}
