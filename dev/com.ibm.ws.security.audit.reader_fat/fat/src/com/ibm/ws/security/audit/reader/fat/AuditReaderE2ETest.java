package com.ibm.ws.security.audit.reader.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.AuditFileStream;
import com.ibm.ws.security.audit.fat.common.tooling.AuditMessageConstants;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.TestServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * This test verifies that the auditUtility auditReader command can be used unsign and decrypt a signed and encrypted log
 * which contains a specified number of events.  The auditReader command will use the values in the log header for the 
 * signing keystore and encryption keystore locations.
 *
 * The keystores used in this test are copied over from the /com.ibm.ws.security.audit_fat.common.tooling/securitykeys directory.
 *
 * 1. AuditEncryptionKeyStore2.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 192, signature algorithm SHA256wECDSA
 * encryptioncert
 * Signer Certificate / Public Key (extracted from personal certs)
 * enccert2
 *
 * 2. AuditSigningKeyStore.jks
 * Personal Certificates/ Private Key (version X509 V3, keysize 2048, signature algorithm SHA256wRSA
 * auditsigning
 * Signer Certificates / Public Key (extracted from personal certs)
 * signcert
 *
 *
 *  WARNING WARNING WARNING WARNING WARNING WARNING - IN CASE THIS TEST FAILS
 *  
 *  If this test fails, then it needs to be determined whether the log file format for signed/encrypted logs has changed. If so,
 *  then the pre-generated log files used in the AuditReaderUtilityTest may need to be re-generated.
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditReaderE2ETest extends AuditCommonTest {

	protected static final String CONFIG_SIGN_ENCRYPT_TRUE_DIFF_KEYSTORE_DIFF_ALGS = "audit_encryptSign_diffKS_diffAlgs.xml";

	public static TestServer myTestServer = new TestServer("com.ibm.ws.security.audit.reader.fat.e2e",CONFIG_SIGN_ENCRYPT_TRUE_DIFF_KEYSTORE_DIFF_ALGS);
    
    protected static DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
	private static String USER1 = "user1";
    private static String USER1PWD = "user1pwd";
    private static Boolean RESTART_SERVER = true;
    private static int DEFAULT_DELAY_SECONDS = 20;
    private static final Class<?> thisClass = AuditReaderE2ETest.class;

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
    private static final String PARM_ENCRYPTION_KEYSTORE_TYPE = "--encKeyStoreType=";
    private static final String PARM_SIGNING_KEYSTORE_TYPE = "--signingKeyStoreType=";

    private static String DECRYPTED_UNSIGNED_AUDIT_OUTPUT = "/auditout/encSignedAudit.out";
    private static String TYPE_JKS = "JKS";
    private static String KEYSTORE_PASSWORD = "Liberty";
    private static int SUCCESS_RC = 0;
    
    static List<String> checkApps = new ArrayList<String>();	
	static List<String> startMsgs = new ArrayList<String>();
	static List<String> startFailedMsgs = new ArrayList<String>();
	static List<String> startFailedHandlerMsgs = new ArrayList<String>();
	static List<String> startMsgsWithDefaultKS = new ArrayList<String>();

    public AuditReaderE2ETest() {
        super(myTestServer.getServer(), thisClass);
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
        Log.info(thisClass, "setUp", "Prep for test");
        httpclient = new DefaultHttpClient();
        urlBase = "http://" + myTestServer.getServer().getHostname() + ":" + myTestServer.getHttpDefaultPort();
        
        auditLogFile = myTestServer.getServer().getLogsRoot() + DEFAULT_AUDIT_LOG;
        installRoot = myTestServer.getServer().getInstallRoot();
        serverRoot = myTestServer.getServer().getServerRoot();
        env = new Properties();
        machine = myTestServer.getServer().getMachine();
        
        checkApps.add(AUDIT_BASICAUTH_SERVLET);
        
        // Messages expected for start of audit with good server config
     	startMsgs.add(AuditMessageConstants.CWWKF0012I_AUDIT_FEATURE_INSTALLED);
    	startMsgs.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY);
    	startMsgs.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);
    	startMsgs.add(AuditMessageConstants.CWWKS0008I_SECURITY_SERVICE_READY);
     
    	myTestServer.startServer(CONFIG_SIGN_ENCRYPT_TRUE_DIFF_KEYSTORE_DIFF_ALGS, "AuditReaderE2ETest", checkApps, startMsgs, Constants.JUNIT_REPORTING,null);
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        myTestServer.stopServer();
    }

	/**
	 * Test description: 
	 * This test verifies that when 5 security authentication events are generated and written to a signed and encrypted
	 * audit.log, that the log can be unsigned and decrypted by the auditUtility auditReader command using the default
	 * location for the signing and encryption keystores in the audit log signing and encryption headers.  After unsigning and 
	 * decrypting the audit.log, the same 5 security authentication events should be present in the unsigned and decrypted output log.
	 * 
	 *  WARNING WARNING WARNING WARNING WARNING WARNING - IN CASE THIS TEST FAILS
	 *  
	 *  If this test fails, then it needs to be determined whether the log file format for signed/encrypted logs has changed. If so,
	 *  then the pre-generated log files used in the AuditReaderUtilityTest may need to be re-generated.
	 */
	@Test
	public void testAuditReader_E2E_signedEncryptedAuditLog_Success() throws Exception {

			// Generate 5 SECURITY_AUTHN events in the signed/encrypted audit log
	        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
	        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");
	        for (int i = 0; i < 4; i++) {
	            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
	        }
	        // Delay for writes to complete to the audit.log
	        delay(DEFAULT_DELAY_SECONDS);

	        // Issue auditUtility auditReader command to unsign and decrypt the audit log to a specified output location
			String[] params = new String[] { CMD_AUDIT_READER, PARM_AUDIT_FILE_LOCATION + auditLogFile,
					PARM_OUTPUT_FILE_LOCATION + serverRoot + DECRYPTED_UNSIGNED_AUDIT_OUTPUT, PARM_ENCRYPTED + "true",
					PARM_ENCRYPTION_KEYSTORE_TYPE + TYPE_JKS, PARM_ENCRYPTION_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD,
					PARM_SIGNED + "true", PARM_SIGNING_KEYSTORE_TYPE + TYPE_JKS,
					PARM_SIGNING_KEYSTORE_PASSWORD + KEYSTORE_PASSWORD, };
			ProgramOutput po = machine.execute(installRoot + CMD_AUDIT_UTILITY, params, installRoot, env);

			Log.info(thisClass, name.getMethodName(),
					"Executed auditUtility auditReader command:\n   " + po.getCommand() + Arrays.toString(params));
			Log.info(thisClass, name.getMethodName(), "Return code:  " + po.getReturnCode());
			Log.info(thisClass, name.getMethodName(), "Result:\n   " + po.getStdout());
			assertEquals("auditUtility auditReader command did not result in expected successful return code.",
					SUCCESS_RC, po.getReturnCode());

			// Verify that the unsigned decrypted log contains the expected audit events
			AuditFileStream allEvents = new AuditFileStream(serverRoot + DECRYPTED_UNSIGNED_AUDIT_OUTPUT);
			AuditAsserts asserts = new AuditAsserts(thisClass, allEvents);
					asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=AuditService", AuditEvent.TARGET_TYPEURI + "=service/audit/start",
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
                    asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                            AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                            AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_LTPATOKEN2,
                            AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                            AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                    asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                            AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                            AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_LTPATOKEN2,
                            AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                            AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                    asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                            AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                            AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_LTPATOKEN2,
                            AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                            AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                    asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                            AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                            AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_LTPATOKEN2,
                            AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
                            AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
		}
	}
