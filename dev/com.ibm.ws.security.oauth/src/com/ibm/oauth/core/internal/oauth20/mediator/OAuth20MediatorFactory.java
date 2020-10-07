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
package com.ibm.oauth.core.internal.oauth20.mediator;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;

/**
 * Finds the mediator to use for the component configuration. If none is
 * configured, uses the default mediator which does nothing.
 */
public class OAuth20MediatorFactory {
    final static String CLASS = OAuth20MediatorFactory.class.getName();
    final static Logger _log = Logger.getLogger(CLASS);

    static Map<String, OAuth20Mediator> /*
                                         * componentId -> OAuth20Mediator
                                         */_mediatorMap = null;

    public static synchronized OAuth20Mediator getMediator(
            OAuth20ComponentInternal component)
            throws OAuthConfigurationException {
        String methodName = "getMediator";
        _log.entering(CLASS, methodName);
        OAuth20Mediator result = null;
        try {
            // check if handler already exists
            if (_mediatorMap == null) {
                // otherwise create a new one and put it in the map
                _mediatorMap = new HashMap<String, OAuth20Mediator>();
            }

            String componentId = component.getParentComponentInstance()
                    .getInstanceId();
            result = _mediatorMap.get(componentId);
            boolean inCache = (result != null);

            if (!inCache) {
                OAuth20ConfigProvider config = component.get20Configuration();
                result = config.getMediators();
                _mediatorMap.put(componentId, result);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
        return result;
    }
}
