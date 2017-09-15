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
package com.ibm.ws.webcontainer.security.internal;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.CookieHelper;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

/**
 * In case where authentication failed, and the challenge type is Custom,
 * the user is redirected to the (re)login page to provide authentication data.
 * This sends a 302 and specified the URL to be redirected to. This URL is
 * obtained
 * from the security configuration.
 */
public class RedirectReply extends WebReply {

    private final List<Cookie> cookieList;

    public RedirectReply(String url,
                         List<Cookie> list) {
        super(HttpServletResponse.SC_MOVED_TEMPORARILY, url);
        cookieList = list;
    }

    @Override
    public void writeResponse(HttpServletResponse resp) throws IOException {
        if (resp.isCommitted())
            return;

        if (cookieList != null && cookieList.size() > 0) {
            CookieHelper.addCookiesToResponse(cookieList, resp);
        }

        if (getStatusCode() != HttpServletResponse.SC_SEE_OTHER) {
            resp.sendRedirect(resp.encodeURL(message));
        } else {
            if (resp instanceof IExtendedResponse) {
                ((IExtendedResponse) resp).sendRedirect303(resp.encodeURL(message));
            } else {
                resp.sendRedirect(resp.encodeURL(message));
            }
        }
    }

}