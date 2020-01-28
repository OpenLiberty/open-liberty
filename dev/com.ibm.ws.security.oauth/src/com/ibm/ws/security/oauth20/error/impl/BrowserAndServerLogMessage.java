/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.error.impl;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.oauth.core.api.error.oauth20.TraceConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Small helper class to obtain an NLS message in both the locale of the browser and the locale of the server. This is meant
 * to avoid maintaining two or more lines to get the same NLS message. It can be easy to miss updating one of the lines if the
 * other line is updated at some point.
 */
public class BrowserAndServerLogMessage {
    private Enumeration<Locale> requestLocales = null;
    private final TraceComponent tc;
    private final String msgKey;
    private final Object[] inserts;

    public BrowserAndServerLogMessage(TraceComponent tc, String msgKey, Object... inserts) {
        this.tc = tc;
        this.msgKey = msgKey;
        this.inserts = inserts;
    }

    public String getBrowserErrorMessage() {
        return Tr.formatMessage(tc, getLocale(requestLocales), msgKey, inserts);
    }

    public String getServerErrorMessage() {
        return Tr.formatMessage(tc, msgKey, inserts);
    }

    public void setLocales(Enumeration<Locale> requestLocales) {
        this.requestLocales = requestLocales;
    }

    /**
     * Determines the preferred Locale of the request, <b>as supported by the Liberty profile</b>.
     * In other words, if the most-preferred Locale that is requested that is not supported by the
     * Liberty runtime, then the next most-preferred Locale will be used, finally resulting in the
     * JVM's default Locale.
     * <p>
     * The net effect of this is any French locale (fr, fr_ca, fr_fr, etc) would resolve to just 'fr'.
     * Any Portugese locale ('pt') other than Brazillian ('pt_br') would resolve to the JVM default
     * encoding. Portugese Brazillian is tranlated to, so 'pt_br' is returned. Any English locale
     * is returned as 'en'. Any unrecognized locale resolves to the JVM default.
     *
     * @return The Locale for the request. The best match supported by the Liberty runtime is returned, or the defualt Locale.
     */
    public static Locale getLocale(final Enumeration<Locale> locales) {
        System.out.println("here");
        // System.out.println(locales.nextElement());
        System.out.println(locales);
        if (locales == null) {
            return Locale.getDefault();
        }

        while (locales.hasMoreElements()) {
            final Locale requestedLocale = locales.nextElement();
            // If its English, we're done. Just exit with that because we support all English.
            if (requestedLocale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                return requestedLocale;
            }
            final Locale loadedLocale = ResourceBundle.getBundle(TraceConstants.MESSAGE_BUNDLE, requestedLocale).getLocale();
            if (!loadedLocale.toString().isEmpty() && requestedLocale.toString().startsWith(loadedLocale.toString())) {
                return loadedLocale;
            }
        }

        return Locale.getDefault();
    }
}
