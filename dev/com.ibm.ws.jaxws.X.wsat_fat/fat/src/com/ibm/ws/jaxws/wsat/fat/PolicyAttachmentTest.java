package com.ibm.ws.jaxws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class PolicyAttachmentTest {

    // Server Information
    public static String server1Port = "8091";
    
    // Basic URL
    @Server("policyattachments_client")
    public static LibertyServer client;

    @Server("policyattachments_service")
    public static LibertyServer server1;

    // Test URL
    // Client with URI and EndpointReference policy attachment configurations
    public static String clientApp1 = "policyAttachmentsClient1";

    // Client without policy attachment configurations
    public static String clientApp2 = "policyAttachmentsClient2";

    // Service with URI policy attachment configuration
    public static String serviceApp1 = "policyAttachmentsService1";

    // Service with EndpointReference policy attachment configuration
    public static String serviceApp2 = "policyAttachmentsService2";

    // Service with URI policy attachment configuration and WSDL defination
    public static String serviceApp3 = "policyAttachmentsService3";

    public static String ClientServlet1 = "ClientServlet1";
    public static String ClientServlet2 = "ClientServlet2";

    public static String helloWithoutPolicy = "helloWithoutPolicy";
    public static String helloWithPolicy = "helloWithPolicy";
    public static String helloWithOptionalPolicy = "helloWithOptionalPolicy";
    public static String helloWithYouWant = "helloWithYouWant";

    public static String helloWithoutPolicyResult = "helloWithoutPolicy invoked";
    public static String helloWithPolicyResult = "helloWithPolicy invoked";
    public static String helloWithOptionalPolicyResult = "helloWithOptionalPolicy invoked";
    public static String helloWithYouWantResult = "helloWithYouWant invoked";

    public static String errorResult = "WS-AT Feature is not installed";

    @BeforeClass
    public static void setup() throws Exception {
    
        WebArchive serviceWar1 = ShrinkWrap.create(WebArchive.class, serviceApp1 + ".war")
                        .addPackages(false, "com.ibm.ws.policyattachments.service1");
        

        WebArchive serviceWar2 = ShrinkWrap.create(WebArchive.class, serviceApp2 + ".war")
                        .addPackages(false, "com.ibm.ws.policyattachments.service2");
        
        WebArchive serviceWar3 = ShrinkWrap.create(WebArchive.class, serviceApp3 + ".war")
                .addPackages(false, "com.ibm.ws.policyattachments.service3");
        
        WebArchive clientWar1 = ShrinkWrap.create(WebArchive.class, clientApp1 + ".war")
        		.addPackages(false, "com.ibm.ws.policyattachments.client1")
                .addPackages(false, "com.ibm.ws.policyattachments.client1.service1")
                .addPackages(false, "com.ibm.ws.policyattachments.client1.service2")
                .addPackages(false, "com.ibm.ws.policyattachments.client1.service3");
        
        WebArchive clientWar2 = ShrinkWrap.create(WebArchive.class, clientApp2 + ".war")
        		.addPackages(false, "com.ibm.ws.policyattachments.client2")
                .addPackages(false, "com.ibm.ws.policyattachments.client2.service1")
                .addPackages(false, "com.ibm.ws.policyattachments.client2.service2");

        ShrinkHelper.addDirectory(serviceWar1, "test-applications/" + serviceApp1 + "/resources");
        ShrinkHelper.addDirectory(serviceWar2, "test-applications/" + serviceApp2 + "/resources");
        ShrinkHelper.addDirectory(serviceWar3, "test-applications/" + serviceApp3 + "/resources");
        ShrinkHelper.addDirectory(clientWar1, "test-applications/" + clientApp1 + "/resources");
        ShrinkHelper.addDirectory(clientWar2, "test-applications/" + clientApp2 + "/resources");
        
        ShrinkHelper.exportDropinAppToServer(server1, serviceWar1);
        ShrinkHelper.exportDropinAppToServer(server1, serviceWar2);
        ShrinkHelper.exportDropinAppToServer(server1, serviceWar3);
        
        ShrinkHelper.exportDropinAppToServer(client, clientWar1);
        ShrinkHelper.exportDropinAppToServer(client, clientWar2);
        
        // Make sure we don't fail because we try to start an
        // already started server
        try {
            client.startServer();
            server1.startServer();
            assertNotNull("The server did not start", server1.waitForStringInLog("CWWKF0011I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server1 != null) {
            server1.stopServer();
        }
        if (client != null) {
            client.stopServer();
        }
    }

    /**
     * Client with URI policy attachment, service with URI policy attachment
     */
    @Test
    public void testAttachments_URI_Without() {
        commonTest(clientApp1, ClientServlet1, serviceApp1, helloWithoutPolicy, helloWithoutPolicyResult);
    }

    @Test
    public void testAttachments_URI_With() {
        commonTest(clientApp1, ClientServlet1, serviceApp1, helloWithPolicy, errorResult);
    }

    @Test
    public void testAttachments_URI_Optional() {
        commonTest(clientApp1, ClientServlet1, serviceApp1, helloWithOptionalPolicy, helloWithOptionalPolicyResult);
    }

    @Test
    public void testAttachments_URI_YouWant() {
        commonTest(clientApp1, ClientServlet1, serviceApp1, helloWithYouWant, errorResult);
    }

    /**
     * Client without URI policy attachment, service with URI policy attachment
     */
    @Test
    public void testAttachments_URI_Without_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp1, helloWithoutPolicy, helloWithoutPolicyResult);
    }

    @Test
    public void testAttachments_URI_With_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp1, helloWithPolicy, errorResult);
    }

    @Test
    public void testAttachments_URI_Optional_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp1, helloWithOptionalPolicy, helloWithOptionalPolicyResult);
    }

    @Test
    public void testAttachments_URI_YouWant_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp1, helloWithYouWant, errorResult);
    }

    /**
     * Client with EndpointReference policy attachment, service with URI policy attachment
     */
    @Test
    public void testAttachments_EndpointReference_Without() {
        commonTest(clientApp1, ClientServlet1, serviceApp2, helloWithoutPolicy, errorResult);
    }

    @Test
    public void testAttachments_EndpointReference_With() {
        commonTest(clientApp1, ClientServlet1, serviceApp2, helloWithPolicy, errorResult);
    }

    @Test
    public void testAttachments_EndpointReference_Optional() {
        commonTest(clientApp1, ClientServlet1, serviceApp2, helloWithOptionalPolicy, errorResult);
    }

    @Test
    public void testAttachments_EndpointReference_YouWant() {
        commonTest(clientApp1, ClientServlet1, serviceApp2, helloWithYouWant, errorResult);
    }

    /**
     * Client without EndpointReference policy attachment, service with URI policy attachment
     */
    @Test
    public void testAttachments_EndpointReference_Without_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp2, helloWithoutPolicy, errorResult);
    }

    @Test
    public void testAttachments_EndpointReference_With_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp2, helloWithPolicy, errorResult);
    }

    @Test
    public void testAttachments_EndpointReference_Optional_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp2, helloWithOptionalPolicy, errorResult);
    }

    @Test
    public void testAttachments_EndpointReference_YouWant_ClientNo() {
        commonTest(clientApp2, ClientServlet2, serviceApp2, helloWithYouWant, errorResult);
    }

    /**
     * Client with URI policy attachment, service with URI policy attachment and WSDL defination
     */
    @Test
    public void testAttachments_URIWSDL_With() {
        commonTest(clientApp1, ClientServlet1, serviceApp3, helloWithPolicy, helloWithPolicyResult);
    }

    @Test
    public void testAttachments_URIWSDL_YouWant() {
        commonTest(clientApp1, ClientServlet1, serviceApp3, helloWithYouWant, helloWithYouWantResult);
    }

    public static void commonTest(String clientName, String servletName, String serviceName, String testMethod, String expectResult) {
        System.out.println("README:");
        System.out.println("client1 is with URI and EndpointReference policy attachment config, client2 is without policy attachment config");
        System.out.println("service1 is with URI policy attachment config, service2 is with EndpointReference policy attachment config");
        System.out.println("This test is for " + clientName + " and " + serviceName + ", method is " + testMethod);

        String resultURL = "http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/" + clientName + "/" + servletName + "?app=" + serviceName + "&method="
                + testMethod;

        try {
            String result = executeApp(resultURL);
            System.out.println("Expect result is " + expectResult);
            System.out.println("Execute result is " + result);
            assertTrue("Check result, expect is " + expectResult
                       + ", result is " + result, expectResult.equals(result));

        } catch (Exception e) {
            fail("Exception happens: " + e.toString());
        }
    }

    public static String executeApp(String url) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(url),
                                                            HttpURLConnection.HTTP_OK, 60);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String result = br.readLine();
        System.out.println("Execute WS-AT Policy Attachment test from " + url);
        return result;
    }
}
