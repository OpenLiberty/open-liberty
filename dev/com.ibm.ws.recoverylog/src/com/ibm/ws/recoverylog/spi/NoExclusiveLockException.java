/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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

package com.ibm.ws.recoverylog.spi;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//------------------------------------------------------------------------------
// Class: NoExclusiveLockException
//------------------------------------------------------------------------------
/**
 * This exception indicates that an operation has failed as the caller currently
 * does not hold the exclusive lock (see Lock.java)
 */
public class NoExclusiveLockException extends Exception {
    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(NoExclusiveLockException.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    //------------------------------------------------------------------------------
    // Method: NoExclusiveLockException,.NoExclusiveLockException
    //------------------------------------------------------------------------------
    /**
     * Exception constructor.
     */
    public NoExclusiveLockException() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "NoExclusiveLockException");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "NoExclusiveLockException", this);
    }
}
