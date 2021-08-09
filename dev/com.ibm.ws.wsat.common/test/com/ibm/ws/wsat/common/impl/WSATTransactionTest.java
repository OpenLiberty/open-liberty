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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * Internal representation of WS-AT global transaction
 */
public class WSATTransactionTest {

    @Before
    public void setUp() throws Exception {
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());
    }

    @Test
    public void testGetSetTransaction() {
        String tranId = Utils.tranId();
        WSATTransaction tran1 = new WSATTransaction(tranId, 10);
        WSATTransaction.putTran(tran1);

        WSATTransaction tran2 = WSATTransaction.getTran(tranId);
        assertNotNull(tran2);
        assertEquals(tranId, tran2.getGlobalId());

        WSATTransaction.removeTran(tranId);
        assertNull(WSATTransaction.getTran(tranId));

        assertNull(WSATTransaction.getTran("xyz"));
    }

    @Test
    public void testGetCoordinatorTran() {
        String tranId1 = Utils.tranId();
        String tranId2 = Utils.tranId();
        WSATTransaction tran1 = new WSATTransaction(tranId1, 10);
        WSATCoordinatorTran tran2 = new WSATCoordinatorTran(tranId2, 10);

        WSATTransaction.putTran(tran1);
        WSATTransaction.putTran(tran2);

        assertNull(WSATTransaction.getCoordTran(tranId1));
        assertNotNull(WSATTransaction.getCoordTran(tranId2));
    }

    @Test
    public void testGetTimeout() {
        String tranId = Utils.tranId();
        WSATTransaction tran = new WSATTransaction(tranId, 1234);
        long timeout = tran.getTimeout();
        assertEquals(1234, timeout);
    }
}
