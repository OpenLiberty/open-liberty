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
package com.ibm.ws.security.audit.fat.config;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.ibm.wsspi.security.audit.AuditService;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
/**
 * Test Description:
 * 
 * The test verifies that various configuration values for the auditFileHandler events with eventName and outcome
 * results in the correct audit records being emitted to the audit log.
 */

//TODO - Tests below do a reconfigServer with restartServer=true so the server is restarted. 
// Eventually it should be possible to reconfigure without a restart and have changes dynamically applied.
// Need to change to restartServer=false once dynamic update capability for the auditFileHander is in place
// Once the change is made to reconfig without restart (stop and start) the message checks will need
// to be adjusted and may be able to be added to messages passed to reconfig.

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditEventOutcomeConfigTest extends AuditCommonTest {

	public static TestServer myTestServer = new TestServer("com.ibm.ws.security.audit.fat.config","audit-1.0_eventNameAuthn.xml");
    private static String USER1 = "user1";
    private static String USER1PWD = "user1pwd";
    private static String BADUSER = "baduser";
    private static String BADUSERPWD = "baduserpwd";  
    private static String SERVICE_AUDIT_START = "service/audit/start";
    	
    protected static Class<?> logClass = AuditEventOutcomeConfigTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    
    static List<String> checkApps = new ArrayList<String>();	
	static List<String> startMsgs = new ArrayList<String>();
	static List<String> startFailedMsgs = new ArrayList<String>();
	
//	static String[] configFailureExceptions = {AuditMessageConstants.CWWKE0700W_AUDIT_COLLECTOR_MANAGER_FAILED,
//											AuditMessageConstants.CWWKG0032W_AUDIT_UNEXPECTED_CONFIG_VALUE,
//											AuditMessageConstants.CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED};
//    
//	static String[] configFailureExceptions2 = {AuditMessageConstants.CWWKE0700W_AUDIT_COLLECTOR_MANAGER_FAILED,
//			AuditMessageConstants.CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED};
	
    public AuditEventOutcomeConfigTest() {
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
   
    	// Messages expected when config error causes audit feature not to start
    	startFailedMsgs.add(AuditMessageConstants.CWWKG0032W_AUDIT_UNEXPECTED_CONFIG_VALUE);
    	startFailedMsgs.add(AuditMessageConstants.CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED);

        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKE0700W_AUDIT_COLLECTOR_MANAGER_FAILED);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKG0032W_AUDIT_UNEXPECTED_CONFIG_VALUE);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5853E_AUDIT_HANDLER_INVALID_EVENTNAME);
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5855E_AUDIT_INVALID_MISSING_EVENT);
        
        
    	myTestServer.startServer("audit-1.0_eventNameAuthn.xml", "AuditEventOutComeConfigTest", checkApps, startMsgs, Constants.JUNIT_REPORTING,null);
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
     * When audit-1.0 feature is specified without auditFileHandler configured, then
     * all events are audited.
     * 
     * - Start the server with the audit-1.0 feature and NO auditFileHandler configuration.
     * - Access a servlet unsuccessfully and successfully with Basic Authentication.
     * Expected results:
     * - 1) The audit.log should contain all generated events -- AUTHN and SECURITY_AUDIT_MGMT events. 
     * - 2) the audit log should contain all generated outcomes -- SUCCESS, CHALLENGE, DENIED.
     */
    @Test
    public void testConfigMinAuditFeature_logsAllGeneratedEventsAndOutcomes() throws Exception {
    // 1. Reconfigure the server with minimal config with audit-1.0 and no auditFileHandler
    myTestServer.reconfigServer("audit-1.0_noAuditFileHandler.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify the following are found in the audit log
    // - the security audit management event is logged for start of the server with outcome=success
    // - AUTHN event for the Basic Authentication requests with outcomes denied, challenge, success
    asserts.assertFoundInData(
      	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    
   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    }
    /**
     * Test description:
     * Tests when a single event is specified with no outcome, that the log contains only that
     * event type and all the generated outcomes for that type.
     * When audit-1.0 feature is specified with a single eventName for AUTHN and no outcome then  AUTHN events are 
     * logged for all outcomes (denied, challenge, success). No other events are logged.
     * 
     *  eg.  <events name="AuditEvent_1" eventName="SECURITY_AUTHN"/>
     * 
     * - Start the server with the audit-1.0 feature is enabled. 
     * - Access a servlet with an incorrect user/password and then a successful authentication.
     * 
     * Expected results:
     * - 1) The audit.log should contain all generated AUTHN events for SUCCESS, CHALLENGE, DENIED 
     * - 2) the audit log should not contain MGMT events.
     */
    @Test
    public void testConfigSingleEventNoOutcome_logsSpecifiedEventWithAllOutcomes() throws Exception {
    	
    // 1. Reconfig the server to generate AUTHN events only.
	myTestServer.reconfigServer("audit-1.0_eventNameAuthn.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify that AUTHN events for all generated Outcomes are found in the log
    // - AUTHN event for the Basic Authentication requests with outcomes denied, challenge, success
    asserts.assertFoundInData(
   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    
    // 6. Verify that only AUTHN events are found and not the other generated AUDIT_MGMT and AUTHZ events.
    asserts.assertNotFoundInData(
  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT),
	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ));
    }
    
    /**
     * Test description:
     * Tests when a single MGMT event is specified with no outcome, that the log contains only that
     * event type and all the generated outcomes for that type.
     * When audit-1.0 feature is specified with a single eventName for MGMT and no outcome then  MGMT events are 
     * logged for the generated success on start. Other event types (AUTHN, AUTHZ) are NOT logged.
     * 
     *  eg.  <events name="AuditEvent_1" eventName="SECURITY_AUDIT_MGMT"/>
     * 
     * - Start the server with the audit-1.0 feature is enabled. 
     * - Access a servlet with an incorrect user/password and then a successful authentication.
     * 
     * Expected results:
     * - 1) The audit.log should contain only SECURITY_AUDIT_MGMT events. 
     * - 2) the audit log should NOT contain any AUTHN or AUTHZ events.
     */
    @Test
    public void testConfigSingleMgmtEventNoOutcome_logsSingleSpecifiedEvent() throws Exception {
    	
    // 1. Reconfig the server to generate the AUDIT_MGMT_EVENT event.
	myTestServer.reconfigServer("audit-1.0_eventNameMgmt.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
  
    // 3. Access the servlet with good userId and password to generate a successful AUTH event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD , HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 4. Verify that MGMT event only is logged.
    asserts.assertFoundInData(
          	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
    			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
    			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    
    // 5. Verify that no AUTHN events are found.
    asserts.assertNotFoundInData(
  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN),
	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ));
    }
    
    /**
     * Test description:
     * When audit-1.0 feature is specified with the auditFileHandler and multiple
     * events but no outcome results in all generated outcomes for each event being emitted.
     * 
     * Eg. 		<events name="MgmtAll" eventName="SECURITY_AUDIT_MGMT"  />
	 *  	    <events name="AuthnAll" eventName="SECURITY_AUTHN"  />
     * 
     * - Start the server with the audit-1.0 feature enabled. 
     * - Access a servlet unsuccessfully and successfully with Basic Authentication.
     *  
     * Expected results:
     * - 1) The audit.log should contain all generated events -- AUTHN and SECURITY_AUDIT_MGMT events. 
     * - 2) the audit log should contain all generated outcomes -- SUCCESS, CHALLENGE, DENIED.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfigMultipleEventsNoOutcomes_logsAllOutcomesForEvents() throws Exception {
    // 1. Reconfigure the server with multiple events specified
	myTestServer.reconfigServer("audit-1.0_eventNameAuthnMgmt_noOutcomes.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify the following are found in the audit log
    // - the security audit management event is logged for start of the server with outcome=success
    // - AUTHN event for the Basic Authentication requests with outcomes denied, challenge, success
    asserts.assertFoundInData(
      	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    
   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    }
    /**
     * Test description:
     * When audit-1.0 feature is specified with auditFileHandler and MGMT and AUTHN events
     * with each outcome listed specifically results in all generated events emitted.
     * 
     * Eg. 	 <events name="Mgmt1" eventName="SECURITY_AUDIT_MGMT" outcome="SUCCESS" />
	 *	 	 <events name="Mgmt2" eventName="SECURITY_AUDIT_MGMT" outcome="FAILURE" />
	 *  	 <events name="Authn1" eventName="SECURITY_AUTHN" outcome="DENIED" />
	 *  	 <events name="Authn2" eventName="SECURITY_AUTHN" outcome="SUCCESS" />
	 *  	 <events name="Authn3" eventName="SECURITY_AUTHN" outcome="CHALLENGE" />
	 *  	 <events name="Authn4" eventName="SECURITY_AUTHN" outcome="FAILURE" />
     * 
     * - Start the server with the audit-1.0 feature enabled.
     * - Access a servlet unsuccessfully and successfully with Basic Authentication.
     * Expected results:
     * - 1) The audit.log should contain all generated events -- AUTHN and SECURITY_AUDIT_MGMT events. 
     * - 2) the audit log should contain all generated outcomes -- SUCCESS, CHALLENGE, DENIED.
     */
    @Test
    public void testConfigMultipleEventsAllOutcomes_logsAllOutcomesForEvents() throws Exception {
    // 1. Reconfigure the server to specifically list all generated events and outcomes
	myTestServer.reconfigServer("audit-1.0_eventNameAuthnMgmt_allOutcomes.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify the following are found in the audit log
    // - the security audit management event is logged for start of the server with outcome=success
    // - AUTHN event for the Basic Authentication requests with outcomes denied, challenge, success
    asserts.assertFoundInData(
      	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    
   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    }
    
    /**
     * Test description:
     * Tests when a single event is specified with a single outcome that the log contains only
     * that event type and outcome.
     * When audit-1.0 feature is specified with an eventName for AUTHN and only outcome DENIED 
     * only AUTH events with outcome DENIED are logged.
     * 
     * - Start the server with the audit-1.0 feature and events Security authentication and single outcome denied. 
     * 
     *      eg.  <events name="AuditEvent_1" eventName="SECURITY_AUTHN" outcome="DENIED"/>
     *      
     * - Access a servlet with an incorrect user/password and then a successful authentication to generate different outcomes.
     * 
     * Expected results:
     * - 1) The audit.log should contain only the AUTHN event with DENIED outcome
     * - 2) the audit log should NOT contain other outcomes or AUTHZ or SECURITY_AUDIT_MGMT events
     */
    @Test
    public void testConfigSingleEventSingleOutcome_logsSpecifiedEventAndOutcome() throws Exception {
    	
    // 1.  Reconfigure the server to emit single event and single outcome
	myTestServer.reconfigServer("audit-1.0_eventNameAuthn_OutcomeDenied.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
 
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify that AUTHN events for all generated Outcomes are found in the log
    // - AUTHN event for the Basic Authentication requests with outcomes denied, challenge, success
    asserts.assertFoundInData(
   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));

    // 6. Verify that only AUTHN events are found (no audit mgmt and no authz) and no other generated outcomes besides DENIED
    asserts.assertNotFoundInData(
	  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT),
	  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ),
		asserts.asJson(AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
		asserts.asJson(AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }
    
    /**
     * Test description: 
     * This is a test for auditing a single event with multiple (but not all) outcomes.
     * When audit-1.0 feature is specified with an eventName for AUTHN and only outcome denied and challenge
     * only AUTH events with outcome denied and challenge are logged.
     * 
     * - Start the server with the audit-1.0 feature and events Security authentication and 2 outcomes logged. 
     * 
     *      eg.  <events name="AuditEvent_1" eventName="SECURITY_AUTHN" outcome="denied"/>
     *           <events name="AuditEvent_2" eventName="SECURITY_AUTHN" outcome="challenge"/>
     *      
     * - Access a servlet with an incorrect user/password and then a successful authentication.
     * 
     * Expected results:
     * - 1) The audit.log should contain only the AUTHN event with outcomes DENIED and CHALLENGE but not SUCCESS.
     * - 2) the audit log should NOT contain any AUTHZ or MGMT events
     */
    @Test
    public void testConfigSingleEventMultipleOutcomes_logsSpecifiedEventSpecifiedOutcomes() throws Exception {
    	
    // 1.  Reconfigure the server to emit single event with multiple outcomes
	myTestServer.reconfigServer("audit-1.0_eventNameAuthn_OutcomesDeniedChallenge.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
 
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify that AUTHN events for only denied and challenge outcomes are found
    // - AUTHN event for the Basic Authentication requests with outcomes denied, challenge
    asserts.assertFoundInData(
   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE));

    // 6. Verify that only AUTHN events are found (no audit mgmt and no authz) and no SUCCESS outcome
    asserts.assertNotFoundInData(
  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT),
  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ),
	asserts.asJson(AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    }
    
    /**
     * Test description:
     * When audit-1.0 feature is specified with multiple events and different outcomes
     * for those events, then the log contains the correct results.
     * 
     * - Start the server with the audit-1.0 feature and AUTHN with outcome DENIED and MGMT with SUCCESS. 
     * - Access a servlet unsuccessfully and successfully with Basic Authentication.
     * 
     * Eg. 	    <events name="AuthnOnlyDenied" eventName="SECURITY_AUTHN" outcome="DENIED" />
     *  <events name="MgmtOnlySuccess" eventName="SECURITY_AUDIT_MGMT" outcome="SUCCESS" />
     * 
     * Expected results:
     * - 1) The audit.log should contain all generated events -- AUTHN and SECURITY_AUDIT_MGMT events. 
     * - 2) the audit log should contain all generated outcomes -- only DENIED for AUTHN and only SUCCESS for AUDIT_MGMT
     * - 3) the audit.log should NOT contain AUTHN SUCCESS or CHALLENGE outcome.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfigMultipleEventsEachWithDifferentOutcomes_logsSpecifiedEventsAndOutcomes() throws Exception {
    	
    // 1. Reconfigure the server with multiple events AUTHN, MGMT each with different outcome
	myTestServer.reconfigServer("audit-1.0_eventNameAuthnMgmt_OutcomesDeniedSuccess.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify the following are found in the audit log
    // - the security audit management event is logged for start of the server with outcome=success
    // - AUTHN event for the Basic Authentication requests with outcomes denied
    asserts.assertFoundInData(
      	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    
   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));
    
    // 6. Verify that no AUTHZ events are found and no AUTHN events with Outcome SUCCESS or CHALLENGE.
    asserts.assertNotFoundInData(
  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ),
  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
		AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
  	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
		AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE));
    }
    
    /**
     * Test description:
     * When audit-1.0 feature is specified with the auditFileHandler and an
     * invalid event name is specified with a valid outcome, and an valid event name is specified
     * with a valid outcome, the auditFileHandler service will stop.   We will issue an exception.
     * No audit log will be generated.
     * 
     * - Start the server with the audit-1.0 feature and one valid event name and outcome of SUCCESS,
     *   and one invalid event name with outcome of SUCCESS.
     * 
     * Eg. 	<auditFileHandler> 
     *      <events name="ValidEvent" eventName="SECURITY_AUDIT_MGMT" outcome="SUCCESS" />
	 *      <events name="InvalidEvent" eventName="INVALID_EVENT_NAME" outcome="SUCCESS" />
	 *   </auditFileHandler>
     * 
     * Expected results:
     * - 1) The audit.log should not be generated. 
     * - 2) The message CWWKS5855E is logged to indicate that an invalid event name has been specified in the configuration.
     * - 3) The auditFileHandler service should be stopped.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfigInvalidEvent_serviceIsStopped() throws Exception {
    	// 1. Reconfigure the server for single outcome and invalid event name.   Expect denied messages.
//    	myTestServer.setIgnoredServerExceptions(configFailureExceptions);
//    	myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5855E_AUDIT_INVALID_MISSING_EVENT);
    	myTestServer.reconfigServer("audit-1.0_invalidEvent.xml", name.getMethodName(), startFailedMsgs, true, Constants.JUNIT_REPORTING);
        myTestServer.searchValueInServerLog(AuditMessageConstants.CWWKS5855E_AUDIT_INVALID_MISSING_EVENT + ".*event name for outcome " + AuditEvent.OUTCOME_SUCCESS.toUpperCase() + ".*audit handler " + AuditService.AUDIT_FILE_HANDLER_NAME + " configuration.*following event names:.*"  + AuditConstants.SECURITY_AUTHN,"messages.log");
        

 
    	// 2. Verify we have no audit log in the default log location
    	File dir = new File(myTestServer.getServer().getLogsRoot());
    	List<File> list = Arrays.asList(dir.listFiles(new FilenameFilter() {
    		@Override
    		public boolean accept(File dir, String name) {
    			return name.startsWith("audit");
    		}
    	}));
       assertTrue("An audit log was found when none was expected for a configuration with invalid event name.", list.size() == 0);
    }
    /**
     * Test description:
     * When audit-1.0 feature is specified with the auditFileHandler and a single
     * outcome but no events, then NO events are emitted to an audit log and 
     * the audit handler is stopped
     * 
     * - Start the server with the audit-1.0 feature and event outcome of SUCCESS. 
     * 
     * Eg. 	<auditFileHandler> 
	 *      <events name="SuccessOnly" outcome="SUCCESS" />
	 *   </auditFileHandler>
     * 
     * Expected results:
     * - 1) NO audit.log should be generated.
     * - 2) Failure messages are emitted to indicate an invalid configuration.
     * - 3) audit handler is stopped
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfigSingleOutcomeNoEvents_noAuditLogGenerated() throws Exception {
    // 1. Reconfigure the server for single outcome.   Except denied messages
	List<String> startFailedMsgs2 = new ArrayList<String>();
	startFailedMsgs2.add(AuditMessageConstants.CWWKE0701E_AUDIT_HANLDER_ACTIVATE_FAILED);

//    myTestServer.setIgnoredServerExceptions(configFailureExceptions2);
//    myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKS5855E_AUDIT_INVALID_MISSING_EVENT);
    
    myTestServer.reconfigServer("audit-1.0_outcomeSuccess_NoEvents.xml", name.getMethodName(), startFailedMsgs2, true, Constants.JUNIT_REPORTING);
    myTestServer.searchValueInServerLog(AuditMessageConstants.CWWKS5855E_AUDIT_INVALID_MISSING_EVENT + ".*event name for outcome " + AuditEvent.OUTCOME_SUCCESS.toUpperCase() + ".*audit handler " + AuditService.AUDIT_FILE_HANDLER_NAME + " configuration.*following event names:.*"  + AuditConstants.SECURITY_AUTHN,"messages.log");
	// 2. Verify we have no audit log in the default log location

	File dir = new File(myTestServer.getServer().getLogsRoot());
	List<File> list = Arrays.asList(dir.listFiles(new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith("audit");
		}
	}));
	
    assertTrue("An audit log was found when none was expected for a configuration with only an outcome and no event name.", list.size() == 0);
    }
    /**
     * Test description:
     * When audit-1.0 feature is specified with the auditFileHandler a single
     * outcome is listed for multiple event types, then only those are emitted.
     * 
     * - Start the server with the audit-1.0 feature. 
     * - Access a servlet unsuccessfully and successfully with Basic Authentication.
     * 
     * Eg. 		<auditFileHandler> 
	 *   	 <events name="SuccessOnly1" outcome="SUCCESS" eventName="SECURITY_AUTHN" />
	 *  	 <events name="SuccessOnly2" outcome="SUCCESS" eventName="SECURITY_AUDIT_MGMT" />
	 *		</auditFileHandler>
     * 
     * Expected results:
     * - 1) The audit.log should contain SUCCESS outcome for both AUTHN and SECURITY_AUDIT_MGMT events. 
     * - 2) the audit log should NOT contain any outcomes of DENIED or CHALLENGE.
     * - 3) The audit.log should NOT contain SUCCESS for AUTHZ.
     */
    @Test
    public void testConfigSingleOutcomeMultipleEvents_logsAllEventsWithOutcome() throws Exception {
    // 1. Reconfigure the server for single outcome listed for multiple events.
  
	myTestServer.reconfigServer("audit-1.0_outcomeSuccess_AuthnMgmtEvents.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
    
    // 4. Access the servlet with good userId and password to generate a successful AUTH event
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 5. Verify the following are found in the audit log
    // - the security audit management event is logged for start of the server with outcome=success
    // - AUTHN event for the Basic Authentication requests with outcome success
    // - No events are found for other outcome types CHALLENGE, DENIED.
    asserts.assertFoundInData(
      	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
    
    	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    
    asserts.assertNotFoundInData(
	   	asserts.asJson(	AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
	  	asserts.asJson(	AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    }
    /**
     * Test description:
     * When audit-1.0 feature is specified  with the auditFileHandler and a single
     * event type which is not generated by the authentication sequence, there is nothing
     * emitted to the audit log and no audit log created.
     * 
     * - Start the server with the audit-1.0 feature and event name of JMS 
     * - Access a servlet successfully with Basic Authentication.
     * 
     * Eg. 	<auditFileHandler> 
	 *      <events name="JMXOnly" eventName="JMX" />
	 *   </auditFileHandler>
     * 
     * Expected results:
     * - 1) The audit.log is not found. Expect exception.
     */
    @Test
    public void testConfigSingleJMSEvent_JMSEventNotGenerated_noAuditLogGenerated() throws Exception {
    // 1. Reconfigure the server to emit an event that will not be generated in this scenario.
    myTestServer.reconfigServer("audit-1.0_eventNameJMS.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with good userId and password to generate a successful AUTH event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 4. Verify that we catch an java.io.FileNotFoundException when trying to read from the audit log because no
    // new audit data was logged for JMS event.
    
    thrown.expect(java.io.FileNotFoundException.class);
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN)) ;
    }
    /**
     * Test description:
     * When audit-1.0 feature is specified with the auditFileHandler and a single
     * outcome type which is not generated by the authentication sequence, there is nothing
     * emitted to the audit log.
     * 
     * - Start the server with the audit-1.0 feature and outcome of DENIED
     * - Access a servlet successfully with Basic Authentication.
     * 
     * Eg. 	<auditFileHandler> 
	 *        <events name="FailureOnly" eventName="SECURITY_AUDIT_MGMT" outcome="FAILURE" />
	 *   </auditFileHandler>
     * 
     * Expected results:
     * - 1) The audit.log should not be found since no events were emitted. Test expects an exception.
     */
    @Test
    public void testConfigSingleOutcomeFailure_FailureOutcomeNotGenerated_noAuditLogGenerated() throws Exception {
    // 1. Reconfigure the server to look only for DENIED outcome which is not generated by scenario.
    myTestServer.reconfigServer("audit-1.0_outcomeFailure_MgmtEvent.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
	
    // 2.  Set up audit stream to capture the entire audit file contents
    AuditFileStream allEvents = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
    
    // 3. Access the servlet with good userId and password to generate a successful AUTH event
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
   
    // 4. Verify that we catch an java.io.FileNotFoundException when trying to read from the audit log because no
    // new audit data was logged for the DENIED outcome.
    
    thrown.expect(java.io.FileNotFoundException.class);
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN)) ;
    }
   /**
     * Test description:
     * When audit-1.0 feature is specified with an invalid configuration with auditFileHandler and a bad (unsupported)
     * event type along with one good event, then the audit service does not start, no events are emitted and no audit log is created.
     * 
     * Eg. 		<auditFileHandler> 
	 *	<events name="AllAuthn" eventName="SECURITY_AUTHN" />
	 *   <events name="SuccessForBadEvent" outcome="SUCCESS" eventName="BAD_EVENT" />
	 * </auditFileHandler>
     * 
     * Expected results:
     * - 1) A message CWWKS5855E is logged to indicate the event name is not supported and that the audit service is stopped.
     * - 2) Audit not started and no audit.log is generated because this is an invalid configuration. Expect exception.
     */
    @Test
    public void testConfigInvalidAndValidEventNames_noAuditLogGenerated() throws Exception {
    // 1. Reconfigure the server for single outcome. Expect denied messages.
    	
//    myTestServer.setIgnoredServerExceptions(configFailureExceptions);
//    myTestServer.searchValueInServerLog(AuditMessageConstants.CWWKS5855E_AUDIT_INVALID_MISSING_EVENT);
    
    myTestServer.reconfigServer("audit-1.0_outcomeSuccess_InvalidAndValidEvent.xml", name.getMethodName(), startFailedMsgs, true, Constants.JUNIT_REPORTING);
    myTestServer.searchValueInServerLog(AuditMessageConstants.CWWKS5855E_AUDIT_INVALID_MISSING_EVENT + ".*event name for outcome " + AuditEvent.OUTCOME_SUCCESS.toUpperCase() + ".*audit handler " + AuditService.AUDIT_FILE_HANDLER_NAME + " configuration.*following event names:.*"  + AuditConstants.SECURITY_AUTHN,"messages.log");
	// 2. Verify we have no audit log in the default log location

	File dir = new File(myTestServer.getServer().getLogsRoot());
	List<File> list = Arrays.asList(dir.listFiles(new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith("audit");
		}
	}));
	
    assertTrue("An audit log was found when none was expected for a configuration with invalid event name.", list.size() == 0);
    }
    /**
     * Test description:
     * When audit-1.0 feature is specified with an invalid configuration with auditFileHandler with good event types
     * but misspelled outcome (SUCCES vs SUCCESS), then the default is to log all outcomes for the specified events.
     * 
     * - Start the server with the audit-1.0 feature  
     * - Access a servlet unsuccessfully and successfully with Basic Authentication.
     * 
     * Eg. 			<auditFileHandler> 
	 *	<events name="MgmtMisspellSuccess" eventName="SECURITY_AUDIT_MGMT" outcome="SUCCES"/>
	 * 	<events name="AuthnMisspellSuccess" eventName="SECURITY_AUTHN" outcome="SUCCES"/>
	 *  </auditFileHandler>
     * 
     * Expected results:
     * - 1) A message is logged by config to indicate the outcome is not valid
     * - 2) Default is to log all outcomes for the specified events when an outcome is not valid
     */
    @Test
    public void testConfigEventsWithInvalidOutcomes_defaultsToAllOutcomesEmitted() throws Exception {
        // 1. Reconfigure the server for single outcome. Expect error message.
        myTestServer.addIgnoredServerException(AuditMessageConstants.CWWKG0032W_AUDIT_UNEXPECTED_CONFIG_VALUE);
        myTestServer.reconfigServer("audit-1.0_outcomeSuccess_EventsWithInvalidOutcomes.xml", name.getMethodName(), startMsgs, true, Constants.JUNIT_REPORTING);
        myTestServer.searchValueInServerLog(AuditMessageConstants.CWWKG0032W_AUDIT_UNEXPECTED_CONFIG_VALUE,"messages.log");
 
        // 2.  Set up audit stream to capture the entire audit file contents
        AuditFileStream allEvents = new AuditFileStream(auditLogFile);
        AuditAsserts asserts =  new AuditAsserts(logClass, allEvents);
       
        // 3. Access the servlet with bad userId and password to generate a denied AUTHN event
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,BADUSER, BADUSERPWD, HttpServletResponse.SC_UNAUTHORIZED);
        
        // 4. Access the servlet with good userId and password to generate a successful AUTH event
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,USER1, USER1PWD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

        // 5. Verify the following are found in the audit log
        // - the security audit management event is logged for start of the server with outcome=success
        // - AUTHN event for the Basic Authentication requests with outcomes denied, challenge, success
        asserts.assertFoundInData(
          	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
    			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START ,
    			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
        
       		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + BADUSER,
    			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
    			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
        
        	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
        
        	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    			AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + USER1,
    			AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    			AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
    			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
        }
}
