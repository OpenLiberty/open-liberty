/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.session.impl;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.SessionCookieConfigImpl;
import com.ibm.ws.session.utils.LoggingUtil;

import jakarta.servlet.SessionCookieConfig;

/*
 * Servlet 6.0 - added APIs: getAttribute, getAttributes, setAttribute
 */

public class SessionCookieConfigImpl60 extends SessionCookieConfigImpl implements SessionCookieConfig, Cloneable {
    private static final String methodClassName = "SessionCookieConfigImpl60";

    private static TraceNLS nls = TraceNLS.getTraceNLS(SessionCookieConfigImpl60.class, "io.openliberty.session60.internal.resources.SessionMessages");
    //Enforced characters NOT for use in Cookie names; see Cookie API for this String
    private static final String TSPECIALS = "/()<>@,;:\\\"[]?={} \t";

    public SessionCookieConfigImpl60() {
        super();

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " Constructor default");
        }
    }

    public SessionCookieConfigImpl60(String name, String domain, String path, String comment, int maxAge, boolean httpOnly, boolean secure) {
        super(name, domain, path, comment, maxAge, httpOnly, secure);

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " Constructor");
        }
    }

    @Override
    public String getAttribute(String name) {
        return super.getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return super.getAttributes();
    }

    @Override
    @Deprecated(since = "Servlet 6.0", forRemoval = true)
    public String getComment() {
        return comment;
    }

    @Override
    public void setAttribute(String name, String value) {
        setAttribute(name, value, EXTERNALCALL);
    }

    @Override
    @Deprecated(since = "Servlet 6.0", forRemoval = true)
    public void setComment(String c) {
        setComment(c, EXTERNALCALL);
    }

    public void setAttribute(String name, String value, boolean externalCall) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " setAttribute , name [" + name + "] , value [" + value + "] , contextInitialized ["
                                                            + contextInitialized + "] , externalCall [" + externalCall + "]");
        }

        if (externalCall) {
            programmaticChange = true;
        }
        if (contextInitialized) {
            throwWarning();
        }

        if (name == null)
            throw new IllegalArgumentException(nls.getString("cookie.attribute.name.null"));

        if (hasReservedCharacters(name)) {
            String msg = nls.getFormattedMessage("cookie.attribute.name.invalid.[{0}]", new Object[] { name }, "Cookie attribute name is invalid [" + name + "]");
            throw new IllegalArgumentException(msg);
        }

        if ("Max-Age".equalsIgnoreCase(name) && value != null) {
            setMaxAge(Integer.parseInt(value), externalCall);
        } else {
            putAttribute(name, value);
        }
    }

    @Override
    public SessionCookieConfig clone() throws CloneNotSupportedException {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " clone");
        }

        SessionCookieConfigImpl60 temp = new SessionCookieConfigImpl60(getName(), getDomain(), getPath(), comment, getMaxAge(), isHttpOnly(), isSecure());
        temp.attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        temp.attributes.putAll(this.attributes);

        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.log(Level.FINE, methodClassName + " returns cloned SessionCookieConfig [" + temp + "]");
        }
        return temp;
    }

    private static boolean hasReservedCharacters(String value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c < ' ' || c >= '' || TSPECIALS.indexOf(c) != -1)
                return true;
        }
        return false;
    }
}
