/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken4.TwoServerTests;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.util.ArrayList;
import java.util.List;
//issue 18363
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.wssecurity.fat.cxf.samltoken4.common.CxfSAMLCallerTests;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyServerWrapper;


/**
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

@SkipForRepeat({ EE9_FEATURES })
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CxfSAMLCaller2ServerTests extends CxfSAMLCallerTests {

    private static final Class<?> thisClass = CxfSAMLCaller2ServerTests.class;

    String defaultIDPRealm = "testuser";
    String defaultIDPIdentifierRealm = "test_realmIdentifier";
    String defaultLocalRealm = "BasicRealm";
    String defaultServerCfgRealm = "saml.test";

    String defaultLocalUser = "testuser";
    String defaultIDPUser = "testuser";
    String defaultIDPIdentifierUser = "test_userIdentifier";
    String defaultIDPUniqueIdentifierUser = "test_userUniqueIdentifer";

    String defaultIDPGroup = "testuser";
    String defaultIDPIdentifierGroup = "test_groupIdentifier";
    String defaultLocalGroups = "group:" + defaultLocalRealm + "/" + defaultIDPIdentifierGroup;
    String defaultIDPGroups = "group:" + defaultIDPIdentifierRealm + "/" + defaultIDPIdentifierGroup;
    String defaultServerCfgGroups = "group:" + defaultServerCfgRealm + "/" + defaultIDPIdentifierGroup;
    String defaultNullGroup = "null";

    String default2ServerServiceName = "FatSamlC02aService";
    String default2ServerServicePort = "SamlCallerToken02a";
    String default2ServerPolicy = "CallerHttpsPolicy";
    String default2ServerServiceClientTitle = "CXF SAML Caller Service Client";;

    String badValueString = "SomeBadValue";

    String userIdentifierString = "userIdentifier";
    String userUniqueIdentifierString = "userUniqueIdentifier";
    String groupIdentifierString = "groupIdentifier";
    String realmIdentifierString = "realmIdentifier";

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        userName = "testuser";
        userPass = "testuserpwd";

        flowType = SAMLConstants.SOLICITED_SP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        List<String> extraMsgs2 = new ArrayList<String>();
        extraMsgs.add("CWWKT0016I.*samlcallerclient.*");
        extraMsgs.add("CWWKS5000I");
        extraMsgs.add("CWWKS5002I");
        extraMsgs2.add("CWWKT0016I.*samlcallertoken.*");

        List<String> extraApps = new ArrayList<String>();
        List<String> extraApps2 = new ArrayList<String>();
        extraApps.add("samlcallerclient");
        extraApps2.add("samlcallertoken");

        copyMetaData = false;
        testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.IDP_SERVER_TYPE, null, null, SAMLConstants.SKIP_CHECK_FOR_SECURITY_STARTED);
        copyMetaData = true;
        testSAMLServer2 = commonSetUp("com.ibm.ws.wssecurity_fat.saml.2servers", "server_2_caller.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_SERVER_TYPE, extraApps2, extraMsgs2, SAMLConstants.EXAMPLE_CALLBACK, SAMLConstants.EXAMPLE_CALLBACK_FEATURE);
        copyMetaData = true;
        testSAMLServer = commonSetUp("com.ibm.ws.wssecurity_fat.saml.caller", "server_1.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_SERVER_TYPE, extraApps, extraMsgs);
        
        // now, we need to update the IDP files
        shibbolethHelpers.fixSPInfoInShibbolethServer(testSAMLServer, testIDPServer);
        shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
        // now, start the shibboleth app with the updated config info
        startShibbolethApp(testIDPServer);

        testSAMLServer.getServer().copyFileToLibertyInstallRoot("lib/features", "internalFeatures/securitylibertyinternals-1.0.mf");
        helpers.setSAMLServer(testSAMLServer);

        commonUtils.fixServer2Ports(testSAMLServer2);

        servicePort = Integer.toString(testSAMLServer2.getServerHttpPort());
        serviceSecurePort = Integer.toString(testSAMLServer2.getServerHttpsPort());

        setActionsForFlowType(flowType);
        testSettings.setIdpUserName(userName);
        testSettings.setIdpUserPwd(userPass);
        testSettings.setSpTargetApp(testSAMLServer.getHttpString() + "/samlcallerclient/CxfSamlCallerSvcClient");
        testSettings.setSamlTokenValidationData(testSettings.getIdpUserName(), testSettings.getSamlTokenValidationData().getIssuer(), testSettings.getSamlTokenValidationData().getInResponseTo(), testSettings.getSamlTokenValidationData().getMessageID(), testSettings.getSamlTokenValidationData().getEncryptionKeyUser(), testSettings.getSamlTokenValidationData().getRecipient(), SAMLConstants.AES256);

        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES, SAMLMessageConstants.CWWKF0001E_FEATURE_MISSING);
        testSAMLServer2.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS3107W_GROUP_USER_MISMATCH, SAMLMessageConstants.CWWKW0232E_CANNOT_CREATE_SUBJECT_FOR_USER, SAMLMessageConstants.CWWKW0210E_CANNOT_CREATE_SUBJECT, SAMLMessageConstants.CWWKW0228E_SAML_ASSERTION_MISSING, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES, SAMLMessageConstants.CWWKF0001E_FEATURE_MISSING);
        
        //issue 18363
        setFeatureVersion("EE7");

    }

    /**
     * Build expecations to validate identity tests
     *
     * @param settings
     *            - test settings
     * @param realmName
     *            - the realm name to validate
     * @param user
     *            - user/princ to validate
     * @param group
     *            - group to validate
     * @return - a list of expectations
     * @throws Exception
     */
    public List<validationData> buildExpectationsWithIdentityInfo(SAMLTestSettings settings, String realmName, String user, String group) throws Exception {

        String searchString = ".*realm name:.*" + realmName + ".*PrincipalUserID:.*" + user + ".*Groups:.*" + group + ".*";
        List<validationData> expectations = helpers.setDefaultGoodSAMLCXFExpectations(null, flowType, settings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not receive the expected identity settings.", null, searchString);

        return expectations;
    }

    /**
     * Sets the CXF settings for caller tests
     *
     * @param partToCheck
     *            - what we'll validate in the body of the msg
     * @param testMode
     *            - is this a positive or negative test
     * @return - returns updated SAML test settings
     * @throws Exception
     */
    public SAMLTestSettings setCallerCXFSettings(String partToCheck, String testMode) throws Exception {

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setCXFSettings(_testName, "cxf", servicePort, serviceSecurePort, "testuser", "testuserpwd", default2ServerServiceName, default2ServerServicePort, "", "False", null, null);
        updatedTestSettings.getCXFSettings().setBodyPartToCheck(partToCheck);
        updatedTestSettings.getCXFSettings().setTitleToCheck(default2ServerServiceClientTitle);
        updatedTestSettings.getCXFSettings().setTestMode(testMode);
        return updatedTestSettings;
    }

    /**
     * Performs the common steps of a positive - caller passes in values specific to it's test - all of the "steps" are common to
     * a positive test, so these steps are consolidated here
     *
     * @param serverCfgFile
     *            - name of the server 2 config file to reconfig to
     * @param expectedRealm
     *            - the realm name used by this test
     * @param expectedUser
     *            - the user used by this test
     * @param expectedGroup
     *            - the group used by this test
     * @throws Exception
     */
    public void generalPositiveTest(String serverCfgFile, String expectedRealm, String expectedUser, String expectedGroup) throws Exception {

        testSAMLServer2.reconfigServer(buildSPServerName(serverCfgFile), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        // Create the conversation object which will maintain state for us
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        // Added to fix hostname mismatch to Common Name on the server certificate. This change ignore this check
        // If set to true, the client will accept connections to any host, regardless of whether they have valid certificates or not.
        webClient.getOptions().setUseInsecureSSL(true); 
     
        String partToCheck = "pass:true::FatSamlC02aService";
        String testMode = "positive";

        SAMLTestSettings updatedTestSettings = setCallerCXFSettings(partToCheck, testMode);

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, buildExpectationsWithIdentityInfo(updatedTestSettings, expectedRealm, expectedUser, expectedGroup));

    }

    /**
     * Performs the common steps of a not found exception type test - caller passes in values specific to it's test - all of the
     * steps are common to a not found exception tests, they these steps are consolidated here
     *
     * @param serverCfgFile
     *            - name of the server 2 config file to reconfig to
     * @throws Exception
     */
    public void notfoundExceptionTest(String serverCfgFile) throws Exception {

        testSAMLServer2.reconfigServer(buildSPServerName(serverCfgFile), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        // Create the conversation object which will maintain state for us
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        
        // Added to fix hostname mismatch to Common Name on the server certificate. This change ignore this check
        // If set to true, the client will accept connections to any host, regardless of whether they have valid certificates or not.
        webClient.getOptions().setUseInsecureSSL(true); 

        String partToCheck = ".*pass:false::FatSamlC02aService.*SOAPFaultException.*security token could not be authenticated or authorized.*";
        
        String testMode = "negative";

        SAMLTestSettings updatedTestSettings = setCallerCXFSettings(partToCheck, testMode);

        List<validationData> expectations = helpers.setErrorSAMLCXFExpectationsMatches(null, flowType, updatedTestSettings, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive the proper failure.", null, "SOAPFaultException");

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }
    
    public void notfoundExceptionTestEE8(String serverCfgFile) throws Exception {

        testSAMLServer2.reconfigServer(buildSPServerName(serverCfgFile), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        // Create the conversation object which will maintain state for us
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        
        // Added to fix hostname mismatch to Common Name on the server certificate. This change ignore this check
        // If set to true, the client will accept connections to any host, regardless of whether they have valid certificates or not.
        webClient.getOptions().setUseInsecureSSL(true); 

        String partToCheck = ".*pass:false::FatSamlC02aService.*SOAPFaultException.*An error happened processing a SAML Token: \"invalid user ID\"*";//@AV999 TODO set the message depending on the runtime
        
        String testMode = "negative";

        SAMLTestSettings updatedTestSettings = setCallerCXFSettings(partToCheck, testMode);

        List<validationData> expectations = helpers.setErrorSAMLCXFExpectationsMatches(null, flowType, updatedTestSettings, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive the proper failure.", null, "SOAPFaultException");

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    /**
     * Performs the common steps of a bad attribute type test - caller passes in values specific to it's test - all of the steps
     * are common to a bad attribute exception tests, they these steps are consolidated here
     *
     * @param serverCfgFile
     *            - name of the server 2 config file to reconfig to
     * @param attrName
     *            - the attribute name that is bad (and will be checked for in the logged message)
     * @throws Exception
     */
    public void badAttrValueTest(String serverCfgFile, String attrName) throws Exception {

        testSAMLServer2.reconfigServer(buildSPServerName(serverCfgFile), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        // Create the conversation object which will maintain state for us
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        
        // Added to fix hostname mismatch to Common Name on the server certificate. This change ignore this check
        // If set to true, the client will accept connections to any host, regardless of whether they have valid certificates or not.
        webClient.getOptions().setUseInsecureSSL(true); 

        String partToCheck = ".*pass:false::FatSamlC02aService.*CWWKW0228E.*\\[" + badValueString + "\\].*\\[" + attrName + "\\].*";
        String testMode = "negative";

        SAMLTestSettings updatedTestSettings = setCallerCXFSettings(partToCheck, testMode);

        List<validationData> expectations = helpers.setErrorSAMLCXFExpectationsMatches(null, flowType, updatedTestSettings, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive the proper failure.", null, "CWWKW0228E");

        genericSAML(_testName, webClient, updatedTestSettings, standardFlow, expectations);

    }

    //This is the start test for saml.4

    
    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = True
     * userIdentifier = valid value
     * userUniqueIdentifier = BAD value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userUniqueIdentifier in the message
     */
   
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad.xml", userUniqueIdentifierString);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad_ee8.xml", userUniqueIdentifierString);
    	} //End of 18363
    	
    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = BAD value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userUniqueIdentifier in the message
     */
    
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBad.xml", userUniqueIdentifierString);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBad_ee8.xml", userUniqueIdentifierString);
    	}//End of 18363
    	
    }
  
    /******* mapToUserRegistry = User *******/
    /********** identifiers are Good **********/
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local user's group
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersGood() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersGood.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersGood_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);
    	}//End of 18363
    	
    }
  
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * EntryNotFoundException expectd
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersGood() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersGood.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersGood_ee8.xml");
    	} //End of 18363
    	
    }

    /********** identifiers are Omitted **********/
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = omitted
     * userUniqueIdentifier = omitted
     * groupIdentfier = omitted
     * realmIdentifier = omitted
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: testuser
     * Group: Local user's group
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersOmitted.xml", defaultLocalRealm, defaultIDPUser, defaultNullGroup);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersOmitted_ee8.xml", defaultLocalRealm, defaultIDPUser, defaultNullGroup);
    	}//End of 18363
    	
    }
  
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = Omitted
     * userUniqueIdentifier = Omitted
     * groupIdentfier = Omitted
     * realmIdentifier = Omitted
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersOmitted.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersOmitted_ee8.xml");
    	}//End of 18363
    	
    }
   
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = Omitted
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local server's group
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	}// End of 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = Omitted
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted_ee8.xml");
    	}//End of 18363
    	
    }
 
    
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = Omitted
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local server's group
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	}//End of 18363
    	
    }
  
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = Omitted
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID }) 
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID, EE8FeatureReplacementAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierOmitted.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted_ee8.xml");
    	}//End of 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = Omitted
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: testuser
     * Group: Local Server's group
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierOmitted.xml", defaultLocalRealm, defaultLocalUser, defaultLocalGroups);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultLocalUser, defaultLocalGroups);
    	}//End of 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = Omitted
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierOmitted() throws Exception {
       
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierOmitted.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierOmitted_ee8.xml");
    	}//End of 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = Omitted
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local Server's group
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmitted() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	}//End of 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = Omitted
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmitted() throws Exception {
       
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmitted.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmitted_ee8.xml");
    	}//End of 18363
    	
    }
 
    /********** identifiers are Bad **********/
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = BAD value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local Server groups
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierBad() throws Exception {
       
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierBad.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierBad_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);
    	}//End of 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = BAD value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID }) 
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierBad.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierBad_ee8.xml");
    	}//End of 18363
    	
    }

    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local Server groups
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierBad.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierBad_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);
    	}//End of 18363
    	
    }
    
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = BAD value
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID }) 
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierBad.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierBad_ee8.xml");
    	}//End of 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = BAD value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userIdentifier in the message
     */
    
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    badAttrValueTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierBad.xml", userIdentifierString);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		badAttrValueTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierBad_ee8.xml", userIdentifierString);
    	}//End of 18363
    	
    }

    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = BAD value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userIdentifier in the message
     */
   
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    badAttrValueTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierBad.xml", userIdentifierString);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		badAttrValueTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierBad_ee8.xml", userIdentifierString);
    	}//End of 18363
    	
    }

    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = BAD value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local Server groups
     */
    
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);
    	} else if ("EE8".equals(getFeatureVersion())) {
    		generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);
    	}//End f 18363
    	
    }
 
    /*
     * config settings:
     * mapToUserRegistry = User
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = BAD value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * EntryNotFoundException expected
     */
    
    @ExpectedFFDC(value = { "org.apache.wss4j.common.ext.WSSecurityException" }, repeatAction = { EmptyAction.ID })
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }, repeatAction = { EmptyAction.ID })
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBad() throws Exception {
        
    	//issue 18363
    	if ("EE7".equals(getFeatureVersion())) {
    	    notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBad.xml");
    	} else if ("EE8".equals(getFeatureVersion())) {
    		notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBad_ee8.xml");
    	}//End of 18363
    	
    }

}
