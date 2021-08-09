/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.fat.common.utils;

import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;

public class AutomationTools {
    public static Class<?> thisClass = AutomationTools.class;

    public static String getResponseText(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseText", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof String) {
            return (String) pageOrResponse;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getText();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            return ((com.gargoylesoftware.htmlunit.WebResponse) pageOrResponse).getContentAsString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getWebResponse().getContentAsString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getWebResponse().getContentAsString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getWebResponse().getContentAsString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getWebResponse().getContentAsString();
        }

        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static int getResponseStatusCode(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseStatusCode", "pageOrResponse is null");
            return 999;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getResponseCode();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            return ((com.gargoylesoftware.htmlunit.WebResponse) pageOrResponse).getStatusCode();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getWebResponse().getStatusCode();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getWebResponse().getStatusCode();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getWebResponse().getStatusCode();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getWebResponse().getStatusCode();
        }
        Log.info(thisClass, "getResponseStatusCode", "Page returned is of type:  " + pageOrResponse.getClass().getName());
        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static String getResponseTitle(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseTitle", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof String) {
            return (String) pageOrResponse;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getTitle();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            //                return ((com.gargoylesoftware.htmlunit.WebResponse) response).getContentAsString();
            throw new Exception("get Title not supported with type: com.gargoylesoftware.htmlunit.WebResponse");
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getTitleText();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return "TextPage has no Title";
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return "XmlPage has no Title";
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return "";
        }
        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static String getResponseUrl(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseUrl", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof String) {
            return (String) pageOrResponse;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getURL().toString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            //                return ((com.gargoylesoftware.htmlunit.WebResponse) response).getContentAsString();
            throw new Exception("get URL not supported with type: com.gargoylesoftware.htmlunit.WebResponse");
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getUrl().toString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getUrl().toString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getUrl().toString();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getUrl().toString();
        }
        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static String getResponseMessage(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseMessage", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof String) {
            return (String) pageOrResponse;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getResponseMessage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            return ((com.gargoylesoftware.htmlunit.WebResponse) pageOrResponse).getStatusMessage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getWebResponse().getStatusMessage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getWebResponse().getStatusMessage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getWebResponse().getStatusMessage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getWebResponse().getStatusMessage();
        }

        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    // may want/need to add other versions of this that actually return the name/value pairs...
    public static String[] getResponseHeaderNames(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseHeaderNames", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getHeaderFieldNames();
        } else {
            List<NameValuePair> headerMap = null;
            if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
                headerMap = ((com.gargoylesoftware.htmlunit.WebResponse) pageOrResponse).getResponseHeaders();
            }
            if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
                headerMap = ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getWebResponse().getResponseHeaders();
            }
            if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
                headerMap = ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getWebResponse().getResponseHeaders();
            }
            if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
                headerMap = ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getWebResponse().getResponseHeaders();
            }
            if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
                headerMap = ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getWebResponse().getResponseHeaders();
            }

            if (headerMap == null) {
                throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
            }
            //            Log.info(thisClass, "getResponseHeaderName", "headers: " + headerMap.toString());
            //            Log.info(thisClass, "getResponseHeaderName", "number of headers: " + headerMap.size());
            String[] returnList = new String[headerMap.size()];
            int count = 0;
            for (NameValuePair x : headerMap) {
                returnList[count] = x.getName();
                count++;
            }
            return returnList;
        }
    }

    public static String getResponseHeaderField(Object pageOrResponse, String headerName) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseHeaderField", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof String) {
            return (String) pageOrResponse;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getHeaderField(headerName);
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            return ((com.gargoylesoftware.htmlunit.WebResponse) pageOrResponse).getResponseHeaderValue(headerName);
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getWebResponse().getResponseHeaderValue(headerName);
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getWebResponse().getResponseHeaderValue(headerName);
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getWebResponse().getResponseHeaderValue(headerName);
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getWebResponse().getResponseHeaderValue(headerName);
        }

        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static String[] getResponseCookieNames(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseCookieNames", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getNewCookieNames();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            //                return ((com.gargoylesoftware.htmlunit.WebResponse) response).getContentAsString();
            throw new Exception("get CookieNames not supported with type: com.gargoylesoftware.htmlunit.WebResponse (cookies come from webClient)");
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            Set<Cookie> cookies = ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getWebClient().getCookieManager().getCookies();
            String[] cookieNames = new String[cookies.size()];
            int i = 0;
            for (Cookie cookie : cookies) {
                cookieNames[i] = cookie.getName();
                i++;
            }
            return cookieNames;
//            throw new Exception("get CookieNames not supported with type: com.gargoylesoftware.htmlunit.html.HtmlPage (cookies come from webClient)");
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            throw new Exception("get CookieNames not supported with type: com.gargoylesoftware.htmlunit.UnexpectedPage (cookies come from webClient)");
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            throw new Exception("get CookieNames not supported with type: com.gargoylesoftware.htmlunit.TextPage (cookies come from webClient)");
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            throw new Exception("get CookieNames not supported with type: com.gargoylesoftware.htmlunit.XmlPage (cookies come from webClient)");
        }

        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static Boolean getResponseIsHtml(Object pageOrResponse) throws Exception {

        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseIsHtml", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).isHTML();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            // let's assume true for now
            return true;
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).isHtmlPage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).isHtmlPage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).isHtmlPage();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).isHtmlPage();
        }

        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static URL getNewUrl(String theApp) throws Exception {

        HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);

        URL url = new URL(theApp);

        return url;
    }

    /**
     * Some of our failure messages will only end up including minimal information if an expectation check fails. In the case of
     * an unexpected response status, for instance, the failure message will essentially just say "Expected status code X but got
     * status code Y." If the failure message also included the web response, it should save us some debugging time and headaches
     * when multiple defects are opened for the same failure, or when wildly different problems are reported under the same defect
     * because the test failure messages were all the same.
     */
    public static String getFullResponseContentForFailureMessage(Object response, String expectedWhere) throws Exception {
        if (Constants.RESPONSE_FULL.equals(expectedWhere)) {
            return "";
        }
        try {
            return " Full response content was: [" + AutomationTools.getResponseText(response) + "].";
        } catch (Exception e) {
            return " (Failed to read the response text due to exception: " + e + ").";
        }
    }

}
