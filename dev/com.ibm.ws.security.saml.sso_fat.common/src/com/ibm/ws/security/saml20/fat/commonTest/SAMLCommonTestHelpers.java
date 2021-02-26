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
package com.ibm.ws.security.saml20.fat.commonTest;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.opensaml.xml.util.Base64;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.ValidationData;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings.CXFSettings;

public class SAMLCommonTestHelpers extends TestHelpers {
    //
    private final static Class<?> thisClass = SAMLCommonTestHelpers.class;
    public static SAMLCommonTestTools samlcttools = new SAMLCommonTestTools();
    public static SAMLCommonValidationTools validationTools = new SAMLCommonValidationTools();
    private final ValidationData vData = new ValidationData(SAMLConstants.ALL_TEST_ACTIONS);

    public static SAMLMessageTools msgUtils = new SAMLMessageTools();
    private static SAMLTestServer testSAMLServer = null;
    private static SAMLTestServer testOIDCServer = null;
    private static SAMLTestServer testSAMLOIDCServer = null;
    private static SAMLTestServer testAppServer = null;
    private static SAMLTestServer testIDPServer = null;
    protected static Boolean OverrideRedirect = false;

    public SAMLCommonTestHelpers() {
    }

    public void setSAMLServer(SAMLTestServer theServer) {
        testSAMLServer = theServer;
    }

    public void setOIDCServer(SAMLTestServer theServer) {
        testOIDCServer = theServer;
    }

    public void setSAMLOIDCServer(SAMLTestServer theServer) {
        testSAMLOIDCServer = theServer;
    }

    public void setAppServer(SAMLTestServer theServer) {
        testAppServer = theServer;
    }

    public void setIDPServer(SAMLTestServer theServer) {
        testIDPServer = theServer;
    }

    public void setServer(String serverType, SAMLTestServer theServer) throws Exception {

        if (serverType.equals(SAMLConstants.SAML_SERVER_TYPE)) {
            setSAMLServer(theServer);
        } else {
            if (serverType.equals(SAMLConstants.SAML_OIDC_SERVER_TYPE)) {
                setSAMLOIDCServer(theServer);
                setSAMLServer(theServer);
                setOIDCServer(theServer);
            } else {
                if (serverType.equals(SAMLConstants.OIDC_SERVER_TYPE)) {
                    setSAMLOIDCServer(theServer);
                } else {
                    if (serverType.equals(SAMLConstants.APP_SERVER_TYPE)) {
                        setAppServer(theServer);
                    } else {
                        if (serverType.equals(SAMLConstants.IDP_SERVER_TYPE)) {
                            Log.info(thisClass, "setServer", "Setting IDP server");
                            setIDPServer(theServer);
                        } else {
                            if (serverType.equals(SAMLConstants.SAML_APP_SERVER_TYPE)) {
                                setAppServer(theServer);
                                setSAMLServer(theServer);
                            } else {
                                throw new RuntimeException("Unknown server type:" + serverType + " Do not know what to do with this - tests should abort");
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object buildPostIDPInitiatedRequest(String testcase, WebClient webClient, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {

        String thisMethod = "buildPostIDPInitiatedRequest";
        msgUtils.printMethodName(thisMethod);

        setMarkEndOfLogs();

        URL url = AutomationTools.getNewUrl(settings.getIdpChallenge());
        WebRequest request = new WebRequest(url, HttpMethod.POST);

        CXFSettings cxfSettings = settings.getCXFSettings();

        request.setRequestParameters(new ArrayList());

        // Setup the rest of the HTTP POST signon request to SP
        updateCXFRequestParms(request, cxfSettings);
        updateRSSAMLRequestParms(request, settings);

        request.getRequestParameters().add(new NameValuePair("RequestBinding", "HTTPPost"));
        setRequestParameterIfSet(request, "providerId", settings.getSpConsumer());
        setRequestParameterIfSet(request, "target", settings.getRelayState());
        setRequestParameterIfSet(request, "NameIdFormat", "email");
        setRequestParameterIfSet(request, "RelayState", encodeString(settings.getRelayState()));
        setRequestParameterIfSet(request, "relayState", settings.getRelayState());

        msgUtils.printRequestParts(webClient, request, testcase, "Outgoing request");

        msgUtils.printAllCookies(webClient);
        Object thePage = webClient.getPage(request);
        // make sure the page is processed before continuing
        waitBeforeContinuing(webClient);

        msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");
        validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
        validationTools.validateResult(webClient, thePage, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, expectations, settings);

        return thePage;
    }

    public String encodeString(String rawString) throws Exception {
        if (rawString == null) {
            return null;
        }
        byte[] bytes = rawString.getBytes("UTF-8");
        String encodedHeaderContent = Base64.encodeBytes(bytes);
        String encodedValue = URLEncoder.encode(encodedHeaderContent, "UTF-8");
        return encodedValue;
    }

    public void updateCXFRequestParms(WebRequest request, CXFSettings cxfSettings) throws Exception {
        if (cxfSettings != null) {
            //builtTarget = settings.getSpTargetApp() + "?samlClient=" + URLEncoder.encode("cxf", "UTF-8") + "&httpDefaultPort=" + URLEncoder.encode(cxfSettings.getPortNumber(), "UTF-8")  ;
            setRequestParameterIfSet(request, "samlClient", cxfSettings.getClientType());
            setRequestParameterIfSet(request, "testName", cxfSettings.getTestMethod());
            setRequestParameterIfSet(request, "httpDefaultPort", cxfSettings.getPortNumber());
            setRequestParameterIfSet(request, "httpDefaultSecurePort", cxfSettings.getSecurePort());
            setRequestParameterIfSet(request, "id", cxfSettings.getId());
            setRequestParameterIfSet(request, "pw", cxfSettings.getPw());
            setRequestParameterIfSet(request, "serviceName", cxfSettings.getServiceName());
            setRequestParameterIfSet(request, "servicePort", cxfSettings.getServicePort());
            setRequestParameterIfSet(request, "msg", cxfSettings.getSendMsg());
            setRequestParameterIfSet(request, "replayTest", cxfSettings.getReplayTest());
            setRequestParameterIfSet(request, "managedClient", cxfSettings.getManagedClient());
            setRequestParameterIfSet(request, "clntWsdlLocation", cxfSettings.getClientWSDLFile());
            setRequestParameterIfSet(request, "testMode", cxfSettings.getTestMode());
        }
    }

    public void updateRSSAMLRequestParms(WebRequest request, SAMLTestSettings settings) throws Exception {

        String thisMethod = "updateRSSAMLRequestParms";
        if (settings.getRSSettings() != null) {
            Log.info(thisClass, thisMethod, "Setting RS Settings parms");
            setRequestParameterIfSet(request, "targetApp", settings.getSpDefaultApp());
            setRequestParameterIfSet(request, "headerFormat", settings.getRSSettings().getHeaderFormat());
            setRequestParameterIfSet(request, "header", settings.getRSSettings().getHeaderName());
            setRequestParameterIfSet(request, "formatType", settings.getRSSettings().getSamlTokenFormat());
        }

    }

    public Object buildGetSolicitedSPInitiatedRequest(String testcase, WebClient webClient, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "buildGetSolicitedSPInitiatedRequest";
        msgUtils.printMethodName(thisMethod);
        return buildSolicitedSPInitiatedRequest(testcase, webClient, settings, expectations, HttpMethod.GET);
    }

    public Object buildPostSolicitedSPInitiatedRequest(String testcase, WebClient webClient, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        String thisMethod = "buildPostSolicitedSPInitiatedRequest";
        msgUtils.printMethodName(thisMethod);
        return buildSolicitedSPInitiatedRequest(testcase, webClient, settings, expectations, HttpMethod.POST);

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object buildSolicitedSPInitiatedRequest(String testcase, WebClient webClient, SAMLTestSettings settings, List<validationData> expectations, HttpMethod method) throws Exception {

        String thisMethod = "buildSolicitedSPInitiatedRequest";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkEndOfLogs();

            CXFSettings cxfSettings = settings.getCXFSettings();

            URL url = AutomationTools.getNewUrl(settings.getSpTargetApp());
            WebRequest request = new WebRequest(url, method);
            request.setRequestParameters(new ArrayList());
            updateCXFRequestParms(request, cxfSettings);
            updateRSSAMLRequestParms(request, settings);

            Log.info(thisClass, thisMethod, "Returned request is: " + request.toString());
            List<NameValuePair> parms = request.getRequestParameters();
            if (parms != null) {
                Log.info(thisClass, thisMethod, "Returned request parms are: " + parms.toString());
            }

            msgUtils.printRequestParts(request, testcase, "Outgoing request");
            Log.info(thisClass, thisMethod, "Trying to invoke: " + settings.getSpTargetApp());
            thePage = webClient.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, expectations, settings);

            String id = null;
            if (thePage instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
                id = getValueFromCookieInConversation(webClient, "SAML20RelayState");

                Log.info(thisClass, thisMethod, "relay state: " + id);
            }

            settings.setSamlTokenValidationData(settings.getSamlTokenValidationData().getNameId(), settings.getSamlTokenValidationData().getIssuer(), id, settings.getSamlTokenValidationData().getMessageID(), settings.getSamlTokenValidationData().getEncryptionKeyUser(), settings.getSamlTokenValidationData().getRecipient(), settings.getSamlTokenValidationData().getEncryptAlg());
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, e);
        }
        return thePage;
    }

    public Object handleIdpClientJsp(String testcase, WebClient webClient, Object startPage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {

        String thisMethod = "handleIdpClientJsp";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkEndOfLogs();

            // Read response from HTTP POST request, should be a IDP client jsp
            Log.info(thisClass, thisMethod, "Calling IdP Client jsp...");

            //             HtmlForm form = ((HtmlPage) startPage).getForms().get(0);
            HtmlForm form = getForm0WithDebug((HtmlPage) startPage, testcase);

            HtmlElement button = null;

            button = form.getButtonByName("processIdp");

            HtmlTextInput textField = form.getInputByName("urlIdpRequest");
            if (samlcttools.isIDPADFS(settings.getIdpRoot())) {
                textField.setValueAttribute(settings.getIdpChallenge() + "?RelayState=RPID" + URLEncoder.encode("=" + settings.getSpConsumer() +
                        "&RelayState=" + settings.getSpTargetApp(), "UTF-8"));
            } else {
                textField.setValueAttribute(settings.getIdpChallenge());
            }

            HtmlTextInput textField2 = form.getInputByName("urlPartnerId");
            textField2.setValueAttribute(settings.getSpConsumer());
            HtmlTextInput textField3 = form.getInputByName("nameIdFormat");
            textField3.setValueAttribute("email");
            HtmlTextInput textField4 = form.getInputByName("urlTarget");
            textField4.setValueAttribute(settings.getSpTargetApp());
            HtmlTextInput textField5 = form.getInputByName("testCaseName");
            textField5.setValueAttribute(testcase);

            Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");

            msgUtils.printFormParts(form, thisMethod, "Parms for IDPClientJSP: ");
            thePage = button.click();
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.HANDLE_IDPCLIENT_JSP, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.HANDLE_IDPCLIENT_JSP, e);
        }
        return (thePage);

    }

    public Object processIdpJsp(String testcase, WebClient webClient, Object startPage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {

        String thisMethod = "processIdpJsp";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkEndOfLogs();

            // Read response from HTTP POST request, should be a IDP client jsp
            Log.info(thisClass, thisMethod, "Processing IdP jsp...");

            // try to work around a bug in htmlunit where it returns a null list of forms
            // even though there are forms
            HtmlForm form = getForm0WithDebug((HtmlPage) startPage, testcase);

            HtmlElement button = null;

            button = form.getButtonByName("processidpform");
            msgUtils.printFormParts(form, thisMethod, "Parms for IDPClientJSP: ");

            thePage = button.click();
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);

            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.PROCESS_IDP_JSP, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.PROCESS_IDP_JSP, e);
        }
        return (thePage);

    }

    public Object performIDPLogin(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {

        String thisMethod = "performIDPLogin";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkEndOfLogs();

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Calling IdP Login...");

            // try to work around a bug in htmlunit where it returns a null list of forms
            // even though there are forms
            HtmlForm form = getForm0WithDebug(startPage, testcase);
            webClient.getOptions().setJavaScriptEnabled(false);

            // Fill in the login form and submit the login request

            HtmlElement button = null;

            if (AutomationTools.getResponseText(startPage).contains(SAMLConstants.SAML_REQUEST)) {
                button = form.getButtonByName("redirectform");
            } else {
                button = form.getButtonByName("_eventId_proceed");

                HtmlTextInput textField = form.getInputByName("j_username");
                Log.info(thisClass, thisMethod, "username field is: " + textField);
                textField.setValueAttribute(settings.getIdpUserName());
                HtmlPasswordInput textField2 = form.getInputByName("j_password");
                Log.info(thisClass, thisMethod, "password field is: " + textField2);
                textField2.setValueAttribute(settings.getIdpUserPwd());
                Log.info(thisClass, thisMethod, "Setting: " + textField + " to: " + settings.getIdpUserName());
                Log.info(thisClass, thisMethod, "Setting: " + textField2 + " to: " + settings.getIdpUserPwd());

                msgUtils.printFormParts(form, thisMethod, "Parms for performIDPLogin: ");
            }

            Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");

            thePage = button.click();
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.PERFORM_IDP_LOGIN, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.PERFORM_IDP_LOGIN, e);
        }
        return (thePage);

    }

    public Object processLoginContinue(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return processContinue(testcase, webClient, startPage, settings, expectations, SAMLConstants.PROCESS_LOGIN_CONTINUE);
    }

    public Object processIDPContinue(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return processContinue(testcase, webClient, startPage, settings, expectations, SAMLConstants.PROCESS_IDP_CONTINUE);
    }

    public Object processLogoutContinue(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return processContinue(testcase, webClient, startPage, settings, expectations, SAMLConstants.PROCESS_LOGOUT_CONTINUE);
    }

    public Object processContinue(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations, String step) throws Exception {

        String thisMethod = "processContinue";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkEndOfLogs();

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Processing continue...");

            HtmlForm form = startPage.getFormByName("form1");
            //            webClient.getOptions().setJavaScriptEnabled(false);

            // Fill in the login form and submit the login request

            try {
                HtmlElement button = form.getButtonByName("_eventId_proceed");

                Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");
                thePage = button.click();
            } catch (com.gargoylesoftware.htmlunit.ElementNotFoundException e) {
                HtmlInput continueButton = form.getInputByValue("Continue");
                Log.info(thisClass, thisMethod, "\'Pressing the Continue button\'");
                thePage = continueButton.click();
            } catch (Exception e) {
                // we should never get a different exception, so, rethrow and let the test case fail
                throw e;
            }
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, step, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, step, e);
        }
        return (thePage);

    }

    public Object processContinue2(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "processContinue";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkEndOfLogs();

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Processing continue...");

            List<HtmlForm> forms = startPage.getForms();
            //            webClient.getOptions().setJavaScriptEnabled(false);

            // Fill in the login form and submit the login request

            try {
                for (HtmlForm form : forms) {
                    HtmlInput continueButton = form.getInputByValue("Continue");
                    Log.info(thisClass, thisMethod, "\'Pressing the Continue button\'");
                    thePage = continueButton.click();
                    // make sure the page is processed before continuing
                    waitBeforeContinuing(webClient);
                }
            } catch (Exception e) {
                // we should never get a different exception, so, rethrow and let the test case fail
                throw e;
            }

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.PROCESS_LOGOUT_CONTINUE2, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.PROCESS_LOGOUT_CONTINUE2, e);
        }
        return (thePage);

    }

    public Object processLogoutPropagateYes(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "processLogoutPropagateYes";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            setMarkEndOfLogs();

            //            webClient.getOptions().setJavaScriptEnabled(true);

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Processing propagate logout...");

            HtmlForm form = null;
            List<HtmlForm> forms = startPage.getForms();
            for (HtmlForm aForm : forms) {
                if ("propagate_form".equals(aForm.getId())) {
                    form = aForm;
                }
            }

            if (form == null) {
                thePage = startPage;
            } else {
                try {
                    HtmlButton button = null;
                    List<HtmlButton> buttons = form.getButtonsByName("_eventId");
                    for (HtmlButton aButton : buttons) {
                        if ("propagate_yes".equals(aButton.getId())) {
                            button = aButton;
                        }
                    }

                    Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");
                    thePage = button.click();
                } catch (Exception e) {
                    // we should never get a different exception, so, rethrow and let the test case fail
                    throw e;
                }
            }
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            List<FrameWindow> frames = ((HtmlPage) thePage).getFrames();
            Log.info(thisClass, thisMethod, "found " + frames.size() + " frames");
            if (frames.size() == 0) {
                if (thePage instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
                    HtmlForm form2 = ((HtmlPage) thePage).getForms().get(0);

                    HtmlInput continueButton = form2.getInputByValue("Continue");
                    thePage = continueButton.click();
                    waitBeforeContinuing(webClient);
                    msgUtils.printAllCookies(webClient);
                    msgUtils.printResponseParts(thePage, testcase, thisMethod + " continue click");
                }
            } else {
                for (FrameWindow frame : frames) {
                    //                for (FrameWindow frame : sortFrames(frames)) {
                    Log.info(thisClass, thisMethod, "iframe " + frame.toString());
                    String id = frame.getFrameElement().getId();
                    Log.info(thisClass, thisMethod, "iframe id: ::" + id + "::");

                    Log.info(thisClass, thisMethod, "Using frame with id: " + frame.getFrameElement().getId());
                    thePage = frame.getEnclosedPage();
                    msgUtils.printResponseParts(thePage, testcase, thisMethod + " iframe");
                    if (thePage instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
                        int numForms = ((HtmlPage) thePage).getForms().size();
                        Log.info(thisClass, thisMethod, "Found " + numForms + " forms ");
                        // use size() check cause checking ((HtmlPage) thePage).getForms() != null never catches the case where there are no frames, then the next line throws an npe
                        if (numForms > 0) {
                            HtmlForm form2 = ((HtmlPage) thePage).getForms().get(0);
                            HtmlInput continueButton = form2.getInputByValue("Continue");
                            thePage = continueButton.click();
                            waitBeforeContinuing(webClient);
                            msgUtils.printAllCookies(webClient);
                            msgUtils.printResponseParts(thePage, testcase, thisMethod + " continue click");
                            if (id != null && !id.isEmpty() && !(thePage instanceof com.gargoylesoftware.htmlunit.TextPage)) {
                                processLogoutRedirect(testcase, webClient, (HtmlPage) thePage, settings, expectations);
                            }
                        }
                    }
                }
            }

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, e);
        }
        return (thePage);

    }

    /***
     * Sort frames by frame id name, not by sp name
     * Seems like some OS's are returning frames in reverse order
     *
     * @param frames
     * @return - sorted frames
     * @throws Exception
     */
    List<FrameWindow> sortFrames(List<FrameWindow> frames) throws Exception {

        String thisMethod = "sortFrames";

        List<FrameWindow> sortedFrames = new ArrayList<FrameWindow>();
        List<String> sortedIds = new ArrayList<String>();
        for (FrameWindow frame : frames) {
            //            Log.info(thisClass, thisMethod, "iframe " + frame.toString());
            String id = frame.getFrameElement().getId();
            sortedIds.add(id);
            Log.info(thisClass, thisMethod, "iframe id: ::" + id + "::");
        }
        Collections.sort(sortedIds);
        // uncomment the next line to for the failure where we do not get the subject doesn't exist error message.
        //        Collections.reverse(sortedIds);
        for (String id : sortedIds) {
            for (FrameWindow frame : frames) {
                if (id.equals(frame.getFrameElement().getId())) {
                    sortedFrames.add(frame);
                }
            }
        }
        return sortedFrames;
    }

    public Object processIDPRequest(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return processRequest(testcase, webClient, startPage, settings, expectations, SAMLConstants.PROCESS_IDP_REQUEST);
    }

    public Object processLoginRequest(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return processRequest(testcase, webClient, startPage, settings, expectations, SAMLConstants.PROCESS_LOGIN_REQUEST);
    }

    public Object processLogoutRequest(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return processRequest(testcase, webClient, startPage, settings, expectations, SAMLConstants.PROCESS_LOGOUT_REQUEST);
    }

    public Object processLogoutRedirect(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return processRequest(testcase, webClient, startPage, settings, expectations, SAMLConstants.PROCESS_LOGOUT_REDIRECT);
    }

    public Object processRequest(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings, List<validationData> expectations, String step) throws Exception {

        String thisMethod = "processRequest";
        msgUtils.printMethodName(thisMethod);

        Object thePage = null;

        try {

            if (!step.equals(SAMLConstants.PROCESS_LOGOUT_REDIRECT)) {
                setMarkEndOfLogs();
            }

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Processing request...");
            //            Log.info(thisClass, thisMethod, "Raw page is: " + startPage.asXml());

            HtmlForm form = startPage.getFormByName("redirectform");

            try {
                HtmlElement button = form.getButtonByName("redirectform");

                Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");
                thePage = button.click();
            } catch (Exception e) {
                // we should never get a different exception, so, rethrow and let the test case fail
                throw e;
            }
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, step, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, step, e);
        }
        return (thePage);

    }

    public Object invokeACSWithSAMLResponse(String testcase, WebClient webClient, Object startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        return invokeACSWithSAMLResponse(testcase, webClient, startPage, settings, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, true);
    }

    public Object invokeACSWithSAMLResponse(String testcase, WebClient webClient, Object startPage, SAMLTestSettings settings,
            List<validationData> expectations, String stepName, Boolean returnResultingResponse) throws Exception {

        String thisMethod = "invokeACSWithSAMLResponse";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;

        try {

            // work around issue:  10585
            deleteCookiesLike(webClient, "WASSamlReq_");

            setMarkEndOfLogs();

            // We get a "continue page" submit that to complete the "login" process
            WebRequest request = samlcttools.getRequestWithSamlToken(startPage, settings);

            msgUtils.printAllCookies(webClient);

            if (!stepName.equals(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES)) {
                // Clear LTPA cookie sent by IdP
                // we'll end up with multiple ltpa cookies on the next response
                // and it will be hard to know which is the one we want - clear this one
                webClient.getCookieManager().clearCookies();
                Log.info(thisClass, thisMethod, "Cookies after we clear cookies (psst: this should not log any cookies)");
                msgUtils.printAllCookies(webClient);
            }

            // sleep before trying to re-use the saml token (replay attack testing)
            if (stepName.equals(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN)) {
                testSleep(settings.getTokenReuseSleep());
            } else {
                // sleep before trying to use the saml token (clockskew testing)
                if ((stepName.equals(SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE)) && (settings.getSleepBeforeTokenUse() != 0)) {
                    testSleep(settings.getSleepBeforeTokenUse());
                }
            }

            Log.info(thisClass, thisMethod, "ACS request: " + request.getUrl());
            thePage = webClient.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, stepName, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, stepName, e);
        }

        if (returnResultingResponse) {
            return thePage;
        } else {
            return startPage;
        }
    }

    public void invokeAlternateApp(String testcase, WebClient webClient, Object thePage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {
        invokeApp(testcase, webClient, thePage, settings, settings.getSpAlternateApp(), expectations, SAMLConstants.INVOKE_ALTERNATE_APP);
    }

    public void invokeDefaultApp(String testcase, WebClient webClient, Object thePage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {
        invokeApp(testcase, webClient, thePage, settings, settings.getSpDefaultApp(), expectations, SAMLConstants.INVOKE_DEFAULT_APP);
    }

    public void invokeApp(String testcase, WebClient webClient, Object startPage, SAMLTestSettings settings,
            String app, List<validationData> expectations, String step) throws Exception {

        String thisMethod = "invokeApp";
        msgUtils.printMethodName(thisMethod);
        Object thePage;

        try {

            if (app == null) {
                throw new RuntimeException("Application not specifed - nothing to run");
            } else {
                Log.info(thisClass, thisMethod, "Will be invoking: " + app);
            }

            setMarkEndOfLogs();

            WebClient webClient2 = getWebClient();

            URL url = AutomationTools.getNewUrl(app);
            WebRequest request = new WebRequest(url, HttpMethod.POST);

            Log.info(thisClass, thisMethod, "Trying to invoke: " + settings.getSpTargetApp());

            // if a conversation was passed in,
            // get the LTPA Token out of the original conversation
            // create a new conversation and use the saved token
            String ltpaToken2 = extractLtpaCookie(webClient);
            ArrayList<String[]> SP_cookies = extractSPCookies(webClient);
            Boolean spCookieFound = false;
            String expectedCookieName = settings.getSpCookieName();
            if (expectedCookieName != null) {
                spCookieFound = true;
            }

            if (webClient != null) {
                if ((settings.getAccessTokenType().equals(SAMLConstants.SP_ACCESS_TOKEN_TYPE)) || (settings.getAccessTokenType().equals(SAMLConstants.SP_AND_LTPA_ACCESS_TOKEN_TYPE))) {
                    Log.info(thisClass, thisMethod, "Retrieving SP cookie(s)");
                    //ArrayList<String[]> SP_cookies = extractSPCookies(wc);
                    for (String[] cookie : SP_cookies) {
                        if (!cookie[0].equals(expectedCookieName)) {
                            spCookieFound = true;
                        }
                        Log.info(thisClass, thisMethod, "Adding SP cookie: " + cookie[0] + " with value: " + cookie[1]);
                        Cookie aCookie = webClient.getCookieManager().getCookie(cookie[0]);
                        if (webClient2.getCookieManager() == null) {
                            Log.info(thisClass, thisMethod, "Cookie Manager is null");
                        }
                        webClient2.getCookieManager().addCookie(aCookie);
                        //                        wc2.putCookie(cookie[0], cookie[1]);
                    }
                    msgUtils.assertTrueAndLog(thisMethod, "Did NOT find any SP Cookies in the conversation", ((SP_cookies != null) && (!SP_cookies.isEmpty())));
                    msgUtils.assertTrueAndLog(thisMethod, "Expected SP Cookie: " + expectedCookieName + "was NOT found", spCookieFound);

                    if (!settings.getAccessTokenType().equals(SAMLConstants.SP_AND_LTPA_ACCESS_TOKEN_TYPE)) {
                        Log.info(thisClass, thisMethod, "LTPA cookie is: " + ltpaToken2);
                        msgUtils.assertTrueAndLog(thisMethod, "Found LTPA Cookie and should not have", (ltpaToken2 == null));
                    }
                }

                if ((settings.getAccessTokenType().equals(SAMLConstants.LTPA_ACCESS_TOKEN_TYPE)) ||
                        (settings.getAccessTokenType().equals(SAMLConstants.SP_AND_LTPA_ACCESS_TOKEN_TYPE)) ||
                        (settings.getAccessTokenType().equals(SAMLConstants.LTPA_ALTERED_ACCESS_TOKEN_TYPE))) {
                    Log.info(thisClass, thisMethod, "Retrieving LTPA cookie");
                    // otherwise it's LTPA
                    //	String ltpaToken2 = extractLtpaCookie(wc);
                    // if we want and altered LTPA token, mangle it by cutting off the first 5 characters
                    if (settings.getAccessTokenType().equals(SAMLConstants.LTPA_ALTERED_ACCESS_TOKEN_TYPE)) {
                        ltpaToken2 = ltpaToken2.substring(5);
                    }
                    Log.info(thisClass, thisMethod, "Adding LTPA cookie: " + ltpaToken2);
                    Cookie aCookie = webClient.getCookieManager().getCookie(SAMLConstants.LTPA_TOKEN_NAME);
                    webClient2.getCookieManager().addCookie(new Cookie(aCookie.getDomain(), aCookie.getName(), ltpaToken2));
                    msgUtils.assertTrueAndLog(thisMethod, "Did NOT find any LTPA Cookies in the conversation", (ltpaToken2 != null));
                    if (!settings.getAccessTokenType().equals(SAMLConstants.SP_AND_LTPA_ACCESS_TOKEN_TYPE)) {
                        if (SP_cookies != null) {
                            msgUtils.assertTrueAndLog(thisMethod, "Found SP Cookie and should not have", (!((SP_cookies != null) && (!SP_cookies.isEmpty()))));
                        }
                    }
                }
            }

            if (settings.getAccessTokenType().equals(SAMLConstants.SP_ACCESS_TOKEN_TYPE)) {
                Log.info(thisClass, thisMethod, "Invoking SP URL:  " + app + " with new conversation (using incoming " + SAMLConstants.SP_COOKIE_PREFIX + "*).");
            } else {
                if (settings.getAccessTokenType().equals(SAMLConstants.SP_AND_LTPA_ACCESS_TOKEN_TYPE)) {
                    Log.info(thisClass, thisMethod, "Invoking SP URL:  " + app + " with new conversation (using incoming " + SAMLConstants.SP_COOKIE_PREFIX + "*).");
                    Log.info(thisClass, thisMethod, "Invoking SP URL:  " + app + " with new conversation (using incoming " + SAMLConstants.LTPA_TOKEN_NAME + ").");
                } else {
                    if (settings.getAccessTokenType().equals(SAMLConstants.LTPA_ACCESS_TOKEN_TYPE)) {
                        Log.info(thisClass, thisMethod, "Invoking SP URL:  " + app + " with new conversation (using incoming " + SAMLConstants.LTPA_TOKEN_NAME + ").");
                    } else {
                        Log.info(thisClass, thisMethod, "Invoking SP URL:  " + app + " with new conversation (using incoming unknown cookie.");
                    }
                }

            }

            thePage = webClient2.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient2);

            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, step, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, step, e);
        }

    }

    public void invokeDefaultAppSameConversation(String testcase, WebClient webClient, Object somePage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {
        invokeAppSameConversation(testcase, webClient, somePage, settings, settings.getSpDefaultApp(), expectations, SAMLConstants.INVOKE_DEFAULT_APP);
    }

    public void invokeAlternateAppSameConversation(String testcase, WebClient webClient, Object somePage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {
        invokeAppSameConversation(testcase, webClient, somePage, settings, settings.getSpAlternateApp(), expectations, SAMLConstants.INVOKE_ALTERNATE_APP);
    }

    public void invokeAppSameConversation(String testcase, WebClient webClient, Object somePage, SAMLTestSettings settings,
            String app, List<validationData> expectations, String step) throws Exception {

        String thisMethod = "invokeAppSameConversation";
        msgUtils.printMethodName(thisMethod);

        try {

            setMarkEndOfLogs();

            if (app == null) {
                throw new RuntimeException("Application not specifed - nothing to run");
            } else {
                Log.info(thisClass, thisMethod, "Will be invoking: " + app);
            }

            URL url = AutomationTools.getNewUrl(app);
            WebRequest request = new WebRequest(url, HttpMethod.POST);

            msgUtils.printAllCookies(webClient);
            msgUtils.printRequestParts(request, testcase, "Outgoing request");

            Log.info(thisClass, thisMethod, "Invoking SP URL:  " + app + " with the SAME conversation.");
            Object thePage = webClient.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, step, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, step, e);
        }

    }

    public Object performFormLogin(String testcase, WebClient webClient, HtmlPage startPage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {

        String thisMethod = "performFormLogin";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;

        try {

            setMarkEndOfLogs();

            // Read response from HTTP POST request, should be a login challenge
            Log.info(thisClass, thisMethod, "Processing Form Login...");

            //             HtmlForm form = startPage.getForms().get(0);
            // try to work around a bug in htmlunit where it returns a null list of forms
            // even though there are forms
            HtmlForm form = getForm0WithDebug(startPage, testcase);
            webClient.getOptions().setJavaScriptEnabled(false);

            // Fill in the login form and submit the login request

            HtmlElement button = null;
            HtmlSubmitInput loginButton = null;

            try {
                button = form.getButtonByName("_eventId_proceed");
            } catch (com.gargoylesoftware.htmlunit.ElementNotFoundException e) {
                Log.info(thisClass, thisMethod, "App login page doesn't have a submit button");
                loginButton = form.getInputByValue("Login"); // must be on the apps login page - get that button instead
            } catch (Exception e) {
                // we should never get a different exception, so, rethrow and let the test case fail
                throw e;
            }

            HtmlTextInput textField = form.getInputByName("j_username");
            Log.info(thisClass, thisMethod, "username field is: " + textField);
            textField.setValueAttribute(settings.getSpRegUserName());
            HtmlPasswordInput textField2 = form.getInputByName("j_password");
            Log.info(thisClass, thisMethod, "password field is: " + textField2);
            textField2.setValueAttribute(settings.getSpRegUserPwd());
            Log.info(thisClass, thisMethod, "Setting: " + textField + " to: " + settings.getSpRegUserName());
            Log.info(thisClass, thisMethod, "Setting: " + textField2 + " to: " + settings.getSpRegUserPwd());

            msgUtils.printFormParts(form, thisMethod, "Parms for FormLogin: ");

            if (button == null) {
                Log.info(thisClass, thisMethod, "\'Pressing the Login button\'");
                thePage = loginButton.click();
            } else {
                Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");
                thePage = button.click();
            }
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.PROCESS_FORM_LOGIN, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.PROCESS_FORM_LOGIN, e);
        }

        return (thePage);

    }

    public Object invokeSAMLMetaDataEndpoint(String testcase, WebClient inWebClient, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeSAMLMetaDataEndpoint";
        msgUtils.printMethodName(thisMethod);

        setMarkEndOfLogs();

        WebClient webClient = getWebClient(inWebClient);

        URL url = AutomationTools.getNewUrl(settings.getSpMetaDataEdpt());
        WebRequest request = new WebRequest(url, HttpMethod.POST);

        Object thePage = webClient.getPage(request);
        // make sure the page is processed before continuing
        waitBeforeContinuing(webClient);

        msgUtils.printAllCookies(webClient);
        msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

        validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
        validationTools.validateResult(webClient, thePage, SAMLConstants.SAML_META_DATA_ENDPOINT, expectations, settings);

        return thePage;
    }

    /*
     * The SP and LTPA cookies created during the SAML flow should be cleaned up by the form logout,
     * however a user might still have residual cookies left over from the IDP server that are still valid.
     * The tests should ensure that the cookies we are responsible for have been properly cleaned up
     * and that the flow behaves appropriately based on the contents of any remaining cookies.
     */

    public Object performSPLogout(String testcase, WebClient webClient, Object somePage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {

        String thisMethod = "performSPLogout";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;
        try {
            setMarkEndOfLogs();
            //            testSleep(60);

            // Perform logout request
            URL url = AutomationTools.getNewUrl(settings.getSpLogoutURL());
            WebRequest request = new WebRequest(url, HttpMethod.POST);

            msgUtils.printAllCookies(webClient);
            msgUtils.printRequestParts(webClient, request, testcase, "Outgoing request");
            thePage = webClient.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.PERFORM_SP_LOGOUT, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.PERFORM_SP_LOGOUT, e);
        }
        return thePage;
    }

    public Object performIDPLogout(String testcase, WebClient webClient, Object somePage, SAMLTestSettings settings,
            List<validationData> expectations) throws Exception {

        String thisMethod = "performIDPLogout";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;
        try {
            setMarkEndOfLogs();
            //            testSleep(60);

            // Perform logout request
            URL url = AutomationTools.getNewUrl(settings.getIdpLogoutURL());
            WebRequest request = new WebRequest(url, HttpMethod.POST);

            msgUtils.printAllCookies(webClient);
            msgUtils.printRequestParts(webClient, request, testcase, "Outgoing request");
            thePage = webClient.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");

            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.PERFORM_IDP_LOGOUT, expectations, settings);
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.PERFORM_IDP_LOGOUT, e);
        }
        return thePage;
    }

    public void deleteCookiesLike(WebClient webClient, String cookiePrefix) {

        msgUtils.printAllCookies(webClient);
        CookieManager cm = webClient.getCookieManager();
        Set<Cookie> cookies = cm.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie != null && cookie.getName().startsWith(cookiePrefix)) {
                deleteCookie(webClient, cookie.getName());
            }
        }
    }

    // if the cookie is not truly removed, add some more debug to see if it already expired
    // I've seen some mention that remove seems to have trouble with expired cookies
    public void deleteCookie(WebClient webClient, String cookieName) {

        msgUtils.printAllCookies(webClient);
        CookieManager cm = webClient.getCookieManager();
        Cookie cookie = cm.getCookie(cookieName);
        if (cookie == null) {
            Log.info(thisClass, "deleteCookie", "Can NOT delete cookie " + cookieName + "since the cookie was NOT found");
            return;
        }
        Log.info(thisClass, "deleteCookie", "Deleting cookie: " + cookieName);
        cm.removeCookie(cookie);
    }

    public String extractLtpaCookie(WebClient webClient) {
        return extractSpecificCookieValue(webClient, SAMLConstants.LTPA_TOKEN_NAME);
    }

    public String extractSpecificCookieValue(WebClient webClient, String cookieName) {
        String cookieValue = null;
        Cookie cookie = extractSpecificCookie(webClient, cookieName);
        if (cookie != null) {
            cookieValue = cookie.getValue();
        }

        return (cookieValue);
    }

    public Cookie extractSpecificCookie(WebClient webClient, String cookieName) {

        msgUtils.printAllCookies(webClient);
        CookieManager cm = webClient.getCookieManager();
        Cookie cookie = cm.getCookie(cookieName);
        return cookie;

    }

    public ArrayList<String[]> extractSPCookies(WebClient webClient)

    {
        msgUtils.printAllCookies(webClient);

        CookieManager cm = webClient.getCookieManager();
        Set<Cookie> cookies = cm.getCookies();
        ArrayList<String[]> cookieList = new ArrayList<String[]>();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().startsWith(SAMLConstants.SP_COOKIE_PREFIX)) {
                    String[] spCookie = new String[2];
                    Log.info(thisClass, "extractSPCookies", "Matched: " + cookie.getName());
                    spCookie[0] = cookie.getName();
                    spCookie[1] = cookie.getValue();
                    cookieList.add(spCookie);
                }
            }
        }

        return cookieList;
    }

    public ArrayList<Cookie> extractJsessionCookies(WebClient webClient) {

        msgUtils.printAllCookies(webClient);

        CookieManager cm = webClient.getCookieManager();
        Set<Cookie> cookies = cm.getCookies();
        ArrayList<Cookie> cookieList = new ArrayList<Cookie>();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                Log.info(thisClass, "extractAllCookiesExcept", "Processing: " + cookie.getName());
                if (cookie.getName().startsWith(SAMLConstants.JSESSIONID)) {
                    Log.info(thisClass, "extractJsession", "Matched: " + cookie.getName());
                    cookieList.add(cookie);
                }
            }
        }
        return cookieList;
    }

    public ArrayList<Cookie> extractAllCookiesExcept(WebClient webClient, String exceptCookieName)

    {
        msgUtils.printAllCookies(webClient);

        CookieManager cm = webClient.getCookieManager();
        Set<Cookie> cookies = cm.getCookies();
        ArrayList<Cookie> cookieList = new ArrayList<Cookie>();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                Log.info(thisClass, "extractAllCookiesExcept", "Processing: " + cookie.getName());
                if (!cookie.getName().startsWith(exceptCookieName)) {
                    Log.info(thisClass, "extractAllCookiesExcept", "Adding: " + cookie.getName());
                    //                    String[] currentCookie = new String[2];
                    Log.info(thisClass, "extractSPCookies", "Matched: " + cookie.getName());
                    cookieList.add(cookie);
                }
            }
        }

        for (Cookie cookie : cookieList) {
            Log.info(thisClass, "extractAllCookiesExcept", "Logging cookie: " + cookie.getName() + " with value: " + cookie.getValue());
        }
        return cookieList;
    }

    public WebClient addAllCookiesExcept(WebClient webClient, ArrayList<Cookie> cookieList, String exceptCookieName) throws Exception

    {
        for (Cookie cookie : cookieList) {
            Log.info(thisClass, "addAllCookiesExcept", "Checking cookie: " + cookie.getName());
            if (!cookie.getName().equals(exceptCookieName)) {
                Log.info(thisClass, "addAllCookiesExcept", "Adding cookie: " + cookie.getName() + " with value: " + cookie.getValue());
                CookieManager cm = webClient.getCookieManager();
                if (cm == null) {
                    throw new Exception("addAllCookiesExcept: CookieManager was null");
                }
                cm.addCookie(cookie);
            }
        }
        Log.info(thisClass, "addAllCookiesExcept", "Updated cookies:");
        msgUtils.printAllCookies(webClient);
        return webClient;
    }

    public List<validationData> setDefaultGoodSAMLExpectations(String flowType, SAMLTestSettings testSettings) throws Exception {
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            return setDefaultGoodSAMLIDPInitiatedExpectations(testSettings);
        }
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            return setDefaultGoodSAMLSolicitedSPInitiatedExpectations(testSettings);
        }
        if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            return setDefaultGoodSAMLUnSolicitedSPInitiatedExpectations(testSettings);
        }
        return null;

    }

    public List<validationData> setDefaultGoodSAMLIDPInitiatedExpectations(SAMLTestSettings testSettings) throws Exception {
        return setDefaultGoodSAMLExpectations(SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST, SAMLConstants.CALL_EXTRA_APP, testSettings, null);
    }

    public List<validationData> setDefaultGoodSAMLSolicitedSPInitiatedExpectations(SAMLTestSettings testSettings) throws Exception {
        return setDefaultGoodSAMLExpectations(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.CALL_EXTRA_APP, testSettings, null);
    }

    public List<validationData> setDefaultGoodSAMLSolicitedSPInitiatedExpectations(SAMLTestSettings testSettings, String checkType) throws Exception {
        return setDefaultGoodSAMLExpectations(SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.CALL_EXTRA_APP, testSettings, checkType);
    }

    // Unsolicited flow is basically the same as solicited (but behaviours should be more like idp initiated
    public List<validationData> setDefaultGoodSAMLUnSolicitedSPInitiatedExpectations(SAMLTestSettings testSettings) throws Exception {
        List<validationData> expectations = setDefaultGoodSAMLExpectations(SAMLConstants.PROCESS_IDP_JSP, SAMLConstants.CALL_EXTRA_APP, testSettings, null);
        expectations = vData.addExpectation(expectations, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP Client jsp", null, SAMLConstants.IDP_CLIENT_JSP_TITLE);
        expectations = vData.addExpectation(expectations, SAMLConstants.HANDLE_IDPCLIENT_JSP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP Client jsp", null, SAMLConstants.IDP_PROCESS_JSP_TITLE);
        return expectations;

    }

    public List<validationData> setDefaultGoodSAMLExpectations(String startingAction, Boolean callAltApp, SAMLTestSettings testSettings, String checkType) throws Exception {
        List<validationData> expectations = setDefaultGoodSAMLExpectationsThroughACSOnly(startingAction, testSettings, checkType);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);
        if (callAltApp) {
            expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land alternate app", null, SAMLConstants.APP2_TITLE);
        }
        setSimpleServletExpecatations(expectations, testSettings);
        return expectations;
    }

    public List<validationData> setDefaultGoodSAMLExpectationsThroughACSOnly(String startingAction, SAMLTestSettings testSettings, String checkType) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, startingAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, samlcttools.getLoginTitle(testSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, samlcttools.getResponseTitle(testSettings.getIdpRoot()));
        expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        if (SAMLConstants.STRING_CONTAINS.equals(checkType)) {
            expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN_CONTAINS, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);
        } else {
            expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);
        }
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE);

        return expectations;

    }

    public void setSimpleServletExpecatations(List<validationData> expectations, SAMLTestSettings testSettings) throws Exception {

        //		getRequestURL
        // determine where/if we're using SimpleServlet (usually using it as the alternate app, but check both to be sure)
        if (testSettings.getSpTargetApp() != null && testSettings.getSpTargetApp().contains(SAMLConstants.ALTERNATE_SERVLET)) {
            setSimpleServletExpecatations(expectations, testSettings, SAMLConstants.INVOKE_DEFAULT_APP, testSettings.getSpTargetApp());
        }
        if (testSettings.getSpAlternateApp() != null && testSettings.getSpAlternateApp().contains(SAMLConstants.ALTERNATE_SERVLET)) {
            setSimpleServletExpecatations(expectations, testSettings, SAMLConstants.INVOKE_ALTERNATE_APP, testSettings.getSpAlternateApp());
        }

    }

    public static String assembleRegExRealmNameInRunAsSubject(String realm) {
        return "(?s)\\A.*?\\bRunAs subject: Subject:.*?\\bPublic Credential:.*?realmName=" + realm;
    }

    public void setSimpleServletExpecatations(List<validationData> expectations, SAMLTestSettings testSettings, String step, String theApp) throws Exception {

        if (step != null) {
            //			expectations = vData.addExpectation(expectations, step, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on correct app for step: " + step, null, SAMLConstants.APP2_TITLE);
            expectations = vData.addExpectation(expectations, step, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not land on correct app for step: " + step, null, "getRequestURL: " + theApp);

            if (testSettings.getIncludeTokenInSubject()) {
                expectations = vData.addExpectation(expectations, step, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get the expected issuer", null, "SAMLIssuerName:" + testSettings.getIdpIssuer());
            }
        }
    }

    public List<validationData> setDefaultGoodSAMLCXFExpectations(List<validationData> expectations, String flowType, SAMLTestSettings testSettings) throws Exception {
        return setDefaultGoodSAMLCXFExpectations(expectations, flowType, testSettings, null);
    }

    public List<validationData> setDefaultGoodSAMLCXFExpectations(List<validationData> expectations, String flowType, SAMLTestSettings testSettings, String serverMsg) throws Exception {

        // other tests will have validated the SAML Token and other steps and values in the SAML flow
        // this method will only set expectations for the CXF portion of a flow
        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }
        CXFSettings cxfSettings = testSettings.getCXFSettings();
        String titleToCheck = SAMLConstants.CXF_SAML_TOKEN_SERVLET;
        if (!(cxfSettings.getTitleToCheck().isEmpty())) {
            titleToCheck = cxfSettings.getTitleToCheck();
        }
        String part = SAMLConstants.CXF_SAML_TOKEN_SERVICE;
        if (serverMsg != null) {
            part = serverMsg;
        } else {
            if (!(cxfSettings.getBodyPartToCheck().isEmpty())) {
                part = cxfSettings.getBodyPartToCheck();
            }
        }
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Service Cleint", null, titleToCheck);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Service Cleint", null, part);
        //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);

        return expectations;

    }

    public List<validationData> setErrorSAMLCXFExpectationsMatches(List<validationData> expectations, String flowType, SAMLTestSettings testSettings, String errorResponse) throws Exception {
        return setErrorSAMLCXFExpectationsWithTypeCheck(expectations, flowType, testSettings, errorResponse, SAMLConstants.STRING_MATCHES);
    }

    public List<validationData> setErrorSAMLCXFExpectations(List<validationData> expectations, String flowType, SAMLTestSettings testSettings, String errorResponse) throws Exception {
        return setErrorSAMLCXFExpectationsWithTypeCheck(expectations, flowType, testSettings, errorResponse, SAMLConstants.STRING_CONTAINS);
    }

    public List<validationData> setErrorSAMLCXFExpectationsWithTypeCheck(List<validationData> expectations, String flowType, SAMLTestSettings testSettings, String errorResponse, String checkType) throws Exception {

        // other tests will have validated the SAML Token and other steps and values in the SAML flow
        // this method will only set expectations for the CXF portion of a flow
        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        CXFSettings cxfSettings = testSettings.getCXFSettings();
        String titleToCheck = SAMLConstants.CXF_SAML_TOKEN_SERVLET;
        if (!(cxfSettings.getTitleToCheck().isEmpty())) {
            titleToCheck = cxfSettings.getTitleToCheck();
        }
        String part = SAMLConstants.CXF_SAML_TOKEN_SERVICE;
        if (errorResponse != null) {
            part = errorResponse;
        } else {
            if (!(cxfSettings.getBodyPartToCheck().isEmpty())) {
                part = cxfSettings.getBodyPartToCheck();
            }
        }
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Service Client", null, titleToCheck);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE, SAMLConstants.RESPONSE_FULL, checkType, "Service Client did not report the correct failure", null, part);
        //		expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_IDP_LOGIN, SAMLConstants.SAML_TOKEN, SAMLConstants.STRING_CONTAINS, "SAML Token did not contain expected values", null, null);

        return expectations;

    }

    public String getValueFromCookieInConversation(WebClient webClient, String key) throws Exception {

        Cookie cookie = webClient.getCookieManager().getCookie(key);
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();

    }

    public String getLastEntryInList(String[] theList) throws Exception {

        String Entry = null;
        for (String entry : theList) {
            Entry = entry;
        }
        return Entry;
    }

    public void setMarkEndOfLogs() throws Exception {

        // add other servers as we support them
        setMarkEndOfLog(testOIDCServer);
        setMarkEndOfLog(testSAMLOIDCServer);
        setMarkEndOfLog(testSAMLServer);
        setMarkEndOfLog(testIDPServer);
    }

    public void setMarkEndOfLog(SAMLTestServer theServer) throws Exception {

        if (theServer != null) {
            theServer.setMarkToEndOfLogs();
        }
    }

    public void runGetMethod(String testcase, Object startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {
        runGetMethod(testcase, startPage, settings, expectations, settings.getSpTargetApp());
    }

    public void runGetMethod(String testcase, Object startPage, SAMLTestSettings settings, List<validationData> expectations, String theApp) throws Exception {

        String thisMethod = "runGetMethod";
        msgUtils.printMethodName(thisMethod);

        setMarkEndOfLogs();

        String samlValue = null;

        // return the full Saml token, or just the SAML Assertion
        Boolean getFullSamlToken = false;
        if (settings.getRSSettings() != null && settings.getRSSettings().getSamlTokenFormat() == SAMLConstants.TOKEN_TEXT_ONLY) {
            getFullSamlToken = true;
        }
        if (startPage != null) {
            String samlAssertion = samlcttools.getSAMLTokenAssertionFromRequest(startPage, settings, getFullSamlToken);

            Log.info(thisClass, thisMethod, "Raw SAML Token to be passed: " + samlAssertion);

            if (settings.getRSSettings() != null) {
                samlValue = samlcttools.convertXmlToSamlResponse(samlAssertion, settings.getRSSettings().getSamlTokenFormat());
            } else {
                samlValue = samlcttools.convertXmlToSamlResponse(samlAssertion, SAMLConstants.ASSERTION_ENCODED);
            }
            Log.info(thisClass, thisMethod, "Token to be passed: " + samlValue);
        }

        //        HostnameVerifier verifier = new HostnameVerifier() {
        //            @Override
        //            public boolean verify(String urlHostname, SSLSession session) {
        //                return true;
        //            }
        //        };
        //        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
        //
        //        URL url = new URL(theApp);

        URL url = AutomationTools.getNewUrl(theApp);
        Log.info(thisClass, thisMethod, "URL connection to make: " + url.toString());

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            String authString = "Authorization";
            if ((settings.getRSSettings() != null) && (startPage != null)) {
                String samlNameForHeader = settings.getRSSettings().getHeaderName();
                if (samlNameForHeader != null) {
                    if (settings.getRSSettings().getHeaderFormat().equals(SAMLConstants.SAML_HEADER_1)) {
                        con.setRequestProperty("saml_name", authString);
                        con.setRequestProperty(authString, settings.getRSSettings().getHeaderName() + "=" + samlValue);
                        Log.info(thisClass, thisMethod, "Header format 1: " + authString + "=" + settings.getRSSettings().getHeaderName() + "=" + samlValue);
                    }
                    if (settings.getRSSettings().getHeaderFormat().equals(SAMLConstants.SAML_HEADER_2)) {
                        con.setRequestProperty("saml_name", authString);
                        con.setRequestProperty(authString, settings.getRSSettings().getHeaderName() + "=\"" + samlValue + "\"");
                        Log.info(thisClass, thisMethod, "Header format 2: " + authString + "=" + settings.getRSSettings().getHeaderName() + "=\"" + samlValue + "\"");
                    }
                    if (settings.getRSSettings().getHeaderFormat().equals(SAMLConstants.SAML_HEADER_3)) {
                        con.setRequestProperty("saml_name", authString);
                        con.setRequestProperty(authString, settings.getRSSettings().getHeaderName() + " " + samlValue);
                        Log.info(thisClass, thisMethod, "Header format 3: " + authString + "=" + settings.getRSSettings().getHeaderName() + " " + samlValue);
                    }
                    if (settings.getRSSettings().getHeaderFormat().equals(SAMLConstants.SAML_HEADER_4)) {
                        con.setRequestProperty("saml_name", settings.getRSSettings().getHeaderName());
                        con.setRequestProperty(settings.getRSSettings().getHeaderName(), samlValue);
                        Log.info(thisClass, thisMethod, "Header format 4: " + settings.getRSSettings().getHeaderName() + "=" + samlValue);
                    }
                } else {
                    Log.info(thisClass, thisMethod, "NOT Passing SAML Assertion on call");
                }
            } else {
                Log.info(thisClass, thisMethod, "NOT Passing SAML Assertion on call");
            }

            processResponse(testcase, SAMLConstants.INVOKE_JAXRS_GET, con, settings, expectations);

        } finally {
            con.disconnect();
        }
    }

    // Compress the input string, using Gzip

    public static byte[] compressString(String str) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();

        return out.toByteArray();

    }

    protected void processResponse(String testcase, String currentAction, HttpURLConnection connection, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "processResponse";
        msgUtils.printMethodName(thisMethod);

        validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);

        Log.info(thisClass, thisMethod, "Connection request: " + connection.toString());
        String responseContent = null;
        String responseHeaders = "";
        String fullResponseText = null;
        InputStream responseStream = null;
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   Start Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        int statusCode = connection.getResponseCode();
        Log.info(thisClass, thisMethod, "Response (StatusCode):  " + statusCode);

        String url = connection.getURL().toString();
        Log.info(thisClass, thisMethod, "Response (Url):  " + url);

        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            String value = header.getValue().toString().replaceAll("^\\[|\\]$", "");
            Log.info(thisClass, thisMethod, "Response (Header): " + header.getKey() + ": " + value);
            responseHeaders = responseHeaders + header.getKey() + ": " + value + " ; ";
        }

        String message = connection.getResponseMessage();
        Log.info(thisClass, thisMethod, "Response (Message):  " + message);

        if (statusCode == 200) {
            responseStream = connection.getInputStream();
        } else {
            responseStream = connection.getErrorStream();
        }
        if (responseStream != null) {
            char[] buffer = new char[1024];
            StringBuffer sb2 = new StringBuffer();
            InputStreamReader reader = new InputStreamReader(responseStream, "UTF-8");
            int bytesRead;
            do {
                Log.info(thisClass, thisMethod, "HttpUrlConnection buffer.length  " + buffer.length);
                bytesRead = reader.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    sb2.append(buffer, 0, bytesRead);
                }
            } while (bytesRead >= 0);
            reader.close();
            fullResponseText = new String(sb2.toString().trim());
            Log.info(thisClass, thisMethod, "Response (Full): " + fullResponseText);

            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        } else {
            Log.info(thisClass, thisMethod, "Response (Full):  No response body");
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            //			return "Response (Full):  No response body";
        }

        if (expectations == null) {
            Log.info(thisClass, thisMethod, "No expectations against which to validate");
            return;
        }

        Log.info(thisClass, thisMethod, "currentAction is: " + currentAction);

        //  Validate
        // can't use the normal validation routines as they're set up to process a WebResponse
        // Adding the specific checking here - may need to move it later if we end up with more URL Connection type calls
        // Had problems trying to process the stream multiple times - only got valid data on the first use (the next one was empty)
        for (validationData expected : expectations) {
            if (samlcttools.isInList(validationTools.validLogLocations, expected.getWhere())) {
                validationTools.validateWithServerLog(expected);
            } else {
                responseContent = null;
                if (currentAction.equals(expected.getAction())) {
                    Log.info(thisClass, thisMethod, "Validating action: " + currentAction);

                    if (expected.getWhere().equals(SAMLConstants.RESPONSE_STATUS)) {
                        responseContent = Integer.toString(statusCode);
                    } else {
                        if (expected.getWhere().equals(SAMLConstants.RESPONSE_FULL)) {
                            responseContent = fullResponseText;
                        } else {
                            if (expected.getWhere().equals(SAMLConstants.RESPONSE_HEADER)) {
                                responseContent = responseHeaders;
                            } else {
                                if (expected.getWhere().equals(SAMLConstants.RESPONSE_MESSAGE)) {
                                    responseContent = message;
                                }
                            }
                        }
                    }

                    validationTools.validateResponseContent(fullResponseText, responseContent, expected);
                }
            }
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void invokeSvcClient(String testcase, WebClient webClient, Object startPage, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeSvcClient";
        msgUtils.printMethodName(thisMethod);

        try {

            setMarkEndOfLogs();

            URL url = AutomationTools.getNewUrl(settings.getSpAlternateApp());
            WebRequest request = new WebRequest(url, HttpMethod.GET);

            request.setRequestParameters(new ArrayList());
            setRequestParameterIfSet(request, "targetApp", settings.getSpDefaultApp());
            setRequestParameterIfSet(request, "header", settings.getRSSettings().getHeaderName());
            setRequestParameterIfSet(request, "headerFormat", settings.getRSSettings().getHeaderFormat());
            setRequestParameterIfSet(request, "formatType", settings.getRSSettings().getSamlTokenFormat());

            msgUtils.printAllCookies(webClient);
            msgUtils.printRequestParts(webClient, request, thisMethod + ": " + testcase, "Outgoing request");

            Object thePage = webClient.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");
            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.INVOKE_JAXRS_GET_VIASERVICECLIENT, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.INVOKE_JAXRS_GET_VIASERVICECLIENT, e);
        }

    }

    /**
     * Adds the expectation to find the provided message in the specified log. Also adds logMessage to the list of ignored
     * server exceptions for this particular server.
     *
     * @param theServer
     *            - the server to register the allowed excpetion to.
     * @param expected
     * @param step
     * @param log
     * @param checkType
     * @param failureMessage
     * @param logMessage
     * @return
     * @throws Exception
     */
    public List<validationData> addMessageExpectation(SAMLTestServer theServer, List<validationData> expected, String step, String log, String checkType, String failureMessage, String logMessage) throws Exception {
        // Set the actual expectation to find the message in the server log
        expected = vData.addExpectation(expected, step, log, checkType, failureMessage, null, logMessage);

        // have the server ignore the expected error
        theServer.addIgnoredServerException(logMessage);

        return expected;
    }

    /**
     * I've been seeing an issue where HTMLUnit is not handling some of the log pages correctly.
     * When we look for the title in the response, it comes back null even though we can see the title when we print the entire
     * response.
     * And, we're also seeing an issue where it can't get the login form from the response
     *
     * *********** after more debugging, I think the issue is a timing problem - the javascript hasn't completed... and
     * the page isn't fully formed - add "waits" after invoking each frame request
     * - leaving this getForm0WithDebug method just in case
     *
     * @param startPage
     * @param testcase
     * @return
     * @throws Exception
     */
    public HtmlForm getForm0WithDebug(HtmlPage startPage, String testcase) throws Exception {

        String thisMethod = "getForm0WithDebug";
        HtmlForm form = null;
        try {
            HtmlForm tempForm = startPage.getForms().get(0);
            form = tempForm;
        } catch (Exception e) {
            try {
                Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
                Log.info(thisClass, testcase, "Searching for \"<form\" in the text version of the page");
                msgUtils.assertTrueAndLog(thisMethod, "Can NOT find the string \"<form\" in the page", AutomationTools.getResponseText(startPage).contains("<form"));
                Log.info(thisClass, testcase, "Found \"<form\" in the page - if getForms() fails, it is an issue with HTMLUnit");
                Log.info(thisClass, testcase, "Will try again to get the form");
                HtmlForm tempForm = startPage.getForms().get(0);
                form = tempForm;
            } catch (Exception e2) {
                Log.error(thisClass, testcase, e2, "Exception occurred in " + thisMethod);
                List<HtmlForm> allForms = startPage.getForms();
                if (allForms.size() == 0) {
                    fail("Can NOT find any forms on the login page");
                }
                HtmlForm tempForm = allForms.get(0);
                form = tempForm;
            }

        }
        return form;
    }

    public void genericInvokePage(String testcase, WebClient webClient, String requestedUrl, HttpMethod getOrPost, SAMLTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "genericInvokePage";
        msgUtils.printMethodName(thisMethod);

        try {

            setMarkEndOfLogs();

            URL url = AutomationTools.getNewUrl(requestedUrl);
            WebRequest request = new WebRequest(url, getOrPost);

            msgUtils.printAllCookies(webClient);
            msgUtils.printRequestParts(webClient, request, thisMethod + ": " + testcase, "Outgoing request");

            Object thePage = webClient.getPage(request);
            // make sure the page is processed before continuing
            waitBeforeContinuing(webClient);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, thisMethod + " response");
            validationTools.setServers(testSAMLServer, testSAMLOIDCServer, testOIDCServer, testAppServer, testIDPServer);
            validationTools.validateResult(webClient, thePage, SAMLConstants.GENERIC_INVOKE_PAGE, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, SAMLConstants.GENERIC_INVOKE_PAGE, e);
        }

    }

}
