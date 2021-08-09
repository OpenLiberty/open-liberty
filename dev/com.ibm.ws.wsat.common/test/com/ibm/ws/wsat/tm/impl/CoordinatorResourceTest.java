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

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.test.MockClient;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * CoordinatorResource test
 */
public class CoordinatorResourceTest {

    private WSATTransaction tran = null;
    private WSATCoordinator coordinator = null;

    private boolean prepared = false;

    @Before
    public void setup() throws Exception {
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());
        tran = new WSATTransaction(Utils.tranId(), 50);
        coordinator = new WSATCoordinator(tran.getGlobalId(), null);
    }

    @Test
    public void testReplayCompletion() {
        new MockClient() {
            @Override
            public void prepared() {
                prepared = true;
            }
        };

        prepared = false;
        CoordinatorResource testRes = new CoordinatorResource(coordinator);
        testRes.replayCompletion(tran.getGlobalId());
        assertTrue(prepared);
    }
}
