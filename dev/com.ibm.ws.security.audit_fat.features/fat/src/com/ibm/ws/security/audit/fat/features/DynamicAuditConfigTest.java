/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.audit.fat.features;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.TestServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
/**
 * Test Description:
 * 
 * The test verifies that a dynamic addition and removal of the audit-1.0 feature from server.xml
 * either enables or disables the audit function. 
 */


@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class DynamicAuditConfigTest extends AuditCommonTest {

	public static TestServer myTestServer = new TestServer("com.ibm.ws.security.audit.fat.dynamic.config","appSecurity-2.0_servlet-3.1.xml");
    protected static Class<?> logClass = DynamicAuditConfigTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    
    private static int DEFAULT_DELAY_SECONDS = 3;
    private static final String javaVersion = System.getProperty("java.version");
    
    static List<String> ignoredServerExceptions = new ArrayList<String>();	
    
    static List<String> checkApps = new ArrayList<String>();	
	static List<String> startMsgsWithAudit = new ArrayList<String>();
	static List<String> startMsgsNoAudit = new ArrayList<String>();
	static List<String> startMsgsUpdateAdd = new ArrayList<String>();
	static List<String> startMsgsUpdate = new ArrayList<String>();
    
    private static String USER1 = "user1";
    private static String USER0 = "user0";
    private static String SERVICE_AUDIT_START = "service/audit/start";
    
       
    public DynamicAuditConfigTest() {
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
        
        ignoredServerExceptions.add("CWWKS5802E");
        
        // Messages expected for start without audit - just security service starts
     	startMsgsNoAudit.add(AuditMessageConstants.CWWKS0008I_SECURITY_SERVICE_READY);
        
        // Messages expected for start of audit with good server config
     	startMsgsWithAudit.add(AuditMessageConstants.CWWKF0012I_AUDIT_FEATURE_INSTALLED);
     	startMsgsWithAudit.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY);
     	startMsgsWithAudit.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);
       	// Messages expected when updating and removing the audit feature
        startMsgsUpdate.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);  
        startMsgsUpdate.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY); 
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
    	myTestServer.stopServer();
        httpclient.getConnectionManager().shutdown();
    }

    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have audit max files set to 2
     * - Access a servlet again with basic authentication and verify that only 2 audit files are generated.
     * Expected results:
     * - 1) After the server has been reconfigured to have max of 2 audit files run multiple calls and verify we only have 2 files."
     */
    @Test
    public void testDynamicallyUpdateAuditMaxFilesAttr() throws Exception {
   	
 	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
   
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;

    //TODO: I could not get test results to work properly when trying to do multi invoke to check for 5 files, I only got one for some reason, so only verifying weget 2 when setting max to 2
    
    // setting max files to 2
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler2.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
   
    for (int i = 0; i < 800; i++) {
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    }
    
    sleep(DEFAULT_DELAY_SECONDS);

    // verify there are 2 audit logs, one archived the other not
    List<File> list = returnListOfAuditLogs();  

    assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
    }
    
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have audit max file size set to zero, making no limit on file size
     * - Access a servlet again with basic authentication and verify we get a large audit log file
     * Expected results:
     * - 1) After the server has been reconfigured to have max file size to 0 run multiple calls and verify we have a file larger than 20 MB default size
     */
    @Test
    public void testDynamicallyUpdateAuditMaxFileSizeAttr() throws Exception {
     
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    File logFile = new File(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
   
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileSize.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
    
    for (int i = 0; i < 30000; i++) {
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    }
    
    sleep(DEFAULT_DELAY_SECONDS);

    // verify the audit log file is larger than 20 MB
    Long audit_file_length = logFile.length();
    Log.info(logClass, "testDynamicallyUpdateAuditMaxFileSizeAttr", "File size with filesize attr set to zero is " + audit_file_length );
    assertTrue("Expected large file size for audit.log", audit_file_length >= 20 * 1000000);
   
    }    
 
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have no audit event not specified
     * - Access a servlet again with basic authentication and verify that all events are posted
     * Expected results:
     * - 1) The audit.log should contain AUTHN event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to have no events, verify all events get logged
     */
    @Test
    public void testDynamicallyUpdateAuditEventToNoEvent() throws Exception {
    
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent); 
    
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
	   
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // verify we only get AUTHN since it is only event posted, and that we don't get AUTHZ or Audit Mgmt since not posted
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
   
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ),
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT));
    
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditNoEventsSpecified.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
    
    AuditFileStream stream = new AuditFileStream(auditLogFile);
	AuditAsserts asserts2 =  new AuditAsserts(logClass, stream);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

    // verifying that with no events posted in server.xml we get all expected events
    asserts2.assertFoundInData(asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
    										   AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
    										   AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    						   asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    										   AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    						   asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
    								  		   AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    
    }
 
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have audit event specified
     * - Access a servlet again with basic authentication and, verify just that event is posted
     * Expected results:
     * - 1) The audit.log should contain AUTHN event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to have 1 events, verify only that event gets posted
     */
    @Test
    public void testDynamicallyUpdateAuditNoEventToEvent() throws Exception {
    
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent); 
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditNoEventsSpecified.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
	   
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // verify we get all expected events sionce no events are posted
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
    										 AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
    										 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    						  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    								  		 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    						  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
    								  		 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)); 
    
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
   
    AuditFileStream stream = new AuditFileStream(auditLogFile);
	AuditAsserts asserts2 =  new AuditAsserts(logClass, stream);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    //verify only AUTHN gets logged since it is only event posted
    asserts2.assertFoundInData(asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    
    asserts2.assertNotFoundInData(
    		asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ),
    		asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT));
    }    
 
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have audit event specified
     * - Access a servlet again with basic authentication and, verify just that event is configured
     * Expected results:
     * - 1) The audit.log should contain AUTHN event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to have 1 events, verify only that event gets logged
     */
    @Test
    public void testDynamicallyUpdateAuditAuthnEventToAuthzEvent() throws Exception {
    
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent); 
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
	   
     String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // verify we get all expected events logged
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
   
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ));

    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditAuthzEvent.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
   
    AuditFileStream stream = new AuditFileStream(auditLogFile);
	AuditAsserts asserts2 =  new AuditAsserts(logClass, stream);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    //verify only AUTHZ event was logged
    asserts2.assertFoundInData(asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    
    asserts2.assertNotFoundInData(
    		asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN));
    }    
    
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have audit event specified with no outcome
     * - Access a servlet again with basic authentication and, verify just that event is configured
     * Expected results:
     * - 1) The audit.log should contain AUTHN with only success event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to have 1 event and no outcome, ensure all outcomes are configured
     */
    @Test
    public void testDynamicallyUpdateAuditAuthnEventFromOutcomeSpecifiedToNoOutcome() throws Exception {
    
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent); 
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
	
	   
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // verify we only get outcome configured
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
   
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE));
    
    //clear httpclient so it will allow for secondary login
    httpclient.getConnectionManager().shutdown();
    httpclient = new DefaultHttpClient();
    
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditAuthnNoOutcome.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
   
    AuditFileStream stream = new AuditFileStream(auditLogFile);
	AuditAsserts asserts2 =  new AuditAsserts(logClass, stream);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user0", "user0pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER0, GET_REMOTE_USER + USER0);
    
    //verify we get all expected outcomes since no outcome is configured
    asserts2.assertFoundInData(asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 								   AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
    						   asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    								   		   AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }     
  
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have audit event specified with one outcome
     * - Access a servlet again with basic authentication and, verify just that event is posted
     * Expected results:
     * - 1) The audit.log should contain AUTHN with only success event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to have 1 event and no outcome, ensure all outcomes are posted
     */
    @Test
    public void testDynamicallyUpdateAuditAuthnEventFromNoOutcomeToSpecifiedOutcome() throws Exception {
   
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent); 
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditAuthnNoOutcome.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
	
	   
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // verify all expected outcome are logged
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    										 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
    						  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    								  		 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
     
    //clear httpclient so it will allow for secondary login
    httpclient.getConnectionManager().shutdown();
    httpclient = new DefaultHttpClient();
    
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
   
    AuditFileStream stream = new AuditFileStream(auditLogFile);
	AuditAsserts asserts2 =  new AuditAsserts(logClass, stream);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user0", "user0pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER0, GET_REMOTE_USER + USER0);
    
    //verify only the configured outcome is logged
    asserts2.assertFoundInData(asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
  
    asserts2.assertNotFoundInData(
   		asserts2.asJson(AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE));
    
    }     
      
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to have audit event specified with different outcome
     * - Access a servlet again with basic authentication and, verify just that event and configured outcome is logged
     * Expected results:
     * - 1) The audit.log should contain AUTHN with only success event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to have 1 event and the expected outcome
     */
    @Test
    public void testDynamicallyUpdateAuditAuthnEventFromSuccessOutcomeToFailureOutcome() throws Exception {
    
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent); 
	
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
	   
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        
    //clear httpclient so it will allow for secondary login
    httpclient.getConnectionManager().shutdown();
    httpclient = new DefaultHttpClient();
    
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditAuthnDeniedOutcome.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
   
    AuditFileStream stream = new AuditFileStream(auditLogFile);
	AuditAsserts asserts2 =  new AuditAsserts(logClass, stream);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user0", "user1pwd", HttpServletResponse.SC_UNAUTHORIZED);
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user0", "user0pwd", HttpServletResponse.SC_OK);

    //verify that only the denied outcome is logged
    asserts2.assertFoundInData(asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));
   
    asserts2.assertNotFoundInData(
    		asserts2.asJson(AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        
    }   
    
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to both sign and encrypt the audit log
     * - Access a servlet again with basic authentication and verify that a new log is created and that the log is signed and encrypted
     * Expected results:
     * - 1) Initial log can be processed with auditAsserts showing it is in JSON format
     * - 2) After the server has been reconfigured to add signing and encryption, a new audit log is generated and has headers for signing and encryption values 
     * (keystore, ailas and algorithm) as specified in the new configuration.
     * 
     */
    @Test
    public void testDynamicallyUpdateToAddSignEncrypt() throws Exception {
   	
 	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
   
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;

    // Set sign=true and encrypt=true
    // The startMsgsUpdate will make server wait for file handler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandlerSignEncrypt.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
  
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    
    sleep(DEFAULT_DELAY_SECONDS);

    // verify there are 2 audit logs, original log archived, signed and encrypted log not
    List<File> list = returnListOfAuditLogs();  
    assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
    
   // Verify the expected values are found in the header of the audit log for signing
 	List<String> header = readSecureLogHeader(auditLogFile,SIGNING_SECTION);
 	Map<String,String> found = parseValues(header,HEADER_SIGNING_SHARED_KEY,HEADER_SIGNING_CERT_ALIAS,HEADER_SIGNING_KEYSTORE,HEADER_SIGNING_CERTIFICATE);
 	assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
 	assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS));
 	assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE));   
 	assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));   
  
    // Verify the expected values are found in the header of the audit log for encryption
 	header = readSecureLogHeader(auditLogFile,ENCRYPTION_SECTION);
 	found = parseValues(header,HEADER_ENCRYPTED_SHARED_KEY,HEADER_ENCRYPTION_CERT_ALIAS,HEADER_ENCRYPTION_KEYSTORE,HEADER_ENCRYPTION_CERTIFICATE);
 	assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
 	assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS2 + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS2));
 	assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE2 + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE2));   
 	assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_EC + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_EC));   
    }
    
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml along with signing and encryption.
     * - Access a servlet with basic authentication to generate an audit record and validate the log contains headers for signing and encryption.
     * - Then reconfigure the server to disable signing and encryption (sign=false, encrypt=false)
     * - Access a servlet again with basic authentication and verify that a new log is created and that the log can be queried for the Basic authentication record
     * Expected results:
     * - 1) The initial audit log and has headers for signing and encryption, and the values for the keystore, ailas and algorithm are as specified in the configuration.
     * - 2) After reconfiguring, the audit log can be processed with auditAsserts showing it is in JSON format
     */
    @Test
    public void testDynamicallyUpdateToRemoveSignEncrypt() throws Exception {
   	
    myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandlerSignEncrypt.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
   
   // Verify the expected values are found in the header of the audit log for signing
 	List<String> header = readSecureLogHeader(auditLogFile,SIGNING_SECTION);
 	Map<String,String> found = parseValues(header,HEADER_SIGNING_SHARED_KEY,HEADER_SIGNING_CERT_ALIAS,HEADER_SIGNING_KEYSTORE,HEADER_SIGNING_CERTIFICATE);
 	assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
 	assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS));
 	assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE));   
 	assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));   
  
    // Verify the expected values are found in the header of the audit log for encryption
 	header = readSecureLogHeader(auditLogFile,ENCRYPTION_SECTION);
 	found = parseValues(header,HEADER_ENCRYPTED_SHARED_KEY,HEADER_ENCRYPTION_CERT_ALIAS,HEADER_ENCRYPTION_KEYSTORE,HEADER_ENCRYPTION_CERTIFICATE);
 	assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
 	assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS2 + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS2));
 	assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE2 + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE2));   
 	assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_EC + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_EC)); 
 
    // Set config so that signing and encryption default to false and verify that the log is in JSON format so can be examined with auditAsserts
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandlerSignEncryptFalse.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
    
    AuditFileStream recent = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    sleep(DEFAULT_DELAY_SECONDS);

    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;

    // verify there are 2 audit logs, original log archived, new log is not
    List<File> list = returnListOfAuditLogs();  
    assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
  
    }
    
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml along with signing and encryption.
     * - Access a servlet with basic authentication to generate an audit record and validate the log contains headers for signing and encryption.
     * - Then reconfigure the server to use a different keystore, certificate alias and algorithm for both signing and encryption.
     * - Access a servlet again with basic authentication and verify that a new log is created and that the log contains headers for signing and encryption.
     * Expected results:
     * - 1) With the initial configuration, the audit log has headers for signing and encryption values 
     * (keystore, ailas and algorithm) as specified in the first configuration.
     * - 2) After the server has been reconfigured with new keystores, alias and algorithm for signing and encryption, a new audit log is generated and has headers for the changed values 
     * for signing and encryption (keystore, ailas and algorithm) as specified in the new configuration.
     */
    @Test
    public void testDynamicallyUpdateToChangeEncryptSignKeystoresAndCerts() throws Exception {
   	
    myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandlerSignEncrypt.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
   
   // Verify the expected values are found in the header of the audit log for signing
 	List<String> header = readSecureLogHeader(auditLogFile,SIGNING_SECTION);
 	Map<String,String> found = parseValues(header,HEADER_SIGNING_SHARED_KEY,HEADER_SIGNING_CERT_ALIAS,HEADER_SIGNING_KEYSTORE,HEADER_SIGNING_CERTIFICATE);
 	assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
 	assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS));
 	assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE));   
 	assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));   
  
    // Verify the expected values are found in the header of the audit log for encryption
 	header = readSecureLogHeader(auditLogFile,ENCRYPTION_SECTION);
 	found = parseValues(header,HEADER_ENCRYPTED_SHARED_KEY,HEADER_ENCRYPTION_CERT_ALIAS,HEADER_ENCRYPTION_KEYSTORE,HEADER_ENCRYPTION_CERTIFICATE);
 	assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
 	assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS2 + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS2));
 	assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE2 + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE2));   
 	assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_EC + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_EC)); 
 
    // Set config so that signing and encryption default to false
    // The startMsgsUpdate will make server wait for filehandler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandlerSignEncrypt2.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
  
    sleep(DEFAULT_DELAY_SECONDS);
    
    // Verify the expected values from the new configuration are found in the header of the audit log for signing
  	header = readSecureLogHeader(auditLogFile,SIGNING_SECTION);
  	found = parseValues(header,HEADER_SIGNING_SHARED_KEY,HEADER_SIGNING_CERT_ALIAS,HEADER_SIGNING_KEYSTORE,HEADER_SIGNING_CERTIFICATE);
  	assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
  	assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS2 + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS2));
  	assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE2 + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE2));   
  	assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_EC + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_EC));   
   
     // Verify the expected values from the new configuration are found in the header of the audit log for encryption
  	header = readSecureLogHeader(auditLogFile,ENCRYPTION_SECTION);
  	found = parseValues(header,HEADER_ENCRYPTED_SHARED_KEY,HEADER_ENCRYPTION_CERT_ALIAS,HEADER_ENCRYPTION_KEYSTORE,HEADER_ENCRYPTION_CERTIFICATE);
  	assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
  	assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS));
  	assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE));   
  	assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_RSA)); 

    // verify there are 2 audit logs, original log archived, new log is not
    List<File> list = returnListOfAuditLogs();  
    assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
  
    }
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml along with signing of the audit log.
     * - Access a servlet with basic authentication to generate an audit record and validate the log contains a header for signing.
     * - Then reconfigure the server to enable just encryption.
     * - Access a servlet again with basic authentication and verify that a new log is created and contains the header for encryption.
     * Expected results:
     * - 1) With the initial configuration, the audit log has a header for signing with expected values, but no header for encryption.
     * - 2) After the server has been reconfigured, a new audit log is created with a header for encryption, but none for signing.
     */
    @Test
    public void testDynamicallyUpdateToChangeFromSigningToEncrypting() throws Exception {
   	
    myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandlerSign.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
   
   // Verify the expected values are found in the header of the audit log for signing.
 	List<String> header = readSecureLogHeader(auditLogFile,SIGNING_SECTION);
 	Map<String,String> found = parseValues(header,HEADER_SIGNING_SHARED_KEY,HEADER_SIGNING_CERT_ALIAS,HEADER_SIGNING_KEYSTORE,HEADER_SIGNING_CERTIFICATE);
 	assertNotNull("Expected signing shared key value not found.", found.get(HEADER_SIGNING_SHARED_KEY));
 	assertTrue("Expected signing cert alias of " + SIGNING_PRIVATE_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_SIGNING_CERT_ALIAS), found.get(HEADER_SIGNING_CERT_ALIAS).contains(SIGNING_PRIVATE_KEY_ALIAS));
 	assertTrue("Expected signing keystore of " + SIGNING_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_SIGNING_KEYSTORE), found.get(HEADER_SIGNING_KEYSTORE).contains(SIGNING_KEYSTORE));   
 	assertTrue("Expected signing certificate  " + SIGNING_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_SIGNING_CERTIFICATE), found.get(HEADER_SIGNING_CERTIFICATE).contains(SIGNING_CERTIFICATE_RSA));   
	assertNull("Encryption section found when none was expected", readSecureLogHeader(auditLogFile,ENCRYPTION_SECTION));

 	
    // Set config so that encryption is set and there is no signing
    // The startMsgsUpdate will make server wait for file handler service ready and audit service ready message
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandlerEncrypt.xml", name.getMethodName(), startMsgsUpdate, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
    
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
   
     // Verify the expected values are found in the header of the audit log for encryption, but not for signing.
  	header = readSecureLogHeader(auditLogFile,ENCRYPTION_SECTION);
  	found = parseValues(header,HEADER_ENCRYPTED_SHARED_KEY,HEADER_ENCRYPTION_CERT_ALIAS,HEADER_ENCRYPTION_KEYSTORE,HEADER_ENCRYPTION_CERTIFICATE);
  	assertNotNull("Expected encrypted shared key value not found.", found.get(HEADER_ENCRYPTED_SHARED_KEY));
  	assertTrue("Expected encryption cert alias of " + ENCRYPTION_PUBLIC_KEY_ALIAS + " not found. Instead found: " + found.get(HEADER_ENCRYPTION_CERT_ALIAS), found.get(HEADER_ENCRYPTION_CERT_ALIAS).contains(ENCRYPTION_PUBLIC_KEY_ALIAS));
  	assertTrue("Expected encryption keystore of " + ENCRYPTION_KEYSTORE + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_KEYSTORE), found.get(HEADER_ENCRYPTION_KEYSTORE).contains(ENCRYPTION_KEYSTORE));   
  	assertTrue("Expected encryption certificate  " + ENCRYPTION_CERTIFICATE_RSA + " not found.  Instead found: " + found.get(HEADER_ENCRYPTION_CERTIFICATE), found.get(HEADER_ENCRYPTION_CERTIFICATE).contains(ENCRYPTION_CERTIFICATE_RSA)); 
	assertNull("Signing section found when none was expected", readSecureLogHeader(auditLogFile,SIGNING_SECTION));
  	
    // verify there are 2 audit logs, original log archived, new log is not
    List<File> list = returnListOfAuditLogs();  
    assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
    }
        
    
    private List<File> returnListOfAuditLogs() throws IOException {
    	List<File> list = new ArrayList<File>();
        java.nio.file.Path dir = java.nio.file.Paths.get(server.getLogsRoot());
        try {
           java.nio.file.DirectoryStream<java.nio.file.Path> dirStream = java.nio.file.Files.newDirectoryStream(dir);
           for (java.nio.file.Path entry: dirStream) {

               if (entry.getFileName().toString().startsWith("audit")) {
                   list.add(entry.toFile());
               }
           }
           dirStream.close();
        } catch (java.nio.file.DirectoryIteratorException ex) {
                   // I/O error encountered during the iteration, the cause is an IOException
                   throw ex.getCause();
        }
        return list;

    }  
  
    private void sleep(int sleepSecs) {
 		try {
 			Thread.sleep(sleepSecs * 1000);
 		} catch (InterruptedException e) {
 		}
 	}    
    
}