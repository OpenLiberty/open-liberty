/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 * Tr wrapper to handle things for handling NLS for requests.
 * Central place to get the right NLS messages.
 */
public class RequestNLS {
    private static final ThreadLocal<RESTRequest> tlReq = new ThreadLocal<RESTRequest>();

    /**
     * Set the RESTRequest for the current request.
     * 
     * @param request The RESTRequest to set for the current request
     */
    public static void setRESTRequest(RESTRequest request) {
        tlReq.set(request);
    }

    /**
     * Get the RESTRequest for the current request.
     * 
     * @return The RESTRequest for the current request
     */
    public static RESTRequest getRESTRequest() {
        return tlReq.get();
    }

    /**
     * Clears the RESTRequest from the request.
     */
    public static void clearThreadLocal() {
        tlReq.remove();
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

    /**
     * Obtain the effective locale based on the current request.
     * 
     * We still need this because right now we do our own ResourceBundle lookup,
     * which really should be changed.
     * 
     * @see #getLocale(Enumeration)
     * @return The effect locale of the request, which may be the default Locale.
     */
    public static Locale getLocale() {
        Locale l = Locale.getDefault();
        // Do some voodoo to get the locale of the request (if we have one)
        RESTRequest req = tlReq.get();
        if (req != null) {
            l = getLocale(req.getLocales());
        }
        return l;
    }

    /**
     * Wraps NLS APIs so we can grab the Locales from the current request
     * (if we have one) and pass it through.
     * 
     * @see Tr#formatMessage(TraceComponent, Enumeration, String, Object[])
     * @param tc
     * @param msgKey
     * @param objs
     * @return
     */
    public static final String formatMessage(TraceComponent tc, String msgKey, Object... objs) {
        RESTRequest req = getRESTRequest();
        if (req != null) {
            return Tr.formatMessage(tc, req.getLocales(), msgKey, objs);
        } else {
            return Tr.formatMessage(tc, msgKey, objs);
        }
    }

}
