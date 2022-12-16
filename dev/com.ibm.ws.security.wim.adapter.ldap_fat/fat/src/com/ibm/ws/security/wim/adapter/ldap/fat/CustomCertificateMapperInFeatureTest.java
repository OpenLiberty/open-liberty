/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.X509CertificateMapper;
import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.webcontainer.security.test.servlets.ClientCertAuthClient;
import com.unboundid.ldap.sdk.Entry;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test importing {@link X509CertificateMapper} instances bundled in a user feature.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@SuppressWarnings("restriction")
public class CustomCertificateMapperInFeatureTest {

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.custom.certmapper.feature");
    private static final Class<?> c = CustomCertificateMapperInFeatureTest.class;
    private static ClientCertAuthClient client;

    private final static String CLIENT_CERT_SERVLET = "ClientCertServlet";
    private final static String KEYSTORE_PASSWORD = "security";
    private final static String AUTH_TYPE_CERT = "CLIENT_CERT";
    private final static String USER1_CERT_FILE = "LDAPUser1.jks";
    private final static String USER2_CERT_FILE = "LDAPUser2.jks";
    private final static String USER3_CERT_FILE = "LDAPUser3.jks";
    private final static String LDAP_USER_1 = "LDAPUser1";
    private final static String LDAP_USER_2 = "LDAPUser2";
    private final static String LDAP_USER_3 = "LDAPUser3";

    private static ServerConfiguration originalConfiguration = null;
    private static InMemoryLDAPServer ds;

    private static final String LDAP_PARTITION_1_DN = "O=IBM,C=US";
    private static final String LDAP_PARTITION_2_DN = "O=IBM,C=UK";
    private static final String LDAP_USER_1_DN = "CN=" + LDAP_USER_1 + "," + LDAP_PARTITION_1_DN;
    private static final String LDAP_USER_2_DN = "CN=" + LDAP_USER_2;
    private static final String LDAP_USER_3_DN = "CN=" + LDAP_USER_3 + "," + LDAP_PARTITION_1_DN;

    private static final String ID_MAPPER_1 = "mapper1";
    private static final String ID_MAPPER_2 = "mapper2";
    private static final String ID_MAPPER_3 = "mapper3";
    private static final String ID_MAPPER_4 = "mapper4";
    private static final String ID_MAPPER_5 = "mapper5";
    private static final String ID_MAPPER_6 = "mapper6";

    @BeforeClass
    public static void setUp() throws Exception {
        setupLdapServer();
        setupLibertyServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        myServer.stopServer(new String[] { "CWIML4538E", "CWWKS1102E" });

        try {
            if (ds != null) {
                ds.shutDown(true);
            }
        } catch (Exception e) {
            Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
        }
        /*
         * Delete any files we copied to the test server and uninstall the user bundles.
         */
        myServer.uninstallUserBundle("com.ibm.ws.security.wim.adapter.ldap.certificate.mapper.sample_1.0");
        myServer.uninstallUserFeature("ldapCertificateMapperSample-1.0");

    }

    /**
     * Setup the Liberty server.
     *
     * @throws Exception If setup failed.
     */
    private static void setupLibertyServer() throws Exception {

        myServer.installUserBundle("com.ibm.ws.security.wim.adapter.ldap.certificate.mapper.sample_1.0");
        myServer.installUserFeature("ldapCertificateMapperSample-1.0");
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
         * The original server configuration.
         */
        originalConfiguration = myServer.getServerConfiguration();
    }

    /**
     * Configure the LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    private static void setupLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(LDAP_PARTITION_1_DN, LDAP_PARTITION_2_DN);

        /*
         * Add the partition entries.
         */

        Entry entry = new Entry(LDAP_PARTITION_1_DN);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "domain");
        ds.add(entry);

        entry = new Entry(LDAP_PARTITION_2_DN);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "domain");
        ds.add(entry);
        /*
         * Create the users.
         */
        entry = new Entry(LDAP_USER_1_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", LDAP_USER_1);
        entry.addAttribute("sn", LDAP_USER_1);
        entry.addAttribute("cn", LDAP_USER_1);
        ds.add(entry);

        entry = new Entry(LDAP_USER_3_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", LDAP_USER_3);
        entry.addAttribute("sn", LDAP_USER_3);
        entry.addAttribute("cn", LDAP_USER_3);
        ds.add(entry);
    }

    /**
     * Convenience method to configure the Liberty server with an {@link LdapRegistry} configuration.
     *
     * @param certificateMapperId The ID for the {@link X509CertificateMapper} instance to use.
     * @throws Exception If there was an error configuring the server.
     */
    private static void updateLibertyServer(String certificateMapperId) throws Exception {
        ServerConfiguration server = originalConfiguration.clone();

        /*
         * Configure LDAP.
         */
        LdapRegistry ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_PARTITION_1_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode("CUSTOM");
        server.getLdapRegistries().add(ldap);

        if (certificateMapperId != null) {
            ldap.setCertificateMapperId(certificateMapperId);
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
     * @param response The response from the HTTPS request.
     */
    private static void verifyProgrammaticAPIValues(String loginUser, String response) {
        assertTrue("Failed to find expected getAuthType: " + loginUser, response.contains("getAuthType: " + AUTH_TYPE_CERT));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, response.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, response.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
    }

    /**
     * Test mapping with a {@link X509CertificateMapper} that maps the X.509 certificate to a
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
        String trace = "The custom X.509 certificate mapper returned the following mapping: " + LDAP_USER_1_DN;
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
     * Test mapping with a {@link X509CertificateMapper} that maps the X.509 certificate to a
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
        String trace = "The custom X.509 certificate mapper returned the following mapping: " + LDAP_USER_2_DN;
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());

        /*
         * Expect JNDI error searching for non-existent partition.
         */
        trace = "LDAP: error code 32 - Unable to perform the search because base entry '" + LDAP_USER_2_DN + "' does not exist in the server.";
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
     * Test mapping with a {@link X509CertificateMapper} that maps the X.509 certificate to an
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
        String trace = "The custom X.509 certificate mapper returned the following mapping: \\(CN=" + LDAP_USER_1 + "\\)";
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
     * Test mapping with a {@link X509CertificateMapper} that maps the X.509 certificate to an
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
        String trace = "The custom X.509 certificate mapper returned the following mapping: \\(CN=" + LDAP_USER_2 + "\\)";
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
     * Test handling of a {@link X509CertificateMapper} implementation that throws {@link CertificateMapNotSupportedException}
     * from the {@link X509CertificateMapper#mapCertificate(java.security.cert.X509Certificate)} method.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.security.registry.CertificateMapNotSupportedException" })
    public void certificate_map_not_supported_exception() throws Exception {

        updateLibertyServer(ID_MAPPER_3);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWIML4542E";
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
     * Test handling of a {@link X509CertificateMapper} implementation that throws {@link CertificateMapFailedException}
     * from the {@link X509CertificateMapper#mapCertificate(java.security.cert.X509Certificate)} method.
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
        String trace = "CWIML4503E";
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
        String trace = "CWIML4500W";
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
     * Test handling of no certificateMapperId set in the configuration.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.CertificateMapFailedException" })
    public void no_mapper_id() throws Exception {

        updateLibertyServer(null);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWIML4500W";
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
     * Test handling of when an invalid value (null) is returned from the mapper implementation.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.CertificateMapFailedException" })
    public void certificate_map_null() throws Exception {

        updateLibertyServer(ID_MAPPER_5);

        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWIML4504W";
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
     * Test that when there are multiple federated repositories and one of them throws a
     * CertificateMapNotSupportedException and one of them returns a valid result that the
     * authentication is honored.
     *
     * This is not really specifically a custom certificate mapper test, but they make it easy to verify
     * this behavior.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    public void multiple_repositories_failed_valid() throws Exception {
        ServerConfiguration server = originalConfiguration.clone();

        /*
         * This LDAP server will throw a CertificateMapNotSupportedException.
         */
        LdapRegistry ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm1");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_PARTITION_2_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode("CUSTOM");
        server.getLdapRegistries().add(ldap);
        ldap.setCertificateMapperId(ID_MAPPER_3);

        /*
         * This LDAP server will allow LDAP user 1 to authenticate.
         */
        ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm2");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_PARTITION_1_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode("CUSTOM");
        server.getLdapRegistries().add(ldap);
        ldap.setCertificateMapperId(ID_MAPPER_1);

        updateConfigDynamically(myServer, server);

        /*******************/
        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(LDAP_USER_1, response);

        /*
         * Check for CertificateMapper.mapCertificate() call.
         */
        String trace = "The custom X.509 certificate mapper returned the following mapping: " + LDAP_USER_1_DN;
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());
    }

    /**
     * Test that when CertificateMapNotSupportedException is thrown by multiple federated repositories,
     * that the CertificateMapNotSupportedException is returned.
     *
     * This is not really specifically a custom certificate mapper test, but they make it easy to verify
     * this behavior.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.security.registry.CertificateMapNotSupportedException" })
    public void multiple_repositories_notsupported_notsupported() throws Exception {
        ServerConfiguration server = originalConfiguration.clone();

        /*
         * This LDAP server will throw a CertificateMapNotSupportedException.
         */
        LdapRegistry ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm1");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_PARTITION_2_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode("CUSTOM");
        ldap.setCertificateMapperId(ID_MAPPER_3);
        server.getLdapRegistries().add(ldap);

        /*
         * This LDAP server will ignore any certificate authentication requests and
         * instead throw a CertificateMapNotSupportedException.
         */
        ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm2");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_PARTITION_1_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode(ConfigConstants.CONFIG_VALUE_CERT_NOT_SUPPORTED_MODE);
        server.getLdapRegistries().add(ldap);

        updateConfigDynamically(myServer, server);

        /*******************/
        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWIML4542E";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CertificateMapNotSupportedException in logs.", matching.isEmpty());
    }

    /**
     * Test that when both CertificateMapFailedException and CertificateMapNotSupportedException are
     * thrown from separate federated repositories, that the CertificateMapFailedException is returned.
     *
     * This is not really specifically a custom certificate mapper test, but they make it easy to verify
     * this behavior.
     *
     * @throws Exception If the test failed for some reason.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.wsspi.security.wim.exception.CertificateMapFailedException" })
    public void multiple_repositories_failed_notsupported() throws Exception {
        ServerConfiguration server = originalConfiguration.clone();

        /*
         * This LDAP server will throw a CertificateMapNotSupportedException.
         */
        LdapRegistry ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm1");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_PARTITION_2_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode("CUSTOM");
        ldap.setCertificateMapperId(ID_MAPPER_3);
        server.getLdapRegistries().add(ldap);

        /*
         * This LDAP server will throw a CertificateMapFailedException.
         */
        ldap = new LdapRegistry();
        ldap.setRealm("LDAPRealm1");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_PARTITION_1_DN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setCertificateMapMode("CUSTOM");
        ldap.setCertificateMapperId(ID_MAPPER_4);
        server.getLdapRegistries().add(ldap);

        updateConfigDynamically(myServer, server);

        /*******************/
        client = setupClient(USER1_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 403);
        assertNull("Expected null response.", response);

        /*
         * Check for the expected error message.
         */
        String trace = "CWIML4503E";
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find CertificateMapFailedException in logs.", matching.isEmpty());
    }

    /**
     * Test mapping a certificate that is part of a certificate chain. The mapper checks for some properties
     * on both the subject's certificate and the CA's certificate.
     *
     * @throws Exception If the test failed for an unforeseen reason.
     */
    @Test
    public void map_certificate_chain() throws Exception {

        updateLibertyServer(ID_MAPPER_6);

        client = setupClient(USER3_CERT_FILE, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(LDAP_USER_3, response);

        /*
         * Check for CertificateMapper.mapCertificate() call.
         */
        String trace = "The custom X.509 certificate mapper returned the following mapping: " + LDAP_USER_3_DN;
        List<String> matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertFalse("Did not find mapping result in logs.", matching.isEmpty());

        /*
         * Check for the CWWKS1101W error message.
         */
        trace = "CWWKS1101W: CLIENT-CERT Authentication did not succeed for the client certificate with dn " + LDAP_USER_3_DN + ". The dn does not map to a user in the registry.";
        matching = myServer.findStringsInLogsAndTraceUsingMark(trace);
        assertTrue("Found unexpected CWWKS1101W error in logs.", matching.isEmpty());
    }
}
