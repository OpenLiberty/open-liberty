/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.IDPInitiated;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.saml.fat.jaxrs.common.RSSamlBasicTests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class were ported from tWAS' test SamlWebSSOTests.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
 * to an application installed on a WebSphere SP. When the Web client
 * invokes the SP application, it is redirected to a TFIM IdP which issues
 * a login challenge to the Web client. The Web Client fills in the login
 * form and after a successful login, receives a SAML 2.0 token from the
 * TFIM IdP. The client invokes the SP application by sending the SAML
 * 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RSSamlIDPInitiatedBasic2ServerTests extends RSSamlBasicTests {

    private static final Class<?> thisClass = RSSamlIDPInitiatedBasic2ServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.IDP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);
        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add("helloworld");

        copyMetaData = false;

        testAppServer = commonSetUp(APPServerName, "server_1.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.APP_SERVER_TYPE, extraApps2, extraMsgs2, false);
        commonUtils.fixServer2Ports(testAppServer);
        startSPWithIDPServer(SPServerName, "server_1.xml", extraMsgs, extraApps, true);

        // Allow the warning on the ignored attributes of samlWebSso20 inboundPropagation true or false
        testAppServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

        // set default values for jaxrs settings
        testSettings.setRSSettings();

        // set test app
        testSettings.setSpTargetApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.setSpDefaultApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.updatePartnerInSettings("sp1", true);

    }

}
