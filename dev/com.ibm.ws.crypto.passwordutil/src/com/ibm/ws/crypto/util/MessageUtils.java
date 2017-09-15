/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Message handling helper class.
 */
public class MessageUtils {
    public static final String RB = "com.ibm.ws.crypto.util.internal.resources.Messages";
    public static final ResourceBundle messages = ResourceBundle.getBundle(RB);

    public static String getMessage(String key, Object... args) {
        String message = messages.getString(key);
        return args.length == 0 ? message : MessageFormat.format(message, args);
    }
}
