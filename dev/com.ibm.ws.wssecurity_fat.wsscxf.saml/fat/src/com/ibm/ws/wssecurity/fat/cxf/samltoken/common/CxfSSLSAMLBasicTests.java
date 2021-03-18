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

import componenttest.annotation.SkipForRepeat;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import componenttest.annotation.AllowedFFDC;
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
//1/20/2021 the FULL mode at class level was already in CL FAT and will not work for LITE in OL, instead, we mix the FULL modes in the test case level below
//with the existing LITE mode
//@Mode(TestMode.FULL)
//1/21/2021 added
@RunWith(FATRunner.class)
public class CxfSSLSAMLBasicTests extends SAMLCommonTest {

    private static final Class<?> thisClass = CxfSSLSAMLBasicTests.class;
    protected static String servicePort = null;
    protected static String serviceSecurePort = null;
    protected static CXFSAMLCommonUtils commonUtils = new CXFSAMLCommonUtils();

    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Transport/Https is in the server side policy.
     * The service client uses the server side policy.
     * The service client invokes the request using https.
     * Test should succeed in accessing the server side service.
     * 
     */
    
    //3/2021 to run with EE7
    @Mode(TestMode.LITE)
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    //Orig:
    //public void testSAMLCxfSvcClient_TransportEnabled() throws Exception {	
    public void testSAMLCxfSvcClient_TransportEnabledEE7Only() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SamlTokenTransportSecure",
                "SamlTokenTransportSecurePort", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SSL_SAML_TOKEN_SERVICE));

    }
    
    //3/2021 to run with EE8
    @Mode(TestMode.LITE)
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception",  "java.util.MissingResourceException", "java.lang.ClassNotFoundException"}) //@AV999 TODO: look into CNFE
    @Test
    public void testSAMLCxfSvcClient_TransportEnabledEE8Only() throws Exception {	

    	//3/2021
    	if (testSAMLServer2 == null) {
            //1 server reconfig
    		testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} else {
    	    //2 servers reconfig
    		testSAMLServer2.reconfigServer("server_2_ee8.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		testSAMLServer.reconfigServer("server_1_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} //End 3/2021
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SamlTokenTransportSecure",
                "SamlTokenTransportSecurePort", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SSL_SAML_TOKEN_SERVICE));

    }
    
    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Transport/Https is in the server side policy.
     * The service client uses the server side policy.
     * The service client invokes the request using http only.
     * Test should fail as policy can't be enforced.
     * 
     */
    
    //3/2021 to run with EE7, then the corresponding error message is expected
    //1/20/2021 added FULL mode
    @Mode(TestMode.FULL)
    //Ask Aruna if this is temporary?
    //@Mode(TestMode.LITE)
    //3/2021
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    //3/2021 Orig:
    //public void testSAMLCxfSvcClient_TransportEnabled_httpFromClient() throws Exception {
    public void testSAMLCxfSvcClient_TransportEnabled_httpFromClientEE7Only() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "SamlTokenTransportSecure",
                "SamlTokenTransportSecurePort", "", "False", null, null);

        
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, SAMLConstants.CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED));
       
    }

    //3/2021 to run with EE8, then the corresponding error message is expected
    //1/20/2021 added FULL mode
    @Mode(TestMode.FULL)
    //Ask Aruna if this is temporary?
    //@Mode(TestMode.LITE)
    //3/2021
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    public void testSAMLCxfSvcClient_TransportEnabled_httpFromClientEE8Only() throws Exception {

    	//3/2021
    	if (testSAMLServer2 == null) {
            //1 server reconfig
    		testSAMLServer.reconfigServer(buildSPServerName("server_2_in_1_ee8.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} else {
    	    //2 servers reconfig
    		testSAMLServer2.reconfigServer("server_2_ee8.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    		testSAMLServer.reconfigServer("server_1_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
    	} //End 3/2021
    	
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, null, "user1", "user1pwd", "SamlTokenTransportSecure",
                "SamlTokenTransportSecurePort", "", "False", null, null);

        //3/2021
        //@AV999
        String CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED = "HttpsToken could not be asserted: Not an HTTPs connection"; // @AV999 slightly different error with new runtime
        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setErrorSAMLCXFExpectations(null, flowType, updatedTestSettings, CXF_SAML_TOKEN_SERVICE_HTTPS_NOT_USED));
    }
    
    /**
     * TestDescription:
     * 
     * This test invokes a jax-ws cxf service client, which invokes
     * a jax-ws cxf SAML web service.
     * Transport/Https is NOT in the server side policy.
     * The service client uses the server side policy.
     * The service client invokes the request using https.
     * Test should succeed in accessing the server side service as the client side is
     * "more secure" than the server
     * 
     */
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "java.lang.Exception" })
    @Test
    //Orig:
    //public void testSAMLCxfSvcClient_TransportNotEnabled_httpsFromClient() throws Exception {
    public void testSAMLCxfSvcClient_TransportNotEnabled_httpsFromClientEE7Only() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2",
                "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "java.lang.Exception", "java.util.MissingResourceException" })
    @Test
    public void testSAMLCxfSvcClient_TransportNotEnabled_httpsFromClientEE8Only() throws Exception {

    	//3/2021
    	testSAMLServer2.reconfigServer("server_2_ee8.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
		testSAMLServer.reconfigServer("server_1_wss4j.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);
		
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, null, servicePort, serviceSecurePort, "user1", "user1pwd", "SAMLSOAPService2",
                "SAMLSoapPort2", "", "False", null, null);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, updatedTestSettings));

    }
    
}
