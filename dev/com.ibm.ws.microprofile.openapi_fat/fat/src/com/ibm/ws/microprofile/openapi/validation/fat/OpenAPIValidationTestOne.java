package com.ibm.ws.microprofile.openapi.validation.fat;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Tests to ensure that OpenAPI model validation works and proper events (errors, warning) are reported.
 *
 */
@RunWith(FATRunner.class)
public class OpenAPIValidationTestOne {
	
	@Server("validationServerOne")
    public static LibertyServer server;
    private static final String OPENAPI_VALIDATION_YAML = "openapi_validation";

    //private static final int TIMEOUT = 10000; //in milliseconds (10 seconds)

    @BeforeClass
    public static void setUpTest() throws Exception {
        HttpUtils.trustAllCertificates();

        server.startServer("OpenAPIValidationTestOne.log", true);

        server.validateAppLoaded(OPENAPI_VALIDATION_YAML);
        
        assertNotNull("Web application is not available at /openapi_validation/",
                      server.waitForStringInLog("CWWKT0016I.*/openapi_validation/")); //wait for endpoint to become available
        assertNotNull("CWWKF0011I.* not recieved on relationServer",
                      server.waitForStringInLog("CWWKF0011I.*")); // wait for server is ready to run a smarter planet message
        assertNotNull("The application openapi_validation was processed and an OpenAPI document was produced.",
                	  server.waitForStringInLog("CWWKO1660I.*and an OpenAPI document was produced.")); //wait for application to be processed
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKO1603E", "CWWKO1650E", "CWWKO1651W");
        }
    }
    
    //Tests for correct validation messages provided for the validation errors in the following models:
    //Info, Contact, License, ServerVariable(s), Server(s), PathItem, Operation, ExternalDocumentation, SecurityRequirement, RequestBody, Response, Responses
    
    @Test
    public void testInfoValidation() throws Exception {
    	
    	assertNotNull("The Info object was not validated properly", 
    			server.waitForStringInLog("Message: The Info Object must contain a valid URL. The \"not in URL format\" value specified for \"termsOfService\"*"));
    	assertNotNull("The Info object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"version\" field is missing or is set to an invalid value, Location: #/info"));
    	assertNotNull("The Info object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"title\" field is missing or is set to an invalid value, Location: #/info"));
    }
    
    @Test
    public void testContactValidation(){ 	
    	assertNotNull("The Contact object was not validated properly", 
    			server.waitForStringInLog("Message: The Contact Object must contain a valid URL. The \"not in URL Format\" value specified*"));
    	assertNotNull("The Contact object was not validated properly", 
    			server.waitForStringInLog("Message: The Contact Object must contain a valid email address. The \"not an email\" value*"));
    }
    
    @Test
    public void testLicenseValidation(){
    	assertNotNull("The License object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"name\" field is missing or is set to an invalid value"));
    	assertNotNull("The License object was not validated properly", 
    			server.waitForStringInLog("The License Object must contain a valid URL. The \"not in URL format\" value"));
    }
    
    @Test
    public void testServerValidation(){
    	assertNotNull("The Server object was not validated properly", 
    			server.waitForMultipleStringsInLog(4, "Message: The Server Object must contain a valid URL*"));
    }
    
    @Test
    public void testServersValidation(){
    	assertNotNull("The Server object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"url\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/get/servers"));
    }
    
    @Test
    public void testServerVariableValidation(){
    	assertNotNull("The Server Variable object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"default\" field is missing or is set to an invalid value*"));
    	assertNotNull("The Server Variable object was not validated properly", 
    			server.waitForStringInLog("he \"extraVariable\" variable in the Server Object is not defined*"));
    	assertNotNull("The Server Variable object was not validated properly", 
    			server.waitForStringInLog("Message: The \"id\" variable in the Server Object is not defined*"));
    }
    
    @Test
    public void testPathItemValidation(){
    	assertNotNull("The PathItem object was not validated properly", 
    			server.waitForStringInLog("The \"id\" path parameter from the \"GET\" operation of the path*"));
    	assertNotNull("The PathItem object was not validated properly", 
    			server.waitForStringInLog("The \"GET\" operation of the*"));
    	assertNotNull("The PathItem object was not validated properly", 
    			server.waitForStringInLog("The \"GET\" operation from the*"));
    }
    
    @Test
    public void testOperationValidation(){
    	assertNotNull("The Operation object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"responses\" field is missing or is set to an invalid value, Location: #/paths/~1/get"));
    	assertNotNull("The Operation object was not validated properly", 
    			server.waitForStringInLog("Message: More than one Operation Objects with \"getReviewById\" value for \"operationId\" field was found. The \"operationId\" must be unique"));
    }
    
    @Test
    public void testExternalDocsValidation(){
    	assertNotNull("The ExternalDocumentation object was not validated properly", 
    			server.waitForStringInLog("Message: The External Documentation Object must contain a valid URL. The \"not a URL\" value"));
    }
    
    @Test
    public void testSecurityRequirementValidation(){
    	assertNotNull("The Parameter object was not validated properly", 
    			server.waitForStringInLog("The \"reviewoauth2\" name provided for the Security Requirement Object does not correspond to a declared security scheme"));
    }
    
    @Test
    public void testRequestBodyValidation(){
    	assertNotNull("The RequestBody object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"content\" field is missing or is set to an invalid value, Location: #/paths/~1reviews/post/requestBody"));
    }
    
    @Test
    public void testResponseValidation(){
    	assertNotNull("The Response object was not validated properly", 
    			server.waitForStringInLog("Message: Required \"description\" field is missing or is set to an invalid value*"));
    }
    
    @Test
    public void testResponsesValidation(){
    	assertNotNull("The Responses object was not validated properly", 
    			server.waitForStringInLog("Message: The Responses Object should contain at least one response code for a successful operation"));
    }

}
