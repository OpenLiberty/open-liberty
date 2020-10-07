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
package com.ibm.oauth.core.internal.oauth20.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.audit.OAuthAuditHandler;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentInternal;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;

public class OAuth20AuditHandlerFactory {
    final static String CLASS = OAuth20AuditHandlerFactory.class.getName();
    final static Logger _log = Logger.getLogger(CLASS);

    static Map<String, OAuthAuditHandler> _auditHandlerMap = new HashMap<String, OAuthAuditHandler>();

    public static OAuthAuditHandler getAuditHandler(
            OAuth20ComponentInternal component)
            throws OAuthConfigurationException {
        String methodName = "getAuditHandler";
        _log.entering(CLASS, methodName);
        OAuthAuditHandler result = null;
        try {
            String componentId = component.getParentComponentInstance()
                    .getInstanceId();

            if (!_auditHandlerMap.containsKey(componentId)) {
                synchronized (_auditHandlerMap) {
                    // double check whether key exists
                    if (!_auditHandlerMap.containsKey(componentId)) {
                        OAuth20ConfigProvider config = component.get20Configuration();
                        result = config.getAuditHandler();
                        _auditHandlerMap.put(componentId, result);
                    }
                }
            }

            result = _auditHandlerMap.get(componentId);
        } finally {
            _log.exiting(CLASS, methodName);
        }
        return result;
    }
}
