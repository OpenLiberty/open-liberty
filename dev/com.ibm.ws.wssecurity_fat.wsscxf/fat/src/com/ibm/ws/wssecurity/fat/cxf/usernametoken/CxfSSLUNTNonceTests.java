/*******************************************************************************
S * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;

@SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES })
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
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntNonceOnlySSL() throws Exception {

        genericTest("testCxfUntNonceOnlySSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA5Service",
                    "UrnBasicPlcyBA5", "", "",
                    "Response: WSSECFVT FVTVersion_ba05",
                    "The test expected a successful message from the server.");

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
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntNonceAndCreatedSSL() throws Exception {

        genericTest("testCxfUntNonceAndCreatedSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA4Service",
                    "UrnBasicPlcyBA4", "", "",
                    "Response: WSSECFVT FVTVersion_ba04",
                    "The test expected a successful message from the server.");

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
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntNonceAndCreatedNoIdSSL() throws Exception {

        genericTest("testCxfUntNonceAndCreatedNoIdSSL", untSSLClientUrl,
                    portNumberSecure, null, null, "FVTVersionBA4Service",
                    "UrnBasicPlcyBA4", "", "",
                    "Response: WSSECFVT FVTVersion_ba04",
                    "The test expected a successful message from the server.");

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
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntExpiredMsgSSL() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_ee8.xml");

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
     * request should fail as the message is set in the future.
     *
     */

    @Test
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfUntOldExtFutureTimestampSSLEE7Only() throws Exception {

        genericTest(
                    "testCxfUntOldExtFutureTimestampSSLEE7Only",
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

    @Test
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException", "java.net.MalformedURLException" },
                 repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntOldExtFutureTimestampSSLEE9Only() throws Exception {

        genericTest(
                    "testCxfUntOldExtFutureTimestampSSLEE9Only",
                    untSSLClientUrl,
                    portNumberSecure,
                    "user1",
                    "security",
                    "FVTVersionBA7Service",
                    "UrnBasicPlcyBA7",
                    "",
                    "futureTime",
                    "Invalid timestamp: The message timestamp is out of range", //@AV999 new message
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
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntReqTimestampSSL() throws Exception {

        genericTest("testCxfUntReqTimestampSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA5Service",
                    "UrnBasicPlcyBA5", "", "",
                    "Response: WSSECFVT FVTVersion_ba05",
                    "The test expected a successful message from the server.");

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
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfUntReqTimestampMissingSSLEE7Only() throws Exception {

        genericTest(
                    "testCxfUntReqTimestampMissingSSLEE7Only",
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

    @Test
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntReqTimestampMissingSSLEE9Only() throws Exception {

        genericTest(
                    "testCxfUntReqTimestampMissingSSLEE9Only",
                    untSSLClientUrl,
                    portNumberSecure,
                    "user1",
                    "security",
                    "FVTVersionBA5Service",
                    "UrnBasicPlcyBA5",
                    "",
                    "missingTimestamp",
                    morethanOneTimestamp,
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
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfUntReplaySSLEE7Only() throws Exception {

        genericTest("testCxfUntReplaySSLEE7Only", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "", replayAttack,
                    "Second call to FVTVersionBA7Service should have failed");
    }

    @Test
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntReplaySSLEE9Only() throws Exception {

        genericTest("testCxfUntReplaySSLEE9Only", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "", replayAttackNew, //@AV999
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
    @SkipForRepeat({ EE8_FEATURES, EE9_FEATURES })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testCxfUntHardcodedReplaySSLEE7Only() throws Exception {

        genericTest("testCxfUntHardcodedReplaySSLEE7Only", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "strReplayNonce", replayAttack,
                    "Second call to FVTVersionBA7Service should have failed");
    }

    @Test
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { JakartaEE9Action.ID })
    public void testCxfUntHardcodedReplaySSLEE9Only() throws Exception {

        genericTest("testCxfUntHardcodedReplaySSLEE9Only", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "strReplayNonce", replayAttackNew,
                    "Second call to FVTVersionBA7Service should have failed");
    }

}
