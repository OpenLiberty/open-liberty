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

package com.ibm.ws.wssecurity.fat.cxf.samltoken2.TwoServerTests;

import java.util.ArrayList;
import java.util.List;
//3/2021
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
//orig from CL: import com.ibm.ws.security.fat.common.tooling.ValidationData.validationData;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.wssecurity.fat.cxf.samltoken2.common.CxfSAMLCallerTests;

//3/2021
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import componenttest.annotation.SkipForRepeat;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.HttpUtils;

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
@LibertyServerWrapper
@Mode(TestMode.FULL)
//1/26/2021 added
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
        
        //1/26/2021
        //HttpUtils.enableSSLv3();

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

        //Orig: 3/2021 In EE7/EE8 dual test scenario, without adding the ignoring of CWWKF0001E, EE7 test resulted to additional error count
        //testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES);
        //testSAMLServer2.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS3107W_GROUP_USER_MISMATCH, SAMLMessageConstants.CWWKW0232E_CANNOT_CREATE_SUBJECT_FOR_USER, SAMLMessageConstants.CWWKW0210E_CANNOT_CREATE_SUBJECT, SAMLMessageConstants.CWWKW0228E_SAML_ASSERTION_MISSING, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES);
        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES, SAMLMessageConstants.CWWKF0001E_FEATURE_MISSING);
        testSAMLServer2.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS3107W_GROUP_USER_MISMATCH, SAMLMessageConstants.CWWKW0232E_CANNOT_CREATE_SUBJECT_FOR_USER, SAMLMessageConstants.CWWKW0210E_CANNOT_CREATE_SUBJECT, SAMLMessageConstants.CWWKW0228E_SAML_ASSERTION_MISSING, SAMLMessageConstants.CWWKG0101W_CONFIG_NOT_VISIBLE_TO_OTHER_BUNDLES, SAMLMessageConstants.CWWKF0001E_FEATURE_MISSING);
        
        //3/2021 update the config for EE8 test
        ServerConfiguration config = testSAMLServer.getServer().getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        if (features.contains("usr:wsseccbh-2.0")) {
        	testSAMLServer2.getServer().copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbhwss4j.jar");
        	testSAMLServer2.getServer().copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-2.0.mf");
        } //End 3/2021
        
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

        //Orig:
        String partToCheck = ".*pass:false::FatSamlC02aService.*SOAPFaultException.*security token could not be authenticated or authorized.*";
        //3/2021
        //String partToCheck = ".*pass:false::FatSamlC02aService.*SOAPFaultException.*An error happened processing a SAML Token: \"invalid user ID\"*";//@AV999 TODO set the message depending on the runtime
        
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

        //Orig:
        //String partToCheck = ".*pass:false::FatSamlC02aService.*SOAPFaultException.*security token could not be authenticated or authorized.*";
        //3/2021
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

    /***************************************** TESTS **************************************/
    /*
     * This test verifies the realm, user and group that are in the subject. The values
     * are set based on several configuration options.
     * These tests will modify the configuration and then check for the appropriate values in the subject.
     */

    /******* mapToUserRegistry = Group *******/
    /********** identifiers are Good **********/

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: An IPD group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_identifiersGoodEE7Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_identifiersGood.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.lang.ClassNotFoundException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_identifiersGoodEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_identifiersGood_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Default IDP server's realm
     * User: test_userIdentifier
     * Group: Default IDP Group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_identifiersGood() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_identifiersGood.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPIdentifierGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.lang.ClassNotFoundException"}) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_identifiersGoodEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_identifiersGood_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPIdentifierGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is Group1 in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Default IDP server's realm
     * User: test_userIdentifier
     * Group: Default IDP Group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_groupNotInRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_groupNotInRegistry_identifiersGoodEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_groupNotInRegistry_identifiersGood.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_groupNotInRegistry_identifiersGoodEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_groupNotInRegistry_identifiersGood_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is Group1 in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Default IDP server's realm
     * User: test_userIdentifier
     * Group: Default IDP Group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_group2InRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_group2InRegistry_identifiersGoodEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_group2InRegistry_identifiersGood.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_group2InRegistry_identifiersGoodEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_group2InRegistry_identifiersGood_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /********** identifiers are Omitted **********/
    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = omitted
     * userUniqueIdentifier = omitted
     * groupIdentfier = omitted
     * realmIdentifier = omitted
     *
     * Expected/checked values:
     * Realm: Local server's configured realm
     * User: testuser
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_identifiersOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_identifiersOmittedEE7Only() throws Exception {
        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_identifiersOmitted.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.lang.ClassNotFoundException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_identifiersOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_identifiersOmitted_ee8.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = Omitted
     * userUniqueIdentifier = Omitted
     * groupIdentfier = Omitted
     * realmIdentifier = Omitted
     *
     * Expected/checked values:
     * Realm: Configured server's realm
     * User: testuser
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_identifiersOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_identifiersOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_identifiersOmitted.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_identifiersOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_identifiersOmitted_ee8.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = omitted
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test 
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_groupIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_groupIdentifierOmittedEE7Only() throws Exception {
    	
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_groupIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_groupIdentifierOmittedEE8Only() throws Exception {
        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_groupIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = Omitted
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_groupIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_groupIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_groupIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_groupIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_groupIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = omitted
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local server's group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_realmIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_realmIdentifierOmittedEE7Only() throws Exception {
       
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_realmIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_realmIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_realmIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = Omitted
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_realmIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_realmIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_realmIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_realmIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_realmIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = omitted
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: testuser
     * Group: An IPD group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = Omitted
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: testuser
     * Group: Default IDP Group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = Omitted
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: An IPD group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = Omitted
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: Default IDP Group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }

    /********** identifiers are Bad **********/
    /*
     * config settings:
     * mapToUserRegistry = Group
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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_groupIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_groupIdentifierBadEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_groupIdentifierBad.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_groupIdentifierBadEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_inRegistry_groupIdentifierBad_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = BAD value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_groupIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_groupIdentifierBadEE7Only() throws Exception {
        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_groupIdentifierBad.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_groupIdentifierBadEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_groupIdentifierBad_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = BAD value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for realmIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_realmIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_realmIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_Group_inRegistry_realmIdentifierBad.xml", realmIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_realmIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_Group_inRegistry_realmIdentifierBad_ee8.xml", realmIdentifierString);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = BAD value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for realmIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_realmIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_realmIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_realmIdentifierBad.xml", realmIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_realmIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_realmIdentifierBad_ee8.xml", realmIdentifierString);

    }


    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = BAD value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userIdentifier in the message
     */
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userIdentifierBad.xml", userIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userIdentifierBad_ee8.xml", userIdentifierString);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = BAD value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userIdentifierBadEE7Only() throws Exception {
       
    	badAttrValueTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userIdentifierBad.xml", userIdentifierString);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    //@AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.lang.ClassNotFoundException" })
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userIdentifierBad_ee8.xml", userIdentifierString);

    }

    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = BAD value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userUniqueIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierBad.xml", userUniqueIdentifierString);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_Group_inRegistry_userUniqueIdentifierBad_ee8.xml", userUniqueIdentifierString);

    }
    
    /*
     * config settings:
     * mapToUserRegistry = Group
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = BAD value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userUniqueIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierBad.xml", userUniqueIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_Group_notInRegistry_userUniqueIdentifierBad_ee8.xml", userUniqueIdentifierString);

    }

    /******* mapToUserRegistry = No *******/
    /********** identifiers are Good **********/
    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: IPD Server groups
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_identifiersGoodEe7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_identifiersGood.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_identifiersGoodEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_identifiersGood_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Default IDP server's realm
     * User: test_userIdentifier
     * Group: Default IDP Groups
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_identifiersGoodEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_identifiersGood.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_identifiersGoodEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_identifiersGood_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }

    /********** identifiers are Omitted **********/
    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = true
     * userIdentifier = omitted
     * userUniqueIdentifier = omitted
     * groupIdentfier = omitted
     * realmIdentifier = omitted
     *
     * Expected/checked values:
     * Realm: Local server's configured realm
     * User: testuser
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_identifiersOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_identifiersOmittedEE7Only() throws Exception {
        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_identifiersOmitted.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_identifiersOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_identifiersOmitted_ee8.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }
    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = Omitted
     * userUniqueIdentifier = Omitted
     * groupIdentfier = Omitted
     * realmIdentifier = Omitted
     *
     * Expected/checked values:
     * Realm: Local server's configured realm
     * User: testuser
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_identifiersOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_identifiersOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_identifiersOmitted.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_identifiersOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_identifiersOmitted_ee8.xml", defaultServerCfgRealm, defaultIDPUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = Omitted
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_groupIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_groupIdentifierOmittedEE7Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_groupIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_groupIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_groupIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = omitted
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_groupIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_groupIdentifierOmittedEE7Only() throws Exception {
        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_groupIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_groupIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_groupIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = realm
     *
     * Expected/checked values:
     * Realm: Local configured server realm realm
     * User: test_userIdentifier
     * Group: Local server's groups
     */
    //3/2021
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_realmIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_realmIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_realmIdentifierOmitted.xml", defaultServerCfgRealm, defaultIDPIdentifierUser, defaultServerCfgGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_realmIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_realmIdentifierOmitted_ee8.xml", defaultServerCfgRealm, defaultIDPIdentifierUser, defaultServerCfgGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = Omitted
     *
     * Expected/checked values:
     * Realm: Local server's configured realm
     * User: test_userIdentifier
     * Group: Hybrid Group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_realmIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_realmIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_realmIdentifierOmitted.xml", defaultServerCfgRealm, defaultIDPIdentifierUser, defaultServerCfgGroups);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_realmIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_realmIdentifierOmitted_ee8.xml", defaultServerCfgRealm, defaultIDPIdentifierUser, defaultServerCfgGroups);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = true
     * userIdentifier = Omitted
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: testuser
     * Group: An IPD group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_userIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_userIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPIdentifierGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_userIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPIdentifierGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = Omitted
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: testuser
     * Group: Default IDP Groups
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPIdentifierGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPUser, defaultIDPIdentifierGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = true
     * userIdentifier = valid value
     * userUniqueIdentifier = Omitted
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: An IPD group
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierOmittedEE7Only() throws Exception {
        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }
    
    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = Omitted
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: IDP server's realm
     * User: test_userIdentifier
     * Group: Default IDP Groups
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierOmitted.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierOmitted_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultIDPGroups);

    }
    
    /********** identifiers are Bad **********/
    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = True
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = BAD value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Local server's realm
     * User: test_userIdentifier
     * Group: Local user's Groups
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_groupIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_groupIdentifierBadEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_groupIdentifierBad.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.lang.ClassNotFoundException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_groupIdentifierBadEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_inRegistry_groupIdentifierBad_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = BAD value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Realm: Default IDP server's realm
     * User: test_userIdentifier
     * Group: null
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_groupIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_groupIdentifierBadEE7Only() throws Exception {
       
    	generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_groupIdentifierBad.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_groupIdentifierBadEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_No_notInRegistry_groupIdentifierBad_ee8.xml", defaultIDPIdentifierRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = True
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = BAD value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for realmIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_realmIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_realmIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_realmIdentifierBad.xml", realmIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_realmIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_realmIdentifierBad_ee8.xml", realmIdentifierString);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = valid value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = BAD value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for realmIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_realmIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_realmIdentifierBadEE7Only() throws Exception {
       
    	badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_realmIdentifierBad.xml", realmIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_realmIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_realmIdentifierBad_ee8.xml", realmIdentifierString);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = True
     * userIdentifier = BAD value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_userIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_userIdentifierBad.xml", userIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_userIdentifierBad_ee8.xml", userIdentifierString);

    }

    /*
     * config settings:
     * mapToUserRegistry = No
     * is User in the Registry = False
     * userIdentifier = BAD value
     * userUniqueIdentifier = valid value
     * groupIdentfier = valid value
     * realmIdentifier = valid value
     *
     * Expected/checked values:
     * Bad Attribute exception - looking for userIdentifier in the message
     */
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userIdentifierBad() throws Exception {	
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userIdentifierBad.xml", userIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userIdentifierBad_ee8.xml", userIdentifierString);

    }

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad.xml", userUniqueIdentifierString);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad_ee8.xml", userUniqueIdentifierString);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBad.xml", userUniqueIdentifierString);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    public void testCxfCaller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_No_notInRegistry_userUniqueIdentifierBad_ee8.xml", userUniqueIdentifierString);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersGoodEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersGood.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersGoodEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersGood_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersGood() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersGoodEE7Only() throws Exception {
        
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersGood.xml");

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" })//@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersGoodEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersGood_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersGood_ee8.xml");
    	
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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersOmitted.xml", defaultLocalRealm, defaultIDPUser, defaultNullGroup);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_identifiersOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_identifiersOmitted_ee8.xml", defaultLocalRealm, defaultIDPUser, defaultNullGroup);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersOmittedEE7Only() throws Exception {
        
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersOmitted.xml");

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" })//@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_identifiersOmittedEE8Only() throws Exception {

       //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersOmitted_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_identifiersOmitted_ee8.xml");
    	
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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmittedEE7Only() throws Exception {
        
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted.xml");

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" })//@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmittedEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted_ee8.xml");
    	
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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierOmittedEE7Only() throws Exception {
        
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierOmitted.xml");

    }
   
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" }) //@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" })//@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierOmittedEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierOmitted_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierOmitted_ee8.xml");

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierOmitted.xml", defaultLocalRealm, defaultLocalUser, defaultLocalGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultLocalUser, defaultLocalGroups);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierOmittedEE7Only() throws Exception {
       
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierOmitted.xml");

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" })//@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierOmittedEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierOmitted_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierOmitted_ee8.xml");
    	
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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmittedEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmitted.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmittedEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmitted_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmitted() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmittedEE7Only() throws Exception {
       
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmitted.xml");

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" })//@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmittedEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmitted_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierOmitted_ee8.xml");

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierBadEE7Only() throws Exception {
       
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierBad.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException", "java.lang.ClassNotFoundException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_groupIdentifierBadEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_groupIdentifierBad_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultNullGroup);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierBadEE7Only() throws Exception {
        
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierBad.xml");

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }) //@AV999 START HERE
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_groupIdentifierBadEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierBad_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_groupIdentifierBad_ee8.xml");
    	
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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierBadEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierBad.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_realmIdentifierBadEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_realmIdentifierBad_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierBadEE7Only() throws Exception {
        
    	notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierBad.xml");

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_realmIdentifierBadEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierBad_ee8.xml");
    	notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_realmIdentifierBad_ee8.xml");

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierBad.xml", userIdentifierString);

    }
    
    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_User_inRegistry_userIdentifierBad_ee8.xml", userIdentifierString);

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
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierBadEE7Only() throws Exception {
        
    	badAttrValueTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierBad.xml", userIdentifierString);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @ExpectedFFDC(value = { "com.ibm.ws.wssecurity.caller.SamlCallerTokenException" })
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userIdentifierBadEE8Only() throws Exception {

        badAttrValueTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userIdentifierBad_ee8.xml", userIdentifierString);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBadEE7Only() throws Exception {
        
    	generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "java.util.MissingResourceException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBadEE8Only() throws Exception {

        generalPositiveTest("server_2_caller_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad_ee8.xml", defaultLocalRealm, defaultIDPIdentifierUser, defaultLocalGroups);

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
    
    //3/2021 to run with EE7
    @SkipForRepeat(SkipForRepeat.EE8_FEATURES)
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException", "org.apache.ws.security.WSSecurityException" })
    @Test
    //Orig:
    //public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBad() throws Exception {
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBadEE7Only() throws Exception {
        notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBad.xml");

    }

    //3/2021 to run with EE8
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "org.apache.wss4j.common.ext.WSSecurityException", "java.util.MissingResourceException" })//@AV999
    @ExpectedFFDC(value = { "com.ibm.ws.security.registry.EntryNotFoundException" }) //@AV999
    @Test
    public void testCxfCaller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBadEE8Only() throws Exception {

        //notfoundExceptionTest("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBad_ee8.xml");
        notfoundExceptionTestEE8("server_2_caller_mapToUserRegistry_User_notInRegistry_userUniqueIdentifierBad_ee8.xml");

    }
    
}
