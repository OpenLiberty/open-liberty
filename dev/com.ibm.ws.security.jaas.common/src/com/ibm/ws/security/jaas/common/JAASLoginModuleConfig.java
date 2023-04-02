/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common;

import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.spi.LoginModule;

/**
 *
 */
public interface JAASLoginModuleConfig {
    public static final String PROXY = "proxy";
    public static final String LOGIN_MODULE_PROXY = "com.ibm.ws.kernel.boot.security.LoginModuleProxy";
    public static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    public static final String WAS_LM_SHARED_LIB = "WAS_LM_SHAREDLIB";
    public static final String KEY_CLASSLOADING_SVC = "classLoadingSvc";

    /**
     * Get the ID for this {@link JAASLoginModuleConfig}.
     *
     * @return
     */
    String getId();

    /**
     * Get the class name for the configured {@link LoginModule}.
     *
     * @return
     */
    String getClassName();

    /**
     * Returns the loginModule control flag. Default value is OPTIONAL.
     */
    LoginModuleControlFlag getControlFlag();

    /**
     * Get the options that JAAS will pass into the {@link LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, Map, Map) method.
     *
     * @return The map of options.
     */
    Map<String, ?> getOptions();

    /**
     * Is this the configuration for the default {@link LoginModule}?
     *
     * @return True if if this is the default {@link LoginModule} configuration.
     */
    boolean isDefaultLoginModule();

    /**
     * Reload the delegate class that is stored in the {@link LoginModule}'s options. This should
     * typically be called any time the bundles for those classes stored in the options map have
     * been reloaded.
     */
    void reloadDelegateClass();
}