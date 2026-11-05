package com.ibm.ws.security.audit.fat.example;

import static org.junit.Assert.assertTrue;

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
import com.ibm.ws.security.audit.fat.common.tooling.AuditFileStream;
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
 * This is an example test showing the use of audit tooling provided in com.ibm.ws.security.audit_fat.common.tooling
 * 
 * Take the following steps to set up a new FAT test where you enable auditing and check for the audit
 * records generated for auditable events in your FAT test.
 * 
 * 1) Create a new project under an existing FAT project(fat/src) for your new audit tests
 *    Example:  com.ibm.ws.webcontainer.security.fat
 *    			com.ibm.ws.webcontainer.security.fat.audit
 *    
 * 2) Add to this project any FAT tests that cover unique flows which should generate audit records.
 * 3) In the audit FAT tests, create a new liberty server with changes below:
 * 
 * 	
 * 	    a) update the server.xml for your new audit tests to add the audit feature
 * 	          <featureManager>
 * 		          ....
 *		         <feature>audit-1.0</feature>
 *	          </featureManager>
 *
 * 		b) also add auditFileHandler configuration with compact=true to write the audit
 *         log in compact format rather than expanded JSON so the tests run more quickly. 
 *         You may want to set compact=false for easier readability while developing tests.
 *   
 *         Example:
 *	          <auditFileHandler 
 *	    	       compact="true">  
 *	          </auditFileHandler>
 * 	
 *        Optionally, you may filter on audit events and outcomes which you expect to generate from your 
 *        tests by adding the auditFileHandler events configuration. The example below 
 *        will filter the audit records to capture only the SECURITY_AUTHN events in the audit log.
 * 	
 *        <events name="AuthnOnly" eventName="SECURITY_AUTHN" />
 * 
 * 4) Edit your build-test.xml to add the audit tooling jars to your required libraries:
 * 
 *     <target name="addRequiredLibraries">
 *       <echo message="required.lib.dir is set to: ${required.lib.dir}"/>
 *       <copy todir="${required.lib.dir}" overwrite="yes"> 
 *           <fileset dir="../com.ibm.ws.security.audit_fat.common.tooling/collectedJars" includes="*.jar"/>
 *        </copy>
 *     </target>  
 *     
 *     
 *     Update your autofvt zipfile to include the audit common tooling jar:
 *     
 *     <target name="build-bucket-single">
 *        ...
 *        <zip destfile="${autofvt.zipfile}" update="true">
 *            <zipfileset dir="${basedir}/../com.ibm.ws.security.audit_fat.common.tooling/build/lib/" prefix="autoFVT/lib" includes="com.ibm.ws.security.audit_fat.common.tooling.jar" />
 *        </zip>      
 *      
 *        <unzip src="${autofvt.zipfile}" dest="." />
 *           
 *     </target>
 *     
 * 5) Update your .classpath by adding
 *    <classpathentry combineaccessrules="false" kind="src" path="/com.ibm.ws.security.audit_fat.common.tooling"/> 
 *
 * 6) All audit FAT tests should run on Java 8 and above, so annotate your test class with
 *    @MinimumJavaLevel(javaLevel = 8, runSyntheticTest = false)
 *
 * 7) Use the audit FAT tooling to instrument your FAT tests to check that the expected audit log entries are generated
 * for auditable events.  Around the FAT test steps which will generate audit records, you would add:
 *  
 *    a) Before your existing FAT test steps which will cause audit records to be generated,
 *       indicate that you want to capture the recent audit data by adding:
 *     
 *       RecentAuditFileStream recentAuditLogEntries = new RecentAuditFileStream(auditLogFile);
 *       AuditAsserts asserts =  new AuditAsserts(logClass, recentAuditLogEntries);
 * 
 *    b) After your existing FAT test steps which will result in audit events, add an
 *    audit assertion which will cause the audit log to be read and checked for the
 *    test pattern specified.  The test pattern specifies keys and values to be found in the
 *    JSON audit data.
 * 
 *    Below is an example of an assertion to check that a specified test pattern is found
 *    within a chunk of audit data in the audit log.  There is no need to specify every key and value
 *    for the audit record in the test pattern. Only the important fields which provide enough unique information  
 *    to identify the audit record are necessary.
 *    
 *    The test patterns are specified with a simple key=value format and nested JSON structures
 *    are represented with a format where a dot between the keys indicates nesting levels-- key1.key2=value.
 *     
 *     Example test pattern:
 *     
 *     asserts.assertFoundInData(
 *   		asserts.asJson("eventName=SECURITY_AUTHN",
 *  				 "target.credential.token=user1",
 *   				 "target.credential.token=BASIC",
 *   				 "outcome=success")) ;
 *  
 *     For this example, the audit data will be checked for a JSON audit record that contains the
 *     following keys and values:
 *
 *      "eventName": "SECURITY_AUTHN" 
 *     
 *        "target": {
 *         "credential": {
 *            "token": "user1",
 *            "type": "BASIC"
 *         }
 *         
 *       "outcome": "success"
 *       
 *       
 *      Example assert for finding a sequence of test patterns in specified order (will
 *            verify that the specified SECURITY_AUTH event is found BEFORE the specified SECURITY_AUTHZ event)
 *       
 *      asserts.assertFoundInData(
 *   		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
 *    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "user1",
 *   				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
 *   				AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
 *   				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
 *      	asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
 *   				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "user1",
 *   				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
 *   				AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
 *   				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
 *       
 *        
 *      c) Before your tests are run, you should add checks for messages to make sure that the Liberty server
 *      has started the audit feature and services as shown below:
 *      
 *      assertNotNull("FeatureManager did not report that feature update was complete",
 *    		  server.waitForStringInLogUsingMark("CWWKF0008I"));
 *      assertNotNull("FeatureManager did not report the audit feature was installed",
 *            server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKF0021I_AUDIT_FEATURE_INSTALLED));
 *      assertNotNull("Audit service did not report it was ready",
 *            server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKS5851I_AUDIT_SERVICE_READY));
 *      assertNotNull("Audit file handler service did not report it was ready",
 *   	      server.waitForStringInLogUsingMark(AuditMessageConstants.CWWKS5805I_AUDIT_FILEHANDLER_SERVICE_READY));
 *    
 * See the AuditExampleTest below which demonstrates the usage of the audit FAT tooling to
 * check for specific fields and values in the JSON audit data which is generated by auditable
 * events.
 * 
 * The example FAT tests are commented to highlight the items below
 * - the existing FAT test steps which result in auditable events in the audit log
 * - the audit tooling steps that were added to verify the existence of expected audit log entries.
 *    
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuditExampleTest extends AuditCommonTest {

    protected static LibertyServer myServer =
                    LibertyServerFactory.getLibertyServer("com.ibm.ws.security.audit.fat.example");
    protected static Class<?> logClass = AuditExampleTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    
    public AuditExampleTest() {
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
        
        // Verify that the audit feature has started before running FAT tests
        verifyServerStartedWithAuditFeature(myServer);
        
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        auditLogFile = myServer.getLogsRoot() + "audit.log";
    }


    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
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
	 * Test Purpose: Demonstrate usage of asserts.assertFoundInData to check recent audit log entries for
	 * a single expected audit record which contains the test pattern specified in the assert.
	 * 
	 * Verify that an audit record shows success for authentication with
	 * Basic authentication and a valid user and password using assertFoundInData to
	 * test for single audit log entry.
	 *
	 **/
    @Test
    public void testAuditExampleTest_AssertFoundInData_FindSingleAuditLogEntry_CheckRecentEntries() throws Exception {
    // 1. Audit tooling - Set up recent audit stream and audit asserts with recent audit data.
    //      This will result in the recent audit stream containing data from this point until
    //      an audit assertion is issued to read from the stream. he asserts search this chunk of audit data.
    RecentAuditFileStream recentAuditEntries = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recentAuditEntries);
    
    // 2. Existing FAT test steps -  access servlet successfully with Basic Authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

    // 3. Audit tooling - Assert that the audit log contains the specified fields and values
    //      fields and values specified in the audit entries generated by the successful authentication.
    //
    //      This reads the audit data starting from the point where the RecentAuditFileStream was created (the recent audit data).
    //      It skips over audit log records that don't match.
    //      The assertFoundInData will pass if an audit record with the specified fields and values
    //      is found within the recent audit data.
    asserts.assertFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "user1",
    				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    				AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS)) ;
    }
    
	/**
	 * Test Purpose: Demonstrate usage of asserts.assertNotFoundInData to check recent audit log entries to
	 * ensure that the specified test pattern does not occur in the recent audit data.
	 * 
	 * Verify that for a basic authentication request, that there is no audit log entries generated with
	 * a target credential type of CUSTOM.
	 *
	 **/
    @Test
    public void testAuditExampleTest_AssertNotFoundInData_SingleEntryNotFound_CheckRecentEntries() throws Exception {
    // 1. Audit tooling - Set up recent audit stream and audit asserts with recent audit data.
    //      This will result in the recent audit stream containing data from this point until
    //      an audit assertion is issued to be read from the stream. The asserts search this chunk of audit data.
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // 2. Existing FAT test steps -  access servlet successfully with Basic Authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");
    
    //3. Audit tooling - Assert that NONE of the test patterns specified below are found in the same recent audit data.
    asserts.assertNotFoundInData(asserts.asJson(AuditEvent.TARGET_CREDENTIAL_TYPE + "=CUSTOM"));

    }
	/**
	 * Test Purpose: Demonstrate usage of asserts.assertFoundInData to search the entire audit log for
	 * a single expected audit record which contains the test pattern specified in the assert.
	 * 
	 * Note: if you are searching the entire audit log, you must make sure that this record is 
	 * unique or to include enough context in a sequence of records to ensure that the sequence is unique 
	 * within the entire set of audit log entries generated by your test.
	 * 
	 * Verify that an audit record is found in the audit log for a failed authentication with
	 * the user name "unknownUser".
	 *
	 **/
    @Mode(TestMode.LITE)
    @Test
    public void testAuditExampleTest_AssertFoundInData_FindSingleRecord_CheckEntireLog() throws Exception {
    // 1. Audit tooling - Set up audit stream and audit asserts to check the entire audit log.
    //      This will result in the an audit stream containing all entries in the audit log from the beginning of the log until
    //      the point where an audit assertion is issued to be read from the stream. 
    AuditFileStream allEntries = new AuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, allEntries);
    
    // 2. Existing FAT test steps -  access servlet successfully with Basic Authentication
    executeGetRequestBasicAuthCreds(httpclient, urlBase+ AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"unknownUser", "unknownPassword", HttpServletResponse.SC_UNAUTHORIZED);
 
    // 3. Audit tooling - Assert that the audit log contains the specified fields and values
    //      fields and values specified in the audit entries generated by the successful authentication.
    //
    //      This reads the audit data starting from the beginning of the log to the point where the assertFoundInData is issued.
    //      It skips over audit log records that don't match.
    //      The assertFoundInData will pass if an audit record with the specified test pattern
    //      is found within the recent audit data.
    asserts.assertFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "unknownUser",
    				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED)) ;
    }
	/**
	 * Test Purpose: Demonstrate usage of asserts.isFoundInData which returns true/false
	 * if a single expected audit record is found in the recent audit log entries.
	 * 
	 * Verify that an audit record shows success for authentication with
	 * Basic authentication and a valid user and password using isFoundInData to
	 * test for single audit log entry.
	 *
	 **/
    @Test
    public void testAuditExampleTest_IsFoundInData_FindSingleRecord_CheckRecentLogEntries() throws Exception {
    // 1. Audit tooling - Set up recent audit stream and audit asserts with recent audit data.
    //      This will result in the recent audit stream containing data from this point until
    //      an audit assertion is added to read from the stream. he asserts search this chunk of audit data.
	RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // 2. Existing FAT test steps -  access servlet successfully with Basic Authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

    // 3. Audit tooling - use Junit assertTrue to test result from isFoundInData which looks for specified
    //      fields and values in the audit entries generated by the successful authentication.
    //
    //      This reads the audit data starting from the point where the RecentAuditFileStream was created (the recent audit data).
    //      It skips over audit log records that don't match.
    //      The assertFoundInData will pass if an audit record with the specified fields and values
    //      is found within the recent audit data.
    assertTrue(asserts.isFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "user1",
    				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    				AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS) ));
    }
    
	/**
	 * Test Purpose: Demonstrate usage of asserts.isFoundInData which returns true/false
	 * if a single expected audit record is found to match the regular expressions in the
	 * search pattern where   
	 * 	   target.credential.token includes "user" in the name as checked with regEx -- "^.*user.*"
	 *     target.name is a URI ending in "/AuditBasic" as checked with regEx -- "^.*AuditBasic"
	 *     reason.reasonCode starting with 20 as checked with regEx -- "^.*20.*"
	 * 
	 * Regular expressions in search patterns must be anchored at the start with 
	 * the string "^.*"  This informs the tooling that a regular expression is to be used
	 * instead of a string compare.
	 * 
	 * Verify that an audit record is found to match the regular expression. 
	 *
	 **/
    @Test
    public void testAuditExampleTest_IsFoundInData_usingRegularExpression_CheckRecentLogEntries() throws Exception {
    // 1. Audit tooling - Set up recent audit stream and audit asserts with recent audit data.
    //      This will result in the recent audit stream containing data from this point until
    //      an audit assertion is added to read from the stream. he asserts search this chunk of audit data.
	RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // 2. Existing FAT test steps -  access servlet successfully with Basic Authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

    // 3. Audit tooling - use Junit assertTrue to test result from isFoundInData which looks for specified
    //      fields and values in the audit entries generated by the successful authentication.
    //
    //      This reads the audit data starting from the point where the RecentAuditFileStream was created (the recent audit data).
    //      It skips over audit log records that don't match.
    //      The assertFoundInData will pass if an audit record with the specified fields and values
    //      is found within the recent audit data.
    assertTrue(asserts.isFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "^.*user.*",
    				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    				AuditEvent.TARGET_NAME + "=" + "^.*/AuditBasic",
    				AuditEvent.REASON_CODE + "=" + "^.*20.*",
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS) ));
    }
	/**
	 * Test Purpose: Demonstrate usage of asserts.assertFoundInData to check recent audit log entries for
	 * a sequence of multiple expected audit records which contain the test pattern specified in the assert.
	 * Then use asserts.assertNotFoundInData to check the same audit entries to ensure that there are none
	 * which contain the test pattern of outcome=denied.
	 * 
	 * Verify that an audit record shows success (outcome=success) for successful authentication and authorization for user1, 
	 * and verify that there are no authentication or authorization events in the audit data showing failing outcomes (outcome=denied).
	 *
	 **/
    @Mode(TestMode.LITE)
    @Test
    public void testAuditExampleTest_AssertFoundInData_AssertNotFoundInData_FindMulipleRecordsInSequence_CheckRecentLogEntries() throws Exception {
    	
    // 1. Audit tooling - Set up recent audit stream and audit asserts with recent audit data.
    //      This will result in the recent audit stream containing data from this point until
    //      an audit assertion is added to read from the stream.
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // 2. Existing FAT test steps -  access servlet successfully with Basic Authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

    // 3. Audit tooling - Assert that the audit log contains multiple audit records in the specified order.
    //
    //      This reads the audit data starting from the point where the RecentAuditFileStream was created (the recent audit data).
    //
    //      The assertFoundInData will pass if both audit records are found in the SPECIFIED ORDER
    //      within the recent audit data.  There may be other records in between these records and those
    // 		will be ignored.
    asserts.assertFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "user1",
    				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    				AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
       		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
    				AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "user1",
    				AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
    				AuditEvent.TARGET_NAME + "=" + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

    //4. Audit tooling - Assert that NONE of the test patterns specified below are found in the same recent audit data.
    asserts.assertNotFoundInData(
    		asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED),
            asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
    				AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));
    }

	/**
	 * Test Purpose: Demonstrate usage of asserts.assertFoundInData to check recent audit log entries to
	 * verify that the entries specified using a regular expression are not found in the audit log.
	 * Then use asserts.assertNotFoundInData to check that 
	 * a) There are no SECURITY_AUTHN events with Request URI containing the name "AuditFormLoginServlet"
	 * b) There are no target credential tokens containing the user name with characters "user2"
	 * 
	 **/
    @Test
    public void testAuditExampleTest_AssertNotFoundInData_usingRegularExpression_CheckRecentLogEntries() throws Exception {
    	
    // 1. Audit tooling - Set up recent audit stream and audit asserts with recent audit data.
    //      This will result in the recent audit stream containing data from this point until
    //      an audit assertion is added to read from the stream.
    RecentAuditFileStream recent = new RecentAuditFileStream(auditLogFile);
    AuditAsserts asserts =  new AuditAsserts(logClass, recent);
    
    // 2. Existing FAT test steps -  access servlet successfully with Basic Authentication
    String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
    verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");


    //3. Audit tooling - Assert that NONE of the test patterns specified below are found in the same recent audit data.
    asserts.assertNotFoundInData(
            asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
            		AuditEvent.TARGET_CREDENTIAL_TOKEN + "=" + "^.*user2.*"));
            asserts.assertNotFoundInData(
                    asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                    		AuditEvent.TARGET_NAME + "=" + "^.*AuditFormLoginServlet.*"));
    			
 
    } 
}
