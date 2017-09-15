/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Contains cookie related helper functions required by WAS.security.
 * This class contains static helper methods which are commonly used
 * in various other classes.
 */
public class CookieHelper {

    /**
     * Retrieve the value of the first instance of the specified Cookie name
     * from the array of Cookies. Note name matching ignores case.
     * 
     * @param cookies array of Cookie objects, may be {@code null}.
     * @param cookieName the name of the cookie
     * @return String value associated with the specified cookieName, {@code null} if no match could not be found.
     */
    @Sensitive
    public static String getCookieValue(Cookie[] cookies, String cookieName) {

        if (cookies == null)
            return null;

        String retVal = null;
        for (int i = 0; i < cookies.length; ++i) {
            if (cookieName.equalsIgnoreCase(cookies[i].getName())) {
                retVal = cookies[i].getValue();
                break;
            }
        }

        return retVal;
    }

    /**
     * Retrieve the value of the all instances of the specified Cookie name
     * from the array of Cookies. Note name matching ignores case.
     * 
     * @param cookies array of Cookie objects, may be {@code null}.
     * @param cookieName the name of the cookie
     * @return String[] of the values associated with the specified cookieName, {@code null} if no match could not be found.
     */
    public static String[] getCookieValues(Cookie[] cookies, String cookieName) {

        if (cookies == null)
            return null;

        Vector<String> retValues = new Vector<String>();
        for (int i = 0; i < cookies.length; ++i) {
            if (cookieName.equalsIgnoreCase(cookies[i].getName())) {
                retValues.add(cookies[i].getValue());
            }
        }

        if (retValues.size() > 0) {
            return retValues.toArray(new String[retValues.size()]);
        } else {
            return null;
        }
    }

    /**
     * Given a list of Cookie objects, set them into the HttpServletResponse.
     * This method does not alter the cookies in any way.
     * 
     * @param cookieList A List of Cookie objects
     * @param resp HttpServletResponse into which to set the cookie
     */
    public static void addCookiesToResponse(List<Cookie> cookieList, HttpServletResponse resp) {

        Iterator<Cookie> iterator = cookieList.listIterator();
        while (iterator.hasNext()) {
            Cookie cookie = iterator.next();
            if (cookie != null) {
                resp.addCookie(cookie);
            }
        }
    }

    /**
     * Invalidate (clear) the cookie in the HttpServletResponse.
     * Setting age to 0 to invalidate it.
     * 
     * @param res
     */
    @Sensitive
    public static void clearCookie(HttpServletRequest req, HttpServletResponse res, String cookieName, Cookie[] cookies) {
        Cookie existing = getCookie(cookies, cookieName);
        if (existing != null) {
            Cookie c = new Cookie(cookieName, "");

            String path = existing.getPath();
            if (path == null)
                path = "/";
            c.setPath(path);

            c.setMaxAge(0);

            //c.setHttpOnly(existing.isHttpOnly());
            c.setSecure(existing.getSecure());

            res.addCookie(c);
        }

    }

    @Sensitive
    public static Cookie getCookie(Cookie[] cookies, String cookieName) {

        if (cookies == null)
            return null;

        Cookie retVal = null;
        for (int i = 0; i < cookies.length; ++i) {
            if (cookieName.equalsIgnoreCase(cookies[i].getName())) {
                retVal = cookies[i];
                break;
            }
        }

        return retVal;
    }

}
