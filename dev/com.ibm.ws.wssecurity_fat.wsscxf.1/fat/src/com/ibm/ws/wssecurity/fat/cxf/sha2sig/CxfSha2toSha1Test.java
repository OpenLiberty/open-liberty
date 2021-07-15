/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.sha2sig;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.wssecurity.fat.utils.common.CommonTests;

import componenttest.annotation.AllowedFFDC;

/**
 * Removed sha2tosha1 test from the CXFSha2SigTests
 * as there has been a configuration failure when switching
 * from sha1 to sha2 that the framework is improperly
 * reconfiguring. Easiest fix is to just create a standalone test for
 * sha2 to sha1.
 *
 */
public class CxfSha2toSha1Test extends CommonTests {

//  static private UpdateWSDLPortNum newWsdl = null;
    static final private String serverName = "com.ibm.ws.wssecurity_fat.sha2sig";

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(serverName, false,
                    "/sha2sigclient/Sha2SigSvcClient");
    }

    /**
     * TestDescription:
     *
     * A TWAS thin client invokes a simple jax-ws cxf web service.
     * The SOAP Body is signed in the request and response messages,
     * using Sha256 signature algorithm, but the Web service is configured
     * to use the sha1 signature algorithm. The client request is
     * expected to be rejected with an appropriate exception.
     * This is a negative scenario.
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfSha2ToSha1SigAlgorithm() throws Exception {

        String thisMethod = "testCxfSha2ToSha1SigAlgorithm";
        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha1.xml");
        genericTest(
                    // test name for logging
                    thisMethod,
                    // Svc Client Url that generic test code should use
                    clientHttpUrl,
                    // Port that svc client code should use
                    "",
                    // user that svc client code should use
                    "user1",
                    // pw that svc client code should use
                    "security",
                    // wsdl sevice that svc client code should use
                    "Sha2SigService6",
                    // wsdl that the svc client code should use
                    // newClientWsdl,
                    "",
                    // wsdl port that svc client code should use
                    "UrnSha2Sig6",
                    // msg to send from svc client to server
                    "",
                    "Response: javax.xml.ws.soap.SOAPFaultException",
                    // expected response from server
                    "The signature method does not match the requirement",
                    // msg to issue if do NOT get the expected result
                    "The test expected a succesful message from the server.");

    }
}
