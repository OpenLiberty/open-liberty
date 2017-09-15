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
package com.ibm.ws.jaxws.tools.internal;

import java.util.ResourceBundle;

public class JaxWsToolsConstants {
    public static final String PARAM_HELP = "-help";

    public static final String PARAM_VERSION = "-version";

    public static final String PARAM_TARGET = "-target";

    public static final String TR_GROUP = "JaxwsTools";

    public static final String TR_RESOURCE_BUNDLE = "com.ibm.ws.jaxws.tools.internal.resources.JaxWsToolsMessages";

    public static final String ERROR_PARAMETER_TARGET_MISSED_KEY = "error.parameter.target.missed";

    public static final ResourceBundle messages = ResourceBundle.getBundle(TR_RESOURCE_BUNDLE);
}
