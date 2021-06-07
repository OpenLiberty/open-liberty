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

package com.ibm.ws.wssecurity.fat.cxf.samltoken.common;

import org.junit.Test;
import org.junit.runner.RunWith;

//3/2021
import componenttest.annotation.SkipForRepeat;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * WSS Template tests
 */

@LibertyServerWrapper
//1/20/2021 the full mode is already set at class level in CL FAT and kept the same in OL but 
//will no longer need the LITE mode in some of test cases below
@Mode(TestMode.FULL)
//1/21/2021 added
@RunWith(FATRunner.class)
public class CxfSAMLWSSTemplatesTests extends SAMLCommonTest {

    private static final Class<?> thisClass = CxfSAMLWSSTemplatesTests.class;
    protected static CXFSAMLCommonUtils commonUtils = new CXFSAMLCommonUtils();
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static String policyType = "wsdl";

    /**
     * TestDescription:
     * Client matches server side policy (transport enabled)
     * Test should succeed in accessing the server side service.
     * 
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    //Orig:
    //public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSL() throws Exception {
    public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSLEE7Only() throws Exception {
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpTargetApp(testSAMLServer.getHttpsString() + "/samlwsstemplatesclient/CxfWssSAMLTemplatesSvcClient");
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "WSSTemplatesService2", "WSSTemplate2", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_2));

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    //@AllowedFFDC(value = { "java.lang.Exception", "java.util.MissingResourceException", "java.lang.ClassNotFoundException", "java.net.MalformedURLException" }) //@AV999
    //6/2021
    @AllowedFFDC(value = { "java.lang.Exception", "java.util.MissingResourceException", "java.lang.ClassNotFoundException", "java.net.MalformedURLException", "java.security.PrivilegedActionException", "java.lang.NoSuchMethodException"
 })
    @Test
    public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSLEE8Only() throws Exception {

    	//3/2021
    	if (testSAMLServer2 == null) {
            //1 server reconfig
    		testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} else {
            //2 servers reconfig
    		testSAMLServer2.reconfigServer("server_2_wsstemplate_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		testSAMLServer.reconfigServer("server_1_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} //End 3/2021
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpTargetApp(testSAMLServer.getHttpsString() + "/samlwsstemplatesclient/CxfWssSAMLTemplatesSvcClient");
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "WSSTemplatesService2", "WSSTemplate2", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_2));

    }
    

    /**
     * TestDescription:
     * Client matches server side policy (transport enabled), but
     * the client doesn't use https
     * Test should fail to accessing the server side service.
     * 
     */
    
    //3/2021 to run with EE7, then the corresponding error message can be expected
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    //Orig:
    //public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSL_httpFromClient() throws Exception {
    public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSL_httpFromClientEE7Only() throws Exception {
        
    	WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpTargetApp(testSAMLServer.getHttpsString() + "/samlwsstemplatesclient/CxfWssSAMLTemplatesSvcClient");
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService2", "WSSTemplate2", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED));
        
    }
    
    //3/2021 to run with EE8, then the corresponding error message can be expected
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void CxfSAMLWSSTemplatesTests_Saml20TokenOverSSL_httpFromClientEE8Only() throws Exception {

    	//3/2021
    	if (testSAMLServer2 == null) {
            //1 server reconfig
    		testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} else {
    	    //2 servers reconfig
    		testSAMLServer2.reconfigServer("server_2_wsstemplate_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		testSAMLServer.reconfigServer("server_1_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} //End 3/2021
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setSpTargetApp(testSAMLServer.getHttpsString() + "/samlwsstemplatesclient/CxfWssSAMLTemplatesSvcClient");
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService2", "WSSTemplate2", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        
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
    
    //3/2021 to run with EE7, then server_2_in_1_AsymSignEnc.xml, server_2_wsstemplate_AsymSignEnc.xml, server_1_AsymSignEnc.xml can be used
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    //Orig:
    //public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml() throws Exception {
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSamlEE7Only() throws Exception {
        
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

    //3/2021 to run with EE8, then server_2_in_1_AsymSignEnc_wss4j.xml, server_2_wsstemplate_AsymSignEnc_wss4j.xml, server_1_AsymSignEnc_wss4j.xml can be used
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception", "java.lang.ClassNotFoundException", "java.net.MalformedURLException" }) //@AV999 TODO we should not see CNFE and MalformedURL
    @Test
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSamlEE8Only() throws Exception {

        if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_wsstemplate_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
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
    
    //3/2021 to run with EE7, then the corresponding error message can be expected and 
    //server_2_in_1_AsymSignEnc.xml, server_2_wsstemplate_AsymSignEnc.xml, server_1_AsymSignEnc.xml can be used
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitInitiatorToken() throws Exception {
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitInitiatorTokenEE7Only() throws Exception {
    	
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

        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);
        
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        
    }
    
    
    //3/2021 to run with EE8, then the corresponding error message can be expected and 
    //server_2_in_1_AsymSignEnc_wss4j.xml, server_2_wsstemplate_AsymSignEnc_wss4j.xml, server_1_AsymSignEnc_wss4j.xml can be used
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception", "java.net.MalformedURLException" })
    //6/2021 comment out the test temporarily; see https://github.com/OpenLiberty/open-liberty/issues/17588
    //@Test 
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitInitiatorTokenEE8Only() throws Exception {

        if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_wsstemplate_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
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

        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        //3/2021 @AV999     
        if ("externalPolicy".equals(policyType)) { //@AV999 TODO in jaxws-2.3 , this is a successful scenario
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_4)); //@AV999
        } else {
            String CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR = "These policy alternatives can not be satisfied:"; //@AV999, we need to use different error message depending on runtime
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR)); //@AV999
        } //End 3/2021    
    
    }

    /**
     * TestDescription:
     * This test uses a policy with Asymmetric X509 Policy with
     * Mutual Authentication and SAML.
     * Client uses a policy that omits the Recipient Token from the policy
     * Test should fail to access the server side service.
     */
    
    //3/2021 to run with EE7, then server_2_in_1_AsymSignEnc.xml, server_2_wsstemplate_AsymSignEnc.xml, server_1_AsymSignEnc.xml can be used
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitRecipientToken() throws Exception {
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitRecipientTokenEE7Only() throws Exception {
    	
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

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
               
    }
    
    
    //3/2021 to run with EE8, then server_2_in_1_AsymSignEnc_wss4j.xml, server_2_wsstemplate_AsymSignEnc_wss4j.xml, server_1_AsymSignEnc_wss4j.xml can be used
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception", "java.net.MalformedURLException" })
    //6/2021 comment out the test temporarily; see https://github.com/OpenLiberty/open-liberty/issues/17588
    //@Test
    public void CxfSAMLWSSTemplatesTests_AsymmetricX509MutualAuthenticationWithSaml_omitRecipientTokenEE8Only() throws Exception {

        if (testSAMLServer2 == null) {
            // 1 server reconfig
            testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
        } else {
            // 2 server reconfig
            testSAMLServer2.reconfigServer(buildSPServerName("server_2_wsstemplate_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
            testSAMLServer.reconfigServer(buildSPServerName("server_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
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

        //3/2021
        if ("externalPolicy".equals(policyType)) { //@AV999 TODO in jaxws-2.3 , this is a successful scenario
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_4)); //@AV999
        } else {
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        } //End 3/2021
        
    }
    
    
    /**
     * TestDescription:
     * This test uses a policy with Symmetric X509 Message Policy and SAML.
     * Client and Server use the same policy
     * Test should succeed in accessing the server side service.
     */
   
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    //Orig:
    //public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClient() throws Exception {
    public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClientEE7Only() throws Exception {
        
    	WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService6", "WSSTemplate6", "", "False", null, null);
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_WSS_TEMPLATE_SERVICE_6));

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception", "java.net.MalformedURLException" }) //@AV999 TODO
    @Test
    public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClientEE8Only() throws Exception {

    	//3/2021
    	if (testSAMLServer2 == null) {
            //1 server reconfig
    		testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} else {
            //2 servers reconfig
    		testSAMLServer2.reconfigServer("server_2_wsstemplate_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		testSAMLServer.reconfigServer("server_1_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} //End 3/2021
    	
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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @ExpectedFFDC(value = { "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClient_omitProtectionPolicy() throws Exception {
    public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClient_omitProtectionPolicyEE7Only() throws Exception {
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService6", "WSSTemplate6", "", "False", null, commonUtils.processClientWsdl("ClientSymOmitProtectionPolicy.wsdl", servicePort));
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
    }
    
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception", "java.net.MalformedURLException" }) //@AV999
    //6/2021 comment out the test temporarily; see https://github.com/OpenLiberty/open-liberty/issues/17588
    //@Test
    public void CxfSAMLWSSTemplatesTests_X509SymmetricForMessageAndSamlForClient_omitProtectionPolicyEE8Only() throws Exception {

    	//3/2021
    	if (testSAMLServer2 == null) {
            //1 server reconfig
    		testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} else {
            //2 servers reconfig
    		testSAMLServer2.reconfigServer("server_2_wsstemplate_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		testSAMLServer.reconfigServer("server_1_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} //End 3/2021
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "WSSTemplatesService6", "WSSTemplate6", "", "False", null, commonUtils.processClientWsdl("ClientSymOmitProtectionPolicy.wsdl", servicePort));
        updatedTestSettings.getCXFSettings().setTitleToCheck(SAMLConstants.CXF_SAML_TOKEN_WSS_SERVLET);

        //3/2021 Orig:
        //genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        
        //3/2021
        if ("externalPolicy".equals(policyType)) { //@AV999 , jaxws-2.3 this is failing at the client side
            String CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR = "javax.xml.ws.soap.SOAPFaultException: javax.xml.crypto.dsig.TransformException: org.apache.wss4j.common.ext.WSSecurityException: Referenced Token ";
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        } else {
            genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));
        } //End 3/2021
        
    }
    

}
