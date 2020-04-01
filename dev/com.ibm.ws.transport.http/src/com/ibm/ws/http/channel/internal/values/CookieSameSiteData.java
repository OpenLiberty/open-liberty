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
package com.ibm.ws.http.channel.internal.values;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpChannelUtils;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * This class is used to the SameSite attribute of the cookie.
 */
public class CookieSameSiteData extends CookieData {

    /** Trace component for debugging */
    private static final TraceComponent tc = Tr.register(CookieSameSiteData.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /**
     * Constructor for a SameSite Cookie object.
     */
    public CookieSameSiteData() {
        super("samesite");
    }

    /*
     *
     * @see com.ibm.ws.http.channel.internal.values.CookieData#set(com.ibm.wsspi.http.HttpCookie, byte[])
     */
    @Override
    public boolean set(HttpCookie cookie, byte[] attribValue) {
        if (null == cookie || null == attribValue || 0 == attribValue.length) {
            return false;
        }
        //Start parsing the byte array here to set the SameSite value

        //TODO: when cookie API is updated, cookie.setSameSite(value). Match value to possible
        //values, if doesn't match, put default.
        cookie.setAttribute(getName(), HttpChannelUtils.getEnglishString(attribValue));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Cookie SameSite set to: " + cookie.getAttribute(getName()));
        }

        return true;
    }

    /*
     * @see
     * com.ibm.ws.http.channel.internal.values.CookieData#validForHeader(com.ibm
     * .wsspi.genericbnf.HeaderKeys, boolean)
     */
    @Override
    public boolean validForHeader(HeaderKeys hdr, boolean includesDollar) {
        // Only valid for Set-Cookie headers
        if (HttpHeaderKeys.HDR_SET_COOKIE.equals(hdr) || HttpHeaderKeys.HDR_SET_COOKIE2.equals(hdr)) {
            return !includesDollar;
        }
        return false;
    }

}
