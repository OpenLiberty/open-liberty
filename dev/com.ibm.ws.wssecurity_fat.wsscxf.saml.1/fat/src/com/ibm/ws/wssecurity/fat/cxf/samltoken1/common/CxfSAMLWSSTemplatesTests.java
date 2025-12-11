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

package com.ibm.ws.wssecurity.fat.cxf.samltoken1.common;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.SkipForRepeat;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
//import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.annotation.MinimumJavaLevel;


/**
 * WSS Template tests
 */

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfSAMLWSSTemplatesTests extends SAMLCommonTest {

    private static final Class<?> thisClass = CxfSAMLWSSTemplatesTests.class;
    protected static CXFSAMLCommonUtils commonUtils = new CXFSAMLCommonUtils();
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static String policyType = "wsdl";
    
    //issue 23060
    //Note: The saml.1 Full class uses EE7 no modification rule without EE7cbh2 to avoid the extended test execution time
    
    /**
     * TestDescription:
     * Client matches server side policy (transport enabled)
     * Test should succeed in accessing the server side service.
     * 
     */
    
    @Test
    public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSL() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpTargetApp(testSAMLServer.getHttpsString() + "/samlwsstemplatesclient/CxfWssSAMLTemplatesSvcClient");
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "WSSTemplatesService2", "WSSTemplate2", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        updatedTestSettings.setWebClientTimeOut(300000); // Issue #29091
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_2));

    }
    

    /**
     * TestDescription:
     * Client matches server side policy (transport enabled), but
     * the client doesn't use https
     * Test should fail to accessing the server side service.
     * 
     */
    
    @Test
    public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSL_httpFromClient() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpTargetApp(testSAMLServer.getHttpsString() + "/samlwsstemplatesclient/CxfWssSAMLTemplatesSvcClient");
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService2", "WSSTemplate2", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        
        //issue 23060
        String CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED = "HttpsToken could not be asserted: Not an HTTPs connection"; //@AV999 new error message
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED));
        
    	
    }

    /**
     * TestDescription:
     * This test uses a policy with Asymmetric X509 Policy with
     * Mutual Authentication and SAML.
     * Client and Server use the same policy
     * Test should succeed in accessing the server side service.
     */
    @MinimumJavaLevel(javaLevel = 17)
    @Test
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml() throws Exception {
        
        if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_wsstemplate_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        }
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService4", "WSSTemplate4", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_4));

    }
 
    /**
     * TestDescription:
     * This test uses a policy with Asymmetric X509 Policy with
     * Mutual Authentication and SAML.
     * Client uses a policy that omits the Initiator Token from the policy
     * Test should fail to access the server side service.
     */
    @MinimumJavaLevel(javaLevel = 17)
    @Test
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitInitiatorToken() throws Exception {
    	
        if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_wsstemplate_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } 
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        if ("externalPolicy".equals(policyType)) {
            updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService4", "WSSTemplate4", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitInitiatorTokenWithEP.wsdl", servicePort));
        }
        else {
            updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService4", "WSSTemplate4", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitInitiatorToken.wsdl", servicePort));
        }

        //issue 23060 
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        String CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR = "These policy alternatives can not be satisfied:"; //@AV999, we need to use different error message depending on runtime
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR)); //@AV999
        
    }
    
    /**
     * TestDescription:
     * This test uses a policy with Asymmetric X509 Policy with
     * Mutual Authentication and SAML.
     * Client uses a policy that omits the Recipient Token from the policy
     * Test should fail to access the server side service.
     */
    
    @Test
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitRecipientToken() throws Exception {
    	
        if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_wsstemplate_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_AsymSignEnc.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        }
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        if ("externalPolicy".equals(policyType)) {
            updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService4", "WSSTemplate4", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitRecipientTokenWithEP.wsdl", servicePort));
        }
        else {
            updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService4", "WSSTemplate4", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitRecipientToken.wsdl", servicePort));
        }
        
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        //issue 23060 
        if ("externalPolicy".equals(policyType)) { //@AV999 TODO in jaxws-2.3 , this is a successful scenario
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_4)); //@AV999
        } else {
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        } 
        
    }   
    
    /**
     * TestDescription:
     * This test uses a policy with Symmetric X509 Message Policy and SAML.
     * Client and Server use the same policy
     * Test should succeed in accessing the server side service.
     */
    @MinimumJavaLevel(javaLevel = 17)
    @Test
    public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClient() throws Exception {
    	
    	WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService6", "WSSTemplate6", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_6));

    }
    
    /**
     * TestDescription:
     * This test uses a policy with Symmetric X509 Message Policy and SAML.
     * Client uses policy that omits the Protection Policy
     * Test should fail to access the server side service.
     */
    
    @Test
    public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClient_omitProtectionPolicy() throws Exception {
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService6", "WSSTemplate6", "", "False", null, commonUtils.processClientWsdl("ClientSymOmitProtectionPolicy.wsdl", servicePort));
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        //issue 23060 
        if ("externalPolicy".equals(policyType)) { //@AV999 , jaxws-2.3 this is failing at the client side
            String CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR = "javax.xml.ws.soap.SOAPFaultException: javax.xml.crypto.dsig.TransformException: org.apache.wss4j.common.ext.WSSecurityException: Referenced Token ";
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        } else {
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        } 
    	
    }

}
