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
import com.ibm.ws.http.netty.MSP;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;

/**
 *
 */
public enum CookieEncoder {

    INSTANCE;

    private final String SAMESITE = "samesite";

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
                cookie.setAttribute(SAMESITE, sameSiteAttributeValue);

                if (!!!cookie.isSecure() && sameSiteAttributeValue.equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName())) {
                    cookie.setSecure(Boolean.TRUE);
                }

                // Set Partitioned Flag for SameSite=None Cookie
                if (config.getPartitioned() == Boolean.TRUE
                    && sameSiteAttributeValue.equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName())) {
                    if (cookie.getAttribute("partitioned") == null) { // null means no value has been set yet

                        MSP.log("[1] Setting the Partitioned attribute for SameSite=None");
                        cookie.setAttribute("partitioned", "");
                    }
                }
            } else {
                //trace
            }

        }
        if (config.useSameSiteConfig() && cookie.getAttribute("samesite") != null) {
            boolean sameSiteNoneUsed = cookie.getAttribute("samesite").equalsIgnoreCase(HttpConfigConstants.SameSite.NONE.getName());
            if (config.getPartitioned() && sameSiteNoneUsed) {
                if (cookie.getAttribute("partitioned") == null) { // null means no value has been set yet

                    MSP.log("[2] Setting the Partitioned attribute for SameSite=None");
                    cookie.setAttribute("partitioned", "");
                }
            }
        }

        result = CookieUtils.toString(cookie, header, config.isv0CookieDateRFC1123compat(), config.shouldSkipCookiePathQuotes());

        return result;
    }

}
