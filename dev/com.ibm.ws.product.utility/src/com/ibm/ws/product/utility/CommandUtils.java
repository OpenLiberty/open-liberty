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
package com.ibm.ws.product.utility;

import java.text.MessageFormat;

public class CommandUtils {

    public static String getMessage(String key, Object... args) {
        String message = CommandConstants.PRODUCT_MESSAGES.getString(key);
        return args.length == 0 ? message : MessageFormat.format(message, args);
    }

    public static String getOption(String key, Object... args) {
        String option = CommandConstants.PRODUCT_OPTIONS.getString(key);
        return args.length == 0 ? option : MessageFormat.format(option, args);
    }
}
