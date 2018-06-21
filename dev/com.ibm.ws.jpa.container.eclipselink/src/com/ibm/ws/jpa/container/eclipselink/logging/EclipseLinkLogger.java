/*******************************************************************************
 * Copyright (c) 2014,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.eclipselink.logging;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;
import org.eclipse.persistence.platform.server.ServerLog;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jpa.management.JPAConstants;

/**
 * This class is wired in when the eclipselink.target-server property is specified
 * as WebSphere_7.
 *
 * Commented out till we break EclipseLink / OpenJPA dependencies out of the container.
 */
public class EclipseLinkLogger extends ServerLog {

    private final static String ECLIPSELINK_STRING = "eclipselink";
    private final static String EMPTY_CHANNEL = ECLIPSELINK_STRING;
    private final static LogChannel EMPTY_LOG_CHANNEL = new LogChannel(EMPTY_CHANNEL);

    private static final TraceComponent _tc = Tr.register(EclipseLinkLogger.class, JPAConstants.JPA_TRACE_GROUP, JPAConstants.JPA_RESOURCE_BUNDLE_NAME);

    private static final Map<String, LogChannel> _channels = new HashMap<String, LogChannel>();

    static {

        // Register each category with eclipselink prefix as a WebSphere log channel
        for (String category : SessionLog.loggerCatagories) {
            _channels.put(category, new LogChannel(ECLIPSELINK_STRING + "." + category));
        }
        _channels.put(EMPTY_CHANNEL, EMPTY_LOG_CHANNEL);
    }

    public EclipseLinkLogger() {
        // nothing to do.
    }

    @Override
    @Trivial
    public void log(SessionLogEntry entry) {
        String category = entry.getNameSpace();
        int level = entry.getLevel();

        LogChannel channel = getLogChannel(category);
        if (channel.shouldLog(level)) {
            channel.log(entry, formatMessage(entry));
        }
    }

    @Override
    @Trivial
    public boolean shouldLog(int level, String category) {
        return getLogChannel(category).shouldLog(level);
    }

    @Trivial
    private LogChannel getLogChannel(String category) {
        if (category == null) {
            return EMPTY_LOG_CHANNEL;
        }
        LogChannel channel = _channels.get(category);
        if (channel == null) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Found an unmapped logging channel (" + category
                              + ") in log(...). Possibly something wrong in EclipseLink, remapping to base channel.");
            }
            channel = EMPTY_LOG_CHANNEL;
        }
        return channel;
    }
}
