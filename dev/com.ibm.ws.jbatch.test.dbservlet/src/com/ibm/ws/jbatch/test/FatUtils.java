package com.ibm.ws.jbatch.test;

import static org.junit.Assert.assertNotNull;

import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.JdbcDriver;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import componenttest.custom.junit.runner.JavaLevelFilter;
import componenttest.topology.impl.LibertyServer;

/**
 * Various FAT bucket utilities.
 */
public class FatUtils {
    
    /**
     * If java version is less than 1.7, don't bother validating that apps get
     * installed, because they won't, and if we tried to validate them, we'll
     * fail in @BeforeClass
     * @Deprecated is now a no-op
     */
    public static void checkJava7() {
    }
    
    /**
     * Wait for the activation spec started message 
     */
    public static void waitForActivationSpec(LibertyServer server) {
    	 server.resetLogMarks();
    	 assertNotNull("Activation spec not ready",
                 server.waitForStringInLog("CWWKT0016I"));

    }
    
    
    
    /**
     * Wait for the "smarter planet" and "ssl endpoint started" messages.
     */
    public static void waitForStartupAndSsl(LibertyServer server) {
        waitForSmarterPlanet(server);
        waitForSslEndpoint(server);
    }
    
    /**
     * Wait for the "smarter planet" message in the log.
     * 
     * Note: this method resets the log marks before searching the log.
     */
    public static void waitForSmarterPlanet(LibertyServer server) {
        
        server.resetLogMarks();
        assertNotNull("The CWWKF0011I smarter planet message not found for server: " + server.getServerRoot(),
                      server.waitForStringInLog("CWWKF0011I"));
    }
    
    /**
     * Wait for the message indicating the SSL endpoint has started.
     * 
     * Note: this method resets the log marks before searching the log.
     * 
     * @param server
     */
    public static void waitForSslEndpoint(LibertyServer server) {
        // Wait for SSL endpoint
        server.resetLogMarks();
        assertNotNull("defaultHttpEndpoint-ssl was not started",
                      //server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started"));
                      server.waitForStringInLog("CWWKO0219I:(.*)defaultHttpEndpoint-ssl"));

    }
    
    /**
     * Wait for the message indicating the LTPA keys are ready
     * 
     * Note: this method resets the log marks before searching the log.
     * 
     * @param server
     */
    public static void waitForLTPA(LibertyServer server) {
        server.resetLogMarks();
        // Previously it looked like we were trying to match CWWKS4105A
        // in addition to a CWWKS4104A.  As it turns out, there is only CWWKS4105I
        // so we will remove that.
        assertNotNull("Waited for the LTPA keys to be generated, but did not recieve the CWWKS4104A message ",
                       server.waitForStringInLog("CWWKS4104A"));

    }
    
    /**
     * Wait for the message indicating the ssl cert key.p12 are ready
     * 
     * Note: this method resets the log marks before searching the log.
     * 
     * @param server
     */
    public static void waitForSSLKeyFile(LibertyServer server) {
    	server.resetLogMarks();
    	assertNotNull("SSL certificate is not created", server.waitForStringInLog("CWPKI0803A"));
    }
    
    
    /**
     * Wait for the messages indicating the server started, SSL endpoint started, and LTPA key was created
     * 
     * Note: this method resets the log marks before searching the log.
     * 
     * @param server
     */
    public static void waitForStartupSslAndLTPA(LibertyServer server) {
        waitForSmarterPlanet(server);
        waitForSslEndpoint(server);
        waitForLTPA (server);
    }
    
    /**
     * Wait for ssl cert and ltpa
     * 
     * @param server
     */
    public static void waitForSSLKeyAndLTPAKey(LibertyServer server) {
    	waitForSSLKeyFile(server);
    	waitForLTPA(server);
    }
    
    
    /**
     * Wait for the message indicating the REST Handler API is ready
     * 
     * Note: this method resets the log marks before searching the log.
     * 
     * @param server
     */
    public static void waitForRestAPI(LibertyServer server) {
        server.resetLogMarks();
        assertNotNull("REST API not ready",
                      server.waitForStringInLog("CWWKT0016I:(.*)/ibm/api/"));
    }
    
    /**
     * Change the jdbcdriver and datasource in the server config based on
     * the db settings in the build env's bootstrapping file.
     */
    public static void changeDatabase(LibertyServer server) throws Exception {
        
        ServerConfiguration configuration = server.getServerConfiguration();
        
        // Change out the <jdbcDriver> references
        for (JdbcDriver jdbcDriver : configuration.getJdbcDrivers()) {
            jdbcDriver.updateJdbcDriverFromBootstrap(configuration);
        }
        
        // Change out the <dataSource> references
        for (DataSource dataSource : configuration.getDataSources()) {
            dataSource.updateDataSourceFromBootstrap(configuration);
        }
        
        server.updateServerConfiguration(configuration);
        
        if (server.isStarted()) {
            server.waitForConfigUpdateInLogUsingMark(null);
        }

    }

}
