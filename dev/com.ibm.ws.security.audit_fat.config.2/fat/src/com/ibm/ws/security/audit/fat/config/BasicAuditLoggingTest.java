package com.ibm.ws.security.audit.fat.config;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.AuditFileStream;
import com.ibm.ws.security.audit.fat.config.utils.AuditTestHelper;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;


@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class BasicAuditLoggingTest extends AuditCommonTest {
	private static String ARCHIVING_CONFIG_FILE = "/archiving.xml";
    private static String GOOD_LOG_DIRECTORY_CONFIG_FILE = "/goodLogDirectory.xml";
    private static String BAD_LOG_DIRECTORY_CONFIG_FILE = "/badLogDirectory.xml";
    private static String MAX_FILES_ZERO_CONFIG_FILE = "/maxFilesZero.xml";
    private static String MAX_FILESIZE_ZERO_CONFIG_FILE = "/maxFileSizeZero.xml";
    private static String MAX_FILES_LESS_THAN_ZERO_CONFIG_FILE = "/maxFilesLessThanZero.xml";
    private static String MAX_FILESIZE_LESS_THAN_ZERO_CONFIG_FILE = "/maxFileSizeLessThanZero.xml";
    
    private static int DEFAULT_DELAY_SECONDS = 3;

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.audit.fat.logging");
    private static Class<?> myLogClass = BasicAuditLoggingTest.class;
    protected DefaultHttpClient httpclient;
    protected static String urlBase;
    protected static String auditLogFile;
    
    private static final String javaVersion = System.getProperty("java.version");


    public BasicAuditLoggingTest() {
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

    }

    @AfterClass
    public static void tearDown() throws Exception {
    	// expect an error when trying to delete an invalid directory path
        myServer.stopServer("CWWKS5802E");
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
     * Test Purpose: Verify that we have generated 2 audit logs, one that is an archived audit log
     * and the other as audit.log.  Verify that archiving logging behavior: verify the event sequence 
     * number of the first record in audit.log sequentially follows the event sequence number of the last
     * record in the archived audit.log
     *
     **/

    //@Mode(TestMode.LITE)
    //@Test
    
    // commenting out for now until we have a full handle on the number of additional audit fields to 
    // add to the authn and authz audit records.  Else, what is happening, as we add additional audit
    // instrumentation is, we're generating longer records and more logs.
    public void testBasicAuditLogging_verifyArchiving_verifySequentialEventNumbers() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");
        myServer.setServerConfigurationFile(ARCHIVING_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

       
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        for (int i = 0; i < 800; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        }
        
        sleep(DEFAULT_DELAY_SECONDS);

        // verify there are 2 audit logs, one archived the other not
        Log.info(myLogClass, "testBasicAuditLogging_verifyArchiving_verifySequentialEventNumbers", "myServer.getLogsRoot = " + myServer.getLogsRoot());

        List<File> list = returnListOfAuditLogs();  

        assertTrue("Expected two audit logs file, one archived; one not", list.size() == 2);

        AuditFileStream recent1 = new AuditFileStream(list.get(0).getAbsolutePath());
        AuditFileStream recent2 = new AuditFileStream(list.get(1).getAbsolutePath());

        Integer archivedLogLastSequenceNumber = -1;
        Integer firstNewLogSequenceNumber = -1;

        AuditTestHelper asserts1 = new AuditTestHelper(myLogClass, recent1);
        AuditTestHelper asserts2 = new AuditTestHelper(myLogClass, recent2);

        if (!list.get(0).getAbsolutePath().endsWith("audit.log")) {
            // first log is archived, so get the last sequence number from the first log, and the first sequence number from the second log
            archivedLogLastSequenceNumber = new Integer(asserts1.lastSequenceNumber());
            firstNewLogSequenceNumber = new Integer(asserts2.firstSequenceNumber());
        } else {
            // second log is archived, so get the last sequence number from the second log, and the first sequence number from the first log
            firstNewLogSequenceNumber = new Integer(asserts1.firstSequenceNumber());
            archivedLogLastSequenceNumber = new Integer(asserts2.lastSequenceNumber());
        }

        assertTrue("Event sequence numbers have not continued sequentially correctly after audit.log was archived.",
                   (firstNewLogSequenceNumber.intValue() == archivedLogLastSequenceNumber.intValue() + 1));
        
        myServer.stopServer("TRAS0400W");

    }
    
    /**
     * Test Purpose: Verify that we have successfully generated an audit log to the non-default valid configured log directory path.
     *      *
     **/
    
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuditLogging_validate_log_directory_config_goodPath() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");

        myServer.setServerConfigurationFile(GOOD_LOG_DIRECTORY_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);


        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        // verify there are no audit logs in the default log directory


        List<File> list = returnListOfAuditLogs();     
        
        assertTrue("Expected audit logs in the valid configured log location", list.size() == 0);

        // verify there are logs in the specified valid path

        File dir = new File(myServer.getLogsRoot().concat("goodLogsPath"));
        list = Arrays.asList(dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("audit");
            }
        }));

        assertTrue("Expected audit logs in the valid configured log location", list.size() > 0);
    	myServer.stopServer("CWWKZ0014W", "TRAS0400W");

    }
    
    /**
     * Test Purpose: Verify that we do not succeed in generating an audit log to the non-default invalid (non-existent) configured log directory path.
     *      *
     **/

    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuditLogging_validate_log_directory_config_badPath() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");

        myServer.setServerConfigurationFile(BAD_LOG_DIRECTORY_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);


        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        // verify there are no audit logs in the default log directory


        List<File> list = returnListOfAuditLogs();  

        assertTrue("Expected audit logs in the valid configured log location", list.size() == 0);

        // verify there are no logs in the specified invalid path

        list = null;
        try {
        	File dir = new File("k:\\myLogsPath");
            list = Arrays.asList(dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("audit");
                }
            }));       	
        } catch (Exception e) {
            assertNull("Expect no audit logs in the invalid configured log location", list);
        	 
        }
        myServer.stopServer("CWWKS5802E", "TRAS0400W");  // expect error trying to delete the bad log directory path

        
    }
    
    /**
     * Test Purpose: Verify that we when we specify maxFiles=0 in the server.xml, which is by default the min value, that
     * it has no impact on the number of audit logs generated.
     *      
     **/
   
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuditLogging_validate_maxFilesZero() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");

    	
        myServer.setServerConfigurationFile(MAX_FILES_ZERO_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);


        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        for (int i = 0; i < 800; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK, AuditCommonTest.MINIMAL_RESPONSE_OUTPUT);
        }
        sleep(DEFAULT_DELAY_SECONDS);
        
        // verify there are multiple (non ZERO) audit logs in the default log directory
        Log.info(myLogClass, "testBasicAuditLogging_validate_maxFilesZero", "myServer.getLogsRoot = " + myServer.getLogsRoot());

        List<File> list = returnListOfAuditLogs();   
        
        assertTrue("Expected audit logs in the default log location", list.size() > 0);
        
        myServer.stopServer("TRAS0400W");  

    }
    
    /**
     * Test Purpose: Verify that we when we specify maxFiles less than the minimum default in the server.xml, that
     * an error is generated by the metadata.  However the default value is used, so verify an audit log is generated.
     *      
     **/
   
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuditLogging_validate_maxFilesLessThanZero() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");

    	
        myServer.setServerConfigurationFile(MAX_FILES_LESS_THAN_ZERO_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        
        // verify there is an audit logs in the default log directory
        Log.info(myLogClass, "testBasicAuditLogging_validate_maxFileLessThanZero", "myServer.getLogsRoot = " + myServer.getLogsRoot());

        List<File> list = returnListOfAuditLogs();     
        
        assertTrue("Expected audit logs in the default log location", list.size() > 0);

        myServer.stopServer("CWWKG0083W","TRAS0400W");  // expect configuration error via metadata


    }

    
    /**
     * Test Purpose: Verify that we when we specify maxFiles=0 in the server.xml, which is by default the min value, that
     * it has no impact on the number of audit logs generated, that we do successfully generate multiple audit at the expected
     * maxFileSize rollover value of 1MB.
     *      
     **/
   
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuditLogging_validate_maxFilesZero_maxFileSizeRollover() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");

    	
        myServer.setServerConfigurationFile(MAX_FILES_ZERO_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);


        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        for (int i = 0; i < 800; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK, AuditCommonTest.MINIMAL_RESPONSE_OUTPUT);
        }
        sleep(DEFAULT_DELAY_SECONDS);
        
        // verify there are multiple (non ZERO) audit logs in the default log directory

        Log.info(myLogClass, "testBasicAuditLogging_validate_maxFilesZero_maxFileSizeRollover", "myServer.getLogsRoot = " + myServer.getLogsRoot());

        List<File> list = returnListOfAuditLogs();   
        
        assertTrue("Expected audit logs in the default log location", list.size() > 0);
        
        // verify that no file is larger than the configured maxFileSize
        
        for (int i = 0; i < list.size(); i++) {
        	assertTrue("Each audit log should be no larger than the configured maxFileSize of 1MB", list.get(i).length() <= 1024L * 1024L);
        }

        myServer.stopServer("TRAS0400W"); 

    }
    
    /**
     * Test Purpose: Verify that we when we specify maxFileSize=0 in the server.xml, which is by default the min value, that
     * it has no impact on the number of audit logs generated, that we keep adding to one audit log, and we do not rollover any audit logs.
     *      
     **/
   
    @Mode(TestMode.FULL)
    @Test
    public void testBasicAuditLogging_validate_maxFileSizeZero() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");

    	
        myServer.setServerConfigurationFile(MAX_FILESIZE_ZERO_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);


        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        for (int i = 0; i < 800; i++) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK, AuditCommonTest.MINIMAL_RESPONSE_OUTPUT);
        }
        
        sleep(DEFAULT_DELAY_SECONDS * 2);
        
        // verify there is only one audit log in the default log directory
        Log.info(myLogClass, "testBasicAuditLogging_validate_maxFileSizeZero", "myServer.getLogsRoot = " + myServer.getLogsRoot());

        List<File> list = returnListOfAuditLogs();  

        assertTrue("Expected audit logs in the default log location and that we only have 1 audit log (no rollover)", list.size() == 1);
        myServer.stopServer("TRAS0400W"); 
        

    }
    
    /**
     * Test Purpose: Verify that we when we specify maxFileSize less than the minimum default in the server.xml, that
     * an error is generated by the metadata.  However the default value is used, so verify an audit log is generated.
     *      
     **/
   
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuditLogging_validate_maxFileSizeLessThanZero() throws Exception {
    	myServer.stopServer("CWWKS5802E","TRAS0400W");
    	
        myServer.setServerConfigurationFile(MAX_FILESIZE_LESS_THAN_ZERO_CONFIG_FILE);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("AuditBasicAuthServlet");
        verifyServerStartedWithAuditFeatureAndSecurityService(myServer);
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + AUDIT_BASICAUTH_PROTECTED_SERVLET_URI ,"user1", "user1pwd", HttpServletResponse.SC_OK);
        verifyUserResponse(response, GET_USER_PRINCIPAL + "user1", GET_REMOTE_USER + "user1");

        
        // verify there is an audit log in the default log directory
        Log.info(myLogClass, "testBasicAuditLogging_validate_maxFileSizeLessThanZero", "myServer.getLogsRoot = " + myServer.getLogsRoot());

        List<File> list = returnListOfAuditLogs();  

        assertTrue("Expected audit logs in the default log location", list.size() == 1);

        myServer.stopServer("CWWKG0083W", "TRAS0400W");  // expect configuration error via metadata


    }
    
    private List<File> returnListOfAuditLogs() throws IOException {
    	List<File> list = new ArrayList<File>();
        java.nio.file.Path dir = java.nio.file.Paths.get(myServer.getLogsRoot());
        try {
           java.nio.file.DirectoryStream<java.nio.file.Path> dirStream = java.nio.file.Files.newDirectoryStream(dir);
           for (java.nio.file.Path entry: dirStream) {
               Log.info(myLogClass,  "testBasicAuditLogging_verifyArchiving_verifySequentialEventNumbers, entry: ", entry.toFile().toString());
               Log.info(myLogClass,  "testBasicAuditLogging_verifyArchiving_verifySequentialEventNumbers, entry.getFileName: ", entry.getFileName().toString());
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
