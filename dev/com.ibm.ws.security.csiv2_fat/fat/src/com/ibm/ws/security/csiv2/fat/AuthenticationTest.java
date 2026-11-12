/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.security.csiv2.fat.common.CommonTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * certSupported_gssupRequired_noAssertion.xml
 * Test cases for authentication scenarios using authentication layer subject covering rows 2 and 4 of
 * CSIv2 spec table 10.4 - TSS Interpretation of Client Credentials After Validation.
 * Also, test negative scenarios for authentication layer.
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuthenticationTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        upstreamServer = setupUpstreamServer("client_minimal.xml");
        upstreamServer.setAllTestsRestartServer(false);
        downstreamServer = setupDownstreamServer("minimal.xml");
        downstreamServer.setAllTestsRestartServer(false);
        setupBasicAuthClient();
    }

    /*
     * Test case naming convention:
     * <testClassName>_<clientSideValue><Transport Layer><serverSideValue>_<clientSideValue><Client Authentication Layer><serverSideValue>_<clientSideValue><Security Attribute
     * Layer><serverSideValue>_<Additional Info<
     * ie: testAuthentication_SuppTransSupp_ReqLTPAAuthReqLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject
     * Meaning: Client and server with client certificate authentication [(<clientSideValue><Transport Layer><serverSideValue>)=(SuppTransSupp)],
     * authentication layer compatible(LTPA Required-Required) [(<clientSideValue><Client Authentication Layer><serverSideValue>)(ReqLTPAAuthReqLTPA)],
     * and no identity assertion [(<clientSideValue><Security Attribute Layer><serverSideValue>)=(NoIdentAssSecAttrNoIdentAss)]
     * id Authentication layer subject. [(<Additional Info>)=(authLayerSubject)]
     */

    /*
     * Row 2: None, P2B, Absent
     * Client and server with no client certificate authentication, authentication layer compatible(GSSUP Supported-Supported), and no identity assertion
     * yield Authentication layer subject.
     */

    /*
     * Row 2: None, P2B, Absent
     * Client and server with no iiopClient and no iiopServerPolicies respectively
     * yield Authentication layer subject.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthentication_MinimalTransMinimal_MinimalAuthMinimal_MinimalAttrMinimal_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_minimal.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("minimal.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 2: None, P2B, Absent
     * Client and server with no client certificate authentication, authentication layer compatible(GSSUP Required-Required), and no identity assertion
     * yield Authentication layer subject.
     */

    /*
     * Row 2: None, P2B, Absent
     * Client and server with no client certificate authentication, authentication layer compatible(GSSUP Required-Supported), and no identity assertion
     * yield Authentication layer subject.
     */

    /*
     * Row 2: None, P2B, Absent
     * Client and server with no client certificate authentication, authentication layer compatible(LTPA), and no identity assertion
     * yield Authentication layer subject.
     */

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(GSSUP Supported-Supported), and no identity assertion
     * yield Authentication layer subject.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthentication_SuppTransSupp_SuppGSSUPAuthSuppGSSUP_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);

        assertSubjectInResponse(response, programmaticUser);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(GSSUP Supported-Required), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_SuppGSSUPAuthReqGSSUP_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupRequired_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);

        assertSubjectInResponse(response, programmaticUser);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(GSSUP Required-Required), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_ReqGSSUPAuthReqGSSUP_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupRequired_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupRequired_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);

        assertSubjectInResponse(response, programmaticUser);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(GSSUP Required-Supported), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_ReqGSSUPAuthSuppGSSUP_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupRequired_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);

        assertSubjectInResponse(response, programmaticUser);
    }

    /*
     * Same test as above, but testing the "no user registry blocks orb start". Checks that adding
     * the user registry allows the orb to start and that the resulting orb works.
     */
    //will be un-ignored via defect 175604
    @Test
    @Ignore
    public void testAuthentication_SuppTransSupp_ReqGSSUPAuthSuppGSSUP_NoIdentAssSecAttrNoIdentAss_authLayerSubject_add_user_registry() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupRequired_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupSupported_noAssertion_no_user_registry.xml", testName, false, null);

        LibertyServer libertyServer = downstreamServer.getServer();
        assertNotNull("The orb should timeout mentioning the user registry", libertyServer.waitForStringInLog("CWWKS9660E"));

        downstreamServer.reconfigServer("certSupported_gssupSupported_noAssertion.xml");

        String response = driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);

        assertSubjectInResponse(response, programmaticUser);
    }

    /*
     * Client and server with client certificate authentication, authentication layer compatible(GSSUP Required-Never), and no identity assertion
     * yield serviceability message.
     */
    @Test
    public void testPolicy_SuppTransSupp_ReqGSSUPAuthNeverGSSUP_NoIdentAssSecAttrNoIdentAss_PolicyCompliance() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupRequired_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupNever_noAssertion.xml", testName, false, null);

        driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);
        assertPolicyCompliance();
    }

    /*
     * Client and server with client certificate authentication, authentication layer compatible(GSSUP Never-Required), and no identity assertion
     * yield serviceability message.
     */
    @Test
    public void testPolicy_SuppTransSupp_NeverGSSUPAuthReqGSSUP_NoIdentAssSecAttrNoIdentAss_PolicyCompliance() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupNever_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupRequired_noAssertion.xml", testName, false, null);

        driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);
        assertPolicyCompliance();

        //assertSubjectInResponse(response, programmaticUser);
    }

    /*
     * Client and server test misMatched transport layer, yield serviceability message.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testPolicy_NoTransReq_NeverLTPAAuthNeverGSSUP_NoIdentAssSecAttrBackTrust_PolicyCompliance() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_noTransport_ltpaNever_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certRequired_gssupNever_assertionBackwardTrust.xml", testName, false, null);

        driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);
        assertPolicyCompliance();
    }

    /*
     * Client and server test misMatched transport layer, yield serviceability message.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testPolicy_NeverTransSupp_SuppGSSUPAuthReqLTPA_NoIdentAssSecAttrIdenAssDisabled_PolicyCompliance() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certNever_gssupSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("ltpaAuthenticationRequiredEstablishTrustInClientNever.xml", testName, false, null);

        driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);
        assertPolicyCompliance();
    }

    /*
     * Client and server test misMatched transport layer, yield serviceability message.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testPolicy_NeverTransNoSSL_NeverLTPAAuthNoAuth_NoIdentAssSecAttrPresumedTrust_PolicyCompliance() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certNever_ltpaNever_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("noSSL_authNever_assertionPresumedTrust.xml", testName, false, null);

        driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);
        assertPolicyCompliance();
    }

    /******************************************** LTPA **********************************************************/
    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(LTPA Supported-Supported), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_SuppLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaSupported_noAssertion.xml", testName, true, createMessagesToWaitInDownstreamServer());

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(LTPA Supported-Required), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_SupLTPAAuthReqLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaRequired_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(LTPA Required-Required), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_ReqLTPAAuthReqLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaRequired_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaRequired_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(LTPA Required-Supported), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_ReqLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaRequired_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer omitted(LTPA Required-Supported), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_OmitAuthOmit_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_noAuth_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_noAuth_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /**
     * Client and server with client certificate authentication, authentication layers (Omitted (authenticationLayer omitted entirely) - Supported), and no identity assertion
     * yield Authentication layer subject. (Omitting authentication layer means that it will take the defaults)
     *
     * @throws Exception
     */
    @Test
    public void testAuthentication_SuppTransSupp_OmitAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaOmitted_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(LTPA,GSSUP Supported-Supported), and no identity assertion
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthentication_SuppTransSupp_SuppBothAuthSuppBoth_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();

        upstreamServer.reconfigServer("client_certSupported_bothSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_bothSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /**
     * Client and server with client certificate authentication, authentication layers (Supported - Omitted (authenticationLayer omitted entirely)), and no identity assertion
     * yield Authentication layer subject. (Omitting authentication layer means that it will take the defaults)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthentication_SuppTransSupp_SuppLTPAAuthOmit_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaOmitted_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 4: P2A, P2B (by rule 1a), Absent
     * Client and server with client certificate authentication, authentication layer compatible(LTPA Never-Required), and no identity assertion
     * yield an exception
     */
    @Test
    public void testAuthentication_SuppTransSupp_NeverLTPAAuthRequiredLTPA_NoIdentAssSecAttrNoIdentAss_CheckException() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaNever_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaRequired_noAssertion.xml", testName, false, null);
        LibertyServer upstreamLibertyServer = upstreamServer.getServer();

        driveDownstreamFlow();

        assertNotNull("Warning CWWKS9680W: is not logged when Authention Mechanism's establishTrustInClient is \"Never\"",
                      upstreamLibertyServer.waitForStringInLogUsingMark("CWWKS9680W:"));
        // TODO Serviceability: Check for new message indicating that a security service context could not be created due to the conflicting configuration.
    }

    /*
     * Client and server with client certificate authentication, authentication layer not compatible(LTPA Required-Never), and no identity assertion
     * yield - won't go outbound at all...
     */
    @Test
    public void testAuthentication_SuppTransSupp_ReqLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_CheckException() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaRequired_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaNever_noAssertion.xml", testName, false, null);

        driveDownstreamFlow();
        assertPolicyCompliance();
    }

    /**
     * Client and server with client certificate authentication, authentication layers (default (<authenticationLayer/>) - Supported), and no identity assertion
     * yield Authentication subject.
     *
     * @throws Exception
     */
    @Test
    public void testAuthentication_SuppTransSupp_DefAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_default_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);

    }

    /**
     * Client and server with client certificate authentication, authentication layers (Supported-default (<authenticationLayer/>)), and no identity assertion
     * yield No Permission.
     *
     * @throws Exception
     */
    @Test
    public void testAuthentication_SuppTransSupp_SuppLTPAAuthDef_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_default_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);

    }

    /*
     * Negative and serviceability scenarios only after this point
     */

    @AllowedFFDC("java.security.PrivilegedActionException")
    @Test
    public void testAuthentication_SuppTransSupp_SuppGSSUPAuthSuppGSSUP_NoIdentAssSecAttrNoIdentAss_AuthFailure() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupSupported_noAssertion.xml", testName, false, null);
        String response = driveDownstreamFlowGSSUP(badUser, badPwd);

        //TODO Teddy the server is creating the correct exception but the message in the response is not the text you expect.
        assertTrue("The servlet must receive a NO_PERMISSION exception.",
                   response.contains("org.omg.CORBA.NO_PERMISSION")
                                                                          && response.contains("vmcid: 0x49424000 minor code: 0x300  completed: No"));
        // TODO: For some reason the ORB is not showing the associated error message after the NO_PERMISSION
        // response.contains("org.omg.CORBA.NO_PERMISSION: CWWKS1100A: Authentication did not succeed for user ID user3. An invalid user ID or password was specified.:  vmcid: 0x49424000 minor code: 0x300  completed: No"));
    }

    /*
     * Negative testing for Authentication Layer LTPA required.than
     * Authentication Mechanism is set to "xyz", whereas only "LTPA, GSSUP", "LTPA" or "GSSUP" are valid.
     */
    @Test
    public void testAuthentication_SuppTransSupp_InvalidAuthReqLTPA_NoIdentAssSecAttrNoIdentAss_CheckExeption() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_invalidMech_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaRequired_noAssertion.xml", testName, false, null);
        LibertyServer upstreamLibertyServer = upstreamServer.getServer();

        driveDownstreamFlow();

        assertNotNull("Warning CWWKS9600E: is not logged when Authention Mechanism is invalid",
                      upstreamLibertyServer.waitForStringInLogUsingMark("CWWKS9600E:"));
        // Serviceability: Check for new message indicating that a security service context could not be created due to the conflicting configuration.
    }

    @AllowedFFDC("java.security.PrivilegedActionException")
    @Test
    public void testAuthentication_SuppTransSupp_SuppLTPAAuthSuppLTPA_NoIdentAssSecAttrNoIdentAss_AuthFailure() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_ltpaSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow(clOnlyUser, clOnlyPwd);

        assertTrue("The servlet must receive a NO_PERMISSION exception.",
                   response.contains("org.omg.CORBA.NO_PERMISSION")
                                                                          && response.contains("vmcid: 0x49424000 minor code: 0x300  completed: No"));

        //TODO - Serviceability - nothing appears to be logged in either the client or server message logs
    }

    /**
     * Client and server configured with two different realm and confirm the authentication is successful
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthentication_SuppTransSupp_SuppGSSUPAuthSuppGSSUP_NoIdentAssSecAttrNoIdentAss_customRealm() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_gssupSupported_noAssertion_customRealm.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("certSupported_gssupSupported_noAssertion.xml", testName, false, null);
        String response = driveDownstreamFlowGSSUP(programmaticUser, programmaticPassword);
        assertSubjectInResponse(response, programmaticUser);
    }

    /**
     * Test tolerance to dynamic addition of features for CSIv2.
     * Start downstreamServer without EJB support and application configuration.
     * Reconfigure downstreamServer with EJB support and install applications
     *
     * @throws Exception
     */
    @Test
    public void testAuthentication_downstreamServer_dynamicEJBSupport() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        /* Remove apps from the validation list. This is done because we are going to start downstream server with no application configured. */
        downstreamServer.removeAppsFromValidation();

        /* Start downstreamServer with basic configuration, with no ejb remote feature. */
        downstreamServer.restartServer("noRemoteEJBSupport.xml", testName, null, false, null);

        /* Reconfigure downstreamServer with ejb remote support. */
        downstreamServer.reconfigServer("certSupported_ltpaSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /**
     * Test tolerance to dynamic addition of features for CSIv2.
     * Start downstreamServer with EJB support and application configuration.
     * Reconfigure downstreamServer with AppSecurity support.
     *
     * @throws Exception
     */
    @Test
    public void testAuthentication_downstreamServer_dynamicAppSecuritySupport() throws Exception {
        /* Make sure the web app in upstream server is ready to receive the request from client. For this, check for a list of messages. */
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();

        upstreamServer.reconfigServer("client_certSupported_ltpaSupported_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        /* Remove apps from the validation list. This is done because we are going to start downstream server with no application configured. */
        downstreamServer.removeAppsFromValidation();

        /* Start downstreamServer with no AppSecurity support. */
        downstreamServer.setSkipSecurityReadyMsg();
        downstreamServer.restartServer("noAppSecuritySupport.xml", testName, null, false, null);

        /* Reconfigure downstreamServer with ejb remote support. */
        downstreamServer.resetCheckForSecurityReady();
        downstreamServer.reconfigServer("certSupported_ltpaSupported_noAssertion.xml", testName, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Negative testing for Authentication Layer LTPA required.
     * Authentication Mechanism's establishTrustInClient is set to "Never".
     * refer to testAuthentication_SuppTransSupp_NeverLTPAAuthRequiredLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject above
     */

}
