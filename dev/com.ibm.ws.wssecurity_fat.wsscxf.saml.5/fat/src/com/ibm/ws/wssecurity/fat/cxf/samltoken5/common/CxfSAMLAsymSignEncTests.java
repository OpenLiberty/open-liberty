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

package com.ibm.ws.wssecurity.fat.cxf.samltoken5.common;


import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.SkipForRepeat;
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
public class CxfSAMLAsymSignEncTests extends SAMLCommonTest {

    private static final Class<?> thisClass = CxfSAMLAsymSignEncTests.class;
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static CXFSAMLCommonUtils commonUtils = new CXFSAMLCommonUtils();
    
    //issue 23060
    protected static String ehcacheVersion = "";
    public static String getEhcacheVersion() {
        return ehcacheVersion;
    }
    public static void setEhcacheVersion(String version) {
        ehcacheVersion = version;
    } 
    
    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Service client passes a flag with a value of null to indicate that a
     * user/password should be included in the service context. This is NOT
     * needed or used as we've specified SAML in the policy. Unlike using
     * usernametoken which doesn't require it either, in this case, it won't
     * even be looked at.
     * Test should succeed in accessing the server side service.
     * 
     */
    
    @Test
    public void testSAMLCXFSignedSupportingTokens_Asymmmetric() throws Exception {

    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    		    testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } 
    	}   
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsymSignService",
                "SAMLAsymSignPort", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_ASYM_SIGN_SERVICE));

    }
    
    /*
     * In the test, the server side policy requires that the request be signed.
     * The client uses a policy that does not specify that the request be signed
     * The test should fail since the policy can not be satisfied
     */
 
    @Test
    public void testSAMLCXFSignedSupportingTokens_Asymmmetric_ClientNotSigned() throws Exception {

    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    		    testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    }
    	}    
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsymSignService",
                "SAMLAsymSignPort", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitSign.wsdl", servicePort));
        
        //issue 23060 both EE7 old/new format ehcache return with same error expectation with new jaxws-2.2
    	genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_ASYM_SIGN_SERVICE));
    }
    
    @Test
    public void testSAMLCXFEncryptedSupportingTokens_Asymmmetric() throws Exception {
    
    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    		    testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    }
    	}   
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsymEncrService",
                "SAMLAsymEncrPort", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_ASYM_ENCR_SERVICE));

    }
    
    /*
     * In the test, the server side policy requires that the request be encrypted.
     * The client uses a policy that does not specify that the request be encrypted
     * The test should fail since the policy can not be satisfied
     */

    @Test
    public void testSAMLCXFEncryptedSupportingTokens_Asymmmetric_ClientNotEncrypted() throws Exception {

    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    	        testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    }
    	}  
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsymEncrService",
                "SAMLAsymEncrPort", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitEncr.wsdl", servicePort));

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_ENCR_SERVICE_CLIENT_NOT_ENCR));

    }
    
    @Test
    public void testSAMLCXFSignedEncryptedSupportingTokens_Asymmmetric() throws Exception {
    	
    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    	        testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    }
    	}   
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsymSignEncrService",
                "SAMLAsymSignEncrPort", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_ASYM_SIGN_ENCR_SERVICE));

    }
    
    /*
     * In the test, the server side policy requires that the request be encrypted.
     * The client uses a policy that does not specify that the request be encrypted
     * The test should fail since the policy can not be satisfied
     */
    
    @Test
    public void testSAMLCXFSignedEncryptedSupportingTokens_Asymmmetric_ClientNotEncrypted() throws Exception {

    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    		    testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    }
    	} 
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsymSignEncrService",
                "SAMLAsymSignEncrPort", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitEncrKeepSign.wsdl", servicePort));

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SYM_SIGN_ENCR_SERVICE_CLIENT_NOT_SIGN_OR_ENCR));

    }
    
    /*
     * In the test, the server side policy requires that the request be encrypted.
     * The client uses a policy that does not specify that the request be encrypted
     * The test should fail since the policy can not be satisfied
     */
    
    //@AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testSAMLCXFSignedEncryptedSupportingTokens_Asymmmetric_ClientNotSigned() throws Exception {

    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    	        testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    }
    	} 
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsymSignEncrService",
                "SAMLAsymSignEncrPort", "", "False", null, commonUtils.processClientWsdl("ClientAsymOmitSignKeepEncr.wsdl", servicePort));
        
        //issue 23060 both EE7 old/new ehcache return with same error expectation with new jaxws-2.2
    	genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_ASYM_SIGN_ENCR_SERVICE));
    }
    
    @Test
    public void testSAMLCXFSignedEncryptedAsyncSupportingTokens_Asymmmetric() throws Exception {

    	//issue 23060
    	if ("EE7NEWEhcache".equals(getEhcacheVersion())) {
    	    if (testSAMLServer2 == null) {
                //1 server reconfig
    		    testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_AsymSignEnc_wss4j.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    } else {
    		    //2 servers reconfig
    		    testSAMLServer2.reconfigServer("server_2_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		    testSAMLServer.reconfigServer("server_1_AsymSignEnc_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	    }
    	}   
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, null, null, "SAMLAsyncX509Service",
                "SAMLAsyncX509Port", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_ASYNC_ENCR_SERVICE));

    }

}
