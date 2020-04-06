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

import java.io.Serializable;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.osgi.service.component.annotations.Component;

import com.ibm.tx.remote.RecoveryCoordinator;
import com.ibm.tx.remote.RecoveryCoordinatorFactory;
import com.ibm.tx.remote.RecoveryCoordinatorNotAvailableException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.impl.ProtocolImpl;
import com.ibm.ws.wsat.service.impl.RegistrationImpl;

/**
 * Resource factory used by the transaction manager to obtain a RecoveryCoordinator
 * for WSAT transaction participants
 */
@Component(property = { Constants.WS_FACTORY_COORD, "service.vendor=IBM" })
public class CoordinatorFactoryService implements RecoveryCoordinatorFactory {

    private static final String CLASS_NAME = CoordinatorFactoryService.class.getName();
    private static final TraceComponent TC = Tr.register(CoordinatorFactoryService.class);

    private final RegistrationImpl registrationService = RegistrationImpl.getInstance();
    private final ProtocolImpl protocolService = ProtocolImpl.getInstance();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.tx.remote.RecoveryCoordinatorFactory#getRecoveryCoordinator(java.io.Serializable)
     */
    @Override
    public RecoveryCoordinator getRecoveryCoordinator(Serializable recoveryCoordinatorInfo) throws RecoveryCoordinatorNotAvailableException {
        RecoveryCoordinator recovCoord = null;
        WSATCoordinator coord = ParticipantFactoryService.deserialize(recoveryCoordinatorInfo);
        if (coord != null) {
            WSATTransaction wsatTran = reconstructTran(coord);
            WSATCoordinator coordinator = reconstructCoordinator(wsatTran, coord);

            // We must set our own EPR as participant, so when the 'prepared' response is replayed
            // we send our EPR in the replyTo header.  The coordinator may need this to respond with 
            // a rollback if it no longer knows about the transaction.
            try {
                EndpointReferenceType eprPart = protocolService.getParticipantEndpoint(wsatTran.getGlobalId());
                coordinator.setParticipant(new WSATParticipant(wsatTran.getGlobalId(), "", eprPart));
                recovCoord = new CoordinatorResource(coordinator);
            } catch (WSATException e) {
                throw new RecoveryCoordinatorNotAvailableException(e);
            }
        } else {
            throw new RecoveryCoordinatorNotAvailableException(new WSATException(Tr.formatMessage(TC, "UNABLE_TO_DESERIALIZE_CWLIB0208")));
        }

        return recovCoord;
    }

    // Rebuild the WSATTransaction details from the serialized representation
    private WSATTransaction reconstructTran(WSATCoordinator coord) throws RecoveryCoordinatorNotAvailableException {
        String globalId = coord.getGlobalId();
        WSATTransaction wsatTran = WSATTransaction.getTran(globalId);
        if (wsatTran == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Cannot locate transaction, recovering state: {0}", globalId);
            }
            try {
                registrationService.activate(globalId, null, 0, true);
                wsatTran = WSATTransaction.getTran(globalId);
            } catch (WSATException e) {
                throw new RecoveryCoordinatorNotAvailableException(e);
            }
        }

        return wsatTran;
    }

    // Rebuild the WSATCoordinator details from a serialized representation.
    private WSATCoordinator reconstructCoordinator(WSATTransaction wsatTran, WSATCoordinator coord) {
        WSATCoordinator coordinator = wsatTran.getCoordinator();
        if (coordinator == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Cannot locate coordinator, recovering state: {0}", coord);
            }
            coordinator = wsatTran.setCoordinator(coord);
        }

        return coordinator;
    }
}
