package com.ibm.ws.security.fat.common.web;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;

public class WebResponseUtils {
    public static Class<?> thisClass = WebResponseUtils.class;

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
        WebResponse response = getWebResponseObject(pageOrResponse);
        if (response != null) {
            return response.getContentAsString();
        }
        return null;
    }

    private static WebResponse getWebResponseObject(Object pageOrResponse) throws Exception {
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            return ((com.gargoylesoftware.htmlunit.WebResponse) pageOrResponse);
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getWebResponse();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getWebResponse();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getWebResponse();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getWebResponse();
        }
        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static int getResponseStatusCode(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseStatusCode", "pageOrResponse is null");
            return -1;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getResponseCode();
        }
        WebResponse response = getWebResponseObject(pageOrResponse);
        if (response != null) {
            return response.getStatusCode();
        }
        return -1;
    }

    public static String getResponseTitle(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseTitle", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getTitle();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            throw new Exception("Getting title not supported with type: " + pageOrResponse.getClass().getName());
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getTitleText();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return pageOrResponse.getClass().getName() + " has no title";
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return pageOrResponse.getClass().getName() + " has no title";
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return pageOrResponse.getClass().getName() + " has no title";
        }
        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static String getResponseUrl(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseUrl", "pageOrResponse is null");
            return null;
        }
        URL url = getResponseUrlObject(pageOrResponse);
        if (url != null) {
            return url.toString();
        }
        return null;
    }

    private static URL getResponseUrlObject(Object pageOrResponse) throws Exception {
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getURL();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.WebResponse) {
            throw new Exception("Getting URL not supported with type: " + pageOrResponse.getClass().getName());
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return ((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse).getUrl();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.TextPage) {
            return ((com.gargoylesoftware.htmlunit.TextPage) pageOrResponse).getUrl();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.xml.XmlPage) {
            return ((com.gargoylesoftware.htmlunit.xml.XmlPage) pageOrResponse).getUrl();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.UnexpectedPage) {
            return ((com.gargoylesoftware.htmlunit.UnexpectedPage) pageOrResponse).getUrl();
        }
        throw new Exception("Unknown response type: " + pageOrResponse.getClass().getName());
    }

    public static String getResponseMessage(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseMessage", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getResponseMessage();
        }
        WebResponse response = getWebResponseObject(pageOrResponse);
        if (response != null) {
            return response.getStatusMessage();
        }
        return null;
    }

    public static String[] getResponseHeaderNames(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseHeaderNames", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getHeaderFieldNames();
        }
        List<NameValuePair> headerMap = getResponseHeaderList(pageOrResponse);
        return convertHeaderListToNameArray(headerMap);
    }

    public static List<NameValuePair> getResponseHeaderList(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseHeaderList", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            throw new Exception("Getting response header list not supported with type: " + pageOrResponse.getClass().getName());
        }
        List<NameValuePair> headerList = null;
        WebResponse response = getWebResponseObject(pageOrResponse);
        if (response != null) {
            headerList = response.getResponseHeaders();
        }
        return headerList;
    }

    static String[] convertHeaderListToNameArray(List<NameValuePair> headerList) {
        if (headerList == null) {
            return new String[0];
        }
        String[] returnList = new String[headerList.size()];
        int count = 0;
        for (NameValuePair headerNameAndValue : headerList) {
            returnList[count] = headerNameAndValue.getName();
            count++;
        }
        return returnList;
    }

    public static String getResponseHeaderField(Object pageOrResponse, String headerName) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseHeaderField", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getHeaderField(headerName);
        }
        WebResponse response = getWebResponseObject(pageOrResponse);
        if (response != null) {
            return response.getResponseHeaderValue(headerName);
        }
        return null;
    }

    public static String[] getResponseCookieNames(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseCookieNames", "pageOrResponse is null");
            return null;
        }
        if (pageOrResponse instanceof com.meterware.httpunit.WebResponse) {
            return ((com.meterware.httpunit.WebResponse) pageOrResponse).getNewCookieNames();
        }
        if (pageOrResponse instanceof com.gargoylesoftware.htmlunit.html.HtmlPage) {
            return getCookieNamesFromHtmlPage((com.gargoylesoftware.htmlunit.html.HtmlPage) pageOrResponse);
        }
        throw new Exception("Getting cookie names not supported with type: " + pageOrResponse.getClass().getName() + " (cookies come from webClient)");
    }

    static String[] getCookieNamesFromHtmlPage(com.gargoylesoftware.htmlunit.html.HtmlPage page) throws Exception {
        WebClient webClient = page.getWebClient();
        if (webClient == null) {
            throw new Exception("Cannot get cookies in response because the web client cannot be found.");
        }
        CookieManager cookieManager = webClient.getCookieManager();
        if (cookieManager == null) {
            throw new Exception("Cannot get cookies in response because the cookie manager cannot be found.");
        }
        Set<Cookie> cookies = cookieManager.getCookies();
        if (cookies == null) {
            return new String[0];
        }
        List<String> cookieNames = new ArrayList<String>();
        for (Cookie cookie : cookies) {
            if (cookie == null) {
                continue;
            }
            cookieNames.add(cookie.getName());
        }
        return cookieNames.toArray(new String[cookieNames.size()]);
    }

    public static boolean getResponseIsHtml(Object pageOrResponse) throws Exception {
        if (pageOrResponse == null) {
            Log.info(thisClass, "getResponseIsHtml", "pageOrResponse is null");
            return false;
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

}
