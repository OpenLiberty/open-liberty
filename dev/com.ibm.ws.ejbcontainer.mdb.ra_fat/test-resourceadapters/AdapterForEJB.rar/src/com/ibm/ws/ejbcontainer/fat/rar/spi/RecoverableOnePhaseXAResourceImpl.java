/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.spi;

import java.sql.Connection;
import java.util.logging.Logger;

public class RecoverableOnePhaseXAResourceImpl extends OnePhaseXAResourceImpl {
    private final static String CLASSNAME = RecoverableOnePhaseXAResourceImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static int _recoveryToken = 1;

    /**
     * @param conn
     * @param mc
     */
    public RecoverableOnePhaseXAResourceImpl(Connection conn, ManagedConnectionImpl mc) {
        super(conn, mc);
        svLogger.entering(CLASSNAME, "<init>");
        svLogger.exiting(CLASSNAME, "<init>");
    }

    public int getXARecoveryToken() {
        return _recoveryToken++;
    }
}