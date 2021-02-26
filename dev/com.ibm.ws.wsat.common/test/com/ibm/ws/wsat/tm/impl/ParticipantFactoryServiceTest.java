/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.tm.impl;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;

import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.ws.wsat.common.impl.WSATCoordinatorTran;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.impl.ProtocolImpl;
import com.ibm.ws.wsat.service.impl.RegistrationImpl;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * ParticipantFactoryService - provides XAResource instances of WSATParticipants
 * for the transaction manager.
 */
public class ParticipantFactoryServiceTest {

    private final ParticipantFactoryService service = new ParticipantFactoryService();

    private WSATCoordinatorTran tran = null;
    private WSATParticipant participant = null;

    @Before
    public void setup() throws Exception {
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());
        tran = new WSATCoordinatorTran(Utils.tranId(), 10);
        participant = new WSATParticipant(tran.getGlobalId(), "1", null);
    }

    @Test
    public void testSerializeResource() {
        Serializable s1 = ParticipantFactoryService.serialize(participant);
        assertNotNull(s1);
        assertTrue(s1 instanceof ArrayList<?>);
    }

    @Test
    public void testDeserializeResource() {
        Serializable s1 = ParticipantFactoryService.serialize(participant);
        assertNotNull(s1);

        WSATParticipant part2 = ParticipantFactoryService.deserialize(s1);
        assertNotNull(part2);
        assertTrue(part2 != participant);
        assertTrue(part2.equals(participant));
    }

    @Test
    public void testSerializeEqual() {
        WSATParticipant part1 = new WSATParticipant(tran.getGlobalId(), "1", null);
        WSATParticipant part2 = new WSATParticipant(tran.getGlobalId(), "2", null);

        Serializable s0 = ParticipantFactoryService.serialize(participant);
        assertTrue(s0.equals(ParticipantFactoryService.serialize(part1)));
        assertFalse(s0.equals(ParticipantFactoryService.serialize(part2)));
    }

    @Test
    public void testDeserializeBad() throws IOException {
        WSATParticipant p1 = ParticipantFactoryService.deserialize(new Integer(1));
        assertNull(p1);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(new Integer(1));
        out.close();

        byte[] bb = bout.toByteArray();
        ArrayList<Byte> s1 = new ArrayList<Byte>(bb.length);
        for (byte b : bb) {
            s1.add(b);
        }

        WSATParticipant p2 = ParticipantFactoryService.deserialize(s1);
        assertNull(p2);
    }

    @Test
    public void testGetXAResourceActive() throws XAResourceNotAvailableException {
        // Store the transaction and participant
        tran.setCoordinator(Utils.makeEpr("http://test", "test"));
        tran.addParticipant(participant);
        WSATTransaction.putTran(tran);

        XAResource res = service.getXAResource(ParticipantFactoryService.serialize(participant));
        assertNotNull(res);
        assertTrue(res instanceof ParticipantResource);
    }

    @Test
    public void testRecoveryTransaction() throws XAResourceNotAvailableException {
        RegistrationImpl.getInstance().setRegistrationEndpoint(Utils.makeEpr("http://one", "a"));
        ProtocolImpl.getInstance().setCoordinatorEndpoint(Utils.makeEpr("http://two", "b"));

        assertNull(WSATTransaction.getTran(tran.getGlobalId()));

        XAResource res = service.getXAResource(ParticipantFactoryService.serialize(participant));
        assertNotNull(res);

        WSATCoordinatorTran tran2 = WSATTransaction.getCoordTran(tran.getGlobalId());
        assertNotNull(tran2);
        assertTrue(tran2 != tran);
        assertNotNull(tran2.getParticipant(participant.getId()));
        assertNull(tran2.getContext());
    }
}
