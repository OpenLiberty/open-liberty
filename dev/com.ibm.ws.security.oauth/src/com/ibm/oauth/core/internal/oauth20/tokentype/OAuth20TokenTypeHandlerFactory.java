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
package com.ibm.oauth.core.internal.oauth20.tokentype;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;

/**
 * Token type handler factory
 * 
 */
public class OAuth20TokenTypeHandlerFactory {

    final static String CLASS = OAuth20TokenTypeHandlerFactory.class.getName();
    final static Logger _log = Logger.getLogger(CLASS);

    static Map<String, OAuth20TokenTypeHandler> /*
                                                 * componentId ->
                                                 * OAuth20TokenTypeHandler
                                                 */_handlerMap = null;

    public static synchronized OAuth20TokenTypeHandler getHandler(
            OAuth20ComponentInternal component)
            throws OAuthConfigurationException {
        String methodName = "getHandler";
        _log.entering(CLASS, methodName);
        OAuth20TokenTypeHandler result = null;
        try {
            // check if handler already exists
            if (_handlerMap == null) {
                // otherwise create a new one and put it in the map
                _handlerMap = new HashMap<String, OAuth20TokenTypeHandler>();
            }

            String componentId = component.getParentComponentInstance()
                    .getInstanceId();
            result = _handlerMap.get(componentId);
            boolean inCache = (result != null);

            if (!inCache) {

                OAuth20ConfigProvider config = component.get20Configuration();
                result = config.getTokenTypeHandler();
                _handlerMap.put(componentId, result);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
        return result;
    }
}
