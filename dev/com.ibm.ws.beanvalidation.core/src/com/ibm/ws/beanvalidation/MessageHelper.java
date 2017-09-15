/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class to help with generation of messages
 */

public class MessageHelper {

    private static ResourceBundle resourceBundle;

    public static String getMessage(String key) {
        String message = " ";

        if (resourceBundle == null) {
            getResourceBundle();
        }

        try {
            message = resourceBundle.getString(key);
        } catch (Exception e) {
            message = " ";
        }

        return message;
    }

    public static String getMessage(String key, Object[] args) {
        if (resourceBundle == null) {
            getResourceBundle();
        }
        return MessageFormat.format(resourceBundle.getString(key), args);
    }

    private static ResourceBundle getResourceBundle() {
        resourceBundle = ResourceBundle.getBundle(BVNLSConstants.BV_RESOURCE_BUNDLE, Locale.getDefault());
        return resourceBundle;
    }
}
