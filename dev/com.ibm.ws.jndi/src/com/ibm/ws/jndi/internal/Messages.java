/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class Messages {
    public static final String RESOURCE_BUNDLE_NAME = "com.ibm.ws.jndi.internal.resources.JNDIMessages";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);

    @FFDCIgnore(Exception.class)
    public static String formatMessage(final String key, final String defaultMessage, Object... inserts) {
        String message = defaultMessage;
        try {
            try {
                message = BUNDLE.getString(key);
            } catch (MissingResourceException e) {
                message = defaultMessage;
            }
            if (inserts.length != 0)
                message = MessageFormat.format(message, inserts);
            return message;
        } catch (Exception ignored) {
            return message;
        }
    }

}
