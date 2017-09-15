/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.audit;

import java.util.List;
import java.util.Map;

import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.security.audit.InvalidConfigurationException;

/**
 *
 */
public interface AuditService {

    String AUDIT_SOURCE_NAME = "audit";
    // TODO: is this the value we want for location?
    String AUDIT_SOURCE_LOCATION = "server";
    String AUDIT_SOURCE_SEPARATOR = "|";
    String AUDIT_FILE_HANDLER_NAME = "AuditFileHandler";

    void sendEvent(AuditEvent event);

    boolean isAuditRequired(String eventType, String outcome);

    void auditStarted(String serviceName);

    void auditStopped(String serviceName);

    void registerEvents(String handlerName, List<Map<String, Object>> configuredEvents) throws InvalidConfigurationException;

    void unRegisterEvents(String handlerName);

    /**
     * Get the unique identifier String of this server. The format is:
     * "websphere: hostName:userDir:serverName"
     *
     * @return the unique identifier of this server
     */
    String getServerID();

    /**
     * @param configuredEvents
     * @return
     */
    boolean validateEventsAndOutcomes(String handlerName, List<Map<String, Object>> configuredEvents);

}
