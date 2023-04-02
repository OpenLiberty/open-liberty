/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.query.sqlcapture.eclipselink;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.internal.databaseaccess.DatabaseCall;
import org.eclipse.persistence.queries.Call;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;

import com.ibm.ws.jpa.query.sqlcapture.SQLCallListener;

public class ECLSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) throws Exception {
        session.getEventManager().addListener(new ECLListener());
    }

    public class ECLListener extends SessionEventAdapter {

        @Override
        public void preExecuteCall(SessionEvent event) {
            super.preExecuteCall(event);
            Call call = event.getCall();
            if (call instanceof DatabaseCall) {
                SQLCallListener.recordSQLCall(((DatabaseCall) call).getSQLString());
            }
        }
    }
}
