/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.cookie;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.cookies.CookieUtils;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;

/**
 *
 */
public enum CookieEncoder {

    INSTANCE;

//    public String encode(String name, String Value, HttpChannelConfig config) {
//
//        String cookie = null;
//
//        CookieUtils.
//
//        if(config.useSameSiteConfig() )
//
//
//        return cookie;
//
//    }

    public String encode(HttpCookie cookie, HeaderKeys header, HttpChannelConfig config) {

        String result = null;

        if (config.useSameSiteConfig() && Objects.isNull(cookie.getAttribute("samesite"))) {

            String sameSiteAttributeValue = null;
            Matcher matcher;

            if (config.getSameSiteCookies().containsKey(cookie.getName())) {
                sameSiteAttributeValue = config.getSameSiteCookies().get(cookie.getName());
            }

            else if (config.onlySameSiteStar()) {
                sameSiteAttributeValue = config.getSameSiteCookies().get(HttpConfigConstants.WILDCARD_CHAR);
            } else {
                for (Pattern pattern : config.getSameSitePatterns().keySet()) {
                    matcher = pattern.matcher(cookie.getName());
                    if (matcher.matches()) {
                        sameSiteAttributeValue = config.getSameSitePatterns().get(pattern);
                        break;
                    }

                }
            }

            if (Objects.nonNull(sameSiteAttributeValue)) {
                cookie.setAttribute("samesite", sameSiteAttributeValue);

                if (!!!cookie.isSecure() && sameSiteAttributeValue.equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName())) {
                    cookie.setSecure(Boolean.TRUE);
                }
            } else {
                //trace
            }
        }

        result = CookieUtils.toString(cookie, header, config.isv0CookieDateRFC1123compat(), config.shouldSkipCookiePathQuotes());

        return result;
    }

}
