/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATFault;

/**
 * OSGI service implementation provided to support the generated protocol web services.
 */
@Component(property = { "service.vendor=IBM" })
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
    public void coordinatorPrepared(String globalId, String partId, EndpointReferenceType fromEpr) throws WSATException {
        protocolService.prepared(globalId, partId, fromEpr);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#coordinatorReadOnly(java.lang.String)
     */
    @Override
    public void coordinatorReadOnly(String globalId, String partId, String recoveryId) throws WSATException {
        protocolService.readOnly(globalId, partId, recoveryId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#coordinatorAborted(java.lang.String)
     */
    @Override
    public void coordinatorAborted(String globalId, String partId) throws WSATException {
        protocolService.aborted(globalId, partId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#coordinatorCommitted(java.lang.String)
     */
    @Override
    public void coordinatorCommitted(String globalId, String partId) throws WSATException {
        protocolService.committed(globalId, partId);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#participantPrepare(java.lang.String)
     */
    @Override
    public void participantPrepare(Map<String, String> wsatProperties, EndpointReferenceType fromEpr) throws WSATException {
        protocolService.prepare(wsatProperties, fromEpr);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#participantCommit(java.lang.String)
     */
    @Override
    public void participantCommit(Map<String, String> wsatProperties, EndpointReferenceType fromEpr) throws WSATException {
        protocolService.commit(wsatProperties, fromEpr);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.wsat.service.Protocol#participantRollback(java.lang.String)
     */
    @Override
    public void participantRollback(Map<String, String> wsatProperties, EndpointReferenceType fromEpr) throws WSATException {
        protocolService.rollback(wsatProperties, fromEpr);
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
