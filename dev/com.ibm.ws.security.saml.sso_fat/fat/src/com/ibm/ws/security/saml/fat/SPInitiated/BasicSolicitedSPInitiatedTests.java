/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.SPInitiated;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml.fat.common.BasicSAMLTests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class were ported from the BasicIDPIniatedTests which
 * were ported from tWAS.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a solicited SP initiated SAML Web SSO, using
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
public class BasicSolicitedSPInitiatedTests extends BasicSAMLTests {

    private static final Class<?> thisClass = BasicSolicitedSPInitiatedTests.class;

    static List<String> extraMsgs = null;

    static List<String> extraApps = new ArrayList<String>();

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.SOLICITED_SP_INITIATED;

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        extraMsgs = getDefaultSAMLStartMsgs();

        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        startSPWithIDPServer("com.ibm.ws.security.saml.sso-2.0_fat", "server_1.xml", extraMsgs, extraApps, true);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
    }

    /**
     * The focus of this test is to make sure that we get the expected failure when the Partner is
     * not part of the federation that the SP server config references.
     * The federation information in the server.xml is used to construct the call to the IDP server.
     * The request that is build will include federation information for a federation that does
     * not contain the partner requested. The IDP will return a failure indicating that the partner was
     * not found.
     */
    //	@Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void basicSolicitedSPInitiatedTests_noIdAssertNoUser_IDPSign_IDPEncrypt_FederationMismatch() throws Exception {

        testSAMLServer.reconfigServer("server_8_FedMismatch.xml", _testName, SAMLConstants.NO_EXTRA_MSGS, SAMLConstants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp5", true);
        updatedTestSettings.setSpecificIDPChallenge(2);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Error from the IDP", null, SAMLConstants.SP_DEFAULT_ERROR_PAGE_TITLE);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Error from the IDP", null, SAMLConstants.HTTP_ERROR_FORBIDDEN);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Error code from the IDP", SAMLMessageConstants.CWWKS5079E_CANNOT_FIND_IDP_URL_IN_METATDATA);

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

    }

    // the server.xml has wantAssertionsSigned=true, so by removing the signature below, this test could fail if
    // they change the order of the checks (InResponseTo is currently checked first).  By keeping the values set
    // to true, we can use the default config and therefore reduce the number of reconfigs (if they change the
    // order, we'll have to change the config to one with wantAssertionsSigned=false)
    //	@Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    public void basicSolicitedSPInitiatedTests_NoInResponseTo() throws Exception {

        // need to allow no signature, since we change the samlResponse
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setRemoveAttribAll("InResponseTo");
        updatedTestSettings.setRemoveTagInResponse("ds:Signature");

        List<validationData> expectations = msgUtils.addForbiddenExpectation(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, null);

        //		List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE);
        //		expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, Constants.FORBIDDEN_STATUS)  ;
        //
        //		expectations = helpers.addMessageExpectation(testSAMLServer,expectations, SAMLConstants.INVOKE_SP_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", null, SAMLMessageConstants.CWWKS5063E_SAML_END_USER_ERROR_MESSAGE);
        expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive exception that the SAML Token did not validate.", SAMLMessageConstants.CWWKS5067E_INVALID_IN_RESPONSE_TO);

        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void basicSolicitedSPInitiatedTests_validateInitialRequestStatusCode() throws Exception {

        String statusCheck = _testName + "_statusCheck";
        String statusMsg = "HTTP/1.1 200 OK";

        // create the test marker used to start checking
        Log.info(thisClass, _testName, statusCheck);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        // turn off automatic redirect
        webClient.getOptions().setRedirectEnabled(false);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // and make sure we got the redirect
        List<validationData> expectations = vData.addSuccessStatusCodes(null, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST);
        expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.REDIRECT_STATUS);

        // only make the first call to snoop and make sure that we got a 200 instead of a 401 (when it is determined that we need to log in)
        genericSAML(_testName, webClient, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP, expectations);

        // I haven't figured out how to get htmlunit to log the intermediate request/responses, so, we won't see the 200 in the flow
        // TODO add back in if we figure out how to make htmlunit log additional steps
        // msgUtils.assertTrueAndLog(_testName, "Did not find status: + " + statusMsg, checkIntermedStatusCode(statusCheck, statusMsg));
    }

    /**
     *
     * This test uses POST with the common test code in sessionAffinityTest - read its description
     *
     * @throws Exception
     */
    @Test
    public void basicSolicitedSPInitiatedTests_sessionAffinity_POSTMethod() throws Exception {

        sessionAffinityTest(HttpMethod.POST);

    }

    /**
     *
     * This test uses GET with the common test code in sessionAffinityTest - read its description
     *
     * @throws Exception
     */
    @Test
    public void basicSolicitedSPInitiatedTests_sessionAffinity_GETMethod() throws Exception {

        sessionAffinityTest(HttpMethod.GET);

    }

    /**
     * This test attempts to mimic a cluster situation where we show that session affinity does not come into play in the
     * SP-Initiated flow.
     * This test will use a basic config (one used by many tests). We request access to our protected app. The SP
     * makes a request to the IDP on our behalf. The IDP responds with a login form. At this point, we restart the SP. We fill
     * in the login form and submit it to the IDP. It then responds with a SAML response. We then invoke the
     * ACS endpoint with the SAMLResponse. We should now have access to the protected app.
     *
     * @throws Exception
     */
    protected void sessionAffinityTest(HttpMethod method) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        // we want to pass some parms on the first request (without having to duplicate code)
        // dummy up values to pass on initial request - they won't really be used for anything
        updatedTestSettings.setRSSettings(SAMLConstants.SAML_DEFAULT_AUTHORIZATION_HEADER_NAME, "DummyHeaderFormat", "DummyTokenFormat");

        // make sure we get to the login page, make sure we get the saml response and finally get to the test app
        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectationsThroughACSOnly(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, testSettings, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);
        helpers.setSimpleServletExpecatations(expectations, testSettings);

        Object somePage = helpers.buildSolicitedSPInitiatedRequest(_testName, webClient, updatedTestSettings, expectations, method);

        logTestCaseInServerSideLog("MID-Test server Stop", testSAMLServer);
        testSAMLServer.restartServer("server_1.xml", _testName, extraApps, extraMsgs, true);
        logTestCaseInServerSideLog("MID-Test server Start", testSAMLServer);

        somePage = helpers.performIDPLogin(_testName, webClient, (HtmlPage) somePage, updatedTestSettings, expectations);

        helpers.invokeACSWithSAMLResponse(_testName, webClient, somePage, updatedTestSettings, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, true);

    }

    /**
     * This method searches for an intermediate status code in the logged output since a marker was put in the log
     * This method may be moved in the future to make it available to other methods. I'm keeping it here for now because
     * it will need more work to make it useful in a more general way.
     * (Problem caller needs to set a marker in the log before the url request is made. If multiple requests are made as
     * part of the full flow and you want to check an intermediate status of let's say step 2, we would need to update
     * each of the test steps as well as this routine to lock it into just that one step)
     *
     * @param uniqStartMsg
     *            - marker to look for to start the real search
     * @param statusMsg
     *            - message to search for
     * @return - true (found)/false (not found)
     * @throws Exception
     */
    public static Boolean checkIntermedStatusCode(String uniqStartMsg, String statusMsg) throws Exception {
        String method = "checkIntermedStatusCode";

        boolean found = false;
        String outputFile = "./results/output.txt";
        File f = new File(outputFile);
        String theLine = null;
        if (f.exists() && !f.isDirectory()) {
            Log.info(thisClass, method, "Found the output.txt file");
            Scanner in = null;
            try {
                in = new Scanner(new FileReader(f));
                // look for time out msg
                while (in.hasNextLine()) {
                    theLine = in.nextLine();
                    if (theLine.indexOf(uniqStartMsg) >= 0) {
                        break;
                    }
                }
                while (in.hasNextLine()) {
                    theLine = in.nextLine();
                    if (theLine.indexOf(statusMsg) >= 0) {
                        found = true;
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (Exception e) { /* ignore */
                }
            }
        }

        if (found) {
            Log.info(thisClass, method, "Found the status line: " + theLine);
        }
        return found;

    }
}
