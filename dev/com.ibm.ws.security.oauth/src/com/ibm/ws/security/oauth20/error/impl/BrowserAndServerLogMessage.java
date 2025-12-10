/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.error.impl;

import java.util.Enumeration;
import java.util.Locale;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.IgnoreNonStaticTraceComponent;
import com.ibm.ws.security.common.lang.LocalesModifier;

/**
 * Small helper class to obtain an NLS message in both the locale of the browser and the locale of the server. This is meant
 * to avoid maintaining two or more lines to get the same NLS message. It can be easy to miss updating one of the lines if the
 * other line is updated at some point.
 */
public class BrowserAndServerLogMessage {
    // Static TraceComponent for general class tracing
    private static final TraceComponent tcStatic = Tr.register(BrowserAndServerLogMessage.class);
    
    // Instance-specific TraceComponent for different OAuth providers
    // The @IgnoreNonStaticTraceComponent annotation tells the instrumentation tool
    // that this non-static field is intentional and should not trigger warnings
    @IgnoreNonStaticTraceComponent
    private final TraceComponent tc;
    
    private Enumeration<Locale> requestLocales = null;
    private final String msgKey;
    private final Object[] inserts;

    public BrowserAndServerLogMessage(TraceComponent tc, String msgKey, Object... inserts) {
        this.tc = tc;
        this.msgKey = msgKey;
        this.inserts = inserts;
    }

    public String getBrowserErrorMessage() {
        return Tr.formatMessage(tc, LocalesModifier.getPrimaryLocale(requestLocales), msgKey, inserts);
    }

    public String getServerErrorMessage() {
        return Tr.formatMessage(tc, msgKey, inserts);
    }

    public void setLocales(Enumeration<Locale> requestLocales) {
        this.requestLocales = requestLocales;
    }
}
