/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.wsat.service.impl;

import java.util.Map;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.wsat.service.Protocol;
import com.ibm.ws.wsat.service.ProtocolServiceWrapper;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATFault;

/**
 * OSGI service implementation provided to support the generated protocol web services.
 */
@Component
public class ProtocolService implements Protocol {

    RegistrationImpl registrationService = RegistrationImpl.getInstance();
    ProtocolImpl protocolService = ProtocolImpl.getInstance();

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#register(java.lang.String)
     */
    @Override
    public EndpointReferenceType registrationRegister(Map<String, String> wsatProperties, String globalId, EndpointReferenceType participant,
                                                      String recoveryID) throws WSATException {
        return registrationService.register(wsatProperties, globalId, participant, recoveryID);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#coordinatorPrepared(java.lang.String)
     */
    @Override
    public void coordinatorPrepared(ProtocolServiceWrapper wrapper) throws WSATException {
        protocolService.prepared(wrapper);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#coordinatorReadOnly(java.lang.String)
     */
    @Override
    public void coordinatorReadOnly(ProtocolServiceWrapper wrapper) throws WSATException {
        protocolService.readOnly(wrapper);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#coordinatorAborted(java.lang.String)
     */
    @Override
    public void coordinatorAborted(ProtocolServiceWrapper wrapper) throws WSATException {
        protocolService.aborted(wrapper);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#coordinatorCommitted(java.lang.String)
     */
    @Override
    public void coordinatorCommitted(ProtocolServiceWrapper wrapper) throws WSATException {
        protocolService.committed(wrapper);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#participantPrepare(java.lang.String)
     */
    @Override
    public void participantPrepare(ProtocolServiceWrapper wrapper) throws WSATException {
        protocolService.prepare(wrapper);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#participantCommit(java.lang.String)
     */
    @Override
    public void participantCommit(ProtocolServiceWrapper wrapper) throws WSATException {
        protocolService.commit(wrapper);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#participantRollback(java.lang.String)
     */
    @Override
    public void participantRollback(ProtocolServiceWrapper wrapper) throws WSATException {
        protocolService.rollback(wrapper);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#fault(java.lang.String, com.ibm.ws.wsat.service.WSATFault)
     */
    @Override
    public void wsatFault(String globalId, WSATFault fault) throws WSATException {
        // TODO Auto-generated method stub

    }

}
