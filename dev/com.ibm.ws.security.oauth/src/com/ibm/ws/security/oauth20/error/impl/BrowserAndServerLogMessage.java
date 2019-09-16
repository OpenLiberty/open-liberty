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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Small helper class to obtain an NLS message in both the locale of the browser and the locale of the server. This is meant
 * to avoid maintaining two or more lines to get the same NLS message. It can be easy to miss updating one of the lines if the
 * other line is updated at some point.
 */
public class BrowserAndServerLogMessage {

    private final String browserMsg;
    private final String serverMsg;

    public BrowserAndServerLogMessage(TraceComponent tc, Enumeration<Locale> requestLocales, String msgKey, Object... inserts) {
        browserMsg = Tr.formatMessage(tc, requestLocales, msgKey, inserts);
        serverMsg = Tr.formatMessage(tc, msgKey, inserts);
    }

    public String getBrowserErrorMessage() {
        return browserMsg;
    }

    public String getServerErrorMessage() {
        return serverMsg;
    }

}
