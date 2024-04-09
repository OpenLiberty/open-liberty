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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.HttpCookie;

import io.netty.handler.codec.http.Cookie;

/**
 *
 */
public class CookieDecoder {

    public static List<HttpCookie> decode(String cookieString) {

        List<HttpCookie> list = new LinkedList<HttpCookie>();

        boolean isServlet6Cookie = HttpDispatcher.useEE10Cookies();
        boolean foundDollarSign = false;
        String skipDollarSignName = null;
        String name = null;
        String value = null;

        //TODO: Edge cases for $ on Servlet 6

        Set<Cookie> cookies = new HashSet<>();

        if (isServlet6Cookie) {
            String[] rawCookies = cookieString.split(";");

            for (String rawCookie : rawCookies) {
                foundDollarSign = ('$' == rawCookie.trim().charAt(0));

                String[] splitCookie = rawCookie.split("=", 2);
                if (splitCookie.length == 2) {
                    name = splitCookie[0].trim();
                    value = splitCookie[1].trim();

                } else {
                    name = rawCookie.trim();
                }

                if (foundDollarSign) {

                    skipDollarSignName = name.substring(1);

                    if ("version".equalsIgnoreCase(skipDollarSignName)) {
                        continue;
                    }

                    //TODO: validate edge cases
                }

                list.add(new HttpCookie(name, value));
                name = null;
                value = null;
            }

        } else {
            cookies = decodeNetty(cookieString);
            for (Cookie c : cookies) {
                list.add(new HttpCookie(c.getName(), c.getValue()));
            }
        }

        return list;
    }

    public static Set<Cookie> decodeNetty(String cookieString) {

        Set<Cookie> result = null;

        if (Objects.nonNull(cookieString)) {
            result = io.netty.handler.codec.http.CookieDecoder.decode(cookieString);

        }

        return result;
    }

}

//if (foundDollar) {
//    String cName = token.getName();
//    if (cName.equalsIgnoreCase("version")) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "matchAndParse", " dollar version ");
//        }
//        if (!token.validForHeader(hdr, foundDollar)) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "Token not valid for header, " + hdr + " " + token);
//            }
//            token = null;
//        }
//    } else { // $ANY is a new cookie
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "matchAndParse", " dollar " + cName + " , token [" + token + "]");
//        }
//        token = null;
//    }
//} else { // not foundDollar
//         // test whether what we believe to be a token is a valid attribute
//         // for this header instance. If not, then treat it as a new cookie
//         // name
//    if (!token.validForHeader(hdr, foundDollar)) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            Tr.debug(tc, "Token not valid for header, " + hdr + " " + token);
//        }
//        token = null;
//    }
//}
//} else { // prior to Servlet 6.0 path
//// test whether what we believe to be a token is a valid attribute
//// for this header instance. If not, then treat it as a new cookie
//// name
//if (!token.validForHeader(hdr, foundDollar)) {
//    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//        Tr.debug(tc, "Token not valid for header, " + hdr + " " + token);
//    }
//    token = null;
//}
//}
//}
//if (null == token) {
//// New cookie name found
//if (foundDollar && this.useEE10Cookies) { //Servlet 6.0 : $ is part of the name, so put it back and adjust the len
//start--;
//len++;
//}
//this.name = new byte[len];
//System.arraycopy(data, start, this.name, 0, len);
//if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//Tr.debug(tc, "name: " + GenericUtils.getEnglishString(this.name));
//}