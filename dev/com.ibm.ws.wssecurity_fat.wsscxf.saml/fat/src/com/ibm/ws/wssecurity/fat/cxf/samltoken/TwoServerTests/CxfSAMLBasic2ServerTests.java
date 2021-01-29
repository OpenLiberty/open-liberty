/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken.TwoServerTests;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.wssecurity.fat.cxf.samltoken.common.CxfSAMLBasicTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.HttpUtils;

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
//1/20/2021 the FULL mode was already in CL FAT, but commented out to make it as LITE in OL
//@Mode(TestMode.FULL)
//1/21/2021 added
@RunWith(FATRunner.class)
public class CxfSAMLBasic2ServerTests extends CxfSAMLBasicTests {

    private static final Class<?> thisClass = CxfSAMLBasic2ServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.SOLICITED_SP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        //1-12-2021 commented out
        //HttpUtils.enableSSLv3();

        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        List<String> extraMsgs2 = new ArrayList<String>();
        extraMsgs.add("CWWKT0016I.*samlcxfclient.*");
        extraMsgs.add("CWWKS5000I");
        extraMsgs.add("CWWKS5002I");
        extraMsgs2.add("CWWKT0016I.*samltoken.*");

        List<String> extraApps = new ArrayList<String>();
        List<String> extraApps2 = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CXF_CLIENT_APP);
        extraApps2.add(SAMLConstants.SAML_TOKEN_APP);

        copyMetaData = false;
        testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.IDP_SERVER_TYPE, null, null, SAMLConstants.SKIP_CHECK_FOR_SECURITY_STARTED);
        copyMetaData = true;
        testSAMLServer2 = commonSetUp("com.ibm.ws.wssecurity_fat.saml.2servers", "server_2.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_SERVER_TYPE, extraApps2, extraMsgs2);
        copyMetaData = true;
        testSAMLServer = commonSetUp("com.ibm.ws.wssecurity_fat.saml", "server_1.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_SERVER_TYPE, extraApps, extraMsgs, SAMLConstants.EXAMPLE_CALLBACK, SAMLConstants.EXAMPLE_CALLBACK_FEATURE);

        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        testSAMLServer2.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES, SAMLMessageConstants.CWWKF0001E_FEATURE_MISSING);

        // now, we need to update the IDP files
        shibbolethHelpers.fixSPInfoInShibbolethServer(testSAMLServer, testIDPServer);
        shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
        // now, start the shibboleth app with the updated config info
        startShibbolethApp(testIDPServer);

        setActionsForFlowType(flowType);

        helpers.setSAMLServer(testSAMLServer);

        commonUtils.fixServer2Ports(testSAMLServer2);

        servicePort = Integer.toString(testSAMLServer2.getServerHttpPort());
        serviceSecurePort = Integer.toString(testSAMLServer2.getServerHttpsPort());

        setActionsForFlowType(flowType);
        testSettings.setIdpUserName("user1");
        testSettings.setIdpUserPwd("security");
        testSettings.setSpTargetApp(testSAMLServer.getHttpString() + "/samlcxfclient/CxfSamlSvcClient");
        testSettings.setSamlTokenValidationData(testSettings.getIdpUserName(), testSettings.getSamlTokenValidationData().getIssuer(), testSettings.getSamlTokenValidationData().getInResponseTo(), testSettings.getSamlTokenValidationData().getMessageID(), testSettings.getSamlTokenValidationData().getEncryptionKeyUser(), testSettings.getSamlTokenValidationData().getRecipient(), testSettings.getSamlTokenValidationData().getEncryptAlg());

    }

}