/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.ws.jpa.query.sqlcapture.SQLListener;

public class ECLSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) throws Exception {
        session.getEventManager().addListener(new ECLListener());
    }

    public class ECLListener extends SessionEventAdapter {

        @Override
        public void preExecuteQuery(SessionEvent event) {
            super.preExecuteQuery(event);

//            final Session session = event.getSession();
//            AbstractRecord record = dbQuery.getTranslationRow();
//            String sql = dbQuery.getTranslatedSQLString(session, record);
//            SQLListener.recordSQL(sql);

//            SQLListener.recordSQL(event.getQuery().getTranslatedSQLString(event.getSession(), event.getQuery().getTranslationRow()));

            SQLListener.recordSQL(event.getQuery().getSQLString());
        }

        @Override
        public void preExecuteCall(SessionEvent event) {
            super.preExecuteQuery(event);
            Call call = event.getCall();
            if (call instanceof DatabaseCall) {
                SQLListener.recordSQLCall(((DatabaseCall) call).getSQLString());
            }
        }

    }
}
