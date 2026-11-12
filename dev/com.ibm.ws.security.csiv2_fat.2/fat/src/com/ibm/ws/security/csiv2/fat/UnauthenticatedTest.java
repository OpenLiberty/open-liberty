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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.csiv2.fat.common.CommonTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test cases for unauthenticated scenarios covering rows 1 and 13 of CSIv2 spec table 10.4 - TSS Interpretation of Client Credentials After Validation.
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class UnauthenticatedTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        upstreamServer = setupUpstreamServer("client_minimal.xml");
        upstreamServer.setAllTestsRestartServer(false);
        downstreamServer = setupDownstreamServer("minimal.xml");
        downstreamServer.setAllTestsRestartServer(false);
        setupBasicAuthClient();
    }

// Table 10.4, Row 1

    /*
     * Row 1: None, None, Absent
     * Client and server with no client certificate authentication (No transport layer), no authentication layers, and identity assertion disabled
     * TODO: Client code might need to be modified to send a service context when authentication layer is disabled and identity assertion is ITTAbsent.
     * yield Unauthenticated subject.
     */
    @Test
    public void testUnauthenticated_MinimalTransMinimal_MinimalAuthMinimal_MinimalAttrMinimal_unauthenticatedSubject() throws Exception {
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_minimal.xml", testName, true, messagesToWaitInUpstreamServer);
        downstreamServer.reconfigServer("minimal.xml", testName, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

    /*
     * Row 1: None, None, Absent
     * Client and server's client certificate authentication set to Never-Never, authentication layers with Never-Never, and identity assertion disabled
     * yield Unauthenticated subject.
     */
    //@Test
    public void testUnauthenticated_NeverTransNever_NeverLTPAAuthNeverLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        // upstreamServer.reconfigServer("client_certNever_ltpaNever_noAssertion.xml", testName, false, null);
        upstreamServer.restartServer("client_certNever_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // downstreamServer.reconfigServer("certNever_ltpaNever_noAssertion.xml", testName, false, null);
        // downstreamServer.startServer("certNever_ltpaNever_noAssertion.xml", downstreamApps, extraMsgs, testName, null);
        downstreamServer.restartServer("certNever_ltpaNever_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, UNAUTHENTICATED);
    }

    /*
     * Row 1: None, None, Absent
     * Client and server's client certificate authentication set to Never-Never, authentication layers with Never-Supported, and identity assertion disabled
     * yield Unauthenticated subject.
     */

    /*
     * Row 1: None, None, Absent
     * Client and server's client certificate authentication set to Never-Never, authentication layers with Supported-Never, and identity assertion disabled
     * yield Unauthenticated subject.
     */

    /*
     * Row 1: None, None, Absent
     * Client and server's client certificate authentication set to Never-Supported, authentication layers with Never-Never, and identity assertion disabled
     * yield Unauthenticated subject.
     */

    /*
     * Row 1: None, None, Absent
     * Client and server's client certificate authentication set to Supported-Never, authentication layers with Never-Never, and identity assertion disabled
     * yield Unauthenticated subject.
     */

// Table 10.4, Row 13

    /*
     * Row 13: none, No SAS Message
     * Client and server with no client certificate authentication (No transport layer), authentication layers incompatible (Never-Required), and identity assertion disabled
     * yield Unauthenticated subject.
     * Serviceability: The client must issue a warning message.
     */
    //@Test
    public void testUnauthenticated_NoTransNo_NeverLTPAAuthReqLTPA_NoIdentAssSecAttrNoIdentAss_authLayerSubject() throws Exception {
        // upstreamServer.reconfigServer("client_noTransport_ltpaNever_noAssertion.xml", testName, false, null);
        upstreamServer.restartServer("client_noTransport_ltpaNever_noAssertion.xml", testName, upstreamApps, false, null);
        // downstreamServer.reconfigServer("noTransport_ltpaRequired_noAssertion.xml", testName, false, null);
        // downstreamServer.startServer("noTransport_ltpaRequired_noAssertion.xml", downstreamApps, extraMsgs, testName, null);
        downstreamServer.restartServer("noTransport_ltpaRequired_noAssertion.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, UNAUTHENTICATED);
    }

    /*
     * Row 13: none, No SAS Message
     * Client and server with no client certificate authentication (No transport layer), authentication layers incompatible (Required-Never), and identity assertion disabled
     * yield Unauthenticated subject.
     * Serviceability: The client must issue a warning message.
     */

    /*
     * Row 13: none, No SAS Message
     * Client and server with no client certificate authentication (No transport layer), authentication layers incompatible (Never-Required), and identity assertion enabled
     * yield Unauthenticated subject.
     * Serviceability: The client must issue a warning message.
     */

    /*
     * Row 13: none, No SAS Message
     * Client and server with no client certificate authentication (No transport layer), authentication layers incompatible (Required-Never), and identity assertion enabled
     * yield Unauthenticated subject.
     * Serviceability: The client must issue a warning message.
     */

    private void assertNoPermission(String response) {
        boolean noPermissionFound = response.contains("org.omg.CORBA.NO_PERMISSION");
        assertTrue("The servlet must receive a NO_PERMISSION exception.", noPermissionFound);
    }

}
