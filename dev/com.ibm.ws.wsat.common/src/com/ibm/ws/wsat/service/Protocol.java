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
package com.ibm.ws.wsat.service;

import java.util.Map;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Service interface for the WS-AT and WS-Coor protocol service implementations
 */
public interface Protocol {

    // TODO: Methods and parameters still being developed!

    // WS-Coor Activation and WS-AT Completion services are not exposed
    // here as they are only required internally by the implementation.

    /*
     * Registration
     */

    public EndpointReferenceType registrationRegister(Map<String, String> wsatProperties, String globalId, EndpointReferenceType participant,
                                                      String recoveryID) throws WSATException;

    /*
     * Coordinator
     */

    public void coordinatorPrepared(ProtocolServiceWrapper wrapper) throws WSATException;

    public void coordinatorReadOnly(ProtocolServiceWrapper wrapper) throws WSATException;

    public void coordinatorAborted(ProtocolServiceWrapper wrapper) throws WSATException;

    public void coordinatorCommitted(ProtocolServiceWrapper wrapper) throws WSATException;

    /*
     * Participant
     */

    public void participantPrepare(ProtocolServiceWrapper wrapper) throws WSATException;

    public void participantCommit(ProtocolServiceWrapper wrapper) throws WSATException;

    public void participantRollback(ProtocolServiceWrapper wrapper) throws WSATException;

    /*
     * Faults
     */

    // TODO: will need additional parameters (fault codes etc)
    public void wsatFault(String globalId, WSATFault fault) throws WSATException;
}
