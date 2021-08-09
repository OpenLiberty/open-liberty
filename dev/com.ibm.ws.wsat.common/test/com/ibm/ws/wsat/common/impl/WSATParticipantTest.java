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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * WSATParticipant is mainly used to coordinate the WS-AT protocol request
 * and response flows, which are asynchronous.
 */
public class WSATParticipantTest {

    WSATCoordinatorTran tran = null;

    @Before
    public void setUp() throws Exception {
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());
        tran = new WSATCoordinatorTran(Utils.tranId(), 10);
    }

    @Test
    public void testResponseSet() {
        WSATParticipant participant = new WSATParticipant(tran.getGlobalId(), "1", null);
        participant.setState(WSATParticipantState.COMMITTED);
        WSATParticipantState state = participant.waitResponse(10, WSATParticipantState.COMMITTED);
        assertEquals(WSATParticipantState.COMMITTED, state);
    }

    @Test
    public void testResponseWait() {
        final WSATParticipant participant = new WSATParticipant(tran.getGlobalId(), "1", null);
        participant.setState(WSATParticipantState.COMMIT);

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
                participant.setResponse(WSATParticipantState.COMMITTED);
            }
        }.start();

        WSATParticipantState state = participant.waitResponse(2000, WSATParticipantState.COMMITTED);
        assertEquals(WSATParticipantState.COMMITTED, state);
    }

    @Test
    public void testResponseTimeout() {
        final WSATParticipant participant = new WSATParticipant(tran.getGlobalId(), "1", null);
        participant.setState(WSATParticipantState.COMMIT);

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                participant.setResponse(WSATParticipantState.COMMITTED);
            }
        }.start();

        WSATParticipantState state = participant.waitResponse(200, WSATParticipantState.ABORTED);
        assertEquals(WSATParticipantState.TIMEOUT, state);
    }

    @Test
    public void testEquals() {
        WSATParticipant part1 = new WSATParticipant(tran.getGlobalId(), "1", null);
        WSATParticipant part2 = new WSATParticipant(tran.getGlobalId(), "1", null);
        WSATParticipant part3 = new WSATParticipant(tran.getGlobalId(), "2", null);

        WSATCoordinatorTran tran2 = new WSATCoordinatorTran(Utils.tranId(), 10);
        WSATParticipant part4 = new WSATParticipant(tran2.getGlobalId(), "1", null);

        assertTrue(part1.equals(part2));
        assertFalse(part1.equals(part3));
        assertFalse(part1.equals(part4));

        assertTrue(part1.hashCode() == part2.hashCode());
        assertFalse(part1.hashCode() == part3.hashCode());
        assertFalse(part1.hashCode() == part4.hashCode());
    }
}
