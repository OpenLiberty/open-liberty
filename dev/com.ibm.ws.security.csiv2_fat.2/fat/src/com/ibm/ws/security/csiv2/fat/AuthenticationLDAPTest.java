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
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.csiv2.fat.common.CommonTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuthenticationLDAPTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        upstreamServer = setupUpstreamServer("ldap_client_minimal.xml");
        upstreamServer.setAllTestsRestartServer(true);
        downstreamServer = setupDownstreamServer("ldap_minimal.xml");
        downstreamServer.setAllTestsRestartServer(true);
        intermediateServer = setupIntermediateServer("intermediate.xml");
        intermediateServer.setAllTestsRestartServer(true);
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
     * Client and server with no iiopClient and no iiopServerPolicies respectively
     * yield Authentication layer subject.
     */
    @Test
    public void testAuthenticationLDAP_MinimalTransMinimal_MinimalAuthMinimal_MinimalAttrMinimal_authLayerSubject() throws Exception {
        upstreamServer.restartServer("ldap_client_minimal.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("ldap_minimal.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow(user, ldapPwd);

        assertSubjectInResponse(response, userDN);
    }

    /*
     * The client servlet will send a request to an EJB in the intermediate server and the EJB will invoke a remote
     * EJB in the downstream server. The intermediate server must assert a DN.
     */
    @Test
    public void testAuthenticationLDAP_dnAssertion_assertedSubject() throws Exception {
        addIgnoredErrors();
        upstreamServer.restartServer("ldap_client_minimal.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("ldap_assertion.xml", testName, downstreamApps, false, null);
        intermediateServer.restartServer("intermediate.xml", testName, intermediateApps, false, null);

        String response = driveDownstreamFlowViaIntermediate(user, ldapPwd);

        assertSubjectInResponse(response, assertedDN);
        assertNotNull("The assertion must be performed using ITTDistinguishedName.", downstreamServer.getServer().waitForStringInTraceUsingMark("TSSITTDistinguishedName > check"));
    }

    /*
     * The client servlet will send a request to an EJB in the intermediate server, but the intermediate server cannot assert.
     * The intermediate server must throw a NO_PERMISSION exception.
     */
    @Test
    public void testAuthenticationLDAP_assertion_NO_PERM() throws Exception {
        addIgnoredErrors();
        upstreamServer.restartServer("ldap_client_minimal.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("ldap_assertionAnonymousAndDN.xml", testName, downstreamApps, false, null);
        intermediateServer.restartServer("intermediate_assertionAnonymous.xml", testName, intermediateApps, false, null);

        String response = driveDownstreamFlowViaIntermediate(user, ldapPwd);

        assertTrue("The servlet must receive a NO_PERMISSION exception.",
                   response.contains("org.omg.CORBA.NO_PERMISSION") && response.contains("vmcid: 0x49421000 minor code: 0x93  completed: No"));
    }

    /*
     * For DN Assertion: When the intermediate server is using basic registry and the security name is not a DN,
     * it should issue a NO_PERMISSION with the CWWKS9640E message.
     */
    @Test
    public void testAuthenticationLDAP_assertionWithBasicRegistry_NO_PERM() throws Exception {
        addIgnoredErrors();
        upstreamServer.restartServer("client_minimal.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("downstreamBasic_assertionAnonymousAndDN.xml", testName, downstreamApps, false, null);
        intermediateServer.restartServer("intermediateBasic_assertionDN.xml", testName, intermediateApps, false, null);

        String response = driveDownstreamFlowViaIntermediate(user, pwd);

        assertTrue("The servlet must receive a NO_PERMISSION exception.",
                   response.contains("org.omg.CORBA.NO_PERMISSION: CWWKS9640E:") && response.contains("vmcid: 0x49421000 minor code: 0x92  completed: No"));
    }

    /*
     * These ignored errors get cleared every time the server stops, so call this method at the beginning of each test that needs it.
     */
    private void addIgnoredErrors() {
        List<String> ignoredErrors = new ArrayList<String>();
        ignoredErrors.add("CWWKS9542E");
        ignoredErrors.add("CNTR0020E"); // EJB Container wraps the CWWKS9542E message inside this one
        ignoredErrors.add("WLTC0017E"); // Transaction roll back
        intermediateServer.getServer().addIgnoredErrors(ignoredErrors);
    }

}
