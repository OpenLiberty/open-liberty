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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml.fat.jaxrs.common.RSSamlBasicTests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;

import componenttest.annotation.ExpectedFFDC;
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
public class RSSamlIDPInitiatedBasic1ServerTests extends RSSamlBasicTests {

    private static final Class<?> thisClass = RSSamlIDPInitiatedBasic1ServerTests.class;
    static String order1, order2, order1a, order2a = null;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.IDP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);
        extraApps.add("helloworld");

        startSPWithIDPServer(MergedServerName, "server_1.xml", SAMLConstants.SAML_APP_SERVER_TYPE, extraMsgs, extraApps, true);
        testAppServer = testSAMLServer;

        //commonSetUp(SPServerName, "server_1.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.SAML_SERVER_TYPE, extraApps, extraMsgs);
        //        setActionsForFlowType(flowType);
        // Allow the warning on the ignored attributes of samlWebSso20 inboundPropagation true or false
        testAppServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        testSAMLServer.addIgnoredServerExceptions(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES, SAMLMessageConstants.CWWKS5079E_CANNOT_FIND_IDP_URL_IN_METATDATA, SAMLMessageConstants.CWWKS5077E_CANNOT_SELECT_SAML_PROVIDER);

        // set default values for jaxrs settings
        testSettings.setRSSettings();

        // set test app
        testSettings.setSpTargetApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.setSpDefaultApp(testAppServer.getServerHttpsString() + "/" + SAMLConstants.PARTIAL_HELLO_WORLD_URI);
        testSettings.updatePartnerInSettings("sp1", true);

        order1 = cttools.chooseRandomEntry(new String[] { "server_1_bothUseSameFilter1.xml", "server_1_bothUseSameFilter2.xml" });

        if (order1.equals("server_1_bothUseSameFilter1.xml")) {
            order1a = "server_1_bothUseSameFilter1_disableDefaultCfgs.xml";
            order2 = "server_1_bothUseSameFilter2.xml";
            order2a = "server_1_bothUseSameFilter2_disableDefaultCfgs.xml";
        } else {
            order1a = "server_1_bothUseSameFilter2_disableDefaultCfgs.xml";
            order2 = "server_1_bothUseSameFilter1.xml";
            order2a = "server_1_bothUseSameFilter1_disableDefaultCfgs.xml";
        }

    }

    public List<validationData> addConfusedExpectation(String action, List<validationData> expectations) throws Exception {

        // once 207838 is fixed, we should be able to add the normal fobidden expectations, plus a check for the
        // server is confused message
        //207838	List<validationData> expectations = msgUtils.addForbiddenExpectation(actions, null) ;

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
            //    		expectations = vData.addSuccessStatusCodes(null, action);
            //    		expectations = vData.addResponseStatusExpectation(expectations, action, Constants.FORBIDDEN_STATUS)  ;
        }

        expectations = helpers.addMessageExpectation(testAppServer, expectations, action, SAMLConstants.APP_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail because we couldn't decide which config to use.", SAMLMessageConstants.CWWKS5077E_CANNOT_SELECT_SAML_PROVIDER);

        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.HTTP_ERROR_FORBIDDEN);
        //expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.HTTP_ERROR_MESSAGE) ;
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.OK_MESSAGE);
        //        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.FORBIDDEN) ;
        return expectations;
    }

    /**
     * The server config contains the SAML WebSSO config and RS Saml config
     * We do NOT have a cfg for defaultSP or a default cfg for RS Saml
     * Since we have specific filters for each config and we're using apps that only match
     * one of the filters - we're good
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    //	@AllowedFFDC("java.lang.NoClassDefFoundError")
    public void RSSamlIDPInitiatedBasic1ServerTests_noDefaultCfgs() throws Exception {

        testAppServer.reconfigServer(buildSPServerName("server_1_noDefaultCfgs.xml"), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        genericSAML(_testName, webClient, testSettings, throughJAXRSGet, expectations);

    }

    /**
     * The server config contains the SAML WebSSO config and RS Saml config
     * We do have a cfg for defaultSP or a default cfg for RS Saml
     * The filters for each cfg are the same. We should be able to get a
     * SAML Token, but when we try to use that with the RS Saml cfg, we'll
     * pick up the SAML WebSSO cfg instead
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlIDPInitiatedBasic1ServerTests_bothCfgsUseTheSameFilter_defaultCfgsEnabled() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(order1), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient1 = SAMLCommonTestHelpers.getWebClient();

        List<validationData> expectations = addConfusedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient1, testSettings, throughJAXRSGet, expectations);

        testAppServer.reconfigServer(buildSPServerName(order2), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient2, testSettings, throughJAXRSGet, expectations);

    }

    /**
     * The server config contains the SAML WebSSO config and RS Saml config
     * We do NOT have a cfg for defaultSP or a default cfg for RS Saml
     * The filters for each cfg are the same. We should be able to get a
     * SAML Token, but when we try to use that with the RS Saml cfg, we'll
     * pick up the SAML WebSSO cfg instead
     *
     * @throws Exception
     */
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Mode(TestMode.LITE)
    @Test
    public void RSSamlIDPInitiatedBasic1ServerTests_bothCfgsUseTheSameFilter_defaultCfgsDisabled() throws Exception {

        testAppServer.reconfigServer(buildSPServerName(order1a), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient1 = SAMLCommonTestHelpers.getWebClient();

        List<validationData> expectations = addConfusedExpectation(SAMLConstants.INVOKE_JAXRS_GET, null);

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient1, testSettings, throughJAXRSGet, expectations);

        testAppServer.reconfigServer(buildSPServerName(order2a), _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();

        // make sure we get the error page indicating that we're not authorized
        genericSAML(_testName, webClient2, testSettings, throughJAXRSGet, expectations);

    }

    /** we have a test that uses different pkixTrustEngines for SAML Web SSO and RS Saml **/
    /**
     * also have tests with different key/trust in the common test class, this extending
     * class will just run them with only one server
     */

}
