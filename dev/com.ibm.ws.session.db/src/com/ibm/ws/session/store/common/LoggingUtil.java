/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.common;

import java.util.logging.Logger;

public class LoggingUtil {

    /**
     * The resource bundle location
     */
    private static final String WASResourceBundleString = "com.ibm.ws.session.db.resources.WASSession";

    /**
     * The java.util.Logger for the logs coming from WAS specific files. You can specifically set this Logger by adding
     * the Session string to the end of the trace String. (ie. com.ibm.ws.session.WASSession=all)
     */
    public static final Logger SESSION_LOGGER_WAS = Logger.getLogger("com.ibm.ws.session.WASSession", WASResourceBundleString);

}
