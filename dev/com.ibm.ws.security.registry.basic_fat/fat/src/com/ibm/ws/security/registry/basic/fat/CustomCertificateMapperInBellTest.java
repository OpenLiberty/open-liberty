/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.registry.basic.fat;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.X509CertificateMapper;
import com.ibm.websphere.simplicity.config.BasicRegistry;
import com.ibm.websphere.simplicity.config.BasicRegistry.User;
import com.ibm.websphere.simplicity.config.Bell;
import com.ibm.websphere.simplicity.config.File;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.ClientCertAuthClient;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test importing {@link X509CertificateMapper} instances bundled in a shared
 * library via the BELLS feature.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class CustomCertificateMapperInBellTest {
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.basic.fat.custom.certmapper.bell");
    private static final Class<?> c = CustomCertificateMapperInBellTest.class;
    private static ClientCertAuthClient client;

    private final static String CLIENT_CERT_SERVLET = "ClientCertServlet";
    private final static String KEYSTORE_PASSWORD = "security";
    private final static String AUTH_TYPE_CERT = "CLIENT_CERT";
    private final static String BASIC_USER_1_CERT_FILE = "BasicUser1.jks";
    private final static String BASIC_USER_2_CERT_FILE = "BasicUser2.jks";
    private final static String BASIC_USER_3_CERT_FILE = "BasicUser3.jks";
    private final static String BASIC_USER_1 = "BasicUser1";
    private final static String BASIC_USER_3 = "BasicUser3";

    private static final String ID_MAPPER_1 = "mapper1";
    private static final String ID_MAPPER_2 = "mapper2";
    private static final String ID_MAPPER_3 = "mapper3";
    private static final String ID_MAPPER_4 = "mapper4";

    private static final String ID_LIBRARY_1 = "library1";
    private static final String PATH_LIBRARY_1 = "${wlp.user.dir}/shared/com.ibm.ws.security.registry.basic.certificate.mapper.sample_1.0.jar";

    private static ServerConfiguration originalConfiguration = null;

    @BeforeClass
    public static void setUp() throws Exception {
        setupLibertyServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        myServer.stopServer(new String[] { "CWIML4538E", "CWWKS1102E" });
    }

    private static void setupLibertyServer() throws Exception {

        myServer.addInstalledAppForValidation("clientcert");
        myServer.startServer(true);

        //Make sure the application has come up before proceeding
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));
        assertNotNull("Server did not come up",
                      myServer.waitForStringInLog("CWWKF0011I"));

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        originalConfiguration = myServer.getServerConfiguration();
    }

    /**
     * Convenience method to configure the Liberty server with an {@link BasicRegistry} configuration.
     *
     * @param certificateMapperId The ID for the certificate mapper to use with the basic registry.
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer(String certificateMapperId) throws Exception {
        ServerConfiguration server = originalConfiguration.clone();

        BasicRegistry basic = new BasicRegistry();
        basic.setCertificateMapMode("CUSTOM");
        server.getBasicRegistries().add(basic);

        User user1 = new User();
        user1.setName(BASIC_USER_1);
        user1.setPassword("password");
        basic.getUsers().add(user1);

        User user3 = new User();
        user3.setName(BASIC_USER_3);
        user3.setPassword("password");
        basic.getUsers().add(user3);

        File file1 = new File();
        file1.setName(PATH_LIBRARY_1);

        Library library1 = new Library();
        library1.setId(ID_LIBRARY_1);
        library1.setNestedFile(file1);
        server.getLibraries().add(library1);

        Bell bell = new Bell();
        bell.setLibraryRef(ID_LIBRARY_1);
        server.getBells().add(bell);

        if (certificateMapperId != null) {
            basic.setCertificateMapperId(certificateMapperId);
        }

        updateConfigDynamically(myServer, server);
    }

    /**
     * This is an internal method used to setup the ClientCertAuthClient
     */
    private static ClientCertAuthClient setupClient(String certFile, boolean secure) {
        if (secure) {
            String ksFile = myServer.pathToAutoFVTTestFiles + java.io.File.separator + "clientcert" + java.io.File.separator + certFile;
            client = new ClientCertAuthClient(myServer.getHostname(), myServer
                            .getHttpDefaultSecurePort(), true, myServer, CLIENT_CERT_SERVLET, "/clientcert", ksFile, KEYSTORE_PASSWORD);
        } else {
            client = new ClientCertAuthClient(myServer.getHostname(), myServer.getHttpDefaultSecurePort(), false, myServer, CLIENT_CERT_SERVLET, "/clientcert", null, null);
        }
        return client;
    }

    /**
     * Verify programmatic APIs.
     *
     * @param loginUser The user that logged in.
     * @param response  The response from the HTTPS request.
     */
    private static void verifyProgrammaticAPIValues(String loginUser, String response) {
        assertTrue("Failed to find expected getAuthType: " + loginUser, response.contains("getAuthType: " + AUTH_TYPE_CERT));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, response.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, response.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
    }

    /**
     * Test mapping with a {@link X509CertificateMapper} that maps the X.509 certificate to a
     * name that does exist in the basic registry.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void basic_cn_mapper() throws Exception {
        assumeTrue(!isLinuxWithJava8("basic_cn_mapper"));
        updateLibertyServer(ID_MAPPER_1);

        client = setupClient(BASIC_USER_1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(BASIC_USER_1, response);

        /*
         * Check for {@link X509CertificateMapper}.mapCertificate() call.
         */
        String trace = "The custom X.509 certificate mapper returned the following mapping: " + BASIC_USER_1;
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());
    }

    /**
     * Test mapping with a {@link X509CertificateMapper} that maps the X.509 certificate to a
     * name that does NOT exist in the basic registry.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void basic_cn_mapper_no_user() throws Exception {
        assumeTrue(!isLinuxWithJava8("basic_cn_mapper_no_user"));
        updateLibertyServer(ID_MAPPER_1);

        client = setupClient(BASIC_USER_2_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWWKS3109W";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CertificateMapNotSupportedException in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=BasicUser2,O=IBM,C=US. The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of a {@link X509CertificateMapper} implementation that throws {@link CertificateMapNotSupportedException}
     * from the {@link {@link X509CertificateMapper#mapCertificate(java.security.cert.X509Certificate[])}} method.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.security.registry.CertificateMapNotSupportedException" })
    public void certificate_map_not_supported_exception() throws Exception {
        assumeTrue(!isLinuxWithJava8("certificate_map_not_supported_exception"));
        updateLibertyServer(ID_MAPPER_2);

        client = setupClient(BASIC_USER_1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWWKS3110W";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CertificateMapFailedException in logs.", matching.isEmpty());

//
// TODO CertificateLoginModule does not catch CertificateMapNotSupportedException and therefore FFDC is thrown and not this message.
//
//        /*
//         * Check for the expected error message.
//         */
//        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=BasicUser2,O=IBM,C=US. The dn does not map to a user in the registry.";
//        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
//        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of a {@link X509CertificateMapper} implementation that throws {@link CertificateMapFailedException}
     * from the {@link {@link X509CertificateMapper#mapCertificate(java.security.cert.X509Certificate[])}} method.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void certificate_map_failed_exception() throws Exception {
        assumeTrue(!isLinuxWithJava8("certificate_map_failed_exception"));
        updateLibertyServer(ID_MAPPER_3);

        client = setupClient(BASIC_USER_1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWWKS3111E";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CertificateMapFailedException in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=BasicUser1,O=IBM,C=US. The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of an invalid certificateMapperId set in the configuration.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void invalid_mapper_id() throws Exception {
        assumeTrue(!isLinuxWithJava8("invalid_mapper_id"));
        String invalidMapperId = "invalidCertificateMapperId";
        updateLibertyServer(invalidMapperId);

        client = setupClient(BASIC_USER_1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWWKS3108W";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected error in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=BasicUser1,O=IBM,C=US. The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of certificate map mode "NOT_SUPPORTED".
     *
     * This is not really specifically a custom certificate mapper test, but they make it easy to verify
     * this behavior.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.security.registry.CertificateMapNotSupportedException" })
    public void map_mode_not_supported() throws Exception {
        assumeTrue(!isLinuxWithJava8("map_mode_not_supported"));
        ServerConfiguration server = originalConfiguration.clone();

        BasicRegistry basic = new BasicRegistry();
        basic.setCertificateMapMode("NOT_SUPPORTED");
        server.getBasicRegistries().add(basic);

        User user1 = new User();
        user1.setName(BASIC_USER_1);
        user1.setPassword("password");
        basic.getUsers().add(user1);

        updateConfigDynamically(myServer, server);

        /****************************************/

        client = setupClient(BASIC_USER_1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWWKS3113W";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected error in logs.", matching.isEmpty());

//
// TODO CertificateLoginModule does not catch CertificateMapNotSupportedException and therefore FFDC is thrown and not this message.
//
//             /*
//              * Check for the expected error message.
//              */
//             trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=BasicUser2,O=IBM,C=US. The dn does not map to a user in the registry.";
//             matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
//             assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test mapping a certificate that is part of a certificate chain. The mapper checks for some properties
     * on both the subject's certificate and the CA's certificate.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void map_certificate_chain() throws Exception {
        assumeTrue(!isLinuxWithJava8("map_certificate_chain"));
        updateLibertyServer(ID_MAPPER_4);

        client = setupClient(BASIC_USER_3_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(BASIC_USER_3, response);

        /*
         * Check for CertificateMapper.mapCertificate() call.
         */
        String trace = "The custom X.509 certificate mapper returned the following mapping: " + BASIC_USER_3;
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());
    }

    /**
     * Check if the test is running on Linux and a specific java
     *
     * @param methodName
     * @return True if the test is running on the specific OS/JDK combo
     */
    private static boolean isLinuxWithJava8(String methodName) {
        String os = System.getProperty("os.name").toLowerCase();
        String javaVendor = System.getProperty("java.vendor").toLowerCase();
        String javaVersion = System.getProperty("java.version");
        Log.info(CustomCertificateMapperInBellTest.class, methodName,
                 "Checking os.name: " + os + " java.vendor: " + javaVendor + " java.version: " + javaVersion);
        if (os.contains("linux") && (javaVendor.contains("openjdk") || javaVendor.contains(("oracle")))
            && (javaVersion.startsWith("1.8.0"))) {
            /*
             * On Linux on this JDK level, getting a javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
             */
            Log.info(CustomCertificateMapperInBellTest.class, methodName,
                     "Skipping this test due to a bug with the specific OS/JDK combo: " + System.getProperty("os.name")
                                                                          + " " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
            return true;
        }
        return false;
    }

}
