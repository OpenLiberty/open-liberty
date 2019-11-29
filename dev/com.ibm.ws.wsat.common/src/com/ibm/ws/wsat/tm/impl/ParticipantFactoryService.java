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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import javax.transaction.xa.XAResource;

import org.osgi.service.component.annotations.Component;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATCoordinatorTran;
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

    private static final String CLASS_NAME = ParticipantFactoryService.class.getName();
    private static final TraceComponent TC = Tr.register(ParticipantFactoryService.class);

    private final RegistrationImpl registrationService = RegistrationImpl.getInstance();

    /*
     * The xaResInfo objects we pass to and from the transaction manager cannot contain
     * serialized references to classes from our bundle, as the tran mgr will not have
     * access to them. So we have to perform a 'pre-serialization' to a simple list of
     * bytes, as a hokey work-around. We use a List<Byte> here, rathter than byte[] to
     * ensure that if two WSATParticipant instances compare equal, the serialized form
     * also compares equal.
     */
    public static <T extends WSATEndpoint> Serializable serialize(T endpoint) {
        ArrayList<Byte> data = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bout);
            out.writeObject(endpoint);
            out.flush();
            out.close();

            byte[] bb = bout.toByteArray();
            data = new ArrayList<Byte>(bb.length);
            for (byte b : bb) {
                data.add(b);
            }
        } catch (Exception e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Serialization problem: {0}", e);
            }
        }

        return data;
    }

    public static <T extends WSATEndpoint> T deserialize(Serializable key) {
        T endpoint = null;
        if (key instanceof ArrayList<?>) {
            try {
                ArrayList<Byte> data = (ArrayList<Byte>) key;
                byte[] bb = new byte[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    bb[i] = data.get(i);
                }

                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bb));
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
            WSATCoordinatorTran wsatTran = reconstructTran(part);
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
    private WSATCoordinatorTran reconstructTran(WSATParticipant part) throws XAResourceNotAvailableException {
        String globalId = part.getGlobalId();
        WSATCoordinatorTran wsatTran = WSATTransaction.getCoordTran(globalId);
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
    private WSATParticipant reconstructParticipant(WSATCoordinatorTran wsatTran, WSATParticipant part) throws XAResourceNotAvailableException {
        WSATParticipant participant = wsatTran.getParticipant(part.getId());
        if (participant == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Cannot locate participant, recovering state: {0}", part);
            }
            participant = wsatTran.addParticipant(part);
        }

        return participant;
    }
}
