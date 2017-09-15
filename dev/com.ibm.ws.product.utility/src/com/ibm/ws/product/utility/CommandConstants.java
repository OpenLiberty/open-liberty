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

import java.util.ResourceBundle;

public class CommandConstants {

    public static final String COMMAND_OPTION_PREFIX = "--";

    public static final ResourceBundle PRODUCT_MESSAGES = ResourceBundle.getBundle("com.ibm.ws.product.utility.resources.UtilityMessages");

    public static final ResourceBundle PRODUCT_OPTIONS = ResourceBundle.getBundle("com.ibm.ws.product.utility.resources.UtilityOptions");

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static final String WLP_INSTALLATION_LOCATION = "WLP_INSTALLATION_LOCATION";

    public static final String SCRIPT_NAME = "SCRIPT_NAME";

    public static final String OUTPUT_FILE_OPTION = COMMAND_OPTION_PREFIX + "output";
}
