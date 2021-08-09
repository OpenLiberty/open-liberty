/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.uow;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class is a factory providing access to a stateless thread-safe UOWManager
 * instance when executing within a server but outside of a container managed
 * environment such that a JNDI lookup is not possible.
 * 
 * @ibm-spi
 */
public class UOWManagerFactory
{
    private static final TraceComponent tc = Tr.register(UOWManagerFactory.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * Returns a stateless thread-safe UOWManager instance.
     * 
     * @return UOWManager instance
     */
    public static UOWManager getUOWManager()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getUOWManager");

        final UOWManager uowm = com.ibm.ws.uow.embeddable.UOWManagerFactory.getUOWManager();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getUOWManager", uowm);
        return uowm;
    }
}