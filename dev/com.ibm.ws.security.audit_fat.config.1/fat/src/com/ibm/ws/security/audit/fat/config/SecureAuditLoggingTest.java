/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.config.utils.TestableAuditLogs;
import com.ibm.ws.security.audit.fat.config.utils.TestableSecureAuditLog;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
/**
 * Test Description:
 * 
 * The test verifies that the secure (signed and/or encrypted) audit logs are the right size, right number, and contain the correct headers and
 * number of audit records when various values are specified for the  maxFileSize, maxFiles and logDirectory configuration parameters.
 * 
 *  
 * The keystores used in this test are copied over from the /com.ibm.ws.security.audit_fat.common.tooling/securitykeys directory. 
 *
 * 1. AuditEncryptionKeyStore.jks
 * 	  Personal Certificates/ Private Key (version X509 V3, keysize 2048, signature algorithm SHA256wRSA
 * 		auditencryption
 * 	    auditsigning
 *    Signer Certificates / Public Key (extracted from personal certs)		
 * 	    enccert
 * 	    signcert
 * 
 * 2. AuditSigningKeyStore2.jks
 * 	  Personal Certificates/ Private Key (version X509 V3, keysize 192, signature algorithm SHA256wECDSA
 * 		auditsigning2
 *    Signer Certificate / Public Key (extracted from personal certs)		
 * 	    signcert2
 * 
 */

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class SecureAuditLoggingTest extends AuditCommonTest {
    private static String MAX_FILES_ZERO_ENCRYPT_CONFIG_FILE = "/maxFilesZeroEncrypt.xml";
    private static String MAX_FILES_ZERO_SIGN_CONFIG_FILE = "/maxFilesZeroSign.xml";
    private static String MAX_FILES_ZERO_SIGN_ENCRYPT_CONFIG_FILE = "/maxFilesZeroSignEncrypt.xml";
    private static String MAX_FILESIZE_ZERO_ENCRYPT_CONFIG_FILE = "/maxFileSizeZeroEncrypt.xml";
    private static String MAX_FILESIZE_ZERO_SIGN_CONFIG_FILE = "/maxFileSizeZeroSign.xml";
    private static String MAX_FILESIZE_ZERO_SIGN_ENCRYPT_CONFIG_FILE = "/maxFileSizeZeroSignEncrypt.xml";
    private static String MAX_FILESIZE_ONE_MAXFILES_ONE_ENCRYPT_CONFIG_FILE = "/maxFileSizeOneMaxFilesOneEncrypt.xml";
    private static String MAX_FILESIZE_ONE_MAXFILES_ONE_SIGN_CONFIG_FILE = "/maxFileSizeOneMaxFilesOneSign.xml";
    private static String MAX_FILESIZE_ONE_MAXFILES_ONE_SIGN_ENCRYPT_CONFIG_FILE = "/maxFileSizeOneMaxFilesOneSignEnc.xml";
    private static String MAX_FILESIZE_TWO_MAXFILES_THREE_ENCRYPT_CONFIG_FILE = "/maxFileSize2MaxFiles3Encrypt.xml";
    private static String MAX_FILESIZE_TWO_MAXFILES_THREE_SIGN_CONFIG_FILE = "/maxFileSize2MaxFiles3Sign.xml";
    private static String MAX_FILESIZE_TWO_MAXFILES_THREE_SIGN_ENCRYPT_CONFIG_FILE = "/maxFileSize2MaxFiles3SignEnc.xml";
    private static String MAX_FILESIZE_TWO_MAXFILES_NONE_SIGN_ENCRYPT_CONFIG_FILE = "/maxFileSize2MaxFilesNoneSignEnc.xml";
    private static String SECURE_LOG_DIRECTORY_CONFIG_FILE = "/secureLogDirectory.xml";
    
    private static Long ONE_MB = 1024L * 1024L;
    private  Long TWO_MB = ONE_MB * 2;
    
    private static int DEFAULT_DELAY_SECONDS = 3;

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.audit.fat.securelogging");
    private static Class<?> myLogClass = SecureAuditLoggingTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;

    public SecureAuditLoggingTest() {
        super(myServer, myLogClass);
    }

    // TestName should be an instance level Rule, not a ClassRule, but static references are made to it,
    // so we will create it as a static field, then keep a reference to it from a non-static field which
    // is annotated with @Rule - junit can make the test method name changes to that field, which should
    // (hopefully) be reflected in the static references as well.
    private static TestName _name = new TestName();

    @Rule
    public TestName name = _name;
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(myLogClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(myLogClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };


    @BeforeClass
    public static void setUp() throws Exception {
        
        Log.info(myLogClass, "setUp", "Prep for test");
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        auditLogFile = myServer.getLogsRoot() + DEFAULT_AUDIT_LOG;

    }

    @AfterClass
    public static void tearDown() throws Exception {
    	// expect an error when trying to delete an invalid directory path
        myServer.stopServer();
    }
    
    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() throws Exception {
        httpclient.getConnectionManager().shutdown();
        myServer.stopServer();
    }

     
    /**
     * Test Purpose: Verify that we have successfully generated a signed and encrypted audit log to the non-default valid configured log directory path.
     **/
    @Test
    public void testSecureLogging_validate_log_directory_config_goodPath() throws Exception {
    	myServer.stopServer();

        myServer.setServerConfigurationFile(SECURE_LOG_DIRECTORY_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        // verify there are no audit logs in the default log directory but there are logs in the configured logs directory
        TestableAuditLogs<TestableSecureAuditLog> secureDefaultLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
        secureDefaultLogs.ensureLogsNotExist();
        
        Path dir = Paths.get(server.getLogsRoot() + "/secureLogsPath");
        TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer,dir);
        secureLogs.ensureLogsExist();
        
        int totalAuditRecordsCount = 0;
        for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
        	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
        	auditFile.verifyLogSizeLimit(ONE_MB);
        	auditFile.ensureHeaderFound(SIGNING_SECTION);
        	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
        	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
        	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
        	totalAuditRecordsCount += auditFile.getSectionCount(AUDIT_RECORD).get(AUDIT_RECORD);
        }
		assertTrue("Expected multiple auditRecords in the log, but found " + totalAuditRecordsCount , totalAuditRecordsCount >= 1);
    }
 
    /**
     * Test Purpose: Verify that we when logs are encrypted and we specify maxFiles=0 in the server.xml, which is by default, that
     * there is no limit on the number of log files generated (infinite) and that maxFileSize=1 results in rollover value of 1MB.
     *     
     *  Verify that header for encryption is present in each log and that the  encryption header reflects the configuration.
     *  Verify that we have multiple audit logs of 1MB size and a total of 4001 audit records across the logs.   
     **/
    @Mode(TestMode.LITE)
    @Test
    public void testSecureLogging_encrypt_validate_maxFilesZero_maxFileSizeOne_resultsInUnlimitedLogsOfOneMB() throws Exception {
    	
        myServer.setServerConfigurationFile(MAX_FILES_ZERO_ENCRYPT_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);
        
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");
        for (int i = 0; i < 4000; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        }
        sleep(DEFAULT_DELAY_SECONDS * 40);
        
        TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
        secureLogs.ensureLogsExist();
    
        int totalAuditRecordsCount = 0;
        for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
        	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
        	auditFile.verifyLogSizeLimit(ONE_MB);
        	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
        	auditFile.ensureHeaderNotFound(SIGNING_SECTION);
        	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
        	totalAuditRecordsCount += auditFile.getSectionCount(AUDIT_RECORD).get(AUDIT_RECORD);
        }
		assertEquals("Expected 4001 auditRecords across all audit logs.  Instead found: " + totalAuditRecordsCount, 4001, totalAuditRecordsCount); 

    }

    /**
     * Test Purpose: Verify that we when logs are encrypted and we specify maxFiles=0 in the server.xml, which is by default, that
     * there is no limit on the number of log files generated (infinite) and that maxFileSize=1 results in rollover value of 1MB.
     *   
     * Verify that header for signing is present in each log and that the  signing header reflects the configuration.
     *  Verify that we have multiple audit logs of 1MB size and a total of 4001 audit records across the logs.   
     **/
    @Test
    public void testSecureLogging_sign_maxFilesZero_maxFileSizeOne_resultsInUnlimitedLogsOfOneMB() throws Exception {
    	
        myServer.setServerConfigurationFile(MAX_FILES_ZERO_SIGN_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");
        for (int i = 0; i < 4000; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        }
        sleep(DEFAULT_DELAY_SECONDS * 40);
        
        TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
        secureLogs.ensureLogsExist();
    
        int totalAuditRecordsCount = 0;
        
        for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
        	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
        	auditFile.verifyLogSizeLimit(ONE_MB);
        	auditFile.ensureHeaderFound(SIGNING_SECTION);
        	auditFile.ensureHeaderNotFound(ENCRYPTION_SECTION);
        	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
        	totalAuditRecordsCount += auditFile.getSectionCount(AUDIT_RECORD).get(AUDIT_RECORD);
        }
		assertEquals("Expected 4001 auditRecords across all audit logs.  Instead found: " + totalAuditRecordsCount, 4001, totalAuditRecordsCount); 
    }

    /**
     * Test Purpose: Verify that we when logs are signed and encrypted with maxFiles=0 in the server.xml, indicating that
     * there is no limit on the number of log files generated (infinite), and with maxFileSize=1 (rollover at 1MB), then we have
     * multiple 1MB logs generated.
     *   
     * Verify that header for signing is present in each log and that the  signing header reflects the configuration.
     *  Verify that we have multiple audit logs of 1MB size and a total of 4001 audit records across the logs.   
     **/
    @Test
    public void testSecureLogging_signEncrypt_maxFilesZero_maxFileSizeOne_resultsInUnlimitedLogsOfOneMB() throws Exception {
    	
        myServer.setServerConfigurationFile(MAX_FILES_ZERO_SIGN_ENCRYPT_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");
        for (int i = 0; i < 4000; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        }
        sleep(DEFAULT_DELAY_SECONDS * 40);
        
        TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
        secureLogs.ensureLogsExist();
    
        int totalAuditRecordsCount = 0;
        
        for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
        	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
        	auditFile.verifyLogSizeLimit(ONE_MB);
        	auditFile.ensureHeaderFound(SIGNING_SECTION);
        	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
        	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
        	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
        	totalAuditRecordsCount += auditFile.getSectionCount(AUDIT_RECORD).get(AUDIT_RECORD);
        }
		assertEquals("Expected 4001 auditRecords across all audit logs.  Instead found: " + totalAuditRecordsCount, 4001, totalAuditRecordsCount); 
    }

    /**
     * Test Purpose: Verify that we when we  encrypt and specify maxFileSize=0 in the server.xml, which indicates unlimited size, that
     * it has no impact on the number of audit logs generated, that we keep adding to one audit log, and we do not rollover any audit logs.
     * 
     *  Verify that header for encryption is present in the log and that the  encryption header reflects the configuration.
     *  Verify that we have 1 audit log with 2001 auditRecords for successful AUTHN.   
     **/
    @Test
    public void testSecureLogging_encrypt_maxFileSizeZero_resultsInOneUnlimitedLog() throws Exception {
    	
        myServer.setServerConfigurationFile(MAX_FILESIZE_ZERO_ENCRYPT_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        for (int i = 0; i < 2000; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        }
        sleep(DEFAULT_DELAY_SECONDS * 20);
    
        TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
        secureLogs.verifyLogsCount(1);
    
        for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
        	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
        	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
        	auditFile.ensureHeaderNotFound(SIGNING_SECTION);
        	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
         	auditFile.verifyAuditRecordCount(2001);
        }
    }

    /**
     * Test Purpose: Verify that we when we sign and specify maxFileSize=0 in the server.xml, which indicates unlimited size, that
     * it has no impact on the number of audit logs generated, that we keep adding to one audit log, and we do not rollover any audit logs.
     *      
     *  Verify that header for signing is present in the log and that the signing header reflects the configuration.
     *  Verify that we have 1 audit log with 2001 auditRecords for successful AUTHN.   
     **/
    @Test
    public void testSecureLogging_sign_maxFileSizeZero_resultsInOneUnlimitedLog() throws Exception {
    	
        myServer.setServerConfigurationFile(MAX_FILESIZE_ZERO_SIGN_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        for (int i = 0; i < 2000; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        }
        sleep(DEFAULT_DELAY_SECONDS * 20);
    
        TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
        secureLogs.verifyLogsCount(1);
    
        for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
        	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
        	auditFile.ensureHeaderFound(SIGNING_SECTION);
        	auditFile.ensureHeaderNotFound(ENCRYPTION_SECTION);
        	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
         	auditFile.verifyAuditRecordCount(2001);
        }
    }
    
    /**
     * Test Purpose: Verify that we when we sign and encrypt plus specify maxFileSize=0 in the server.xml, which indicates unlimited size, that
     * it has no impact on the number of audit logs generated, that we keep adding to one audit log, and we do not rollover any audit logs.
     *      
     *  Verify that header for signing and encryption are present in the log and that the signing and encryption headers reflect the configuration.
     *  Verify that we have 1 audit log with 2001 auditRecords for successful AUTHN.    
     **/
    @Test
    public void testSecureLogging_signEncrypt_maxFileSizeZero_resultsInOneUnlimitedLog() throws Exception {
    	
        myServer.setServerConfigurationFile(MAX_FILESIZE_ZERO_SIGN_ENCRYPT_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);


        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        for (int i = 0; i < 2000; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        }
        sleep(DEFAULT_DELAY_SECONDS * 20);
    
        TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
        secureLogs.verifyLogsCount(1);
    
        for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
        	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
        	auditFile.ensureHeaderFound(SIGNING_SECTION);
        	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
        	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
        	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
         	auditFile.verifyAuditRecordCount(2001);
        }
    }
  
  /**
   * Test Purpose: Verify that we when we encrypt and specify maxFileSize=1 and maxFiles=1 in the server.xml that a single audit
   * log of 1MB is created and used for logging.
   *      
   **/
  @Mode(TestMode.LITE)
  @Test
  public void testSecureLogging_encrypt_validate_maxFileSizeOne_MaxFilesOne_resultsInSingleLog() throws Exception {
	
      myServer.setServerConfigurationFile(MAX_FILESIZE_ONE_MAXFILES_ONE_ENCRYPT_CONFIG_FILE);
      myServer.startServer(true);
      myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
      verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

      String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

      for (int i = 0; i < 2000; i++) {
          response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      }
      sleep(DEFAULT_DELAY_SECONDS * 20);
      
      TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
      secureLogs.verifyLogsCount(1);

    for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
    	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
    	auditFile.ensureHeaderNotFound(SIGNING_SECTION);
    	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
    	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
     	auditFile.ensureLogContainsCompleteAuditRecords();
    }
  }
  
  /**
   * Test Purpose: Verify that we when we sign and specify maxFileSize=1 and maxFiles=1 in the server.xml that a single audit
   * log of 1MB is created and used for logging.
   *      
   **/
  @Mode(TestMode.LITE)
  @Test
  public void testSecureLogging_sign_validate_maxFileSizeOne_MaxFilesOne_resultsInSingleLog() throws Exception {
	
      myServer.setServerConfigurationFile(MAX_FILESIZE_ONE_MAXFILES_ONE_SIGN_CONFIG_FILE);
      myServer.startServer(true);
      myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
      verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

      String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

      for (int i = 0; i < 2000; i++) {
          response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      }
      sleep(DEFAULT_DELAY_SECONDS * 20);
      
      TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
      secureLogs.verifyLogsCount(1);

    for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
    	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
    	auditFile.ensureHeaderFound(SIGNING_SECTION);
    	auditFile.ensureHeaderNotFound(ENCRYPTION_SECTION);
    	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
    	auditFile.ensureLogContainsCompleteAuditRecords();
    }
  }
  
  /**
   * Test Purpose: Verify that we when we sign and encrypt and specify maxFileSize=1 and maxFiles=1 in the server.xml that a single audit
   * log of 1MB is created and used for logging.
   *      
   **/
  @Test
  public void testSecureLogging_signEncrypt_validate_maxFileSizeOne_MaxFilesOne_resultsInSingleLog() throws Exception {
	
      myServer.setServerConfigurationFile(MAX_FILESIZE_ONE_MAXFILES_ONE_SIGN_ENCRYPT_CONFIG_FILE);
      myServer.startServer(true);
      myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
      verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

      String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

      for (int i = 0; i < 2000; i++) {
          response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      }
      sleep(DEFAULT_DELAY_SECONDS * 20);
      
      TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
      secureLogs.verifyLogsCount(1);

    for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
    	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
    	auditFile.ensureHeaderFound(SIGNING_SECTION);
    	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
    	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
    	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
     	auditFile.ensureLogContainsCompleteAuditRecords();
    }
  }
  //=====================
  /**
   * Test Purpose: Verify that we when we encrypt and specify maxFileSize=2 and maxFiles=3 in the server.xml that three audit logs
   * of 2MB are created and used for logging.
   *      
   **/
  @Test
  public void testSecureLogging_encrypt_validate_maxFileSizeTwo_MaxFilesThree_resultsInThreeLogs() throws Exception {
	
      myServer.setServerConfigurationFile(MAX_FILESIZE_TWO_MAXFILES_THREE_ENCRYPT_CONFIG_FILE);
      myServer.startServer(true);
      myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
      verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

      String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

      for (int i = 0; i < 6000; i++) {
          response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      }
      sleep(DEFAULT_DELAY_SECONDS * 30);
      
      TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
      secureLogs.verifyLogsCount(3);

    for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
    	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
    	auditFile.verifyLogSizeLimit(TWO_MB);
    	auditFile.ensureHeaderNotFound(SIGNING_SECTION);
    	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
    	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
     	auditFile.ensureLogContainsCompleteAuditRecords();
    }
  }
  
  /**
   * Test Purpose: Verify that we when we sign and specify maxFileSize=2 and maxFiles=3 in the server.xml that three audit logs
   * of 2MB are created and used for logging.
   *      
   **/
  @Test
  public void testSecureLogging_sign_validate_maxFileSizeTwo_MaxFilesThree_resultsInThreeLogs() throws Exception {
	
      myServer.setServerConfigurationFile(MAX_FILESIZE_TWO_MAXFILES_THREE_SIGN_CONFIG_FILE);
      myServer.startServer(true);
      myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
      verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

      String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

      for (int i = 0; i < 6000; i++) {
          response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      }
      sleep(DEFAULT_DELAY_SECONDS * 30);
      
      TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
      secureLogs.verifyLogsCount(3);

    for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
    	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
    	auditFile.verifyLogSizeLimit(TWO_MB);
    	auditFile.ensureHeaderFound(SIGNING_SECTION);
    	auditFile.ensureHeaderNotFound(ENCRYPTION_SECTION);
    	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
    	auditFile.ensureLogContainsCompleteAuditRecords();
    }
  }
  
  /**
   * Test Purpose: Verify that we when we sign and encrypt and specify maxFileSize=2 and maxFiles=3 in the server.xml that three audit logs
   * of 2MB are created and used for logging.
   *      
   **/
  @Test
  public void testSecureLogging_signEncrypt_validate_maxFileSizeTwo_MaxFilesThree_resultsInThreeLogs() throws Exception {
	
      myServer.setServerConfigurationFile(MAX_FILESIZE_TWO_MAXFILES_THREE_SIGN_ENCRYPT_CONFIG_FILE);
      myServer.startServer(true);
      myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
      verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

      String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

      for (int i = 0; i < 6000; i++) {
          response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      }
      sleep(DEFAULT_DELAY_SECONDS * 30);
      
      TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);
      secureLogs.verifyLogsCount(3);

    for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
    	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
    	auditFile.verifyLogSizeLimit(TWO_MB);    	
    	auditFile.ensureHeaderFound(SIGNING_SECTION);
    	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
    	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
    	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
     	auditFile.ensureLogContainsCompleteAuditRecords();
    }
  }
  
  /**
   * Test Purpose: Verify that we when we sign and encrypt and specify maxFileSize=2 and no maxiFiles in the server.xml that we generate
   * multiple logs of 2MB for a full set of logs and those logs contain the appropriate headers.are created and used for logging as well as
   * the beginning and ending markers for each audit record-- <auditRecord> </auditRecord>
   **/
  @Test
  public void testSecureLogging_signEncrypt_validate_maxFileSizeTwo_NoMaxFiles_resultsInMultipleLogs() throws Exception {
	
      myServer.setServerConfigurationFile(MAX_FILESIZE_TWO_MAXFILES_NONE_SIGN_ENCRYPT_CONFIG_FILE);
      myServer.startServer(true);
      myServer.addInstalledAppForValidation(AUDIT_BASICAUTH_SERVLET);
      verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

      String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

      for (int i = 0; i < 3000; i++) {
          response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
      }
      sleep(DEFAULT_DELAY_SECONDS * 30);
      
      TestableAuditLogs<TestableSecureAuditLog> secureLogs = new TestableAuditLogs<TestableSecureAuditLog>(TestableSecureAuditLog.class, myServer);

    for (TestableSecureAuditLog auditFile : secureLogs.getLogs()) {
    	Log.info(myLogClass, name.getMethodName(), "\n=============== Checking values in log file " + auditFile.getLogFileName() + "======\n");
    	auditFile.verifyLogSizeLimit(TWO_MB);    	
    	auditFile.ensureHeaderFound(SIGNING_SECTION);
    	auditFile.ensureHeaderFound(ENCRYPTION_SECTION);
    	auditFile.verifySigningHeaderContents(SIGNING_SECTION, SIGNING_PRIVATE_KEY_ALIAS2, SIGNING_KEYSTORE2, SIGNING_CERTIFICATE_EC);
    	auditFile.verifyEncryptionHeaderContents(ENCRYPTION_SECTION, ENCRYPTION_KEYSTORE_PRIVATE_ENCRYPTION_CERT_ALIAS, ENCRYPTION_KEYSTORE, ENCRYPTION_CERTIFICATE_RSA);
     	auditFile.ensureLogContainsCompleteAuditRecords();
    }
  }
    
  
    private void sleep(int sleepSecs) {
		try {
			Thread.sleep(sleepSecs * 1000);
		} catch (InterruptedException e) {
		}
	}

}
