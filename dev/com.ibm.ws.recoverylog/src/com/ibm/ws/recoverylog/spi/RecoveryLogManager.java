/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: RecoveryLogManager
//------------------------------------------------------------------------------
/**
 * <p>
 * The RecoveryLogManager interface provides support for access to the recovery
 * logs associated with a client service.
 * </p>
 *
 * <p>
 * An object that implements this interface is provided to each client service
 * when it registers with the RecoveryDirector.
 * </p>
 */
public interface RecoveryLogManager {
    //------------------------------------------------------------------------------
    // Method: RecoveryLogManager.getRecoveryLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Returns a RecoveryLog that can be used to access a specific recovery log.
     * </p>
     *
     * <p>
     * Each recovery log is contained within a FailureScope. For example, the
     * transaction service on a distributed system has a transaction log in each
     * server node (ie in each FailureScope). Because of this, the caller must
     * specify the FailureScope of the required recovery log.
     * </p>
     *
     * <p>
     * Additionally, the caller must specify information regarding the identity and
     * physical properties of the recovery log. This is done through the LogProperties
     * object provided by the client service.
     * </p>
     *
     * @param FailureScope  The required FailureScope
     * @param LogProperties Contains the identity and physical properties of the
     *                          recovery log.
     *
     * @return The RecoveryLog instance.
     *
     * @exception InvalidLogPropertiesException The RLS does not recognize or cannot
     *                                              support the supplied LogProperties
     */
    public RecoveryLog getRecoveryLog(FailureScope failureScope, LogProperties logProperties) throws InvalidLogPropertiesException;

    /**
     * @param localRecoveryIdentity
     * @param recoveryGroup
     * @param logProperties
     * @return
     * @throws InvalidLogPropertiesException
     */
    SharedServerLeaseLog getLeaseLog(String localRecoveryIdentity, String recoveryGroup, int leaseCheckInterval, String leaseCheckStrategy, int leaseLength,
                                     LogProperties logProperties) throws InvalidLogPropertiesException;
}
