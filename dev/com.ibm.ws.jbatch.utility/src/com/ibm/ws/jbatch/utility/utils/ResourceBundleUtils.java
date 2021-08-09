/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.utils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Lookup and format messages from the nls resource bundle(s).
 * 
 * There are 2 resource bundles:
 * 1) one for general run time messages issued by the utility, and 
 * 2) one for utility usage, description, and option msgs.
 * 
 */
public class ResourceBundleUtils {

    public static final ResourceBundle messages = ResourceBundle.getBundle("com.ibm.ws.jbatch.utility.resources.UtilityMessages");
    public static final ResourceBundle options = ResourceBundle.getBundle("com.ibm.ws.jbatch.utility.resources.UtilityOptions");

    public static String getMessage(String key, Object... args) {
        String message = messages.getString(key);
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
