/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.samltoken5.OneServerTests;


import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.wssecurity.fat.cxf.samltoken5.common.CxfSAMLSymSignEncTests;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
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
public class CxfSAMLSymSignEnc1ServerTests extends CxfSAMLSymSignEncTests {

    private static final Class<?> thisClass = CxfSAMLSymSignEnc1ServerTests.class;
    protected static String repeatAction = "";

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

    	//issue 23060 FAT can't use random chooseRandomFlow()
        flowType = SAMLConstants.SOLICITED_SP_INITIATED ;
        
        idpSupportedType = SAMLConstants.TFIM_TYPE;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKT0016I.*samlcxfclient.*");
        extraMsgs.add("CWWKS5000I");
        extraMsgs.add("CWWKS5002I");

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CXF_CLIENT_APP);
       
        startSPWithIDPServer("com.ibm.ws.wssecurity_fat.saml", "server_2_in_1_symSignEnc.xml", SAMLConstants.SAML_SERVER_TYPE, extraMsgs, extraApps, true, SAMLConstants.EXAMPLE_CALLBACK, SAMLConstants.EXAMPLE_CALLBACK_FEATURE);
 
        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES, SAMLMessageConstants.CWWKF0001E_FEATURE_MISSING);

        servicePort = Integer.toString(testSAMLServer.getServerHttpPort());
        serviceSecurePort = Integer.toString(testSAMLServer.getServerHttpsPort());

        setActionsForFlowType(flowType);
        testSettings.setIdpUserName("user1");
        testSettings.setIdpUserPwd("security");
        testSettings.setSpTargetApp(testSAMLServer.getHttpString() + "/samlcxfclient/CxfSamlSvcClient");
        testSettings.setSamlTokenValidationData(testSettings.getIdpUserName(), testSettings.getSamlTokenValidationData().getIssuer(), testSettings.getSamlTokenValidationData().getInResponseTo(), testSettings.getSamlTokenValidationData().getMessageID(), testSettings.getSamlTokenValidationData().getEncryptionKeyUser(), testSettings.getSamlTokenValidationData().getRecipient(), testSettings.getSamlTokenValidationData().getEncryptAlg());

	    //issue 23060
        //Note that in the new format ehcache "cxf-ehcache_ee8.xml", the wss4j section of "ws-security.nonce.cache.instance" template is commented out 
        //since it's not supported/used in the current runtime
        Log.info(thisClass, "setupBeforeTest", "current repeat action: " + RepeatTestFilter.getRepeatActionsAsString());
        repeatAction = RepeatTestFilter.getRepeatActionsAsString();
        //default NO_MODIFICATION repeat action does not use any name extension
        if (repeatAction == "" || repeatAction == null ) {
            Log.info(thisClass,"setupBeforeTest", "the test is: EE7 to run with OLD format ehcache ");
            setEhcacheVersion("EE7OLDEhcache");
        } else if (repeatAction.contains("_EE7cbh-2.0")) {
            Log.info(thisClass, "setupBeforeTest", "the test is: EE7 to run with NEW format ehcache ");
            setEhcacheVersion("EE7NEWEhcache");
        }
        
    }
}
