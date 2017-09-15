/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence.internal.eclipselink;

import org.eclipse.persistence.logging.SessionLogEntry;
import org.eclipse.persistence.platform.server.ServerLog;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.internal.PersistenceServiceConstants;

@Trivial
public class TraceLog extends ServerLog {
    private final TraceComponent _tc = Tr.register(LogChannel.class, PersistenceServiceConstants.TRACE_GROUP);

    @Override
    public void log(SessionLogEntry entry) {
        if (_tc.isDebugEnabled()) {
            Tr.debug(_tc, formatMessage(entry));
        }
    }
}
