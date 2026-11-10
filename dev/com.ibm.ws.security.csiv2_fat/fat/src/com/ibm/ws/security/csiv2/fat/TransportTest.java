/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.csiv2.fat.common.CommonTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

/**
 * Test cases for authentication scenarios using transport subject covering rows 3 and 14 of
 * CSIv2 spec table 10.4 - TSS Interpretation of Client Credentials After Validation.
 * Also, test negative scenarios for transport.
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class TransportTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        /*
         * These tests have not been configured to run with the local LDAP server.
         */
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        upstreamServer = setupUpstreamServer("client_certSupported_ltpaNever_noAssertion.xml");
        addServerProp(upstreamServer.getServer(), upstreamServer.getServerFileLoc(), "upStream.host");
        upstreamServer.setAllTestsRestartServer(true);
        downstreamServer = setupDownstreamServer("certSupported_ltpaSupported_noAssertion.xml");
        addServerProp(downstreamServer.getServer(), upstreamServer.getServerFileLoc(), "downStream.host");
        downstreamServer.setAllTestsRestartServer(true);
        setupBasicAuthClient();
    }

    /*
     * Row 3: P2A, None, Absent
     * Client and server with client certificate authentication, authentication layers (Never-Supported), and no identity
     * assertion yield Transport subject.
     */
    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaSupported_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_sslOutboundDefault() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_outboundDefault.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaSupported_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    /*
     * Make sure csiv2 works with outbound SSL filter setting.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_sslOutboundFilter() throws Exception {
        // TODO: replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_outboundSSLFilter.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaSupported_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_transportSSLSet() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_transportSSL.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaSupported_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_upstream_sslDefault() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_sslDefault.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaSupported_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_downstream_sslDefault() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaSupported_noAssertion_sslDefault.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    /*
     * Row 3: P2A, None, Absent
     * Client and server with client certificate authentication, authentication layer (Never-Never), and no identity assertion
     * yield Transport subject.
     */
    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaNever_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    /*
     * Row 3: P2A, None, Absent
     * Client's client certificate authentication "Supported" and server with "Required" , client and server's authentication layers with "Never", and no identity assertion
     * yield a Transport Subject
     */
    @Test
    public void testTransport_SuppTransRequired_NeverLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        //upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_extraEndpointSettings.xml", testName, upstreamApps, false, null);
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // configure server
        //downstreamServer.restartServer("certRequired_ltpaNever_noAssertion.xml", testName, downstreamApps, false, null);
        downstreamServer.restartServer("certRequired_ltpaNever_noAssertion_extraEndpointSettings.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTransport_SuppTransRequired_NeverLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_upstream_sslDefault() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        //upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_extraEndpointSettings.xml", testName, upstreamApps, false, null);
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_sslDefault.xml", testName, upstreamApps, false, null);
        // configure server
        //downstreamServer.restartServer("certRequired_ltpaNever_noAssertion.xml", testName, downstreamApps, false, null);
        downstreamServer.restartServer("certRequired_ltpaNever_noAssertion_extraEndpointSettings.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTransport_SuppTransRequired_NeverLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_downstream_sslDefault() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        //upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_extraEndpointSettings.xml", testName, upstreamApps, false, null);
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // configure server
        //downstreamServer.restartServer("certRequired_ltpaNever_noAssertion.xml", testName, downstreamApps, false, null);
        downstreamServer.restartServer("certRequired_ltpaNever_noAssertion_extraEndpointSettings_sslDefault.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

//chc - run this    @Mode(TestMode.LITE)
    //   @Test  - disabled due to NPE that is being investigated
    public void testTransport_SuppTransRequired_NeverLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_BadConfig() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certRequired_ltpaNever_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    /*
     * Row 3: P2A, None, Absent
     * Client and server with client certificate authentication, authentication layers (Supported-Never), and no identity assertion
     * yield Transport subject.
     */
    @Test
    public void testTransport_SuppTransSupp_SupportedLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("certSupported_ltpaNever_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

    @Test
    public void testTransport_sslMismatch() throws Exception {
        downstreamServer.removeAppsFromValidation();
        downstreamServer.restartServer("sslRefMismatch.xml", testName, null, false, null);
        LibertyServer upstreamLibertyServer = downstreamServer.getServer();

        assertNotNull("Error CWWKS962E: is not logged when the Transport Layer is configured with an SSL reference that the iiopEndpoint does not have.",
                      upstreamLibertyServer.waitForStringInLogUsingMark("CWWKS9622E: The SSL configuration defaultSSLConfig in the inbound CSIv2 transport layer does not correspond to an SSL configuration in the IIOP endpoint."));
    }

    @Test
    public void testTransport_noIiopsOptions() throws Exception {
        downstreamServer.removeAppsFromValidation();
        downstreamServer.restartServer("noIiopsOptions.xml", testName, null, false, null);
        LibertyServer upstreamLibertyServer = downstreamServer.getServer();

        assertNotNull("Error CWWKS963E: is not logged when the iiopEndpoint does not have an iiopsOptions when the Transport Layer is configured with an SSL reference.",
                      upstreamLibertyServer.waitForStringInLogUsingMark("CWWKS9623E: The IIOP endpoint does not have an SSL configuration and the inbound CSIv2 transport layer requires the defaultSSLConfig SSL configuration to be used in the IIOP endpoint."));
    }

    /*
     * Make sure csiv2 works with outbound SSL filter setting when the server sends multiple ports.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testTransport_SuppTransSupp_NeverLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_transportSubject_sslOutboundFilter_multiDownstreamPorts() throws Exception {
        // TODO:  replace "restartServer" with "reconfigServer" when dynamic updates are working
        // configure client
        upstreamServer.restartServer("client_certSupported_ltpaNever_noAssertion_outboundSSLFilter.xml", testName, upstreamApps, false, null);
        // configure server
        downstreamServer.restartServer("multiport_ltpaSupported_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, transportMappedUser);
    }

}
