package com.ibm.ws.microprofile.openapi.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
//import com.ibm.ws.openapi.fat.utils.OpenAPIConnection;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests to ensure that OpenAPI model validation works and proper events (errors, warning) are reported.
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTest {
	
	@Server("validationServer")
    public static LibertyServer server;
    private static final String OPENAPI_YAML = "openapi_yaml";

    String DONE_MERGE_MSG = "Done merging OASProvider: OpenAPIWebProvider=.contextRoot=/";
    String REMOVED_PROVIDER_MSG = "Done removing OASProvider: OpenAPIWebProvider=.contextRoot=/";

    private static final int TIMEOUT = 10000; //in milliseconds (10 seconds)

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTest.log", true);

        server.validateAppLoaded(OPENAPI_YAML);
        
        assertNotNull("Web application is not available at /api/docs/",
                      server.waitForStringInLog("CWWKT0016I.*/api/docs/")); //wait for /api/docs/ endpoint to become available
        assertNotNull("CWWKF0011I.* not recieved on relationServer",
                      server.waitForStringInLog("CWWKF0011I.*")); // wait for server is ready to run a smarter planet message
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO1603E");
        }
    }

}
