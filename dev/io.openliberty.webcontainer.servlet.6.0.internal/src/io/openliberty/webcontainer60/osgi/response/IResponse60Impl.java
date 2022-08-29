/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.osgi.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.response.IResponse40Impl;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpInboundConnection;

import jakarta.servlet.http.Cookie;

/*
 * Add to support new Cookie setAttribute, getAttribute
 */

public class IResponse60Impl extends IResponse40Impl {
    private static final TraceComponent tc = Tr.register(IResponse60Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    private Map<String, String> cookieAttrs;

    public IResponse60Impl(IRequest req, HttpInboundConnection connection) {
        super(req, connection);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor , req [" + req + "] , inboundConnection [" + connection + "]");
        }
    }

    public void addCookie(Cookie cookie) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "addCookie , cookie [" + cookie.getName() + "] , this [" + this + "]");
            Tr.debug(tc, cookie.toString());
        }

        HttpCookie hc = super.addCookieHelper(cookie);

        //Add the remaining attributes (excluding the predefined attributes)
        cookieAttrs = cookie.getAttributes();

        ArrayList<String> preDefinedAttList = new ArrayList<String>(Arrays.asList("DOMAIN", "MAX-AGE", "PATH", "SECURE", "HTTPONLY"));

        if (cookieAttrs != null) {
            String key, value;
            for (Entry<String, String> entry : this.cookieAttrs.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if (!preDefinedAttList.contains(key.toUpperCase(Locale.ENGLISH))) { //Exclude the predefined attributes
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "addCookie , setAttribute (" + key + " , " + value + ")");
                    }
                    //Build up an attributes map for HttpCookie to setAttribute later on.
                    hc.setAttribute(key, value);
                }
            }
        }

        this.response.addCookie(hc);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "addCookie " + cookie.getName());
        }
    }
}
