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
package com.ibm.ws.webserver.plugin.utility.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 */
public class CommandUtils {

    public static final ResourceBundle messages = ResourceBundle.getBundle("com.ibm.ws.webserver.plugin.utility.resources.UtilityMessages");
    public static final ResourceBundle cmessages = ResourceBundle.getBundle("com.ibm.ws.webserver.plugin.utility.resources.CUtilityMessages");
    public static final ResourceBundle options = ResourceBundle.getBundle("com.ibm.ws.webserver.plugin.utility.resources.UtilityOptions");

    public static String getMessage(String key, Object... args) {
        String message = messages.getString(key);
        return args.length == 0 ? message : MessageFormat.format(message, args);
    }
    
    public static String getCMessage(String key, Object... args) {
        String message = cmessages.getString(key);
        return args.length == 0 ? message : MessageFormat.format(message, args);
    }

    public static String getOption(String key, Object... args) {
        String option = options.getString(key);
        return args.length == 0 ? option : MessageFormat.format(option, args);
    }

    public static ResourceBundle getOptions() {
        return options;
    }

}
