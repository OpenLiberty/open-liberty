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
package com.ibm.oauth.core.api;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.internal.OAuthComponentInstanceImpl;

/**
 * This API is used for the creation of the OAuth service provider component.
 * This API provides methods to create or reload a unique instance of an OAuth
 * service provider component given an OAuth configuration object, which is
 * uniquely identified by OAuth configuration objects getUniqueId() method. If
 * two OAuth configuration objects getUniqueId() method return the same unique
 * ID, the same OAuth component instance is returned.
 * 
 */
public class OAuthComponentFactory {

    final static String CLASS = OAuthComponentFactory.class.getName();
    static Logger _log = Logger.getLogger(CLASS);
    static Map<String, OAuthComponentInstance> _instances = new HashMap<String, OAuthComponentInstance>();

    /**
     * Get an instance of the component based on a configuration object.
     * Instances are cached based on the uniqueId of the configuration object
     * This method should be used whenever a configuration object has not been
     * changed since the last time it was used to get a component instance.
     * 
     * @param config
     *            The configuration for the component
     * @return A component instance based on this configuration object.
     * @throws OAuthException
     *             if there is an error loading the component from the
     *             configuration
     */
    public static synchronized OAuthComponentInstance getOAuthComponentInstance(
            OAuthComponentConfiguration config) throws OAuthException {
        OAuthComponentInstance result = null;
        String methodName = "getOAuthComponentInstance";
        _log.entering(CLASS, methodName);
        try {
            result = _instances.get(config.getUniqueId());
            if (result == null) {
                result = new OAuthComponentInstanceImpl(config);
                _instances.put(config.getUniqueId(), result);
            }
        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    /**
     * Load a new instance of the component based on the configuration,
     * replacing the cached instance if there is one. This is essentially the
     * same as getOAuthComponentInstance except that the cached copy will never
     * be returned - a new instance will be created and stored in the cache.
     * This method should be used if it is known that the configuration object
     * has been updated.
     * 
     * @param config
     *            The configuration for the component
     * @return A component instance based on this configuration object.
     * @throws OAuthException
     *             if there is an error loading the component from the
     *             configuration
     */
    public static synchronized OAuthComponentInstance reloadOAuthComponentInstance(
            OAuthComponentConfiguration config) throws OAuthException {
        OAuthComponentInstance result = null;
        String methodName = "reloadOAuthComponentInstance";
        _log.entering(CLASS, methodName);
        try {
            _instances.remove(config.getUniqueId());
            result = getOAuthComponentInstance(config);
        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }
}
