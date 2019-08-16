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
            // TODO Auto-generated method stub
            super.postExecuteQuery(event);

            SQLListener.recordSQL(event.getQuery().getSQLString());

        }

    }
}
