/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.WebConversation;

public class CookieTools {

    private static final Class<?> thisClass = CookieTools.class;

    /**
     * removes a cookie from the conversation - just setting the cookie value to null should be sufficient
     * as there is no real removeCookie in WebConversation - other issues occur when we try to create a new
     * conversation an copy all but the cookie we want to remove
     * 
     * @param wc - the conversation
     * @param cookieToRemove - the cookie to remove
     * @return - the updated conversation
     * @throws Exception
     */
    public WebConversation removeCookieFromConverstation(WebConversation wc, String cookieToRemove) throws Exception {

        Log.info(thisClass, "removeCookieFromConverstation", "Removing cookie " + cookieToRemove + " from web conversation");
        wc.putCookie(cookieToRemove, null);
        return wc;
    }

    /**
     * return the cookie value
     * 
     * @param wc - the conversation to get the cookie out of
     * @param cookie - the cookie name to retrieve the value for
     * @return - the string value of the cookie
     * @throws Exception
     */
    public String getCookieValue(WebConversation wc, String cookie) throws Exception {
        return getCookieValue(wc, cookie, false);
    }

    /**
     * return the full cookie name - get the first cookie name that starts with the name passed
     * 
     * @param wc - the conversation to get the cookie out of
     * @param cookie - the cookie name to retrieve the value for
     * @param startsWith - flag indicating that the cookie name passed is just the start of the name, not the whole name
     * @return - the string value of the cookie requested
     * @throws Exception
     */
    public String getCookieName(WebConversation wc, String cookie) throws Exception {
        String[] cookieNames = wc.getCookieNames();
        for (String name : cookieNames) {
            if (name != null && name.startsWith(cookie)) {
                return name;
            }
        }
        return null;
    }

    /**
     * return the cookie value - get the value of first cookie that starts with the name passed
     * 
     * @param wc - the conversation to get the cookie out of
     * @param cookie - the cookie name to retrieve the value for
     * @param startsWith - flag indicating that the cookie name passed is just the start of the name, not the whole name
     * @return - the string value of the cookie requested
     * @throws Exception
     */
    public String getCookieValue(WebConversation wc, String cookie, Boolean startsWith) throws Exception {
        if (!startsWith) {
            return wc.getCookieValue(cookie);
        } else {
            String[] cookieNameValue = getCookieNameAndValue(wc, cookie, startsWith);
            if (cookieNameValue != null) {
                return cookieNameValue[1];
            } else {
                return null;
            }
        }
    }

    /**
     * return the cookie name/value in a hashmap - We have some cookies that we only know how they start ie: "WASOidcClient_" and we want the value
     * We first have to find the cookie, then retrieve the value. Some callers will need the name and value to insert into a new
     * conversation, others just want the value - this method returns both
     * 
     * @param wc - the conversation to get the cookie out of
     * @param cookie - the cookie name to retrieve the value for
     * @param startsWith - flag indicating that the cookie name passed is just the start of the name, not the whole name
     * @return - the string value of the cookie requested
     * @throws Exception
     */
    public String[] getCookieNameAndValue(WebConversation wc, String cookie, Boolean startsWith) throws Exception {
        if (!startsWith) {
            return new String[] { cookie, wc.getCookieValue(cookie) };
        } else {
            String cookieName = getCookieName(wc, cookie);
            if (cookieName != null) {
                return new String[] { cookieName, wc.getCookieValue(cookieName) };
            } else {
                return null;
            }
        }
    }
}
