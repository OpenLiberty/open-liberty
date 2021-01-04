/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import static org.junit.Assert.fail;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CookieTools;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.cookies.Cookie;

public class CommonCookieTools extends CookieTools {

    private static final Class<?> thisClass = CommonCookieTools.class;
    protected static CommonTestHelpers helpers = new CommonTestHelpers();

    /**
     * Build the default cookie name
     *
     * @return
     * @throws Exception
     */
    public String buildDefaultCookieName(TestServer testServer, String testServerName) throws Exception {

        String thisMethod = "buildDefaultOPCookieName";
        String hostName = java.net.InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        if (hostName == null) {
            hostName = "localhost";
        }
        String usrLocation = testServer.getServer().getUserDir();
        Log.info(thisClass, thisMethod, "usrLocation: '" + usrLocation + " Sniffed server name:  " + testServer.getServer().getServerName());
        usrLocation = usrLocation.replace('\\', '/');
        String slash = usrLocation.endsWith("/") ? "" : "/";
        String serverUniq = hostName + "_" + usrLocation + slash + "servers/" + testServerName;
        Log.info(thisClass, thisMethod, "serverUniq cookieLongName:'" + serverUniq + "'");
        // use string of server identity
        String defaultOPCookieName = "WAS_" + hash(serverUniq);
        // use hash of server identity
        //		int serverUniqHash = serverUniq.hashCode();
        //		String defaultOPCookieName = "WAS_" + Integer.toString(serverUniqHash) ;
        Log.info(thisClass, thisMethod, "Default Cookie Name: " + defaultOPCookieName);
        return defaultOPCookieName;
    }

    /**
     * validate the token using the appropriate cookie name
     *
     * @param wc
     * @param expectedCookieName
     * @throws Exception
     */
    public void validateCookie(WebConversation wc, String expectedCookieName) throws Exception {

        validateCookie(wc, expectedCookieName, true);
    }

    public void validateCookie(WebConversation wc, String expectedCookieName, Boolean shouldExist) throws Exception {
        String[] cookies = wc.getCookieNames();
        // for debugging pupposes, lets loop through them all, printing/logging...
        //		Boolean wrongTokenFound = false ;
        for (String cookie : cookies) {
            Log.info(thisClass, "validateCookieName", "Response Cookie Name: " + cookie + " Value: " + wc.getCookieValue(cookie));
            //			if (cookie == Constants.LTPA_TOKEN) {
            //				wrongTokenFound = true ;
            //			}
            //		}
            //		if (wrongTokenFound) {
            //			fail("Found the wrong LTPA token in the response.  Found " + Constants.LTPA_TOKEN + " and should not have") ;
        }
        // do we have a cookie with appropriate name and does that cookie contain a value
        if (!helpers.hasValue(wc.getCookieValue(expectedCookieName))) {
            if (shouldExist) {
                fail("Did not find value for expected cookie name: " + expectedCookieName);
            }
        } else {
            if (!shouldExist) {
                fail("Found a value for expected cookie name: " + expectedCookieName + " and we should NOT have");

            }
        }
    }

    static String hash(String stringToEncrypt) {
        int hashCode = stringToEncrypt.hashCode();
        if (hashCode < 0) {
            hashCode = hashCode * -1;
            return "n" + hashCode;
        } else {
            return "p" + hashCode;
        }
    }

    public void clearExpiredCookies(WebConversation wc) throws Exception {

        String[] cookieNames = wc.getCookieNames();
        for (String name : cookieNames) {
            if (name != null) {
                Cookie details = wc.getCookieDetails(name);
                if (details != null) {
                    Log.info(thisClass, "clearExpiredCookies", "Cookie: " + name + " is expired: " + details.isExpired());
                    Log.info(thisClass, "clearExpiredCookies", "Cookie: " + name + " is expired: " + details.getExpiredTime());
                    if (details.isExpired()) {
                        Log.info(thisClass, "clearExpiredCookies", "Clearing: " + name);
                        wc.putCookie(name, null);
                    }
                }
            }
        }
    }

    public com.gargoylesoftware.htmlunit.util.Cookie retrieveCookie(WebClient wc, String cookieName) {

        CookieManager cookieManager = wc.getCookieManager();
        com.gargoylesoftware.htmlunit.util.Cookie cookie = cookieManager.getCookie(cookieName);
        return cookie;

    }

    public String retrieveCookieValue(WebClient wc, String cookieName) {

        com.gargoylesoftware.htmlunit.util.Cookie cookie = retrieveCookie(wc, cookieName);
        if (cookie != null) {
            return cookie.getValue();
        }
        Log.info(thisClass, "retrieveCookieValue", "The cookie wasn't found");
        return null;
    }

    public void addCookieToWebClient(WebClient wc, com.gargoylesoftware.htmlunit.util.Cookie cookie) {

        CookieManager cm = wc.getCookieManager();
        cm.addCookie(cookie);

    }
}