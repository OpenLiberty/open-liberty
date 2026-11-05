package com.ibm.ws.security.audit.fat.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.AuditMessageConstants;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.TestServer;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017, 2025
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
/**
 * Test Description:
 * 
 * The test verifies that common configuration errors for the auditFileHandler signing and encryption
 * parameters result in a failure to start the audit service and a message is logged to assist the administrator in diagnosing and
 * fixing the problem.
 */

// Tests below do a reconfigServer with restartServer=true so the server is restarted. 
// The dynamic reconfiguration with signing and encryption will be covered in the audit_fat.features test.

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditSignEncryptConfigErrorTest extends AuditCommonTest {

  
    protected static String CONFIG_SIGN_ENCRYPT_FALSE = "audit_signFalse_encryptFalse.xml";
    protected static String CONFIG_ENCRYPT_BAD_ENCRYPT_ALIAS= "audit_encryptTrue_badEncryptAlias.xml";
    protected static String CONFIG_ENCRYPT_BAD_ENCRYPT_KEYSTORE_REF = "audit_encryptTrue_badEncryptKeystoreRef.xml";
    protected static String CONFIG_ENCRYPT_MISSING_ENCRYPT_ALIAS= "audit_encryptTrue_missingEncryptAlias.xml";
    protected static String CONFIG_SIGN_ENCRYPT_EMPTY_ENCRYPT_ALIAS= "audit_signEncryptTrue_emptyEncryptAlias.xml";
    protected static String CONFIG_ENCRYPT_SIGN_NO_ALIAS_NO_KEYSTORE= "audit_encryptTrue_signTrue_noAliasNoKeystore.xml";
    protected static String CONFIG_SIGN_MISSING_KEYSTORE_REF = "audit_signTrue_missingKeystoreRef.xml";
    protected static String CONFIG_SIGN_EMPTY_KEYSTORE_REF = "audit_signTrue_emptyKeystoreRef.xml";
    protected static String CONFIG_SIGN_BAD_KEYSTORE_PWD= "audit_signTrue_badKeystorePassword.xml";
    protected static String CONFIG_SIGN_PUBLIC_SIGNING_ALIAS= "audit_signTrue_publicSigningAlias.xml";	
    
	public static TestServer myTestServer = new TestServer("com.ibm.ws.security.audit.fat.securelogerror",CONFIG_SIGN_ENCRYPT_FALSE);
    
    protected static Boolean  RESTART_SERVER= true;
    
    protected static Class<?> logClass = AuditSignEncryptConfigErrorTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    
    static List<String> checkApps = new ArrayList<String>();	
	static List<String> startMsgs = new ArrayList<String>();
	static List<String> startingMsgs = new ArrayList<String>();
	static List<String> startFailedMsgs = new ArrayList<String>();

	
    public AuditSignEncryptConfigErrorTest() {
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
  
    	// Messages expected when config error causes audit file handler and collector manager to fail to start
    	startFailedMsgs.add(AuditMessageConstants.CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED);
 //     RTC 261255 - as of 03/19, we are no longer getting this collector manager error CWWKE0700W. The rest of the errors are still generated to tell the user what problem occurred.   	
 //   	startFailedMsgs.add(AuditMessageConstants.CWWKE0700W_AUDIT_COLLECTOR_MANAGER_FAILED);
  	
    	// Failure messages are expected in this test so they should not result in test errors when stopping the server
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKE0700W_AUDIT_COLLECTOR_MANAGER_FAILED);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5807E_AUDIT_HANDLER_INVALID_ENCRYPT_CONFIG);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5808E_AUDIT_HANDLER_INVALID_SIGN_CONFIG);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5809E_AUDIT_HANDLER_ENCRYPT_INIT_FAILED);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5810E_AUDIT_HANDLER_SIGN_INIT_FAILED);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWPKI0033E_KEYSTORE_FAILED_TO_LOAD_ERROR);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWPKI0809W_KEYSTORE_FAILED_TO_LOAD_SSL_CANNOT_INIT);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKG0033W_KEYSTORE_REFERENCE_DOES_NOT_EXIST);
        
    	myTestServer.startServer(CONFIG_SIGN_ENCRYPT_FALSE, "AuditSignEncryptConfigErrorTest", checkApps, startMsgs, Constants.JUNIT_REPORTING,null);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myTestServer.stopServer();
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
     * When audit-1.0 feature is specified with auditFileHandler, encrypt=true and a certificate alias which
     * does not exist (encryptAlias="notExistAlias"),then the audit service does not start and a message is logged to
     * assist the administrator in resolving the problem.
     * 
     * Expected results:
     * - The audit service cannot be started
     * - Messages.log contains message: CWWKS5807E: The audit service cannot start because the audit handler configuration for encrypting audit records is incorrect. 
     *   Configure a correct keystore reference and certificate alias. Ensure the keystore that is referenced by auditEncKeyStore exists and
     *   ensure that the notExistAlias alias name of the certificate that you use to encrypt the audit records is present in the keystore. 
     */
    @Test
    @AllowedFFDC({ "java.security.cert.CertificateException" })
    public void testSignEncryptConfigError_encrypt_nonExistingCertAlias() throws Exception {
    	List<String> startAuditFailureMsgs = new ArrayList<String>();
	    startAuditFailureMsgs.addAll(startFailedMsgs);
	    startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5807E_AUDIT_HANDLER_INVALID_ENCRYPT_CONFIG + ".*notExistAlias");
	    myTestServer.reconfigServer(CONFIG_ENCRYPT_BAD_ENCRYPT_ALIAS, name.getMethodName(), startAuditFailureMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);
    }
    
    /**
     * Test description:
     * When audit-1.0 feature is specified with auditFileHandler, encrypt=true and missing the encryptAlias parameter in config,
     * then the audit feature fails to start and a message is logged to assist the administrator in resolving the problem.
     * 
     * Expected results:
     * - The audit service cannot be started 
     * - Messages.log contains message: CWWKS5807E: The audit service cannot start because the audit handler configuration for encrypting audit records is incorrect.
     *   Configure a correct keystore reference and certificate alias. Ensure the keystore that is referenced by auditEncKeyStore exists and ensure that the null alias
     *   name of the certificate that you use to encrypt the audit records is present in the keystore. 
     */
    //TODO: Future- add a new message to indicate that an alias was not specified rather than null. Enable test at that point.
	@AllowedFFDC({ "java.security.KeyStoreException" })
//	@Test
	public void testSignEncryptConfigError_encrypt_missingCertAlias() throws Exception {
		List<String> startAuditFailureMsgs = new ArrayList<String>();
		startAuditFailureMsgs.addAll(startFailedMsgs);
		startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5807E_AUDIT_HANDLER_INVALID_ENCRYPT_CONFIG + ".*null alias");
		myTestServer.reconfigServer(CONFIG_ENCRYPT_MISSING_ENCRYPT_ALIAS, name.getMethodName(), startAuditFailureMsgs,RESTART_SERVER, Constants.JUNIT_REPORTING);
	}
    
    /**
     * Test description:
     * When audit-1.0 feature is specified with auditFileHandler, encrypt=true, sign=true and an empty value for the certificate alias (encryptAlias=""),
     * then the audit feature fails to start and a message is logged to assist the administrator in resolving the problem.
     * 
     * Expected results:
     * - The audit service cannot be started 
     * - Messages.log contains message: CWWKS5807E: The audit service cannot start because the audit handler configuration for encrypting audit records is incorrect. 
     *  Configure a correct keystore reference and certificate alias. Ensure the keystore that is referenced by auditEncKeyStore exists and ensure that
     *  the  alias name of the certificate that you use to encrypt the audit records is present in the keystore.  
     */ 
	@AllowedFFDC({ "java.security.KeyStoreException" })
	@Test
	public void testSignEncryptConfigError_signEncrypt_emptyCertAlias() throws Exception {
		List<String> startAuditFailureMsgs = new ArrayList<String>();
		startAuditFailureMsgs.addAll(startFailedMsgs);
		startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5807E_AUDIT_HANDLER_INVALID_ENCRYPT_CONFIG + ".*auditEncKeyStore");
		myTestServer.reconfigServer(CONFIG_SIGN_ENCRYPT_EMPTY_ENCRYPT_ALIAS, name.getMethodName(), startAuditFailureMsgs,RESTART_SERVER, Constants.JUNIT_REPORTING);
	}
	
    /**
     * Test description:
     * When audit-1.0 feature is specified with auditFileHandler, encrypt=true and a non existing name for the keystore reference (encryptKeyStoreRef="notExistRef"),
     * then the audit feature fails to start and a message is logged to assist the administrator in resolving the problem.
     * 
     * Expected results:
     * - The audit service cannot be started
     * - Messages.log contains the message: CWWKS5809E: The audit service cannot start because a failure occurred while the service initialized the audit handler to encrypt audit records.
     *   The exception is The keystore [notExistRef] is not present in the configuration.
     *   
     *   Recommended message update
     *   CWWKS5809E: The audit service cannot start because a failure occurred while the service initialized the audit handler to encrypt audit records.
     *   Check the audit file handler configuration to ensure the keystore that is referenced exists and ensure that the alias name of the certificate that is used to encrypt the audit records
     *   is present in the keystore. Refer to the following exception for more information on the error. The exception is: The keystore [notExistRef] is not present in the configuration.
     *   
     */
    @AllowedFFDC({ "java.security.KeyStoreException" })
    @Test
    public void testSignEncryptConfigError_encrypt_badKeystoreRef() throws Exception {
	    List<String> startAuditFailureMsgs = new ArrayList<String>();
	    startAuditFailureMsgs.addAll(startFailedMsgs);
	    startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5809E_AUDIT_HANDLER_ENCRYPT_INIT_FAILED);
	    myTestServer.reconfigServer(CONFIG_ENCRYPT_BAD_ENCRYPT_KEYSTORE_REF, name.getMethodName(), startAuditFailureMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);
    }
    
    /**
     * Test description:
     * When audit-1.0 feature is specified with auditFileHandler, encrypt=true, sign=true and no values for keystore or certificate alias for
     * signing or encryption, then the audit feature fails to start and a message is logged to assist the administrator in resolving the problem.
     * 
     * Expected results:
     * - The audit service cannot be started 
     * - Messages.log contains the message: CWWKS5809E: The audit service cannot start because a failure occurred while the service initialized the audit handler to encrypt audit records.
     *   The exception is The keystore [null] is not present in the configuration.
     *   
     *   Recommended message update
     *   CWWKS5809E: The audit service cannot start because a failure occurred while the service initialized the audit handler to encrypt audit records.
     *   Check the audit file handler configuration to ensure the keystore that is referenced exists and ensure that the alias name of the certificate that is used to encrypt the audit records
     *   is present in the keystore. Refer to the following exception for more information on the error.  The exception is: The keystore [null] is not present in the configuration.
     *   
     */
    @AllowedFFDC({ "java.security.KeyStoreException" })
    @Test
    public void testSignEncryptConfigError_signEncrypt_missingCertAndKeystore() throws Exception {
		List<String> startAuditFailureMsgs = new ArrayList<String>();
		startAuditFailureMsgs.addAll(startFailedMsgs);
		startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5809E_AUDIT_HANDLER_ENCRYPT_INIT_FAILED );
		myTestServer.reconfigServer(CONFIG_ENCRYPT_SIGN_NO_ALIAS_NO_KEYSTORE, name.getMethodName(), startAuditFailureMsgs, RESTART_SERVER, Constants.JUNIT_REPORTING);
    }
   
	/**
	* Test description:
	* When audit-1.0 feature is specified with auditFileHandler, sign=true and a certificate alias of a signer certificate (public key)
	* rather than that of the Personal certificate (private key), then the audit feature fails to start and a message is logged to assist the administrator in resolving the problem.
	* 
	* Expected results:
	* - The audit service cannot be started 
	* - Messages.log contains the message: CWWKS5808E: The audit service cannot start because the audit handler configuration for signing audit records is incorrect. 
	*   Configure a correct keystore reference and personal certificate alias. Ensure the keystore that is referenced by auditSignKeyStore exists and ensure that
	*   the signcert alias name of the personal certificate that you use to sign the audit records is present in the keystore. 
	*/
	@AllowedFFDC({ "java.security.cert.CertificateException" })
	@Test
	public void testSignEncryptConfigError_sign_publicSigningAlias() throws Exception {
		List<String> startAuditFailureMsgs = new ArrayList<String>();
		startAuditFailureMsgs.addAll(startFailedMsgs);
		startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5808E_AUDIT_HANDLER_INVALID_SIGN_CONFIG + ".*signcert" );
		myTestServer.reconfigServer(CONFIG_SIGN_PUBLIC_SIGNING_ALIAS, name.getMethodName(), startAuditFailureMsgs, true, Constants.JUNIT_REPORTING);
	}
	
	/**
	* Test description:
	* When audit-1.0 feature is specified with auditFileHandler, sign=true and an empty keystore reference (signerKeyStoreRef=""),
	* then the audit feature fails to start and a message is logged to assist the administrator in resolving the problem.
	* 
	* Expected results:
	* - The audit service cannot be started 
	* - Messages.log contains the message: CWWKS5810E: The audit service cannot start because a failure occurred while the service 
	*   initialized the audit handler to sign audit records.  The exception is The keystore [] is not present in the configuration.
	*   
	*   Recommended message update:
	*   
	*   CWWKS5810E: The audit service cannot start because a failure occurred while the service initialized the audit handler to sign audit records.
     *  Check the audit file handler configuration to ensure the keystore that is referenced exists and ensure that the alias name of the certificate that is used to sign the audit records
     *  is present in the keystore. Refer to the following exception for more information on the error.  The exception is: The keystore [] is not present in the configuration.
	*/
   	@Test
	@AllowedFFDC({ "java.security.KeyStoreException" })
	public void testSignEncryptConfigError_sign_emptyKeystoreRef() throws Exception {
		List<String> startAuditFailureMsgs = new ArrayList<String>();
		startAuditFailureMsgs.addAll(startFailedMsgs);
		startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5810E_AUDIT_HANDLER_SIGN_INIT_FAILED );
		myTestServer.reconfigServer(CONFIG_SIGN_EMPTY_KEYSTORE_REF, name.getMethodName(), startAuditFailureMsgs, true, Constants.JUNIT_REPORTING);
	}
	
	/**
	* Test description:
	* When audit-1.0 feature is specified with auditFileHandler, sign=true and missing the keystore reference parameter,
	* then the audit feature fails to start and a message is logged to explain how to diagnose and fix the problem.
	* 
	* Expected results:
	* - The audit service cannot be started 
	* - JPKI message CWPKI0033E is logged indicating that The keystore located at
	*   C:/Users/IBM_ADMIN/libertyGit/WS-CD-Open/dev/build.image/wlp/usr/servers/com.ibm.ws.security.audit.fat.securelogerror/resources/security/AuditSigningKeyStore.jks 
	*   did not load because of the following error: Keystore was tampered with, or password was incorrect
	*   
	* - Messages.log contains message: CWWKS5810E: The audit service cannot start because a failure occurred while the service initialized
	*   the audit handler to sign audit records. The exception is The keystore [] is not present in the configuration.
	*   
	*  Recommended message update. 
	*   
	*  CWWKS5810E: The audit service cannot start because a failure occurred while the service initialized the audit handler to sign audit records.
    *  Check the audit file handler configuration to ensure the keystore that is referenced exists and ensure that the alias name of the certificate that is used to sign the audit records
    *  is present in the keystore. Refer to the following exception for more information on the error.  The exception is: The keystore [] is not present in the configuration.
	*/
	@Test
	@AllowedFFDC({ "java.security.KeyStoreException" })
	public void testSignEncryptConfigError_sign_missingKeystoreRef() throws Exception {
		List<String> startAuditFailureMsgs = new ArrayList<String>();
		startAuditFailureMsgs.addAll(startFailedMsgs);
		startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5810E_AUDIT_HANDLER_SIGN_INIT_FAILED );
		myTestServer.reconfigServer(CONFIG_SIGN_MISSING_KEYSTORE_REF, name.getMethodName(), startAuditFailureMsgs, true, Constants.JUNIT_REPORTING);
	}
	
	/**
	* Test description:
	* When audit-1.0 feature is specified with auditFileHandler, sign=true, a keystore reference, but a bad keystore password,
	* then the audit feature fails to start and a message is logged to explain how to diagnose and fix the problem.
	* 
	* Expected results:
	* - The audit service cannot be started 
	* - Messages.log contains the message: CWWKS5810E: The audit service cannot start because a failure occurred while the service 
	*  initialized the audit handler to sign audit records. The exception is The keystore [auditSignKeyStore] is not present in the configuration.
	*  
	*  Recommended message update. 
	*   
	*  CWWKS5810E: The audit service cannot start because a failure occurred while the service initialized the audit handler to sign audit records.
    *  Check the audit file handler configuration to ensure the keystore that is referenced exists and ensure that the alias name of the certificate that is used to sign the audit records
    *  is present in the keystore. Refer to the following exception for more information on the error.  The exception is: The keystore [auditSignKeyStore] is not present in the configuration.
	*/
	@Test
	@AllowedFFDC({ "java.security.KeyStoreException" })
	public void testSignEncryptConfigError_sign_badKeystorePassword() throws Exception {
		List<String> startAuditFailureMsgs = new ArrayList<String>();
		startAuditFailureMsgs.addAll(startFailedMsgs);
		startAuditFailureMsgs.add(AuditMessageConstants.CWWKS5810E_AUDIT_HANDLER_SIGN_INIT_FAILED );
		myTestServer.reconfigServer(CONFIG_SIGN_BAD_KEYSTORE_PWD, name.getMethodName(), startAuditFailureMsgs, true, Constants.JUNIT_REPORTING);
	}
}
