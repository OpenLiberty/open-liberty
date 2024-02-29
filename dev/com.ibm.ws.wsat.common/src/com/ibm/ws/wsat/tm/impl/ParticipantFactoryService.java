/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.wsat.tm.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.osgi.service.component.annotations.Component;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATEndpoint;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.impl.RegistrationImpl;

/**
 * Resource factory used by the transaction manager to obtain XRResources
 * for WSAT transaction participants
 */
@Component(property = { Constants.WS_FACTORY_PART, "service.vendor=IBM" })
public class ParticipantFactoryService implements XAResourceFactory {

    private static final TraceComponent TC = Tr.register(ParticipantFactoryService.class);

    private static Map<String, EndpointReferenceType> recoveryAddressMap = new HashMap<String, EndpointReferenceType>();

    private final RegistrationImpl registrationService = RegistrationImpl.getInstance();

    /*
     * The xaResInfo objects we pass to and from the transaction manager cannot contain
     * serialized references to classes from our bundle, as the tran mgr will not have
     * access to them.
     */
    public static <T extends WSATEndpoint> Serializable serialize(T endpoint) {
        byte[] bb = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(endpoint);
            out.flush();
            out.close();

            bb = bout.toByteArray();
        } catch (Exception e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Serialization problem: {0}", e);
            }
        }

        return bb;
    }

    public static <T extends WSATEndpoint> T deserialize(Serializable key) {
        T endpoint = null;
        if (key instanceof byte[]) {
            try {
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream((byte[]) key));
                endpoint = (T) in.readObject();
            } catch (Exception e) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Deserialization problem: {0}", e);
                }
            }
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Incorrect resource info type: {0}", key.getClass());
            }
        }

        return endpoint;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.jta.XAResourceFactory#getXAResource(java.io.Serializable)
     */
    @Override
    public synchronized XAResource getXAResource(Serializable xaResInfo) throws XAResourceNotAvailableException {
        XAResource xaRes = null;
        WSATParticipant part = deserialize(xaResInfo);
        if (part != null) {
            WSATTransaction wsatTran = reconstructTran(part);
            WSATParticipant participant = reconstructParticipant(wsatTran, part);
            xaRes = new ParticipantResource(participant);
        } else {
            throw new XAResourceNotAvailableException(new WSATException(Tr.formatMessage(TC, "UNABLE_TO_DESERIALIZE_CWLIB0208")));
        }

        return xaRes;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.jta.XAResourceFactory#destroyXAResource(javax.transaction.xa.XAResource)
     */
    @Override
    public synchronized void destroyXAResource(XAResource xaRes) throws DestroyXAResourceException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Destroy XAResource: {0}", xaRes);
        }
    }

    // Rebuild the WSATTransaction details from the serialized representation
    private WSATTransaction reconstructTran(WSATParticipant part) throws XAResourceNotAvailableException {
        String globalId = part.getGlobalId();
        WSATTransaction wsatTran = WSATTransaction.getCoordTran(globalId);
        if (wsatTran == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Cannot locate coordinator transaction, recovering state: {0}", globalId);
            }
            try {
                registrationService.activate(globalId, 0, true);
                wsatTran = WSATTransaction.getCoordTran(globalId);
            } catch (WSATException e) {
                throw new XAResourceNotAvailableException(e);
            }
        }

        return wsatTran;
    }

    // Rebuild the WSATParticipant details from a serialized representation.
    private WSATParticipant reconstructParticipant(WSATTransaction wsatTran, WSATParticipant part) throws XAResourceNotAvailableException {
        WSATParticipant participant = wsatTran.getParticipant(part.getId());
        if (participant == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Cannot locate participant, recovering state: {0}", part);
            }
            participant = wsatTran.addParticipant(part);
        }

        return participant;
    }

    /**
     * @param globalId
     * @param id
     * @return
     */
    public static EndpointReferenceType getRecoveryAddress(String globalId, String id) {
        return recoveryAddressMap.remove(globalId + "/" + id);
    }

    public static void putRecoveryAddress(String globalId, String id, EndpointReferenceType recoveryAddress) {
        recoveryAddressMap.put(globalId + "/" + id, recoveryAddress);
    }
}
