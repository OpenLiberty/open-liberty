/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.charset.Charset;

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

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.AuditFileStream;
import com.ibm.ws.security.audit.fat.common.tooling.AuditMessageConstants;
import com.ibm.ws.security.audit.fat.config.utils.TestableAuditLog;
import com.ibm.ws.security.audit.fat.config.utils.TestableAuditLogs;
import com.ibm.ws.security.audit.fat.config.utils.TestableSecureAuditLog;

import com.ibm.ws.security.audit.fat.config.utils.TestableLoggingStream;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.TestServer;
import com.ibm.wsspi.security.audit.AuditService;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
/**
 * Test Description:
 * 
 * The test verifies that when logging is specified to route the audit source to console/message log in json format,
 * that all of the audit events key/value pairs present in the audit.log are found in the corresponding json log.
 * The test verifies
 * 	- audit log rollover has no effect on json logging of audit source
 *  - audit filtering on events results in json logging of same filtered events
 *  - audit with compact format and expanded format results in same events in json logging
 *  - audit with signing and encryption results in json logging of readable text format
 *  - audit source can be routed to the console log and message log with json logging
 *  - audit source with format "basic" in logging config does not result in any audit events in json log
 *  
 * 
 * The logging component adds additional key/value pairs including the "type=liberty_audit" to identify the
 * audit events from the audit source. This test does not verify the unique fields which json logging adds to the 
 * audit event data. The logging component prepends the audit event names with "ibm_audit_" and this test removes that
 * preprended string before comparing the events to those in the json log.
 * 
 * The bootstrap.properties for this test server specifies the following to route audit source and messages to the console.log:
 *   com.ibm.ws.logging.console.source=message,audit
 *	 com.ibm.ws.logging.console.format=json
 * 
 * When the logging configuration is set in server.xml, it takes precedence over bootstrap.properties, and 
 * some tests specify that the audit source should be routed to the messages.log. 
 * 
 *  <logging messageFormat="json" messageSource="audit,message"/>
 * 
 */

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditJsonLoggingTest extends TestableLoggingStream {

	
    private static String AUDIT_JSON_MESSAGE_LOGGING = "audit-1.0_jsonMessageLogging.xml";
    private static String AUDIT_BASIC_MESSAGE_LOGGING = "audit-1.0_basicMessageLogging.xml";
    private static String AUDIT_JSON_CONSOLE_LOGGING = "audit-1.0_jsonConsoleLogging.xml";
    private static String AUDIT_SIGNENCRYPT_ROLLOVER_JSON_MESSAGE_LOGGING = "audit-1.0_signEncryptRollover_jsonMessageLogging.xml";
    
	public static TestServer myTestServer = new TestServer("com.ibm.ws.security.audit.fat.jsonlog",AUDIT_JSON_CONSOLE_LOGGING);
    private static String USER1 = "user1";
    private static String USER1PWD = "user1pwd";
    private static String BADUSER = "baduser";
    private static String BADUSERPWD = "baduserpwd"; 
    
    private static String LIBERTY_AUDIT = "liberty_audit";
    private static String EVENT_SEQUENCE_NUMBER = "eventSequenceNumber";
    private static String PREPREND_IBM_AUDIT = "ibm_audit_";
    private static String EMPTY = "";

    private static final String IBM_1047_ENCODING = "IBM-1047";
    
    protected static Boolean  RESTART_SERVER= true;
    
    private static int DEFAULT_DELAY_SECONDS = 3;
    
    private static boolean isZOS;
    	
    protected static Class<?> logClass = AuditJsonLoggingTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    protected static String messageLogFile;
    protected static String consoleLogFile;

    
    static List<String> checkApps = new ArrayList<String>();	
	static List<String> startMsgs = new ArrayList<String>();
	static List<String> startFailedMsgs = new ArrayList<String>();

    private static final boolean IS_RUNNING_ON_ZOS = (System.getProperty("os.name").toLowerCase().indexOf("z/os") > -1)
    || (System.getProperty("os.name").toLowerCase().indexOf("os/390") > -1);
	
	
    public AuditJsonLoggingTest() {
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
        auditLogFile = myTestServer.getServer().getLogsRoot() + "audit.log";
        messageLogFile = myTestServer.getServer().getLogsRoot() + "messages.log";
        consoleLogFile = myTestServer.getServer().getLogsRoot() + "console.log";
        
        Machine localMachine = Machine.getLocalMachine();
        OperatingSystem os = localMachine.getOperatingSystem();
        isZOS = OperatingSystem.ZOS.equals(os);
        
        checkApps.add(AUDIT_BASICAUTH_SERVLET);
        
        // Messages expected for start of audit with good server config
     	startMsgs.add(AuditMessageConstants.CWWKF0012I_AUDIT_FEATURE_INSTALLED);
    	startMsgs.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY);
    	startMsgs.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);
    	startMsgs.add(AuditMessageConstants.CWWKS0008I_SECURITY_SERVICE_READY);
         
    	myTestServer.startServer(AUDIT_JSON_CONSOLE_LOGGING, "AuditJsonLoggingTest", checkApps, startMsgs, Constants.JUNIT_REPORTING,null);
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
  * When audit-1.0 feature is specified with logging configuration to send audit source	
  * in json format to the messages.log, the messages.log contains all of the audit events that
  * are generated in the audit.log with matching keys and values.
  * 
  */
 @Test
 public void testAuditAll_jsonMessageLog_resultsSameAuditEvents() throws Exception {

	 myTestServer.reconfigServer(AUDIT_JSON_MESSAGE_LOGGING, name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
		
	 AuditFileStream allEvents = new AuditFileStream(auditLogFile);
	 AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
	 
	 String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
	 response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
	 verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
	 delay(DEFAULT_DELAY_SECONDS);
	 
	 List<JSONObject> auditLogContents = asserts.auditData();
     Stream<String> jsonLoggingStream = IS_RUNNING_ON_ZOS ?
     Files.lines(Paths.get(messageLogFile), Charset.forName(IBM_1047_ENCODING)) :
     Files.lines(Paths.get(messageLogFile));
	 Map<Object,JSONObject> jsonLoggingContents =  jsonLoggingStream
			 .filter(line -> line.contains(LIBERTY_AUDIT))
			 .map(line -> replaceText(line,PREPREND_IBM_AUDIT,EMPTY))
			 .map(line -> asJson(line))
			 .collect(Collectors.toMap( o -> o.get(EVENT_SEQUENCE_NUMBER), o -> o));
	 jsonLoggingStream.close();
	 
	 Log.info(logClass, "thisMethod", "Compare number audit events in json log to the number expected from audit.log: " + auditLogContents.size() + "\n");
	 assertEquals("JSON log did not contain same number of audit events as audit.log. Expected:"  + auditLogContents.size() + " Found: " +   jsonLoggingContents.size(), jsonLoggingContents.size(), auditLogContents.size());
	
	 for (JSONObject auditLogRecord : auditLogContents){
 			String num = auditLogRecord.get(EVENT_SEQUENCE_NUMBER).toString();
			JSONObject jsonRecordFound = jsonLoggingContents.get(num);
			boolean auditEventsMatch =   contains(auditLogRecord,jsonRecordFound); 
			   		
			assertNotNull("JSON log did not contain entry for audit eventSequenceNumber: " + num, jsonRecordFound);
			assertTrue("JSON log did not include corresponding key/value pairs from audit log eventSequenceNumber:" + num, auditEventsMatch);
	 };
 }
 
 /**
  * Test description:
  * When audit-1.0 feature is specified with logging configuration to send audit source	in json format
  * to the messages.log, and the audit events are filterd and compact, the messages.log contains all of the audit events that
  * are generated in the audit.log with matching keys and values.
  * 
  */
 @Mode(TestMode.LITE)
 @Test
 public void testAudit_AuthnCompact_jsonMessageLog_resultsSameAuditEvents() throws Exception {

	 myTestServer.reconfigServer(AUDIT_JSON_MESSAGE_LOGGING, name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
		
	 AuditFileStream allEvents = new AuditFileStream(auditLogFile);
	 AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
	 
	 String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
	 response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
	 verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
	 delay(DEFAULT_DELAY_SECONDS);
	 
	 List<JSONObject> auditLogContents = asserts.auditData();
	 
	 Stream<String> jsonLoggingStream = IS_RUNNING_ON_ZOS ?
                    Files.lines(Paths.get(messageLogFile), Charset.forName(IBM_1047_ENCODING)) :
                    Files.lines(Paths.get(messageLogFile));
	 Map<Object,JSONObject> jsonLoggingContents =  jsonLoggingStream
			 .filter(line -> line.contains(LIBERTY_AUDIT))
			 .map(line -> replaceText(line,PREPREND_IBM_AUDIT,EMPTY))
			 .map(line -> asJson(line))
			 .collect(Collectors.toMap( o -> o.get(EVENT_SEQUENCE_NUMBER), o -> o));
	 jsonLoggingStream.close();
	 
	 Log.info(logClass, "thisMethod", "EXPECTED number of audit events: " + auditLogContents.size() + "\n");
	 assertEquals("JSON log did not contain same number of audit events as audit.log. Expected:"  + auditLogContents.size() + " Found: " +   jsonLoggingContents.size(), jsonLoggingContents.size(), auditLogContents.size());

	 for (JSONObject auditLogRecord : auditLogContents){
			String num = auditLogRecord.get(EVENT_SEQUENCE_NUMBER).toString();
			JSONObject jsonRecordFound = jsonLoggingContents.get(num);
			boolean auditEventsMatch =   contains(auditLogRecord,jsonRecordFound); 	
			
			assertNotNull("JSON log did not contain entry for audit eventSequenceNumber: " + num, jsonRecordFound);
			assertTrue("JSON log did not include corresponding key/value pairs from audit log eventSequenceNumber:" + num, auditEventsMatch);
	 };

 }
  /**
  * Test description:
  * When audit-1.0 feature is specified with logging configuration to send audit source	
  * messages.log with format of "basic", then no audit events are present in the messages.log.
  * Events should only be present when format is "json"
  * 
  */
 @Test
 public void testAudit_basicMessageLog_resultsNoAuditEventsInMessageLog() throws Exception {

	 myTestServer.reconfigServer(AUDIT_BASIC_MESSAGE_LOGGING, name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
		
	 String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
	 response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
	 verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
	 delay(DEFAULT_DELAY_SECONDS);
	 
     Stream<String> jsonLoggingStream = IS_RUNNING_ON_ZOS ?
     Files.lines(Paths.get(messageLogFile), Charset.forName(IBM_1047_ENCODING)) :
     Files.lines(Paths.get(messageLogFile));
	 boolean auditRecordFound =   jsonLoggingStream
			 .anyMatch(line -> line.contains(LIBERTY_AUDIT));
	 jsonLoggingStream.close();
	 
	 assertFalse("Messages log with basic format should not contain audit events, but liberty_audit entries were found. ",  auditRecordFound);
 }
 
 /**
 * Test description:
 * When audit-1.0 feature is specified with logging configuration to send audit source	
 * messages.log and the audit.log is signed and encrypted, the messages log is not signed/encrypted.
 * The audit.log file will rollover, but this has no effect on the messages.log audit events.
 * The total number of audit events in the messages.log for SECURITY_AUTHN should the number of successful authentications.
 * 
 */
 @Test
 public void testAuditSignedEncrypted_jsonMessageLog_resultsPlainTextMessageLog() throws Exception {
	
	 myTestServer.reconfigServer(AUDIT_SIGNENCRYPT_ROLLOVER_JSON_MESSAGE_LOGGING, name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);

     String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
     verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");
     for (int i = 0; i < 300; i++) {
         response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
     }
     delay(DEFAULT_DELAY_SECONDS * 20);
     
     Stream<String> jsonLoggingStream = IS_RUNNING_ON_ZOS ?
     Files.lines(Paths.get(messageLogFile), Charset.forName(IBM_1047_ENCODING)) :
     Files.lines(Paths.get(messageLogFile));
	 long countAuditAuthn =  jsonLoggingStream
			 .filter(line -> line.contains(LIBERTY_AUDIT) && line.contains("SECURITY_AUTHN"))
			 .count();
	 jsonLoggingStream.close();
	 
	 Log.info(logClass, "thisMethod", "Number of audit log events: " +  countAuditAuthn + "\n");
	 assertTrue("Messages log should contain 301 liberty_audit entries for SECURITY_AUTHZ. ",  countAuditAuthn == 301);
 }
 
 /**
 * Test description:
 * When audit-1.0 feature is specified with bootstrap properties as shown below, the audit entries
 * are routed to the console.log. The console.log contains all of the audit events that
 * are generated in the audit.log with matching keys and values.
 * 
 * Bootstrap.properties:
 * com.ibm.ws.logging.console.source=message,audit
 * com.ibm.ws.logging.console.format=json
 * 
 */
@Test
public void testAuditAll_jsonConsoleLog_resultsSameAuditEvents() throws Exception {

	// Don't run on Z/OS since the console.log is in EBCDIC format
    if (isZOS) {
        Log.info(logClass, name.getMethodName(), "CURRENT PLATFORM IS ZOS, TEST WILL NOT RUN");
        return;
    }	
	
	 myTestServer.reconfigServer(AUDIT_JSON_CONSOLE_LOGGING, name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
		
	 AuditFileStream allEvents = new AuditFileStream(auditLogFile);
	 AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
	 
	 String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
	 response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
	 verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
	 delay(DEFAULT_DELAY_SECONDS);
	 
	 List<JSONObject> auditLogContents = asserts.auditData();
	 
	 Stream<String> jsonLoggingStream = Files.lines(Paths.get(consoleLogFile));
	 Map<Object,JSONObject> jsonLoggingContents =  jsonLoggingStream
			 .filter(line -> line.contains(LIBERTY_AUDIT))
			 .map(line -> replaceText(line,PREPREND_IBM_AUDIT,EMPTY))
			 .map(line -> asJson(line))
			 .collect(Collectors.toMap( o -> o.get(EVENT_SEQUENCE_NUMBER), o -> o));
	 jsonLoggingStream.close();
	 
	 Log.info(logClass, "thisMethod", "EXPECTED number of audit events: " + auditLogContents.size() + "\n");
	 assertEquals("JSON log did not contain same number of audit events as audit.log. Expected:"  + auditLogContents.size() + " Found: " +   jsonLoggingContents.size(), jsonLoggingContents.size(), auditLogContents.size());
	
	 for (JSONObject auditLogRecord : auditLogContents){
			String num = auditLogRecord.get(EVENT_SEQUENCE_NUMBER).toString();
			JSONObject jsonRecordFound = jsonLoggingContents.get(num);
			boolean auditEventsMatch =   contains(auditLogRecord,jsonRecordFound); 
			
			assertNotNull("JSON log did not contain entry for audit eventSequenceNumber: " + num, jsonRecordFound);
			assertTrue("JSON log did not include corresponding key/value pairs from audit log eventSequenceNumber:" + num, auditEventsMatch);
	 };
}
 
}
