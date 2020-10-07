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
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.HashMap;
import java.util.Map;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;

/**
 * Token type handler factory
 */
public class OIDCTokenTypeHandlerFactory {

    private static Map<String, OAuth20TokenTypeHandler> handlerMap = new HashMap<String, OAuth20TokenTypeHandler>();

    public static synchronized OAuth20TokenTypeHandler getHandler(OAuth20ComponentInternal component) throws OAuthConfigurationException {

        OAuth20TokenTypeHandler result = null;

        String componentId = component.getParentComponentInstance().getInstanceId();
        result = handlerMap.get(componentId);
        if (result == null) {
            OAuth20ConfigProvider config = component.get20Configuration();
            result = config.getTokenTypeHandler();
            handlerMap.put(componentId, result);
        }

        return result;
    }

    public static synchronized OAuth20TokenTypeHandler getIDTokenHandler(OAuth20ComponentInternal component) throws OAuthConfigurationException {
        OAuth20TokenTypeHandler result = null;

        String componentId = component.getParentComponentInstance().getInstanceId();
        String componentIdToken = componentId + "_IDTOKEN";
        result = handlerMap.get(componentIdToken);
        if (result == null) {
            OAuth20ConfigProvider config = component.get20Configuration();
            result = config.getIDTokenTypeHandler();
            handlerMap.put(componentIdToken, result);
        }
        return result;
    }

}
