package com.ibm.ws.security.audit.fat.config;


import java.net.InetAddress;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016, 2025
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
/**
 * Test Description:
 * 
 * This is a test to verify that the setting of server ID is happening properly
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditUniqueServerIDTest extends AuditCommonTest {

    protected static LibertyServer myServer =
                    LibertyServerFactory.getLibertyServer("com.ibm.ws.security.audit.fat.uniqueserver");
    protected static Class<?> logClass = AuditUniqueServerIDTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    
    public AuditUniqueServerIDTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();
    
    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(logClass, "setUp", "Prep for test");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        auditLogFile = myServer.getLogsRoot() + "audit.log";
    }


    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer("TRAS0400W");
    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }


	/**
	 * Test Purpose: Verify that 
	 * observer server id is set to 
	 * server hostname is set to InetAddress.getLocalHost().getCanonicalHostName() 
	 * and server name is set to test server name 
	 * when the server.env is not being used.
	 *
	 **/
    @Mode(TestMode.LITE)
    @Test
    public void testAuditUniqueServerID_Success() throws Exception {
   
    String expectedServerHostName = InetAddress.getLocalHost().getCanonicalHostName();
    String expectedServerDirectory = myServer.getUserDir() + "/";
    String expectedServerName = myServer.getServerName();
    
    
    Log.info(logClass, getCurrentTestName(), "value of hostname is " + expectedServerHostName);
    Log.info(logClass, getCurrentTestName(), "value of server directory is " + expectedServerDirectory);
    Log.info(logClass, getCurrentTestName(), "value of server name is " + expectedServerName);
        	
    // Set up recent audit stream and audit asserts with recent audit data
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // Existing sample FAT test steps
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

    // Assert that all of the test patterns specified below are found in the audit data. The records must be found
    // in the order specified below. Extra records in between are ignored.
    asserts.assertFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "user1",
    				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    				AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
    				AuditEvent.OBSERVER_ID + "=" + "websphere: " + expectedServerHostName + ":" + expectedServerDirectory + ":" + expectedServerName,
    				AuditEvent.TARGET_ID + "=" + "websphere: " + expectedServerHostName + ":" + expectedServerDirectory + ":" + expectedServerName));

    // Assert that none of the test patterns specified below are found in the audit data.
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
            asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));
    }
   
}
