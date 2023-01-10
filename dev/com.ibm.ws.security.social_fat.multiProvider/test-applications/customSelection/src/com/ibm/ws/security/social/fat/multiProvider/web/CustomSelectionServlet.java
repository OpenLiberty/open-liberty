/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.multiProvider.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;

@SuppressWarnings("serial")
public class CustomSelectionServlet extends HttpServlet {

    public static final String PARAM_ORIGINAL_REQ_URL = "request_url";
    public static final String PARAM_REQUEST_METHOD = "request_method";
    public static final String PARAM_SUBMIT_PARAM_NAME = "submit_param_name";
    public static final String PARAM_CONFIG_JSON_DATA = "configuration";
    public static final String KEY_SOCIAL_MEDIA_ID = "id";
    public static final String KEY_SOCIAL_MEDIA_WEBSITE = "website";
    public static final String KEY_SOCIAL_MEDIA_DISPLAY_NAME = "display-name";
    public static final String KEY_ALL_SOCIAL_MEDIA = "social-media";

    protected static String TITLE = "Custom Selection Page";
    protected static String HEADER_TEXT = "Welcome to the custom social media selection page";

    protected HttpServletRequest request = null;
    protected String formTarget = null;
    protected String requestMethod = null;
    protected String configurationInfo = null;
    protected String formSubmitParamName = null;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.request = request;

        PrintWriter writer = response.getWriter();

        try {
            recordRequestParameters();
        } catch (Exception e) {
            throw new ServletException("An error occurred while recording request parameters: " + e.getMessage(), e);
        }

        writer.println(getHtmlPage());
        writer.flush();
        writer.close();
    }

    void recordRequestParameters() throws Exception {
        List<String> missingParams = new ArrayList<String>();
        formTarget = request.getParameter(PARAM_ORIGINAL_REQ_URL);
        if (formTarget == null) {
            missingParams.add(PARAM_ORIGINAL_REQ_URL);
        }
        requestMethod = request.getParameter(PARAM_REQUEST_METHOD);
        if (requestMethod == null) {
            missingParams.add(PARAM_REQUEST_METHOD);
        }
        formSubmitParamName = request.getParameter(PARAM_SUBMIT_PARAM_NAME);
        if (formSubmitParamName == null) {
            missingParams.add(PARAM_SUBMIT_PARAM_NAME);
        }
        configurationInfo = request.getParameter(PARAM_CONFIG_JSON_DATA);
        if (configurationInfo == null) {
            missingParams.add(PARAM_CONFIG_JSON_DATA);
        }

        if (!missingParams.isEmpty()) {
            throw new Exception("The request is missing the following parameters: " + missingParams);
        }
    }

    String getHtmlPage() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append(getHtmlHead());
        html.append(getHtmlBody());
        html.append("</html>\n");
        return html.toString();
    }

    String getHtmlHead() {
        StringBuilder html = new StringBuilder();
        html.append("<head>\n");
        html.append("<meta charset=\"utf-8\">\n");
        html.append("<title>" + TITLE + "</title>\n");
        html.append("</head>\n");
        return html.toString();
    }

    String getHtmlBody() {
        StringBuilder html = new StringBuilder();
        html.append("<body>\n");
        html.append("<h1>" + HEADER_TEXT + "</h1>\n");
        html.append(outputRequestParameters());
        html.append(getSubmissionForm());
        html.append("</body>\n");
        return html.toString();
    }

    String outputRequestParameters() {
        StringBuilder html = new StringBuilder();
        html.append("<h3>Request parameters</h3>\n");
        html.append("<p>\n" + getRequestParametersString());
        return html.toString();
    }

    String getRequestParametersString() {
        StringBuilder html = new StringBuilder();
        html.append("<pre>\n");
        Map<String, String[]> params = request.getParameterMap();
        for (Entry<String, String[]> param : params.entrySet()) {
            for (String value : param.getValue()) {
                html.append("param[" + param.getKey() + "]: [" + value + "]\n");
            }
        }
        html.append("</pre>\n");
        return html.toString();
    }

    String getSubmissionForm() {
        StringBuilder html = new StringBuilder();
        html.append("Sign in with one of the following providers:\n");

        String targetUrl = extractTargetUrlFromOriginalRequestUrl();

        // Use the original target URL and request method provided in the request parameters to construct the appropriate form
        html.append("<form action=\"" + targetUrl + "\" method=\"" + requestMethod + "\">\n");
        html.append(getMediaButtonOptions());
        html.append(getHiddenRequestParameterInputs());
        html.append("</form>\n");

        return html.toString();
    }

    String extractTargetUrlFromOriginalRequestUrl() {
        if (formTarget.contains("?")) {
            return formTarget.substring(0, formTarget.indexOf("?"));
        }
        return formTarget;
    }

    String getHiddenRequestParameterInputs() {
        StringBuilder html = new StringBuilder();
        Map<String, List<String>> requestParams = extractRequestParametersFromOriginalRequestUrl();
        if (requestParams != null) {
            for (Entry<String, List<String>> param : requestParams.entrySet()) {
                html.append(getHiddenRequestParameterInputHtml(param.getKey(), param.getValue()));
            }
        }
        return html.toString();
    }

    Map<String, List<String>> extractRequestParametersFromOriginalRequestUrl() {
        if (!formTarget.contains("?")) {
            return null;
        }

        Map<String, List<String>> paramMap = new HashMap<String, List<String>>();

        String rawQueryString = formTarget.substring(formTarget.indexOf("?") + 1);
        String[] paramsSplit = rawQueryString.split("&");
        for (String paramAndValue : paramsSplit) {
            addParameterToMap(paramMap, paramAndValue);
        }
        return paramMap;
    }

    void addParameterToMap(Map<String, List<String>> paramMap, String paramAndValue) {
        try {
            String[] keyAndValue = paramAndValue.split("=");
            String name = URLDecoder.decode(keyAndValue[0], "UTF-8");
            String value = "";
            if (keyAndValue.length > 1) {
                value = URLDecoder.decode(keyAndValue[1], "UTF-8");
            }
            List<String> values = new ArrayList<String>();
            if (paramMap.containsKey(name)) {
                values = paramMap.get(name);
            }
            values.add(value);
            System.out.println("Setting parameter [" + name + "] values: " + values);
            paramMap.put(name, values);
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 must be supported
        }
    }

    String getHiddenRequestParameterInputHtml(String paramName, List<String> paramValues) {
        StringBuilder html = new StringBuilder();
        for (String value : paramValues) {
            html.append("<input type=\"hidden\" name=\"" + StringEscapeUtils.escapeHtml4(paramName) + "\" value=\"" + StringEscapeUtils.escapeHtml4(value) + "\" >\n");
        }
        return html.toString();
    }

    String getMediaButtonOptions() {
        StringBuilder html = new StringBuilder();
        JsonArray socialMedia = getSocialMediaArray();

        html.append("Raw social media data:\n");
        html.append("<pre>\n" + (socialMedia == null ? "&lt;null&gt;" : socialMedia.toString()) + "\n</pre>\n");

        html.append(getButtonsHtmlFromSocialMediaArray(socialMedia));

        return html.toString();
    }

    JsonArray getSocialMediaArray() {
        JsonObject configInfoObj = parseConfigurationInfo();
        return configInfoObj.getJsonArray(KEY_ALL_SOCIAL_MEDIA);
    }

    /**
     * Configuration info provided in the request is a JSON string containing the necessary IDs and display values for the all of
     * the available social media providers.
     */
    JsonObject parseConfigurationInfo() {
        JsonReader reader = Json.createReader(new StringReader(configurationInfo));
        return reader.readObject();
    }

    String getButtonsHtmlFromSocialMediaArray(JsonArray socialMedia) {
        StringBuilder html = new StringBuilder();
        if (socialMedia == null) {
            return html.toString();
        }

        Iterator<JsonValue> iter = socialMedia.iterator();
        while (iter.hasNext()) {
            JsonObject mediumData = (JsonObject) iter.next();
            html.append(getSocialMediaButton(mediumData));
        }

        return html.toString();
    }

    String getSocialMediaButton(JsonObject socialMedium) {
        StringBuilder html = new StringBuilder();

        String id = socialMedium.getString(KEY_SOCIAL_MEDIA_ID);
        String displayName = socialMedium.getString(KEY_SOCIAL_MEDIA_DISPLAY_NAME);

        // The formSubmitParamName should map to "social_login_hint" - it's the parameter name we check for that contains the desired config ID to use for authentication
        html.append("<button type=\"submit\" name=\"" + formSubmitParamName + "\" value=\"" + id + "\">" + displayName + "</button>\n");

        return html.toString();
    }

}
