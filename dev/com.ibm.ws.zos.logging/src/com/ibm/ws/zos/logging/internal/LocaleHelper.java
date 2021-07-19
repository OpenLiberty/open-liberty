/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import java.util.Locale;
import java.util.logging.LogRecord;

import com.ibm.wsspi.logging.LogRecordExt;

/**
 * Simple helper class for translating messages to english
 * before writing them WTO or HARDCOPY.
 */
public class LocaleHelper {

    /**
     * Indicates whether or not we're in an ENGLISH locale
     */
    private final boolean isEnglishLocale = Locale.getDefault().getLanguage().equals(Locale.ENGLISH.getLanguage());

    /**
     * @return the msg in english. null if msg could not be translated.
     */
    public String translateToEnglish(String msg, LogRecord logRecord) {
        if (isEnglishLocale) {
            return msg;
        } else if (logRecord != null && logRecord instanceof LogRecordExt) {
            return ((LogRecordExt) logRecord).getFormattedMessage(Locale.ENGLISH);
        } else {
            return null;
        }
    }

}
