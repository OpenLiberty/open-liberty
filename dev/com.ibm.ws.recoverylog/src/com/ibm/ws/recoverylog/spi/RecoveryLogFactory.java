/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

public interface RecoveryLogFactory
{
    /*
     * createRecoveryLog Create a custom recoverylog implementation for use by RLS.
     * 
     * @param props properties to be associated with the new recovery log (eg DBase config)
     * 
     * @param agent RecoveryAgent which provides client service data eg clientId
     * 
     * @param logcomp RecoveryLogComponent which can be used by the recovery log to notify failures
     * 
     * @param failureScope the failurescope (server) for which this log is to be created
     * 
     * @return RecoveryLog or MultiScopeLog to be used for logging
     * 
     * @exception InvalidLogPropertiesException thrown if the properties are not consistent with the logFactory
     */
    public RecoveryLog createRecoveryLog(CustomLogProperties props, RecoveryAgent agent, RecoveryLogComponent logComp, FailureScope failureScope) throws InvalidLogPropertiesException;

    public SharedServerLeaseLog createLeaseLog(CustomLogProperties props) throws InvalidLogPropertiesException;
}
