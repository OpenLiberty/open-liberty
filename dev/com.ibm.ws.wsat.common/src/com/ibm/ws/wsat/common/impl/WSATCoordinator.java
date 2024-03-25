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
package com.ibm.ws.wsat.common.impl;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.w3c.dom.Node;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;

/**
 * Represents the coordinator in a WSAT transaction.
 */
public class WSATCoordinator extends WSATEndpoint {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent TC = Tr.register(WSATCoordinator.class);

    private final String globalId;

    private transient WSATParticipant participant;

    public WSATCoordinator(String tranId, EndpointReferenceType epr) {
        super(epr);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "WSATCoordinator:\nglobalId:\n{0}\nEPR:\n{1}", tranId, DebugUtils.printEPR(epr));
        }
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
            if (TC.isDebugEnabled()) {

                Tr.debug(TC, "Adding this reference parameter: {0}, {1}", ref.getClass().getCanonicalName(), ref);
                if (ref instanceof Node) {
                    Tr.debug(TC, "Local name: {0}", ((Node) ref).getLocalName());
                    if (Constants.WS_WSAT_PART_ID.equals(((Node) ref).getLocalName())) {
                        Tr.debug(TC, "Skipping");
                        continue;
                    }
                }
            }
            refs.getAny().add(ref);
        }
        JAXBElement<String> part = new JAXBElement<String>(Constants.WS_WSAT_PART_REF, String.class, partId);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Now adding this additional reference parameter: {0}", part);
        }
        refs.getAny().add(part);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GlobalId: ").append(globalId);
        if (participant != null) {
            sb.append("\nWSATParticipant: ").append(participant.toString());
        }
        return sb.toString();
    }
}
