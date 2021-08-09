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
package com.ibm.ws.wsat.common.impl;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import javax.transaction.Synchronization;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * Check adding of participants to a coordinator coordTran. Each should get
 * a unique id.
 */
public class WSATCoordinatorTranTest {

    private final TestSyncRegistry syncReg = new TestSyncRegistry();
    private WSATCoordinatorTran coordTran;

    @Before
    public void setUp() throws Exception {
        Utils.setTranService("syncRegistry", syncReg);
        Utils.setTranService("clService", new MockLoader());

        coordTran = new WSATCoordinatorTran(Utils.tranId(), 1000);
        coordTran.setCoordinator(Utils.makeEpr("http://test", "test"));
    }

    @Test
    public void testAddNewParticipants() {
        assertEquals(syncReg.syncObj, coordTran);

        coordTran.addParticipant((EndpointReferenceType) null);
        assertNotNull(coordTran.getParticipant("1"));
        assertNull(coordTran.getParticipant("2"));
        assertNull(coordTran.getParticipant("3"));

        coordTran.addParticipant((EndpointReferenceType) null);
        assertNotNull(coordTran.getParticipant("1"));
        assertNotNull(coordTran.getParticipant("2"));
        assertNull(coordTran.getParticipant("3"));

        coordTran.removeParticipant("1");
        assertNull(coordTran.getParticipant("1"));
        assertNotNull(coordTran.getParticipant("2"));
        assertNull(coordTran.getParticipant("3"));

        coordTran.addParticipant((EndpointReferenceType) null);
        assertNull(coordTran.getParticipant("1"));
        assertNotNull(coordTran.getParticipant("2"));
        assertNotNull(coordTran.getParticipant("3"));
    }

    @Test
    public void testAddKnownParticipants() {
        assertEquals(syncReg.syncObj, coordTran);

        coordTran.addParticipant((EndpointReferenceType) null);
        assertNotNull(coordTran.getParticipant("1"));
        assertNull(coordTran.getParticipant("2"));

        WSATParticipant part1 = coordTran.getParticipant("1");

        coordTran.removeParticipant("1");
        assertNull(coordTran.getParticipant("1"));
        assertNull(coordTran.getParticipant("2"));

        coordTran.addParticipant(part1);
        assertNotNull(coordTran.getParticipant("1"));
        assertNull(coordTran.getParticipant("2"));
    }

    @Test
    public void testRemoveLastParticipant() {

        WSATTransaction.putTran(coordTran);
        assertNotNull(WSATTransaction.getTran(coordTran.getGlobalId()));

        coordTran.addParticipant((EndpointReferenceType) null);
        assertNotNull(coordTran.getParticipant("1"));

        coordTran.removeParticipant("1");
        assertNull(coordTran.getParticipant("1"));
        assertNotNull(WSATTransaction.getTran(coordTran.getGlobalId()));

        coordTran.afterCompletion(0);
        assertNull(WSATTransaction.getTran(coordTran.getGlobalId()));
    }

    /*
     * Mock Transaction services for these tests
     */

    public static class TestSyncRegistry extends MockProxy {

        Synchronization syncObj = null;

        public void registerInterposedSynchronization(Synchronization sync) {
            syncObj = sync;
        }
    }
}
