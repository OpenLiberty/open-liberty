/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapper;
import com.ibm.websphere.simplicity.config.Bell;
import com.ibm.websphere.simplicity.config.File;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.webcontainer.security.test.servlets.ClientCertAuthClient;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@SuppressWarnings("restriction")
public class CustomCertificateMapperTest {

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.custom.certmapper");
    private static final Class<?> c = CustomCertificateMapperTest.class;
    private static ClientCertAuthClient client;

    private final static String CLIENT_CERT_SERVLET = "ClientCertServlet";
    private final static String KEYSTORE_PASSWORD = "security";
    private final static String AUTH_TYPE_CERT = "CLIENT_CERT";
    private final static String USER1_CERT_FILE = "LDAPUser1.jks";
    private final static String USER2_CERT_FILE = "LDAPUser2.jks";
    private final static String LDAP_USER_1 = "LDAPUser1";
    private final static String LDAP_USER_2 = "LDAPUser2";

    private static ServerConfiguration emptyConfiguration = null;
    private static EmbeddedApacheDS ldapServer = null;

    private static final String LDAP_PARTITION_DN = "O=IBM,C=US";
    private static final String LDAP_USER_1_DN = "CN=" + LDAP_USER_1 + "," + LDAP_PARTITION_DN;
    private static final String LDAP_USER_2_DN = "CN=" + LDAP_USER_2;

    private static final String ID_MAPPER_1 = "mapper1";
    private static final String ID_MAPPER_2 = "mapper2";
    private static final String ID_MAPPER_3 = "mapper3";
    private static final String ID_MAPPER_4 = "mapper4";
    private static final String ID_MAPPER_5 = "mapper5";

    private static final String ID_LIBRARY_1 = "library1";
    private static final String PATH_LIBRARY_1 = "${wlp.user.dir}/shared/certificateMapper.jar";

    @BeforeClass
    public static void setUp() throws Exception {
        setupLdapServer();
        setupLibertyServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        myServer.stopServer(new String[] { "CWIML4538E", "CWWKS1102E" });

        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                Log.error(c, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
            }
        }
    }

    /**
     * Setup the Liberty server.
     *
     * @throws Exception If setup failed.
     */
    private static void setupLibertyServer() throws Exception {

        myServer.addInstalledAppForValidation("clientcert");
        myServer.startServer(true);

        //Make sure the application has come up before proceeding
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));
        assertNotNull("Server did not came up",
                      myServer.waitForStringInLog("CWWKF0011I"));

        /*
         * The original server configuration has no registry or Federated Repository configuration.
         */
        emptyConfiguration = myServer.getServerConfiguration();
    }

    /**
     * Configure the LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ldapServer = new EmbeddedApacheDS("subordinate");
        ldapServer.addPartition("testing", LDAP_PARTITION_DN);
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(LDAP_PARTITION_DN);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        /*
         * Create the user and group.
         */
        entry = ldapServer.newEntry(LDAP_USER_1_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "LDAPUser1");
        entry.add("sn", "LDAPUser1");
        entry.add("cn", "LDAPUser1");
        ldapServer.add(entry);
    }

    /**
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration.
     *
     * @param certificateMapperId The ID for the {@link CertificateMapper} instance to use.
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer(String certificateMapperId) throws Exception {
        ServerConfiguration server = emptyConfiguration.clone();

        /*
         * Configure LDAP.
         */
        LdapRegistry ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
        ldap.setBaseDN(LDAP_PARTITION_DN);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode("CUSTOM");
        server.getLdapRegistries().add(ldap);

        if (certificateMapperId != null) {
            ldap.setCertificateMapperId(certificateMapperId);
        }

        /*
         * Setup the library.
         */
        File file1 = new File();
        file1.setName(PATH_LIBRARY_1);
        Library library1 = new Library();
        library1.setId(ID_LIBRARY_1);
        library1.setNestedFile(file1);
        server.getLibraries().add(library1);

        /*
         * Setup the BELL.
         */
        Bell bell = new Bell();
        bell.setLibraryRef(ID_LIBRARY_1);
        server.getBells().add(bell);

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
     * @param response The response from the HTTPS request.
     */
    private static void verifyProgrammaticAPIValues(String loginUser, String response) {
        assertTrue("Failed to find expected getAuthType: " + loginUser, response.contains("getAuthType: " + AUTH_TYPE_CERT));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, response.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, response.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
    }

    /**
     * Test mapping with a {@link CertificateMapper} that maps the X.509 certificate to a
     * distinguished name that does exist on the LDAP server.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void ldap_dn_mapper() throws Exception {

        updateLibertyServer(ID_MAPPER_1);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(LDAP_USER_1, response);

        /*
         * Check for CertificateMapper.mapCertificate() call.
         */
        String trace = "The custom CertificateMapper returned the following mapping: " + LDAP_USER_1_DN;
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());

        /*
         * Check for the CWWKS1101W error message.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_1_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertTrue("Found unexpected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test mapping with a {@link CertificateMapper} that maps the X.509 certificate to a
     * distinguished name that does NOT exist on the LDAP server.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void ldap_dn_mapper_no_user() throws Exception {

        updateLibertyServer(ID_MAPPER_1);

        client = setupClient(USER2_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for CertificateMapper.mapCertificate() call.
         */
        String trace = "The custom CertificateMapper returned the following mapping: " + LDAP_USER_2_DN;
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());

        /*
         * Expect JNDI error searching for non-existent partition.
         */
        trace = "LDAP: error code 32 - NO_SUCH_OBJECT";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find JNDI error in logs.", matching.isEmpty());

        /*
         * Expect CWIML4537E error.
         */
        trace = "CWIML4537E: The login operation could not be completed. The specified principal name extracted from certificate is not found in the back-end repository.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CWIML4537E error in logs.", matching.isEmpty());

        /*
         * Check for the expected error message.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_2_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test mapping with a {@link CertificateMapper} that maps the X.509 certificate to an
     * LDAP filter that does map to a user on the LDAP server.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void ldap_filter_mapper() throws Exception {

        updateLibertyServer(ID_MAPPER_2);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(LDAP_USER_1, response);

        /*
         * Check for CertificateMapper.mapCertificate() call.
         */
        String trace = "The custom CertificateMapper returned the following mapping: \\(CN=" + LDAP_USER_1 + "\\)";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());

        /*
         * Check for the CWWKS1101W error message.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_1_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertTrue("Found unexpected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test mapping with a {@link CertificateMapper} that maps the X.509 certificate to an
     * LDAP filter that does NOT map to a user on the LDAP server.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void ldap_filter_mapper_no_user() throws Exception {

        updateLibertyServer(ID_MAPPER_2);

        client = setupClient(USER2_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for CertificateMapper.mapCertificate() call.
         */
        String trace = "The custom CertificateMapper returned the following mapping: \\(CN=" + LDAP_USER_2 + "\\)";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());

        /*
         * Expect CWIML4537E error.
         */
        trace = "CWIML4537E: The login operation could not be completed. The specified principal name extracted from certificate is not found in the back-end repository.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CWIML4537E error in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_2_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of a {@link CertificateMapper} implementation that throws {@link CertificateMapNotSupportedException}
     * from the {@link CertificateMapper#mapCertificate(java.security.cert.X509Certificate)} method.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.security.registry.CertificateMapNotSupportedException", "com.ibm.wsspi.security.wim.exception.CertificateMapNotSupportedException" })
    public void certificate_map_not_supported_exception() throws Exception {

        updateLibertyServer(ID_MAPPER_3);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "The custom CertificateMapper '" + ID_MAPPER_3 + "' threw a CertificateMapNotSupportedException.";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CertificateMapNotSupportedException in logs.", matching.isEmpty());

//
// TODO CertificateLoginModule does not catch CertificateMapNotSupportedException and therefore FFDC is thrown and not this message.
//
//        /*
//         * Expect CWWKS1101W error.
//         */
//        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_1_DN + ". The dn does not map to a user in the registry.";
//        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
//        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of a {@link CertificateMapper} implementation that throws {@link CertificateMapFailedException}
     * from the {@link CertificateMapper#mapCertificate(java.security.cert.X509Certificate)} method.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.CertificateMapFailedException" })
    public void certificate_map_failed_exception() throws Exception {

        updateLibertyServer(ID_MAPPER_4);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "The custom CertificateMapper '" + ID_MAPPER_4 + "' threw a CertificateMapFailedException.";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CertificateMapFailedException in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_1_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of an invalid certificateMapperId set in the configuration.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.CertificateMapFailedException" })
    public void invalid_mapper_id() throws Exception {

        String invalidMapperId = "invalidCertificateMapperId";
        updateLibertyServer(invalidMapperId);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "A CertificateMapper with ID '" + invalidMapperId + "' was not found.";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected error in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_1_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of an no certificateMapperId set in the configuration.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.CertificateMapFailedException" })
    public void null_mapper_id() throws Exception {

        updateLibertyServer(null);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "No certificateMapperId was found for this registry.";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected error in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_1_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }

    /**
     * Test handling of a {@link CertificateMapper} implementation that returns null from
     * the {@link CertificateMapper#getId()} method.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.CertificateMapFailedException" })
    public void getId_returns_null() throws Exception {

        updateLibertyServer(ID_MAPPER_5);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message. There is a message printed out that the getId()
         * method returns null, but depending on how this is run, it may only get printed
         * out on server startup.
         */
        String trace = "A CertificateMapper with ID '" + ID_MAPPER_5 + "' was not found.";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected error in logs.", matching.isEmpty());

        /*
         * Expect CWWKS1101W error.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_1_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find expected CWWKS1101W error in logs.", matching.isEmpty());
    }
}
