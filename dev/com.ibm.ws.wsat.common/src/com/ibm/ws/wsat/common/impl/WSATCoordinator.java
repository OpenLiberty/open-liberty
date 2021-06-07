/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.common.impl;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;

/**
 * Represents the coordinator in a WSAT transaction.
 */
public class WSATCoordinator extends WSATEndpoint {
    private static final long serialVersionUID = 1L;

    private final String globalId;

    private transient WSATParticipant participant;

    public WSATCoordinator(String tranId, EndpointReferenceType epr) {
        super(epr);
        globalId = tranId;
    }

    @Trivial
    public String getGlobalId() {
        return globalId;
    }

    @Trivial
    public WSATParticipant getParticipant() {
        return participant;
    }

    @Trivial
    public void setParticipant(WSATParticipant part) {
        participant = part;
    }

    /*
     * Remove the coordinator from the transaction when done
     */
    public void remove() {
        WSATTransaction tran = WSATTransaction.getTran(globalId);
        if (tran != null) {
            tran.removeCoordinator();
        }
    }

    /*
     * Return a coordinator EPR for a specific participant. This is the same as the
     * basic coordinator EPR but it has an additional ReferenceParameter containing
     * the participant identifier. Later, when the participant calls us back using
     * this EPR we will easily be able to identify who the caller was.
     */

    public EndpointReferenceType getEndpointReference(String partId) {
        EndpointReferenceType epr = EndpointReferenceUtils.duplicate(getEndpointReference());
        // duplicate doesn't seem to copy the ReferenceParams?, so add
        // back the originals plus our new participant id.
        ReferenceParametersType refs = new ReferenceParametersType();
        for (Object ref : getEndpointReference().getReferenceParameters().getAny()) {
            refs.getAny().add(ref);
        }
        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_PART_REF, String.class, partId));
        epr.setReferenceParameters(refs);
        return epr;
    }

    /*
     * equals method for the transaction manager. Participants are equal if they
     * have the same globalId and participantId
     */
    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other != null && other instanceof WSATCoordinator) {
            WSATCoordinator otherPart = (WSATCoordinator) other;
            result = globalId.equals(otherPart.globalId);
        }
        return result;
    }

    @Override
    public int hashCode() {
        if (globalId != null)
            return globalId.hashCode();

        return 0;
    }
}
