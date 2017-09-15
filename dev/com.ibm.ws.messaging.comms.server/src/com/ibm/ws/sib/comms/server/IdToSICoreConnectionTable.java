/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

/**
 * A table which maps between conversation ID's and SICoreConnection object references.
 */
public class IdToSICoreConnectionTable {
    private static final TraceComponent tc = SibTr.register(IdToSICoreConnectionTable.class,
                                                            CommsConstants.MSG_GROUP,
                                                            CommsConstants.MSG_BUNDLE);

    static {
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "@(#) SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/IdToSICoreConnectionTable.java, SIB.comms, WASX.SIB, aa1225.01 1.10");
    }

    // Maps integer id's to objects.
    private final IdToObjectMap map = new IdToObjectMap();

    /**
     * Adds an SICoreConnection into this map with the specified ID.
     * 
     * @param id
     * @param connection
     */
    public synchronized void add(int id, SICoreConnection connection) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "add", "" + id);
        map.put(id, connection);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "add");
    }

    /**
     * Returns the SICoreConnection previously stored with the specified ID.
     * 
     * @param id
     * @return SICoreConnection
     */
    public synchronized SICoreConnection get(int id) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "get", "" + id);
        SICoreConnection retValue = (SICoreConnection) map.get(id);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "get", retValue);
        return retValue;
    }

    /**
     * Removes an SICoreConnection from the map.
     * 
     * @param id
     */
    public synchronized void remove(int id) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "remove", "" + id);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "remove", "" + id);
        map.remove(id);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
