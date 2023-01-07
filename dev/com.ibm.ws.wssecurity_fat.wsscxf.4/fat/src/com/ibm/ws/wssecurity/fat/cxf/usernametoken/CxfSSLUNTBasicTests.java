/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;

@SkipForRepeat({ RepeatWithEE7cbh20.ID, EE9_FEATURES, EE10_FEATURES })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfSSLUNTBasicTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTBasicTests.class;

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     *
     */

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in the username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is specified
     * in the wsdl.
     * mustUnderstand is enabled by default
     * The request should be successful.
     *
     */
    //issue 23060
    @Test
    public void testUntWssecSvcClientSSL() throws Exception {

        genericTest("testUntWssecSvcClientSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in the username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is specified
     * in the wsdl.
     * ws-security.must-understand="false" is configured on the client (server.xml)
     * The request should be successful.
     *
     */
    //issue 23060
    @Test
    public void testUntWssecSvcClientSSLDisableMustUnderstand() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_disable_mustunderstand.xml");
        genericTest("testUntWssecSvcClientSSLDisableMustUnderstand", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06_NO_mustUnderstand_in_header_expected",
                    "The test expected a successful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in the username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is specified
     * in the wsdl.
     * The request should be successful.
     *
     */
    @Test
    public void testUntWssecSvcClientOverrideUserSSL() throws Exception {

        genericTest("testUntWssecSvcClientOverrideDefUserSSL", untSSLClientUrl,
                    portNumberSecure, "user2", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with no user/password
     * in the username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is specified
     * in the wsdl.
     * The request should be successful.
     *
     */
    @Test
    public void testUntNoUserNoPasswordSSL() throws Exception {

        genericTest("testUntNoUserNoPasswordSSL", untSSLClientUrl,
                    portNumberSecure, null, null, "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username and invalid password
     * in the username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is specified
     * in the wsdl.
     * The request should be fail because of the bad password.
     *
     */
    @Test
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testUntCxfBadPswdSSL() throws Exception {

        genericTest("testUntCxfBadPswdSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "badPw", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "The security token could not be authenticated or authorized", "The test expected an authentication/authorization exception.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with an invalid username and valid password
     * in the username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is specified
     * in the wsdl.
     * The request should be fail because of the bad user id.
     *
     */
    @Test
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    public void testUntCxfBadPUserSSL() throws Exception {

        genericTest("testUntCxfBadPUserSSL", untSSLClientUrl,
                    portNumberSecure, "BadId", "NoMatter", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "The security token could not be authenticated or authorized", "The test expected an authentication/authorization exception.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in username token. The call to the service client is made using http.
     * The call to the server is also made using http. TransportBinding is specified
     * in the wsdl.
     * The request should fail as ssl is required.
     *
     */
    @Test
    public void testUntCxfNoSSL() throws Exception {

        genericTest("testUntCxfNoSSL", untClientUrl,
                    "", "user1", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", badHttpsToken, "The test should have received an exception as we used http when https was required.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in the username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is specified
     * in the wsdl.
     * The request should be successful.
     *
     */
    @Test
    public void testUntWssecSvcClientSSLManaged() throws Exception {

        genericTest("testUntWssecSvcClientSSLManaged", untSSLClientUrl,
                    portNumberSecure, "true", "user1", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is NOT specified
     * in the wsdl.
     * The request should succeed a using SSL when not required is not an issue.
     *
     */
    @Test
    public void testUntCxfSSL() throws Exception {

        genericTest("testUntCxfSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBAService",
                    "UrnBasicPlcyBA", "", "", "Response: WSSECFVT FVTVersion_ba01", "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is NOT specified
     * in the wsdl.
     * The request should succeed a using SSL when not required is not an issue.
     *
     */
    @Test
    public void testUntCxfSSLManaged() throws Exception {

        genericTest("testUntCxfSSLManaged", untSSLClientUrl,
                    portNumberSecure, "true", "user1", "security", "FVTVersionBAService",
                    "UrnBasicPlcyBA", "", "", "Response: WSSECFVT FVTVersion_ba01", "The test expected a successful message from the server.");

    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf service client, with a valid username/password
     * in username token. The call to the service client is made using https.
     * The call to the server is also made using https. TransportBinding is NOT specified
     * in the wsdl.
     * ws-security.must-understand="false" is configured on the client (server.xml)
     * The request should succeed a using SSL when not required is not an issue.
     *
     */
    //issue 23060
    //@Test 12/2022 comment out temporarily until issue 23735 is resolved
    public void testUntCxfSSLManagedDisableMustUnderstand() throws Exception {

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_disable_mustunderstand.xml");
        genericTest("testUntCxfSSLManagedDisableMustUnderstand", untSSLClientUrl,
                    portNumberSecure, "true", "user1", "security", "FVTVersionBAService",
                    "UrnBasicPlcyBA", "", "", "Response: WSSECFVT FVTVersion_ba01_NO_mustUnderstand_in_header_expected", "The test expected a successful message from the server.");

        reconfigServer(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server.xml");

    }

}
