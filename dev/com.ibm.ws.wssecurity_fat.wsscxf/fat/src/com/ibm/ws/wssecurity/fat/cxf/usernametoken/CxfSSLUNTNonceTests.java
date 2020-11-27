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

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;

//12/2020 Setting this test class for LITE bucket
//@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfSSLUNTNonceTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTNonceTests.class;

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding and Nonce are specified in the wsdl. A valid msg is sent
     * (nonce generated) The request should be successful.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCxfUntNonceOnlySSL() throws Exception {

        genericTest("testCxfUntNonceOnlySSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA5Service",
                    "UrnBasicPlcyBA5", "", "",
                    "Response: WSSECFVT FVTVersion_ba05",
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding, Nonce and Created are specified in the wsdl. A valid
     * msg is sent (nonce/created generated) The request should be successful.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCxfUntNonceAndCreatedSSL() throws Exception {

        genericTest("testCxfUntNonceAndCreatedSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA4Service",
                    "UrnBasicPlcyBA4", "", "",
                    "Response: WSSECFVT FVTVersion_ba04",
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with NO username/password
     * in the username token. The client default should be used instead. The
     * call to the service client is made using https. The call to the server is
     * also made using https. TransportBinding, Nonce and Created are specified
     * in the wsdl. A valid msg is sent (nonce/created generated) The request
     * should be successful.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCxfUntNonceAndCreatedNoIdSSL() throws Exception {

        genericTest("testCxfUntNonceAndCreatedNoIdSSL", untSSLClientUrl,
                    portNumberSecure, null, null, "FVTVersionBA4Service",
                    "UrnBasicPlcyBA4", "", "",
                    "Response: WSSECFVT FVTVersion_ba04",
                    "The test expected a succesful message from the server.");

    }

    // need another server with NO id/pw info to really test behavior with no
    // id/pw

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding, Nonce and Created are specified in the wsdl. A
     * hard-coded message containing a timestamp that is expired. The request
     * should fail as the message has expired.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntExpiredMsgSSL() throws Exception {

        genericTest("testCxfUntExpiredMsgSSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA4Service", "UrnBasicPlcyBA4",
                    "", "oldTimeMsg", msgExpires,
                    "The message has expired. It should have failed");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding, Nonce and Created are specified in the wsdl. A
     * hard-coded message containing a timestamp that is in the future. The
     * request should fail as the message is set in th future.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntOldExtFutureTimestampSSL() throws Exception {

        genericTest(
                    "testSslCxfWebService07OldExtFutureTimestampNegative",
                    untSSLClientUrl,
                    portNumberSecure,
                    "user1",
                    "security",
                    "FVTVersionBA7Service",
                    "UrnBasicPlcyBA7",
                    "",
                    "futureTime",
                    "The message has expired (WSSecurityEngine: Invalid timestamp The security semantics of the message have expired)",
                    "A future time stamp in WS request did not fail");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding with Timestamp and Nonce are specified in the wsdl. Test
     * should pass as the Timestamp should be generated.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    public void testCxfUntReqTimestampSSL() throws Exception {

        genericTest("testCxfUntReqTimestampSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA5Service",
                    "UrnBasicPlcyBA5", "", "",
                    "Response: WSSECFVT FVTVersion_ba05",
                    "The test expected a succesful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding with Timestamp and Nonce are specified in the wsdl. Test
     * expects a failure as the timestamp content is missing.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntReqTimestampMissingSSL() throws Exception {

        genericTest(
                    "testCxfUntReqTimestampMissingSSL",
                    untSSLClientUrl,
                    portNumberSecure,
                    "user1",
                    "security",
                    "FVTVersionBA5Service",
                    "UrnBasicPlcyBA5",
                    "",
                    "missingTimestamp",
                    timestampReqButMissing,
                    "The test expected an exception from the server because the timestamp was missing.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding and Nonce are specified in the wsdl. A hard-coded
     * message is passed twice. The request should fail on the second call with
     * a replay exception.
     *
     */
    //Defect 88389 - uncomment next line when issue is resolved
    //@Test
    public void testCxfUntReqTimestampHardcodedMsgTwiceSSL() throws Exception {

        genericTest("testCxfUntReqTimestampHardcodedMsgTwiceSSL",
                    untSSLClientUrl, portNumberSecure, "user1", "security",
                    "FVTVersionBA5Service", "UrnBasicPlcyBA5", "true", "replay",
                    replayAttack, "The CXF SSL Web service test failed");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding and Nonce are specified in the wsdl. A hard-coded
     * message is passed twice. The request should fail on the second call with
     * a replay exception.
     *
     */
    //Defect 88389 - uncomment next line when issue is resolved
    //@Test
    public void testCxfUntReqTimestampSendMsgTwiceSSL() throws Exception {

        genericTest("testCxfUntReqTimestampSendMsgTwiceSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA5Service",
                    "UrnBasicPlcyBA5", "true", "", replayAttack,
                    "The CXF SSL Web service test failed");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding and Nonce are specified in the wsdl. A generated message
     * is passed twice. The request should fail on the second call with a replay
     * exception.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntReplaySSL() throws Exception {
        genericTest("testCxfUntReplaySSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "", replayAttack,
                    "Second call to FVTVersionBA7Service should have failed");
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid
     * username/password in the username token. The call to the service client
     * is made using https. The call to the server is also made using https.
     * TransportBinding and Nonce are specified in the wsdl. A hard-coded
     * message is passed twice. The request should fail on the second call with
     * a replay exception.
     *
     */
    @Test
    //Added 11/2020
    //@Mode(TestMode.FULL)
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntHardcodedReplaySSL() throws Exception {
        genericTest("testCxfUntReplaySSL", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "strReplayNonce", replayAttack,
                    "Second call to FVTVersionBA7Service should have failed");
    }

}
