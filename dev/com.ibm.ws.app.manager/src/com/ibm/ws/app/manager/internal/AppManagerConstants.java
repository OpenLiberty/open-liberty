/*******************************************************************************
 * Copyright (c) 2010-2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal;

import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

public interface AppManagerConstants {

    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String LOCATION = "location";
    public static final String AUTO_START = "autoStart";
    public static final String TRACE_GROUP = "app.manager";
    public static final String TRACE_MESSAGES = "com.ibm.ws.app.manager.internal.resources.AppManagerMessages";
    public static final String APPLICATIONS_PID = "com.ibm.ws.app.manager";
    public static final String MANAGEMENT_PID = "com.ibm.ws.app.management";
    public static final String MONITOR_PID = "com.ibm.ws.app.manager.monitor";
    public static final String APPLICATION_FACTORY_FILTER = "(service.factoryPid=" + APPLICATIONS_PID + ")";
    public static final String SERVER_APPS_DIR = WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "apps/";
    public static final String EXPANDED_APPS_DIR = SERVER_APPS_DIR + "expanded/";
    public static final String SHARED_APPS_DIR = WsLocationConstants.SYMBOL_SHARED_APPS_DIR;
    public static final String AUTO_INSTALL_PROP = ".installedByDropins";
    public static final String USE_JANDEX = "useJandex";
    public static final String XML_SUFFIX = ".xml";
    public static final String START_AFTER = "startAfter";
    public static final String START_AFTER_REF = "startAfterRef";
}
