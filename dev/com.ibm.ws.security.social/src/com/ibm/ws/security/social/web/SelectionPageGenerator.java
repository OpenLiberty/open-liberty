/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.SocialLoginWebappConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.ErrorHandlerImpl;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.ws.security.social.tai.SocialLoginTAI;
import com.ibm.ws.security.social.web.utils.ConfigInfoJsonBuilder;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;

public class SelectionPageGenerator {

    private static TraceComponent tc = Tr.register(SelectionPageGenerator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String HTML_CLASS_CONTAINER = "container";
    public static final String HTML_CLASS_SIDE = "side";
    public static final String HTML_CLASS_SIDE_LEFT = "left";
    public static final String HTML_CLASS_SIDE_RIGHT = "right";
    public static final String HTML_CLASS_MAIN_CONTENT = "main";
    public static final String HTML_CLASS_SOCIAL_MEDIA = "social_media";
    public static final String HTML_CLASS_FIELDSET = "fieldset";
    public static final String HTML_CLASS_LEGEND = "legend";
    public static final String HTML_CLASS_MEDIUM = "medium";
    public static final String HTML_CLASS_MID_SECTION = "mid_section";
    public static final String HTML_CLASS_OR = "or";
    public static final String HTML_CLASS_CREDENTIALS = "credentials";
    public static final String HTML_CLASS_BUTTONS = "buttons";
    public static final String HTML_CLASS_BUTTON = "button";
    public static final String HTML_CLASS_SUBMIT = "submit";
    public static final String HTML_CLASS_CRED_INPUTS = "inputs";
    public static final String HTML_CLASS_LABEL = "label";
    public static final String HTML_CLASS_CRED_INPUT = "cred_input";
    public static final String PARAM_ORIGINAL_REQ_URL = "request_url";
    public static final String PARAM_REQUEST_METHOD = "request_method";
    public static final String PARAM_CONFIG_JSON_DATA = "configuration";
    public static final String PARAM_SUBMIT_PARAM_NAME = "submit_param_name";

    public static final String J_SECURITY_CHECK = "j_security_check";
    public static final String J_USERNAME = "j_username";
    public static final String J_PASSWORD = "j_password";

    private final String createCookieFunctionName = "createHintCookie";

    private HttpServletRequest request = null;
    private Collection<SocialLoginConfig> selectableConfigs = null;
    private String targetUrl = null;
    private String requestMethod = null;
    private Map<String, String[]> parameterMap = null;

    SocialWebUtils webUtils = new SocialWebUtils();

    /**
     * Generates the sign in page to allow a user to select from the configured social login services. If no services are
     * configured, the user is redirected to an error page.
     *
     * @param request
     * @param response
     * @param socialTaiRequest
     * @throws IOException
     */
    public void displaySelectionPage(HttpServletRequest request, HttpServletResponse response, SocialTaiRequest socialTaiRequest) throws IOException {
        setRequestAndConfigInformation(request, response, socialTaiRequest);
        if (selectableConfigs == null || selectableConfigs.isEmpty()) {
            sendDisplayError(response, "SIGN_IN_NO_CONFIGS", new Object[0]);
            return;
        }
        generateOrSendToAppropriateSelectionPage(response);
    }

    void setRequestAndConfigInformation(HttpServletRequest request, HttpServletResponse response, SocialTaiRequest socialTaiRequest) {
        this.request = request;
        selectableConfigs = getSocialLoginConfigs(socialTaiRequest);
        targetUrl = getRequestUrl(request);
        requestMethod = request.getMethod();
        parameterMap = request.getParameterMap();

        saveRequestUrlAndParametersForLocalAuthentication(request, response);
    }

    /**
     * Returns a Collection of social login configurations that are configured to protect the original request.
     *
     * @param socialTaiRequest
     * @return
     */
    Collection<SocialLoginConfig> getSocialLoginConfigs(SocialTaiRequest socialTaiRequest) {
        return socialTaiRequest.getAllMatchingConfigs();
    }

    String getRequestUrl(HttpServletRequest request) {
        StringBuffer reqUrl = request.getRequestURL();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Got original request URL: [" + reqUrl.toString() + "]");
        }
        return reqUrl.toString();
    }

    /**
     * Saves the original request URL and POST parameters in the event that the user will use local authentication instead of
     * choosing a social media provider on the social media selection page.
     *
     * @param request
     * @param response
     * @param socialTaiRequest
     */
    void saveRequestUrlAndParametersForLocalAuthentication(HttpServletRequest request, HttpServletResponse response) {
        webUtils.saveRequestUrlAndParameters(request, response);
    }

    /**
     * Outputs the default, or redirects to the custom, selection page with selectable social media options based on the services
     * configured.
     *
     * @param response
     * @throws IOException
     */
    void generateOrSendToAppropriateSelectionPage(HttpServletResponse response) throws IOException {
        if (isCustomSelectionPageConfigured()) {
            redirectToCustomSelectionPage(response);
        } else {
            generateDefaultSelectionPage(response);
        }
    }

    boolean isCustomSelectionPageConfigured() {
        SocialLoginWebappConfig config = getSocialLoginWebappConfig();
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No webapp configuration present");
            }
            return false;
        }
        String selectionPageUrl = config.getSocialMediaSelectionPageUrl();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Got selection page URL: " + selectionPageUrl);
        }
        return (selectionPageUrl != null);
    }

    void redirectToCustomSelectionPage(HttpServletResponse response) throws IOException {
        SocialLoginWebappConfig config = getSocialLoginWebappConfig();
        if (config == null) {
            // Should redirect to the custom selection page, but the web application configuration is now missing. Use the default page instead.
            Tr.warning(tc, "CUSTOM_SELECTION_INITED_MISSING_WEBAPP_CONFIG");
            generateDefaultSelectionPage(response);
            return;
        }
        String socialMediaSelectionPageUrl = config.getSocialMediaSelectionPageUrl();
        redirectToCustomSelectionPageUrl(response, socialMediaSelectionPageUrl);
    }

    void redirectToCustomSelectionPageUrl(HttpServletResponse response, String url) throws IOException {
        // The URL is not validated because it's pulled directly from the web app config which already enforces proper URL format
        response.sendRedirect(url + "?" + buildCustomRedirectUriQuery());
    }

    String buildCustomRedirectUriQuery() {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ORIGINAL_REQ_URL, getOriginalRequestUrlWithRequestParameters());
        params.put(PARAM_REQUEST_METHOD, requestMethod);
        params.put(PARAM_SUBMIT_PARAM_NAME, ClientConstants.LOGIN_HINT);
        params.put(PARAM_CONFIG_JSON_DATA, getConfigInformationParameterString());

        return buildCustomRedirectUriQuery(params);
    }

    String buildCustomRedirectUriQuery(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        if (params != null) {
            Iterator<Entry<String, String>> iter = params.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, String> entry = iter.next();
                query.append(WebUtils.urlEncode(entry.getKey())).append("=").append(WebUtils.urlEncode(entry.getValue()));
                if (iter.hasNext()) {
                    query.append("&");
                }
            }
        }
        return query.toString();
    }

    String getOriginalRequestUrlWithRequestParameters() {
        StringBuilder url = new StringBuilder();
        if (targetUrl != null) {
            url.append(targetUrl);
        }
        if (parameterMap != null && !parameterMap.isEmpty()) {
            url.append("?");
            url.append(webUtils.getUrlEncodedQueryStringFromParameterMap(parameterMap));
        }
        return url.toString();
    }

    String getConfigInformationParameterString() {
        // Build a JSON object that includes information about all relevant social media configurations
        ConfigInfoJsonBuilder jsonBuilder = new ConfigInfoJsonBuilder(selectableConfigs);
        JSONObject configInformation = jsonBuilder.buildJsonResponse();
        return (configInformation == null) ? "" : configInformation.toString();
    }

    void generateDefaultSelectionPage(HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.print(createSignInHtml());
        writer.close();
    }

    String createSignInHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html " + getHtmlLang() + ">\n");
        html.append(createHtmlHead());
        html.append(createHtmlBody());
        html.append("</html>");
        return html.toString();
    }

    String getHtmlLang() {
        if (request != null) {
            return "lang=\"" + request.getLocale() + "\"";
        }
        return "";
    }

    String createHtmlHead() {
        StringBuilder html = new StringBuilder();
        html.append("<head>\n");
        html.append("<meta charset=\"utf-8\">\n");
        html.append("<title>" + WebUtils.htmlEncode(getHtmlTitle()) + "</title>\n");
        html.append(createCssContentString());
        html.append(createJavascript());
        html.append("</head>\n");
        return html.toString();
    }

    /**
     * Creates a JavaScript function for creating a social_login_hint cookie with the value provided to the function. Each
     * provider button should be configured to call this function when the button is clicked, allowing the login hint to be passed
     * around in a cookie instead of a request parameter.
     */
    String createJavascript() {
        StringBuilder html = new StringBuilder();
        html.append("<script>\n");
        html.append("function " + createCookieFunctionName + "(value) {\n");
        html.append("document.cookie = \"" + ClientConstants.LOGIN_HINT + "=\" + value;\n");
        html.append("}\n");
        html.append("</script>\n");
        return html.toString();
    }

    String getHtmlTitle() {
        return Tr.formatMessage(tc, request.getLocales(), "SELECTION_PAGE_TITLE");
    }

    String createHtmlBody() {
        StringBuilder html = new StringBuilder();
        html.append("<body>\n");
        html.append("<div class=\"" + HTML_CLASS_CONTAINER + "\">\n");
        html.append("<div class=\"" + HTML_CLASS_SIDE + " " + HTML_CLASS_SIDE_LEFT + "\"></div>\n");
        html.append(createHtmlMainContent());
        html.append("<div class=\"" + HTML_CLASS_SIDE + " " + HTML_CLASS_SIDE_RIGHT + "\"></div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        return html.toString();
    }

    String createHtmlMainContent() {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"" + HTML_CLASS_MAIN_CONTENT + "\">\n");
        html.append(createHtmlForSocialMediaSelections());
        if (isLocalAuthenticationEnabled()) {
            html.append(createHtmlForLocalAuthentication());
        }
        html.append("</div>\n");
        return html.toString();
    }

    boolean isLocalAuthenticationEnabled() {
        SocialLoginWebappConfig config = getSocialLoginWebappConfig();
        return (config != null && config.isLocalAuthenticationEnabled());
    }

    String createHtmlForLocalAuthentication() {
        StringBuilder html = new StringBuilder();
        html.append(createHtmlForMiddleSection());
        html.append(createHtmlForCredentials());
        return html.toString();
    }

    String createHtmlForSocialMediaSelections() {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"" + HTML_CLASS_SOCIAL_MEDIA + "\">\n");
        html.append("<header>" + WebUtils.htmlEncode(getPageHeader()) + "</header>\n");
        html.append("<div class=\"" + HTML_CLASS_BUTTONS + "\">\n");
        html.append(createHtmlFormWithButtons());
        html.append("</div>\n");
        html.append("</div>\n");
        return html.toString();
    }

    String getPageHeader() {
        return Tr.formatMessage(tc, request.getLocales(), "SELECTION_PAGE_HEADER");
    }

    String createHtmlFormWithButtons() {
        StringBuilder html = new StringBuilder();
        // Web browsers should default to GET for unknown "method" attributes for forms, so a null requestMethod will still work
        html.append("<form action=\"" + WebUtils.htmlEncode(targetUrl) + "\" method=\"" + requestMethod + "\">\n");

        if (selectableConfigs != null) {
            for (SocialLoginConfig config : selectableConfigs) {
                html.append(createButtonHtml(config));
            }
        }
        html.append(getHiddenInputHtmlForRequestParameters());

        html.append("</form>\n");
        return html.toString();
    }

    /**
     * Creates an HTML button string for the provided {@code SocialLoginConfig} object. Uses the {@code displayName} attribute in
     * the configuration as the display text for the button.
     */
    String createButtonHtml(SocialLoginConfig config) {
        if (config == null) {
            return "";
        }
        String uniqueId = config.getUniqueId();
        String displayName = config.getDisplayName();
        String buttonValue = WebUtils.htmlEncode(getObscuredConfigId(uniqueId));

        StringBuilder buttonHtml = new StringBuilder();
        buttonHtml.append("<button type=\"submit\" ");
        buttonHtml.append("class=\"" + HTML_CLASS_BUTTON + " " + HTML_CLASS_MEDIUM + "\" ");
        buttonHtml.append("value=\"" + buttonValue + "\" ");
        buttonHtml.append("onclick=\"" + createCookieFunctionName + "(" + buttonValue + ")\" ");
        buttonHtml.append(">");
        if (displayName == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "displayName was not configured for this config, will use the id attribute instead");
            }
            displayName = uniqueId;
        }
        buttonHtml.append(WebUtils.htmlEncode(displayName));
        buttonHtml.append("</button>\n");
        return buttonHtml.toString();
    }

    /**
     * Returns the obscured value that corresponds to the provided ID. Obscured configuration IDs are used in user-facing
     * situations where internal configuration data either must not or should not be exposed.
     */
    String getObscuredConfigId(String configId) {
        return SocialLoginTAI.getObscuredIdFromConfigId(configId);
    }

    String createHtmlForMiddleSection() {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"" + HTML_CLASS_MID_SECTION + "\">\n");
        html.append("<header class=\"" + HTML_CLASS_OR + "\">" + getMiddleSectionText() + "</header>\n");
        html.append("</div>\n");
        return html.toString();
    }

    String getMiddleSectionText() {
        return Tr.formatMessage(tc, request.getLocales(), "SELECTION_PAGE_ALTERNATE_TEXT");
    }

    String createHtmlForCredentials() {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"" + HTML_CLASS_CREDENTIALS + "\">\n");
        html.append(createJSecurityCheckForm());
        html.append("</div>\n");
        return html.toString();
    }

    String createJSecurityCheckForm() {
        StringBuilder html = new StringBuilder();
        html.append("<form action=\"" + J_SECURITY_CHECK + "\" method=\"POST\">\n");
        html.append(createCredentialFieldsetHtml());
        html.append("</form>\n");
        return html.toString();
    }

    String createCredentialFieldsetHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<fieldset>\n");
        html.append("<legend>" + getLocalAuthenticationLegendText() + "</legend>\n");
        html.append(createCredentialInputsDiv());
        html.append("</fieldset>\n");
        return html.toString();
    }

    String getLocalAuthenticationLegendText() {
        // TODO
        return "";
        //        return "Local authentication credentials";
        //        return Tr.formatMessage(tc, request.getLocales(), "SELECTION_PAGE_LOCAL_AUTH_LEGEND");
    }

    String createCredentialInputsDiv() {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"" + HTML_CLASS_CRED_INPUTS + "\">\n");
        html.append(createUsernameInputHtml());
        html.append(createPasswordInputHtml());
        html.append(createFormSubmitButtonHtml());
        html.append(getHiddenInputHtmlForRequestParameters());
        html.append("</div>\n");
        return html.toString();
    }

    String createUsernameInputHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<label for=\"" + J_USERNAME + "\">" + getUsernamePlaceholderText() + "</label>\n");
        html.append("<input id=\"" + J_USERNAME + "\" name=\"" + J_USERNAME + "\" class=\"" + HTML_CLASS_CRED_INPUT + "\" placeholder=\"" + getUsernamePlaceholderText() + "\" >\n");
        return html.toString();
    }

    String getUsernamePlaceholderText() {
        return Tr.formatMessage(tc, request.getLocales(), "SELECTION_PAGE_USERNAME");
    }

    String createPasswordInputHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<label for=\"" + J_PASSWORD + "\">" + getPasswordPlaceholderText() + "</label>\n");
        html.append("<input id=\"" + J_PASSWORD + "\" name=\"" + J_PASSWORD + "\" class=\"" + HTML_CLASS_CRED_INPUT + "\" placeholder=\"" + getPasswordPlaceholderText() + " \"type=\"password\" >\n");
        return html.toString();
    }

    String getPasswordPlaceholderText() {
        return Tr.formatMessage(tc, request.getLocales(), "SELECTION_PAGE_PASSWORD");
    }

    String createFormSubmitButtonHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<input type=\"submit\" class=\"" + HTML_CLASS_BUTTON + " " + HTML_CLASS_SUBMIT + "\" value=\"" + getSubmitButtonText() + "\" >\n");
        return html.toString();
    }

    String getSubmitButtonText() {
        return Tr.formatMessage(tc, request.getLocales(), "SELECTION_PAGE_SUBMIT");
    }

    /**
     * Creates hidden {@code <input>} HTML elements for any existing request parameters.
     */
    String getHiddenInputHtmlForRequestParameters() {
        StringBuilder html = new StringBuilder();
        if (parameterMap != null) {
            Set<Entry<String, String[]>> entries = parameterMap.entrySet();
            for (Entry<String, String[]> entry : entries) {
                html.append(getHiddenInputForRequestParam(entry));
            }
        }
        return html.toString();
    }

    String getHiddenInputForRequestParam(Entry<String, String[]> entry) {
        StringBuilder html = new StringBuilder();
        String key = entry.getKey();
        String[] strs = entry.getValue();
        if (strs != null && strs.length > 0) {
            for (String value : strs) {
                html.append(getHiddenInputHtml(key, value));
            }
        }
        return html.toString();
    }

    String getHiddenInputHtml(String paramKey, String paramValue) {
        StringBuilder html = new StringBuilder();
        html.append("<input type=\"hidden\" name=\"" + WebUtils.htmlEncode(paramKey, false, true, true) + "\" value=\"" + WebUtils.htmlEncode(paramValue, false, true, true) + "\" >\n");
        return html.toString();
    }

    /**
     * Traces that there was an error displaying the sign in page, and includes the provided NLS message information to include as
     * an insert (e.g. Will output "CWWKSxxxxE: The default social media sign in page cannot be displayed. {0}", where {0} is
     * replaced with the properly formatted NLS message using the {@code msgKey} and {@code args} provided). Then redirects the
     * response to an error page with a generic front-end failure message.
     *
     * @param response
     * @param msgKey
     * @param args
     * @throws IOException
     */
    void sendDisplayError(HttpServletResponse response, String msgKey, Object... args) throws IOException {
        if (msgKey == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No message key was provided for sub-message, so no sub-message will be included");
            }
            msgKey = "";
        }
        String subMsg = Tr.formatMessage(tc, msgKey, args);
        Tr.error(tc, "ERROR_DISPLAYING_SIGN_IN_PAGE", subMsg);
        ErrorHandlerImpl.getInstance().handleErrorResponse(response);
    }

    /**
     * Creates the CSS content string to be used to format the sign in page.
     *
     * @return
     */
    String createCssContentString() {
        StringBuilder css = new StringBuilder();
        css.append("<style>\n");

        // html, body
        css.append("html, body {");
        css.append("margin: 0;");
        css.append("}\n");
        // body
        css.append("body {");
        css.append("background-color: #152935;");
        css.append("font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;");
        css.append("}\n");
        // .container
        css.append("." + HTML_CLASS_CONTAINER + " {");
        css.append("display: flex;");
        css.append("min-height: 100vh;");
        css.append("}\n");
        // .side
        css.append("." + HTML_CLASS_SIDE + " {");
        css.append("max-width: 33%;");
        css.append("flex: 1;");
        css.append("display: flex;");
        css.append("}\n");
        // .main
        css.append("." + HTML_CLASS_MAIN_CONTENT + " {");
        css.append("background-color: white;");
        css.append("min-width: 300px;");
        css.append("max-width: 40%;");
        css.append("padding: 0px 20px;");
        css.append("}\n");
        // .social_media
        css.append("." + HTML_CLASS_SOCIAL_MEDIA + " {");
        css.append("padding-top: 50px;");
        css.append("}\n");
        // header
        css.append("header {");
        css.append("color: #152935;");
        css.append("font-size: 1.2em;");
        css.append("font-weight: bold;");
        css.append("text-align: center;");
        css.append("padding-bottom: 10px;");
        css.append("}\n");
        // .buttons
        css.append("." + HTML_CLASS_BUTTONS + " {");
        css.append("display: flex;");
        css.append("flex-flow: row wrap;");
        css.append("justify-content: center;");
        css.append("text-align: center;");
        css.append("}\n");
        // fieldset
        css.append(HTML_CLASS_FIELDSET + " {");
        css.append("border: 0;");
        css.append("padding: 0;");
        css.append("}\n");
        // legend
        css.append(HTML_CLASS_LEGEND + " {");
        css.append("color: #152935;");
        css.append("padding-bottom: 10px;");
        css.append("}\n");
        // .button
        css.append("." + HTML_CLASS_BUTTON + " {");
        css.append("background-color: white;");
        css.append("border: 2px solid #2A4E7B;");
        css.append("cursor: pointer;");
        css.append("font-weight: 200;");
        css.append("font-size: 0.9em;");
        css.append("padding: 10px 30px;");
        css.append("text-align: center;");
        css.append("}\n");
        // .button:hover, .button:focus
        css.append("." + HTML_CLASS_BUTTON + ":hover, ." + HTML_CLASS_BUTTON + ":focus {");
        css.append("background-color: #4178BE;");
        css.append("color: white;");
        css.append("}\n");
        // .medium
        css.append("." + HTML_CLASS_MEDIUM + " {");
        css.append("margin: 10px;");
        css.append("width: 250px;");
        css.append("}\n");
        // .mid_section
        css.append("." + HTML_CLASS_MID_SECTION + " {");
        css.append("padding: 30px 0px;");
        css.append("text-align: center");
        css.append("}\n");
        // .or
        css.append("." + HTML_CLASS_OR + " {");
        css.append("overflow: hidden;");
        css.append("text-align: center;");
        css.append("}\n");
        // .or:before, .or:after
        css.append("." + HTML_CLASS_OR + ":before, ." + HTML_CLASS_OR + ":after {");
        css.append("background-color: #C0C0C0;");
        css.append("content: \"\";");
        css.append("display: inline-block;");
        css.append("height: 1px;");
        css.append("position: relative;");
        css.append("vertical-align: middle;");
        css.append("width: 47%;");
        css.append("}\n");
        // .or:before
        css.append("." + HTML_CLASS_OR + ":before {");
        css.append("right: 0.5em;");
        css.append("margin-left: -40%;");
        css.append("}\n");
        // .or:after
        css.append("." + HTML_CLASS_OR + ":after {");
        css.append("left: 0.5em;");
        css.append("margin-right: -40%;");
        css.append("}\n");
        // .credentials .inputs
        css.append("." + HTML_CLASS_CREDENTIALS + " ." + HTML_CLASS_CRED_INPUTS + " {");
        css.append("padding: 0px 20px;");
        css.append("}\n");
        // label
        css.append(HTML_CLASS_LABEL + " {");
        css.append("font-size: 0.8em;");
        css.append("color: #777;");
        css.append("}\n");
        // .cred_input
        css.append("." + HTML_CLASS_CRED_INPUT + " {");
        css.append("box-sizing: border-box;");
        css.append("margin-bottom: 15px;");
        css.append("padding: 10px;");
        css.append("max-width: none;");
        css.append("width: 100%;");
        css.append("}\n");
        // .submit
        css.append("." + HTML_CLASS_SUBMIT + " {");
        css.append("width: 100%;");
        css.append("}\n");

        css.append("</style>\n");
        return css.toString();
    }

    SocialLoginWebappConfig getSocialLoginWebappConfig() {
        return SocialLoginTAI.getSocialLoginWebappConfig();
    }

}
