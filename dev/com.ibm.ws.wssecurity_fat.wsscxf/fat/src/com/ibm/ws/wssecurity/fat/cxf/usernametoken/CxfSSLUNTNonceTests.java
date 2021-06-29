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

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;

//12/2020 Setting this test class for LITE bucket
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfSSLUNTNonceTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTNonceTests.class;

    //2/2021
    //@ClassRule
    //public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().forServers(serverName).removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").removeFeature("usr:wsseccbh-1.0").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0").addFeature("usr:wsseccbh-2.0"));

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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException") //@AV999
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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException") //@AV999
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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException") //@AV999
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
    //Orig: this will trigger failure although test passed
    //@ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //2/2021
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException", "org.apache.ws.security.WSSecurityException" })
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
     * request should fail as the message is set in the future.
     *
     */

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig: need to work on old feature
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
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

    //2/2021 to run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    //2/2021
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })
    public void testCxfUntOldExtFutureTimestampSSLEE8Only() throws Exception {

        genericTest(
                    "testCxfUntOldExtFutureTimestampSSLEE8Only",
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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException") //@AV999
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

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
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

    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }) //@AV999
    public void testCxfUntReqTimestampMissingSSLEE8Only() throws Exception {

        genericTest(
                    "testCxfUntReqTimestampMissingSSLEE8Only",
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

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig: also needs the old variable replayAttack to work on old feature
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntReplaySSLEE7Only() throws Exception {

        genericTest("testCxfUntReplaySSLEE7Only", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "", replayAttack,
                    "Second call to FVTVersionBA7Service should have failed");
    }

    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }) //@AV999
    public void testCxfUntReplaySSLEE8Only() throws Exception {

        genericTest("testCxfUntReplaySSLEE8Only", untSSLClientUrl, portNumberSecure,
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

    //2/2021 run with EE7
    @Test
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    //Orig:
    @ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfUntHardcodedReplaySSLEE7Only() throws Exception {

        genericTest("testCxfUntHardcodedReplaySSLEE7Only", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "strReplayNonce", replayAttack,
                    "Second call to FVTVersionBA7Service should have failed");
    }

    //2/2021 run with EE8
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    //2/2021
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.wss4j.common.ext.WSSecurityException" }) //@AV999
    public void testCxfUntHardcodedReplaySSLEE8Only() throws Exception {

        genericTest("testCxfUntHardcodedReplaySSLEE8Only", untSSLClientUrl, portNumberSecure,
                    "user1", "security", "FVTVersionBA7Service", "UrnBasicPlcyBA7",
                    "true", "strReplayNonce", replayAttackNew, //@AV999
                    "Second call to FVTVersionBA7Service should have failed");
    }

}
