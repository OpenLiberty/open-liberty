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
import org.eclipse.persistence.queries.DatabaseQuery;
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
        public void postExecuteQuery(SessionEvent event) {
            super.postExecuteQuery(event);

//            final Session session = event.getSession();
            final DatabaseQuery dbQuery = event.getQuery();
//            AbstractRecord record = dbQuery.getTranslationRow();
//            String sql = dbQuery.getTranslatedSQLString(session, record);
//            SQLListener.recordSQL(sql);

//            SQLListener.recordSQL(event.getQuery().getTranslatedSQLString(event.getSession(), event.getQuery().getTranslationRow()));

            SQLListener.recordSQL(dbQuery.getSQLString());

        }

    }
}
