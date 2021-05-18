/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 10/2020
@RunWith(FATRunner.class)
public class CxfSSLUNTBasicTests extends SSLTestCommon {

    static private final Class<?> thisClass = CxfSSLUNTBasicTests.class;

    //2/2021
    //@ClassRule
    //public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE8_FEATURES().forServers(serverName).removeFeature("jsp-2.2").removeFeature("jaxws-2.2").removeFeature("servlet-3.1").addFeature("jsp-2.3").addFeature("jaxws-2.3").addFeature("servlet-4.0"));

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
     * The request should be successful.
     *
     */
    @Test
    //2/2021
    //@AllowedFFDC("java.util.MissingResourceException") //@AV999
    //4/2021
    //@AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException", "java.lang.ClassNotFoundException" })
    //5/2021 added PrivilegedActionExc, NoSuchMethodExc as a result of java11 and ee8
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException", "java.lang.ClassNotFoundException", "java.security.PrivilegedActionException",
                           "java.lang.NoSuchMethodException" })
    public void testUntWssecSvcClientSSL() throws Exception {

        genericTest("testUntWssecSvcClientSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a succesful message from the server.");

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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException")
    public void testUntWssecSvcClientOverrideUserSSL() throws Exception {

        genericTest("testUntWssecSvcClientOverrideDefUserSSL", untSSLClientUrl,
                    portNumberSecure, "user2", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a succesful message from the server.");

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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException") //@AV999
    public void testUntNoUserNoPasswordSSL() throws Exception {

        genericTest("testUntNoUserNoPasswordSSL", untSSLClientUrl,
                    portNumberSecure, null, null, "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a succesful message from the server.");

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
    //Orig:
    //@ExpectedFFDC("org.apache.ws.security.WSSecurityException")
    //2/2021
    @AllowedFFDC(value = { "java.util.MissingResourceException", "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException" })
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
    //Orig:
    //@AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //2/2021
    @AllowedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException", "org.apache.ws.security.WSSecurityException" })
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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException")
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
    //2/2021
    //@AllowedFFDC("java.util.MissingResourceException")
    //4/2021
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.net.MalformedURLException", "java.lang.ClassNotFoundException" })
    public void testUntWssecSvcClientSSLManaged() throws Exception {

        genericTest("testUntWssecSvcClientSSLManaged", untSSLClientUrl,
                    portNumberSecure, "true", "user1", "security", "FVTVersionBA6Service",
                    "UrnBasicPlcyBA6", "", "", "Response: WSSECFVT FVTVersion_ba06", "The test expected a succesful message from the server.");

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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException")
    public void testUntCxfSSL() throws Exception {

        genericTest("testUntCxfSSL", untSSLClientUrl,
                    portNumberSecure, "user1", "security", "FVTVersionBAService",
                    "UrnBasicPlcyBA", "", "", "Response: WSSECFVT FVTVersion_ba01", "The test expected a succesful message from the server.");

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
    //2/2021
    @AllowedFFDC("java.util.MissingResourceException")
    public void testUntCxfSSLManaged() throws Exception {

        genericTest("testUntCxfSSLManaged", untSSLClientUrl,
                    portNumberSecure, "true", "user1", "security", "FVTVersionBAService",
                    "UrnBasicPlcyBA", "", "", "Response: WSSECFVT FVTVersion_ba01", "The test expected a succesful message from the server.");

    }

}
