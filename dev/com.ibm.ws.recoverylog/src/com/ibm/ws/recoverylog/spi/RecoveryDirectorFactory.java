/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
//Class: RecoveryDirectorFactory
//------------------------------------------------------------------------------
/**
 * Factory class to create and allows access to the RecoveryDirector. This is the Liberty version of this Factory
 * which creates a LibertyRecoveryDirectorImpl instance when requested by the TxTMHelper.
 */
public class RecoveryDirectorFactory {
    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(RecoveryDirectorFactory.class,
                                                         TraceConstants.TRACE_GROUP, null);

    /**
     * The single instance of the RecoveryDirector implementation class.
     */
    static RecoveryDirector _recoveryDirector;

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorFactory.RecoveryDirectorFactory
    //------------------------------------------------------------------------------
    /**
     * Private construcor to prevent this object being created.
     */
    protected RecoveryDirectorFactory() {
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryDirectorFactory.recoveryDirector()
    //------------------------------------------------------------------------------
    /**
     * Returns the singleton instance of the RecoveryDirector class. This method
     * uses reflection rather then a direct reference to the underlying class to avoid
     * a cyclic build dependency.
     *
     * @return RecoveryDirector The singleton instance of the RecoveryDirectorImpl.
     */
    public static RecoveryDirector recoveryDirector() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryDirector");

        // If the recovery director is null its an error in JET
        if (_recoveryDirector == null) {
            final InternalLogException ile = new InternalLogException();
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recoveryDirector", ile);
            throw ile;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryDirector", _recoveryDirector);
        return _recoveryDirector;
    }

    /**
     * Create a RecoveryDirector singleton
     *
     * @return RecoveryDirector instance
     */
    public static RecoveryDirector createRecoveryDirector() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoveryDirector");
        _recoveryDirector = RecoveryDirectorImpl.instance();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoveryDirector", _recoveryDirector);
        return _recoveryDirector;
    }

    public static void reset() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reset");
        RecoveryDirectorImpl.reset();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reset");
    }
}