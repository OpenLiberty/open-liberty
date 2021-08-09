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
package com.ibm.ws.security.utility.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 */
public class CommandUtils {

    public static final ResourceBundle messages = ResourceBundle.getBundle("com.ibm.ws.security.utility.resources.UtilityMessages");
    public static final ResourceBundle options = ResourceBundle.getBundle("com.ibm.ws.security.utility.resources.UtilityOptions");

    public static String getMessage(String key, Object... args) {
        String message = messages.getString(key);
        return args.length == 0 ? message : MessageFormat.format(message, args);
    }

    /**
     *  get the string from options resource bundle. if forceFormat is set to true or args has value, the code invokes
     * MessageFormat.format method even args is not set. This is for processing double single quotes. 
     * Since NLS_MESSAGEFORMAT_ALL is set for options resource bundle, every single quote ' character which needs to be
     * treated as a single quote, is escaped by another single quote. Otherwise, MessageFormat.format method will treat
     * a single quote as the beginning and ending of the quote. So all of the texts needs to be processed by MessageFormat
     * no matter whether it has variables.
     **/
    public static String getOption(String key, boolean forceFormat, Object... args) {
        String option = options.getString(key);
        if (forceFormat || args.length > 0) {
            return MessageFormat.format(option, args);
        } else {
            return  option;
        }
    }

    public static ResourceBundle getOptions() {
        return options;
    }

}
