/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ssl.core;

import com.ibm.ejs.ras.TraceNLS;

/**
 * Helper class for interacting with an NLS translation bundle.
 * 
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class TraceNLSHelper {
    private static final TraceNLS tnls = TraceNLS.getTraceNLS(TraceNLSHelper.class, "com.ibm.ws.ssl.resources.ssl");
    private static TraceNLSHelper thisClass = null;

    /**
     * Access the singleton instance of this class.
     * 
     * @return TraceNLSHelper
     */
    public static TraceNLSHelper getInstance() {
        if (thisClass == null) {
            thisClass = new TraceNLSHelper();
        }

        return thisClass;
    }

    private TraceNLSHelper() {
        // do nothing
    }

    /**
     * Look for a translated message using the input key. If it is not found, then
     * the provided default string is returned.
     * 
     * @param key
     * @param defaultString
     * @return String
     */
    public String getString(String key, String defaultString) {
        if (tnls != null)
            return tnls.getString(key, defaultString);

        return defaultString;
    }

    /**
     * Look for a translated message using the input key. If it is not found, then
     * the provided default string is returned.
     * 
     * @param key
     * @param args
     * @param defaultString
     * @return String
     */
    public String getFormattedMessage(String key, Object[] args, String defaultString) {
        if (tnls != null)
            return tnls.getFormattedMessage(key, args, defaultString);

        return defaultString;
    }
}
