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
package com.ibm.ws.wsat.tm.impl;

import com.ibm.tx.remote.RecoveryCoordinator;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WebClient;

/**
 * This is a RecoveryCoordinator that wrappers a remote WSATCoordinator. The tran mgr
 * will use the CoordinatorFactoryService to create instances of these RecoveryCoordinators
 * when it needs to trigger replay of the commit/roolback processing on the recovery
 * of participant resources.
 */
public class CoordinatorResource implements RecoveryCoordinator {

    private WSATCoordinator coordinator = null;

    public CoordinatorResource(WSATCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.tx.remote.RecoveryCoordinator#replayCompletion(java.lang.String)
     */
    @Override
    public void replayCompletion(String globalId) {
        WebClient webClient = WebClient.getWebClient(coordinator, coordinator.getParticipant());
        try {
            // We re-send the 'prepared' response, which cause the coordinator to resend 
            // the commit or rollback request, so we can complete the transaction.
            webClient.prepared();
        } catch (WSATException e) {
            // Nothing needed, but log error.  Transaction maanger will perform retries
            // and handle failure if replay fails.
        } finally {
            coordinator.remove();
        }
    }
}
