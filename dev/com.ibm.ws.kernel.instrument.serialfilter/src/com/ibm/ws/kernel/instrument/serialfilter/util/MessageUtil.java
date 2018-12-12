/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;


public class MessageUtil {
    private static final String MESSAGE_BUNDLE = "com.ibm.ws.kernel.instrument.serialfilter.internal.resources.SerialFilterMessages";

    public static String format(String messageName, Object... arguments) {
        return MessageFormat.format(ResourceBundle.getBundle(MESSAGE_BUNDLE).getString(messageName), arguments);
    }
}
