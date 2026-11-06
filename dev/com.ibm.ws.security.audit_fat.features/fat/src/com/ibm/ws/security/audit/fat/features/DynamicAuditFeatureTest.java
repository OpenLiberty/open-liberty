/*******************************************************************************
 * Copyright (c) 2016, 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
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
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts.AuditData.EmptyAuditDataException;
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
 * The test verifies that a dynamic addition and removal of the audit-1.0 feature and auditFileHandler from server.xml
 * either enables or disables the audit function. 
 * 
 * It also covers the interaction of the audit-1.0 feature with other security features like appSecurity and servlet.
 */

//TODO: Story 242279 deals with the audit feature in conjunction with appSecurity-2.0 and servlet features and tests should be added
// when dependencies are removed in product code.

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class DynamicAuditFeatureTest extends AuditCommonTest {

	public static TestServer myTestServer = new TestServer("com.ibm.ws.security.audit.fat.dynamic","appSecurity-2.0_servlet-3.1.xml");
    protected static Class<?> logClass = DynamicAuditFeatureTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    
    static List<String> checkApps = new ArrayList<String>();	
	static List<String> startMsgsWithAudit = new ArrayList<String>();
	static List<String> startMsgsNoAudit = new ArrayList<String>();
	static List<String> startMsgsUpdateAdd = new ArrayList<String>();
	static List<String> startMsgsUpdateRemove = new ArrayList<String>();
	static List<String> startMsgsAppUpdated = new ArrayList<String>();
	static List<String> startMsgsUpdateHandler = new ArrayList<String>();
    
    private static String USER1 = "user1";
    private static String USER1PWD = "user1pwd";
    
    private static final String javaVersion = System.getProperty("java.version");
      
    public DynamicAuditFeatureTest() {
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
        
        // Messages expected for start without audit - just security service starts
     	startMsgsNoAudit.add(AuditMessageConstants.CWWKS0008I_SECURITY_SERVICE_READY);
        
        // Messages expected for start of audit with good server config
     	startMsgsWithAudit.add(AuditMessageConstants.CWWKF0012I_AUDIT_FEATURE_INSTALLED);
     	startMsgsWithAudit.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY);
     	startMsgsWithAudit.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);
    	
    	// Messages expected when updating and removing the audit feature
        startMsgsUpdateRemove.add(AuditMessageConstants.CWWKF0008I_FEATURE_UPDATE_COMPLETE);
        startMsgsUpdateRemove.add(AuditMessageConstants.CWWKF0013I_AUDIT_FEATURE_REMOVED);
        
    	// Messages expected when updating or adding the audit feature 
     	startMsgsUpdateAdd.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY);
     	startMsgsUpdateAdd.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);
        
    	// Messages expected when updating or removing the auditFileHandler
        startMsgsUpdateHandler.add(AuditMessageConstants.CWWKS5806I_AUDIT_FILEHANDLER_STOPPED);
        startMsgsUpdateHandler.add(AuditMessageConstants.CWWKS5804I_AUDIT_FILEHANDLER_STARTING);
        startMsgsUpdateHandler.add(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY);
        startMsgsUpdateHandler.add(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY);
         
        // Messages expected for start without audit - just security service starts
     	startMsgsAppUpdated.add("CWWKZ0003I.* AuditBasicAuthServlet");
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
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to remove the audit-1.0 feature and we get an audit stop event logged for the auditFileHander and audit feature service
     * - Access a servlet again with basic authentication and verify that no audit records are generated.
     * Expected results:
     * - 1) The audit.log should contain AUTHN event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to remove audit, when we try to read audit data we
     *    get an EmptyAuditDataException with "No audit data was read from the audit log."
     */
    @Test
    public void testDynamicallyRemoveAuditFeatureFromConfig() throws Exception {
   	
    // 1. Start server with appSecurity-2.0, servlet-3.1, audit-1.0 and auditFileHander.
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
   
    // 2.  Set up recent audit stream and audit asserts with recent audit data
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // 3. Access a servlet using basic auth
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

    // 4. Verify that audit data is found in the audit log for the basic authentication 
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;

    
    RecentAuditFileStream recent2 = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts2 =  new AuditAsserts(logClass, recent2);
    
    // 5. Reconfigure the server to remove audit-1.0 feature.
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_auditFileHandler.xml", name.getMethodName(), startMsgsUpdateRemove, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);

    // 6. Verify that audit data is found in the audit log for the audit file handler and audit feature service stop events
  asserts2.assertFoundInData(
  	asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
  			AuditEvent.OBSERVER_NAME + "=" + HANDLER_DEFAULT_FILEHANDLER,
  			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_STOP ,
  			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
  	asserts2.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
  			AuditEvent.OBSERVER_NAME + "=" + SERVICE_AUDIT_SERVICE,
  			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_STOP ,
  			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    
    // 7. Access servlet using BasicAuth 
    RecentAuditFileStream recentUpdate = new RecentAuditFileStream(auditLogFile);
    asserts =  new AuditAsserts(logClass, recentUpdate);
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    
    // 8. Verify that servlet access after audit is removed does not result in any audit records.
    // We catch an EmptyAuditDataException when trying to read from the audit log because no
    // new audit data was logged after the audit-1.0 feature was removed.
    
    thrown.expect(EmptyAuditDataException.class);
    thrown.expectMessage("No audit data was read from the audit log.");
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN)) ;
    }
    
    /**
     * Test description:
     * - Begin the test with only appSecurity-2.0 and servlet-3.1 (no audit feature) in server.xml.
     * - Access a servlet with basic authentication and verify that no audit records are generated and no audit log is created.
     * - Then reconfigure the server to add the audit-1.0 feature.
     * - Access a servlet again with basic authentication and verify that audit records are generated for
     *   start of the audit service and basic auth.
     * Expected results:
     * - 1) No audit log is created when the audit-1.0 feature is not present.
     * - 2) The audit.log should contain audit file handler and audit service start along with the AUTHN event after server is updated to add audit-1.0.
     */
     @Test
    public void testDynamicallyAddAuditFeatureToConfig() throws Exception {
    // 1. Start server with appSecurity-2.0 and servlet-3.1 (no audit feature)
	myTestServer.startServer("appSecurity-2.0_servlet-3.1.xml", name.getMethodName(), checkApps, startMsgsNoAudit, Constants.JUNIT_REPORTING,null);
    
    // 2. Access a servlet using Basic authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 3. Verify that no audit log is generated when the audit-1.0 feature is not present. 
    List<File> list = returnListOfAuditLogs(myTestServer.getServer());
    assertTrue("Audit log file exists when none was expected.", list.size() == 0);
   
    // 4. Reconfigure the server to add the audit-1.0 feature.
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0.xml", name.getMethodName(), startMsgsUpdateAdd, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
    
    // 5.  Set up an audit stream and audit asserts for all entries in the audit.log. 
    AuditFileStream allAuditEntries = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allAuditEntries);
    
    // 6. Access a servlet using basic authentication.
    String response2 = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response2, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

    // 7. Verify that audit data is found in the audit log for the audit start event for both the audit feature service and audit file handler and the security AUTHN event 
		asserts.assertFoundInData(
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + SERVICE_AUDIT_SERVICE,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + HANDLER_DEFAULT_FILEHANDLER,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }
 
    /**
     * Test description:
     * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
     * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
     * - Then reconfigure the server to remove BOTH the audit-1.0 feature and the auditFileHandler.
     * - Verify that a new audit.log is created with start and stop events for the audit file handler and audit feature service.
     * - Access a servlet again with basic authentication and verify that no audit records are generated.
     * Expected results:
     * - 1) The audit.log should contain AUTHN event after server with audit-1.0, servlet and appSecurity
     *    is started and the servlet is accessed.
     * - 2) After the server has been reconfigured to remove audit and auditFileHandler, the auditFileHandler is removed
     * first and results in the old log being archived and a new log created with an audit start event for starting of audit
     * with a default file handler and the audit feature service. Next, the removal of the audit-1.0 feature is processed and 
     * it results in an audit stop event in the new log for the audit file handler and audit feature service.
     * - 3) Next when we try to read audit data after another servlet access, we get an EmptyAuditDataException with
     *  "No audit data was read from the audit log." since audit is no longer active with removal of the audit feature.
     */
    @Test
    public void testDynamicallyRemoveAuditFeatureAndAuditFileHandlerFromConfig() throws Exception {
   	
    // 1. Start server with appSecurity-2.0, servlet-3.1, audit-1.0 and auditFileHander.
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
   
    // 2.  Set up recent audit stream and audit asserts with recent audit data
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // 3. Access a servlet using basic auth
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

    // 4. Verify that audit data is found in the audit log for the basic authentication 
    asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;

    AuditFileStream auditStream = new AuditFileStream(auditLogFile);
    asserts =  new AuditAsserts(logClass, auditStream);
    
    // 5. Reconfigure the server to remove audit-1.0 feature and auditFileHandler.
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1.xml", name.getMethodName(), startMsgsUpdateRemove, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);

    // 6. Verify there are now 2 audit logs, one archived the other not
    List<File> list = returnListOfAuditLogs(myTestServer.getServer());  
    assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
    
    // 7. Verify that audit data is found in the new audit log for the audit start and stop events for the audit feature service and file handler. 
		asserts.assertFoundInData(
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + SERVICE_AUDIT_SERVICE,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + HANDLER_DEFAULT_FILEHANDLER,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + HANDLER_DEFAULT_FILEHANDLER,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_STOP,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + SERVICE_AUDIT_SERVICE,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_STOP,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
    
    // 8. Access servlet using BasicAuth 
    RecentAuditFileStream recentUpdate = new RecentAuditFileStream(auditLogFile);
    asserts =  new AuditAsserts(logClass, recentUpdate);
    response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    
    // 9. Verify that servlet access after audit is removed does not result in any audit records.
    // We catch an EmptyAuditDataException when trying to read from the audit log because no
    // new audit data is logged after the audit-1.0 feature has been removed.
    
    thrown.expect(EmptyAuditDataException.class);
    thrown.expectMessage("No audit data was read from the audit log.");
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN)) ;
    }
    /**
     * Test description:
     * - Begin the test with only appSecurity-2.0 and servlet-3.1 (no audit feature) in server.xml.
     * - Access a servlet with basic authentication and verify that no audit records are generated and no audit log is created.
     * - Then reconfigure the server to add the audit-1.0 feature and auditFileHandler.
     * - Access a servlet again with basic authentication and verify that audit records are generated for
     *   start of the audit service and the audit file handler along with the basic auth event.
     * Expected results:
     * - 1) No audit log is created when the audit-1.0 feature is not present.
     * - 2) The audit.log should contain AUDIT MGMT events for start of the audit feature service and audit file handler as well as
     *   the AUTHN event after server is updated to add audit-1.0 and the auditFileHandler.
     */
     @Test
    public void testDynamicallyAddAuditFeatureAndAuditFileHandlerToConfig() throws Exception {
    // 1. Start server with appSecurity-2.0 and servlet-3.1 (no audit feature)
    myTestServer.startServer("appSecurity-2.0_servlet-3.1.xml", name.getMethodName(), checkApps, startMsgsNoAudit, Constants.JUNIT_REPORTING,null);
    
    // 2. Access a servlet using Basic authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);
    
    // 3. Verify that no audit log is generated when the audit-1.0 feature is not present. 
    List<File> list = returnListOfAuditLogs(myTestServer.getServer());
    assertTrue("Audit log file exists when none was expected.", list.size() == 0);
   
    // 4. Reconfigure the server to add the audit-1.0 feature.
    myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), startMsgsUpdateAdd, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
    
    // 5.  Set up an audit stream and audit asserts for all entries in the audit.log. 
    AuditFileStream allAuditEntries = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allAuditEntries);
    
    // 6. Access a servlet using basic authentication.
    String response2 = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response2, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

    // 7. Verify that audit data is found in the audit log for the audit start events for audit service and file handler as well as the basic authentication 
		asserts.assertFoundInData(
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + SERVICE_AUDIT_SERVICE,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + HANDLER_DEFAULT_FILEHANDLER,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    }
     
     /**
      * Test description:
      * - Begin the test with the audit-1.0 feature and auditFileHandler enabled in the server.xml.
      * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
      * - Then reconfigure the server to remove ONLY the auditFileHandler configuration.
      * - Verify that a new audit.log is created with start of audit and a default audit file handler.
      * - Access a servlet again with basic authentication and verify that security AUTHN event is logged.
      * Expected results:
      * - 1) The audit.log should contain AUTHN event after server with audit-1.0, servlet and appSecurity
      *    is started and the servlet is accessed.
      * - 2) After the server has been reconfigured to remove just the auditFileHandler, the auditFileHandler is removed
      *    and results in the stop of the audit service with this file handler and the archive of the audit log.  
      *    The new audit log is created and contains an audit start event for starting of audit with a default file handler and 
      *    starting the audit feature service with that new default handler.
      * - 3) After another servlet access, audit is still active and the audit log contains the security AUTHN event.
      */
     @Test
     public void testDynamicallyRemoveAuditFileHandlerOnlyFromConfig() throws Exception {
    	
     // 1. Start server with appSecurity-2.0, servlet-3.1, audit-1.0 and auditFileHander.
 	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);
    
     // 2.  Set up recent audit stream and audit asserts with recent audit data
     RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
     AuditAsserts asserts =  new AuditAsserts(logClass, recent);
     
     // 3. Access a servlet using basic auth
     String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
     verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

     // 4. Verify that audit data is found in the audit log for the basic authentication 
     asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
     				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;

     
     AuditFileStream auditStream = new AuditFileStream(auditLogFile);
     asserts =  new AuditAsserts(logClass, auditStream);
     
     // 5. Reconfigure the server to remove only the auditFileHandler. The audit-1.0 feature remains in the config.
     myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0.xml", name.getMethodName(), startMsgsUpdateHandler, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);

     // 6. Verify there are now 2 audit logs, one archived the other not
     List<File> list = returnListOfAuditLogs(myTestServer.getServer());  
     assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
     
     // 7. Verify that audit data is found in the new audit log for an audit start event for the audit service and the
     // default audit file handler. This is a result of audit stopping the old file handler and restarting with the default file handler. 
		asserts.assertFoundInData(
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + SERVICE_AUDIT_SERVICE,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
				asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
						AuditEvent.OBSERVER_NAME + "=" + HANDLER_DEFAULT_FILEHANDLER,
						AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
						AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
     
     // 8. Access servlet using BasicAuth 
     RecentAuditFileStream recentUpdate = new RecentAuditFileStream(auditLogFile);
     asserts =  new AuditAsserts(logClass, recentUpdate);
     response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
     
     
     // 9. Verify that auditing is activated and audit data is found in the audit log for the basic authentication
     asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
   				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
     }
     
 

  /**
  * Test description:
  * - Begin the test with the audit-1.0 feature (no auditFileHandler) enabled in the server.xml such that the default file handler is used.
  * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
  * - Then reconfigure the server to add an auditFileHandler configuration.
  * - Verify that a new audit.log is created with start events for the audit feature service with new file handler configuration.
  * - Access a servlet again with basic authentication and verify that audit records are generated.
  * Expected results:
  * - 1) The audit.log should contain AUTHN event after server with audit-1.0, servlet and appSecurity
  *    is started and the servlet is accessed.
  * - 2) After the server has been reconfigured to add just the auditFileHandler, the old log is archived and a new log created with
  *    an audit start event for starting of audit feature service with the new auditFileHandler.
  * - 3) After another servlet access, audit is still active and the audit log contains the security AUTHN event.
  */
 @Test
 public void testDynamicallyAddAuditFileHandlerOnlyToConfig() throws Exception {
	
 // 1. Start server with appSecurity-2.0, servlet-3.1, audit-1.0.
	myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);

 // 2.  Set up recent audit stream and audit asserts with recent audit data
 RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
 AuditAsserts asserts =  new AuditAsserts(logClass, recent);
 
 // 3. Access a servlet using basic auth
 String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
 verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

 // 4. Verify that audit data is found in the audit log for the basic authentication 
 asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
 				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;

 
 AuditFileStream auditStream = new AuditFileStream(auditLogFile);
 asserts =  new AuditAsserts(logClass, auditStream);
 
 // 5. Reconfigure the server to add only the auditFileHandler.
 myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_auditFileHandler.xml", name.getMethodName(), startMsgsUpdateHandler, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);

 // 6. Verify there are now 2 audit logs. The original audit log with default file handler is archived when audit is stopped.
 List<File> list = returnListOfAuditLogs(myTestServer.getServer());  
 assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);
 
 // 7. Verify that audit data is found in the new audit log with audit start events for the audit feature service and new audit file handler
  asserts.assertFoundInData(
			asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
			AuditEvent.OBSERVER_NAME + "=" + SERVICE_AUDIT_SERVICE,
			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUDIT_MGMT,
			AuditEvent.OBSERVER_NAME + "=" + HANDLER_DEFAULT_FILEHANDLER,
			AuditEvent.TARGET_TYPEURI + "=" + SERVICE_AUDIT_START,
			AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
 
 // 8. Access servlet using BasicAuth 
 RecentAuditFileStream recentUpdate = new RecentAuditFileStream(auditLogFile);
 asserts =  new AuditAsserts(logClass, recentUpdate);
 response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
  
 // 9. Verify that audit data is found in the audit log for the basic authentication 
 asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
 }
 /**
  * Test description:
  * - Begin the test with the audit-1.0 feature enabled in the server.xml.
  * - Access a servlet with basic authentication and verify that audit records are generated in the audit log.
  * - Then reconfigure the server to remove the application role mapping (user1 to audit_basic role) from the application-bnd section in server.xml.
  * - Access a servlet again with basic authentication and verify that authorization fails and the audit record reflects that authorization failed because
  *   the user is not in the required role.
  * Expected results:
  * - 1) The audit.log should contain a successful AUTHN and AUTHZ event after server with audit-1.0, servlet and appSecurity
  *    is started and the servlet is accessed.
  * - 2) After the server has been reconfigured to remove the audit_basic role from application-bnd, when we access the application
  *   again, we get a failing AUTHZ audit record for the audit_basic role.
  */
 @Test
 public void testDynamicallyRemoveApplicationRoleFromConfig() throws Exception {
	
 // 1. Start server with appSecurity-2.0, servlet-3.1, audit-1.0.
	  myTestServer.startServer("appSecurity-2.0_servlet-3.1_audit-1.0.xml", name.getMethodName(), checkApps, startMsgsWithAudit, Constants.JUNIT_REPORTING,null);

 // 2.  Set up recent audit stream and audit asserts with recent audit data
 RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
 AuditAsserts asserts =  new AuditAsserts(logClass, recent);
 
 // 3. Access a servlet using basic auth
 String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
 verifyUserResponse(response, GET_USER_PRINCIPAL + USER1, GET_REMOTE_USER + USER1);

 // 4. Verify that audit data is found in the audit log for the basic authentication and authorization of user1 in audit_basic role
	asserts.assertFoundInData(
			asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
					AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
			asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
		              AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
		              AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
		              AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
		              AuditEvent.TARGET_ROLE_NAMES + "=[audit_basic]"));

 // 5. Reconfigure the server to remove application role mapping from the application-bnd section of server.xml
 myTestServer.reconfigServer("appSecurity-2.0_servlet-3.1_audit-1.0_removedAppRole.xml", name.getMethodName(), startMsgsAppUpdated, Constants.DO_NO_RESTART_SERVER, Constants.JUNIT_REPORTING);
 
 // 6. Access servlet using BasicAuth 
 RecentAuditFileStream recentUpdate = new RecentAuditFileStream(auditLogFile);
 asserts =  new AuditAsserts(logClass, recentUpdate);
 response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_FORBIDDEN);
 
 // 7. Verify that servlet access after removal of the application role results in generation of a failed AUTHZ audit record for user1 and audit_basic role.
 asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
         AuditEvent.TARGET_NAME + "=/AuditBasicAuthServlet/AuditBasic",
         AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE,
         AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
         AuditEvent.TARGET_ROLE_NAMES + "=[audit_basic]"));
 }

     
}
