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

import org.junit.Before;
import org.junit.Test;

import com.ibm.tx.remote.RecoveryCoordinator;
import com.ibm.tx.remote.RecoveryCoordinatorNotAvailableException;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.impl.ProtocolImpl;
import com.ibm.ws.wsat.service.impl.RegistrationImpl;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * CoordinatorFactoryService - provides RecoveryCoordinator instances of WSATCoordinators
 * for the transaction manager.
 */
public class CoordinatorFactoryServiceTest {

    private final CoordinatorFactoryService service = new CoordinatorFactoryService();

    private WSATTransaction tran = null;
    private WSATCoordinator coordinator = null;

    @Before
    public void setup() throws Exception {
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());
        tran = new WSATTransaction(Utils.tranId(), 10);
        coordinator = new WSATCoordinator(tran.getGlobalId(), null);
    }

    @Test
    public void testSerializeResource() {
        Serializable s1 = ParticipantFactoryService.serialize(coordinator);
        assertNotNull(s1);
        assertTrue(s1 instanceof ArrayList<?>);
    }

    @Test
    public void testDeserializeResource() {
        Serializable s1 = ParticipantFactoryService.serialize(coordinator);
        assertNotNull(s1);

        WSATCoordinator coord2 = ParticipantFactoryService.deserialize(s1);
        assertNotNull(coord2);
        assertTrue(coord2 != coordinator);
        assertTrue(coord2.equals(coordinator));
    }

    @Test
    public void testSerializeEqual() {
        WSATCoordinator coord1 = new WSATCoordinator(tran.getGlobalId(), null);
        WSATCoordinator coord2 = new WSATCoordinator(Utils.tranId(), null);

        Serializable s0 = ParticipantFactoryService.serialize(coordinator);
        assertTrue(s0.equals(ParticipantFactoryService.serialize(coord1)));
        assertFalse(s0.equals(ParticipantFactoryService.serialize(coord2)));
    }

    @Test
    public void testDeserializeBad() throws IOException {
        WSATCoordinator c1 = ParticipantFactoryService.deserialize(new Integer(1));
        assertNull(c1);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(new Integer(1));
        out.close();

        byte[] bb = bout.toByteArray();
        ArrayList<Byte> s1 = new ArrayList<Byte>(bb.length);
        for (byte b : bb) {
            s1.add(b);
        }

        WSATCoordinator c2 = ParticipantFactoryService.deserialize(s1);
        assertNull(c2);
    }

    @Test
    public void testGetRecoveryActive() throws RecoveryCoordinatorNotAvailableException {
        // Store the transaction and coordinator
        tran.setCoordinator(coordinator);
        WSATTransaction.putTran(tran);

        RecoveryCoordinator rc = service.getRecoveryCoordinator(ParticipantFactoryService.serialize(coordinator));
        assertNotNull(rc);
        assertTrue(rc instanceof CoordinatorResource);
    }

    @Test
    public void testRecoveryCoordinator() throws RecoveryCoordinatorNotAvailableException {
        RegistrationImpl.getInstance().setRegistrationEndpoint(Utils.makeEpr("http://one", "a"));
        ProtocolImpl.getInstance().setCoordinatorEndpoint(Utils.makeEpr("http://two", "b"));

        assertNull(WSATTransaction.getTran(tran.getGlobalId()));

        RecoveryCoordinator rc = service.getRecoveryCoordinator(ParticipantFactoryService.serialize(coordinator));
        assertNotNull(rc);

        WSATTransaction tran2 = WSATTransaction.getTran(tran.getGlobalId());
        assertNotNull(tran2);
        assertTrue(tran2 != tran);
        assertNotNull(tran2.getCoordinator());
        assertNull(tran2.getContext());
    }
}
