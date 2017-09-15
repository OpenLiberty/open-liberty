/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common;

import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

/**
 *
 */
public interface JAASLoginModuleConfig {
    public static final String PROXY = "proxy";
    public static final String LOGIN_MODULE_PROXY = "com.ibm.ws.kernel.boot.security.LoginModuleProxy";
    public static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    public static final String WAS_LM_SHARED_LIB = "WAS_LM_SHAREDLIB";
    public static final String KEY_CLASSLOADING_SVC = "classLoadingSvc";

    String getId();

    String getClassName();

    /**
     * Returns the loginModule control flag. Default value is OPTIONAL.
     */
    LoginModuleControlFlag getControlFlag();

    Map<String, ?> getOptions();

    boolean isDefaultLoginModule();
}