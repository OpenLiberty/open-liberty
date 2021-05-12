/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi;

import java.sql.Connection;

import com.ibm.ejs.ras.Tr;

public class RecoverableOnePhaseXAResourceImpl extends OnePhaseXAResourceImpl {

    private static int _recoveryToken = 1;

    /**
     * @param conn
     * @param mc
     */
    public RecoverableOnePhaseXAResourceImpl(Connection conn,
                                             ManagedConnectionImpl mc) {

        super(conn, mc);

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "<init>");
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "<init>");
        }

    }

    public int getXARecoveryToken() {
        return _recoveryToken++;
    }

} // end class RecoverableOnePhaseXAResourceImpl
