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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.csiv2.fat.common.CommonTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test cases for identity assertions with certificate chain.
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class IdentityAssertionCertChainTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        /*
         * Certificate chain tests requires 3 servers. Namely, upstream, intermediate and downstream.
         * To start with, the upstream server will be configured with "client_minimal.xml"
         */
        upstreamServer = setupUpstreamServer("client_certSupported_ltpaNever_noAssertion.xml");
        upstreamServer.setAllTestsRestartServer(true);
        /*
         * The intermediate server, need to be configured for both as a client and as a server. we use
         * intermediate.xml to make this configuration.
         */
        intermediateServer = setupIntermediateServer("intermediate_certRequired_ltpaNever_noAssertion.xml");
        intermediateServer.setAllTestsRestartServer(true);
        /* The down stream server is created with presumed trust configuration */
        downstreamServer = setupDownstreamServer("certSupported_ltpaSupported_assertionPresumedTrust.xml");
        downstreamServer.setAllTestsRestartServer(true);
        setupBasicAuthClient();
    }

    @Test
    public void testIdentityAssertionCertChain() throws Exception {
        ArrayList<String> messagesToWaitInUpstreamServer = createMessagesToWaitInUpstreamServer();
        upstreamServer.reconfigServer("client_certSupported_ltpaNever_noAssertion.xml", testName, true, messagesToWaitInUpstreamServer);
        /*
         * The intermediate server need to be configured with identity assertion with cert chain.
         */
        intermediateServer.reconfigServer("intermediate_certRequired_ltpaNever_noAssertion.xml", testName, false, null);
        LibertyServer downstreamLibertyServer = downstreamServer.getServer();

        String response = driveDownstreamFlowViaIntermediate(user, pwd);
        assertSubjectInResponse(response, transportMappedUser); // From ITTX509CertChain identity token
        assertNotNull("The message 'TSSCompoundSecMechConfig 3 The attribute layer subject was selected.' is not logged when certificate chain assertion is performed.",
                      downstreamLibertyServer.waitForStringInTrace("TSSCompoundSecMechConfig 3 The attribute layer subject was selected."));
    }

    @Test
    public void testIdentityAssertionCertChain_NO_PERM() throws Exception {
        upstreamServer.restartServer("client_certNever_gssupSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        intermediateServer.restartServer("intermediate_certRequired_assertion.xml", testName, intermediateApps, false, null);

        String response = driveDownstreamFlowViaIntermediate(user, pwd);
        assertTrue("The servlet must receive a NO_PERMISSION exception.",
                   response.contains("org.omg.CORBA.NO_PERMISSION") && response.contains("vmcid: 0x49421000 minor code: 0x92  completed: No"));

    }
}
