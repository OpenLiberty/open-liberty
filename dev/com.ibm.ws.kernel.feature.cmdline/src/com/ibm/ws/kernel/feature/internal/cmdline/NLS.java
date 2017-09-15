/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.cmdline;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 */
public class NLS {
    public static final ResourceBundle messages = ResourceBundle.getBundle("com.ibm.ws.kernel.feature.internal.resources.ProvisionerMessages");

    /**
     * Appends "tool." onto the front of the key and loads the message from the "com.ibm.ws.kernel.feature.internal.resources.ProvisionerMessages" bundle.
     * 
     * @param key
     * @param args
     * @return
     */
    public static String getMessage(String key, Object... args) {
        return getNonToolMessage("tool." + key, args);
    }

    /**
     * Loads the message from the "com.ibm.ws.kernel.feature.internal.resources.ProvisionerMessages" bundle.
     * 
     * @param key
     * @param args
     * @return
     */
    public static String getNonToolMessage(String key, Object... args) {
        String message = messages.getString(key);
        return args.length == 0 ? message : MessageFormat.format(message, args);
    }
}