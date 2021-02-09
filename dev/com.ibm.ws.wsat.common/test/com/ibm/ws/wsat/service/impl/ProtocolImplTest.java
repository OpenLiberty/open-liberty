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
package com.ibm.ws.wsat.service.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.Before;
import org.junit.Test;

import com.ibm.tx.remote.Vote;
import com.ibm.ws.wsat.common.impl.WSATCoordinatorTran;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATParticipantState;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.test.MockClient;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * Tests the WS-Coord and WS-AT protocols
 */
public class ProtocolImplTest {

    private final ProtocolImpl service = ProtocolImpl.getInstance();

    private EndpointReferenceType epr;
    private final TestRemoteTranMgr remoteMgr = new TestRemoteTranMgr();
    private WSATCoordinatorTran coordTran;

    private String action;

    @Before
    public void setup() throws Exception {
        Utils.setTranService("remoteTranMgr", remoteMgr);
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());

        epr = Utils.makeEpr("http://www.example.com/endpoint", "a");

        coordTran = new WSATCoordinatorTran(Utils.tranId(), 1000);
        coordTran.setCoordinator(epr);

        new MockClient();
    }

    @Test
    public void testGetEndpointReferences() throws WSATException {
        String tranId = Utils.tranId();
        service.setCoordinatorEndpoint(epr);
        EndpointReferenceType epr1 = service.getCoordinatorEndpoint(tranId);
        assertEquals(1, epr1.getReferenceParameters().getAny().size());
        Object rp = epr1.getReferenceParameters().getAny().get(0);
        assertTrue(rp instanceof JAXBElement<?>);
        assertEquals(tranId, ((JAXBElement<String>) rp).getValue());

        String tranId2 = Utils.tranId();
        service.setParticipantEndpoint(epr);
        EndpointReferenceType epr2 = service.getCoordinatorEndpoint(tranId);
        assertEquals(1, epr2.getReferenceParameters().getAny().size());
        Object rp2 = epr2.getReferenceParameters().getAny().get(0);
        assertTrue(rp2 instanceof JAXBElement<?>);
        assertEquals(tranId, ((JAXBElement<String>) rp2).getValue());
    }

    @Test
    public void testPrepareCommit() throws WSATException {
        String tranId = Utils.tranId();
        WSATTransaction tran = new WSATTransaction(tranId, 1000);
        tran.setCoordinator(epr);
        WSATTransaction.putTran(tran);
        new MockClient() {
            @Override
            public void prepared() {
                action = "prepared";
            }
        };

        remoteMgr.vote = Vote.VoteCommit;
        service.prepare(tranId, null);
        assertEquals("prepared", action);
        assertEquals(remoteMgr.prepareId, tranId);
    }

    @Test
    public void testPrepareReadonly() throws WSATException {
        String tranId = Utils.tranId();
        WSATTransaction tran = new WSATTransaction(tranId, 1000);
        tran.setCoordinator(epr);
        WSATTransaction.putTran(tran);
        new MockClient() {
            @Override
            public void readOnly() {
                action = "readonly";
            }
        };

        remoteMgr.vote = Vote.VoteReadOnly;
        service.prepare(tranId, null);
        assertEquals("readonly", action);
        assertEquals(remoteMgr.prepareId, tranId);
    }

    @Test
    public void testPrepareRollback() throws WSATException {
        String tranId = Utils.tranId();
        WSATTransaction tran = new WSATTransaction(tranId, 1000);
        tran.setCoordinator(epr);
        WSATTransaction.putTran(tran);
        new MockClient() {
            @Override
            public void aborted() {
                action = "aborted";
            }
        };

        remoteMgr.vote = Vote.VoteRollback;
        service.prepare(tranId, null);
        assertEquals("aborted", action);
        assertEquals(remoteMgr.prepareId, tranId);
    }

    @Test
    public void testPrepareNoTran() throws WSATException {
        new MockClient() {
            @Override
            public void aborted() {
                action = "aborted";
            }
        };

        String id = Utils.tranId();
        service.prepare(id, epr);
        assertEquals("aborted", action);
        assertEquals(remoteMgr.prepareId, id);
    }

    @Test
    public void testCommit() throws WSATException {
        String tranId = Utils.tranId();
        WSATTransaction tran = new WSATTransaction(tranId, 1000);
        tran.setCoordinator(epr);
        WSATTransaction.putTran(tran);
        new MockClient() {
            @Override
            public void committed() {
                action = "committed";
            }
        };

        service.commit(tranId, null);
        assertEquals("committed", action);
        assertEquals(remoteMgr.commitId, tranId);
    }

    @Test
    public void testCommitNoTran() throws WSATException {
        new MockClient() {
            @Override
            public void committed() {
                action = "committed";
            }
        };

        String id = Utils.tranId();
        service.commit(id, epr);
        assertEquals("committed", action);
        assertEquals(remoteMgr.commitId, id);
    }

    @Test
    public void testRollback() throws WSATException {
        String tranId = Utils.tranId();
        WSATTransaction tran = new WSATTransaction(tranId, 1000);
        tran.setCoordinator(epr);
        WSATTransaction.putTran(tran);
        new MockClient() {
            @Override
            public void aborted() {
                action = "aborted";
            }
        };

        service.rollback(tranId, null);
        assertEquals("aborted", action);
        assertEquals(remoteMgr.rollbackId, tranId);
    }

    @Test
    public void testRollbackNoTran() throws WSATException {
        new MockClient() {
            @Override
            public void aborted() {
                action = "aborted";
            }
        };

        String id = Utils.tranId();
        service.rollback(id, epr);
        assertEquals("aborted", action);
        assertEquals(remoteMgr.rollbackId, id);
    }

    @Test
    public void testPrepared() throws WSATException {
        WSATParticipant part = coordTran.addParticipant(epr);
        WSATTransaction.putTran(coordTran);

        service.prepared(coordTran.getGlobalId(), part.getId(), null);
        assertEquals(WSATParticipantState.PREPARED, part.waitResponse(10, WSATParticipantState.PREPARED));
    }

    @Test
    public void testReadonly() throws WSATException {
        WSATParticipant part = coordTran.addParticipant(epr);
        WSATTransaction.putTran(coordTran);

        service.readOnly(coordTran.getGlobalId(), part.getId());
        assertEquals(WSATParticipantState.READONLY, part.waitResponse(10, WSATParticipantState.READONLY));
    }

    @Test
    public void testCommited() throws WSATException {
        WSATParticipant part = coordTran.addParticipant(epr);
        WSATTransaction.putTran(coordTran);

        service.committed(coordTran.getGlobalId(), part.getId());
        assertEquals(WSATParticipantState.COMMITTED, part.waitResponse(10, WSATParticipantState.COMMITTED));
    }

    @Test
    public void testAborted() throws WSATException {
        WSATParticipant part = coordTran.addParticipant(epr);
        WSATTransaction.putTran(coordTran);

        service.aborted(coordTran.getGlobalId(), part.getId());
        assertEquals(WSATParticipantState.ABORTED, part.waitResponse(10, WSATParticipantState.ABORTED));
    }

    /*
     * coordTran Mock Transaction services for these tests
     */

    public static class TestRemoteTranMgr extends MockProxy {

        Vote vote;
        String prepareId;
        String commitId;
        String rollbackId;

        public Vote prepare(String globalId) {
            prepareId = globalId;
            return vote;
        }

        public void commit(String globalId) {
            commitId = globalId;
        }

        public void rollback(String globalId) {
            rollbackId = globalId;
        }
    }
}
