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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.wsat.common.impl.WSATCoordinatorTran;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATParticipantState;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.test.MockClient;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 * ParticipantResource is an XA resource that is enlisted with the transaction on
 * the coordinator to represent a single remote participant.
 * 
 * Mostly this deals with making the WS-AT protocol calls to the remote system
 * and waiting for the (asynchronous) replies.
 */
public class ParticipantResourceTest {

    private WSATCoordinatorTran tran = null;
    private WSATParticipant participant = null;

    @BeforeClass
    public static void before() {
        System.setProperty("com.ibm.ws.wsat.asyncResponseTimeout", "100");
    }

    @AfterClass
    public static void after() {
        System.clearProperty("com.ibm.ws.wsat.asyncResponseTimeout");
    }

    @Before
    public void setup() throws Exception {
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());
        tran = new WSATCoordinatorTran(Utils.tranId(), 50);
        participant = new WSATParticipant(tran.getGlobalId(), "1", null);
    }

    @Test
    public void testPrepareVoteCommit() throws XAException {
        new MockClient() {
            @Override
            public void prepare() {
                participant.setResponse(WSATParticipantState.PREPARED);
            }
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        int vote = testRes.prepare(null);

        // Should vote OK to signal ready to commit and should keep participant active.
        assertEquals(XAResource.XA_OK, vote);
    }

    @Test
    public void testPrepareVoteReadonly() throws XAException {
        new MockClient() {
            @Override
            public void prepare() {
                participant.setResponse(WSATParticipantState.READONLY);
            }
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        int vote = testRes.prepare(null);

        // Should vote RDONLY to signal ready to commit and should remove participant
        assertEquals(XAResource.XA_RDONLY, vote);
    }

    @Test
    public void testPrepareVoteRollback() throws XAException {
        new MockClient() {
            @Override
            public void prepare() {
                participant.setResponse(WSATParticipantState.ABORTED);
            }
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        try {
            testRes.prepare(null);
            fail("Prepare did not get rollback exception");
        } catch (XAException e) {
            // Should get an XAException to signal rollback, don't care too much which one
            assertTrue(e.errorCode >= XAException.XA_RBBASE && e.errorCode <= XAException.XA_RBEND);
        }
    }

    @Test
    public void testPrepareVoteTimeout() throws XAException {
        new MockClient() {
            // Do nothing, so test will timeout
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        try {
            testRes.prepare(null);
            fail("Prepare did not get rollback exception");
        } catch (XAException e) {
            // Should get an XAException to signal rollback
            assertEquals(XAException.XA_RBTIMEOUT, e.errorCode);
        }
    }

    @Test
    public void testPrepareVoteError() throws XAException {
        new MockClient() {
            @Override
            public void prepare() throws WSATException {
                throw new WSATException("Error");
            }
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        try {
            testRes.prepare(null);
            fail("Prepare did not get rollback exception");
        } catch (XAException e) {
            // Should get an XAException to signal rollback
            assertEquals(XAException.XA_RBROLLBACK, e.errorCode);
        }
    }

    @Test
    public void testCommit() throws XAException {
        new MockClient() {
            @Override
            public void commit() {
                participant.setResponse(WSATParticipantState.COMMITTED);
            }
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        testRes.commit(null, false);
    }

    @Test
    public void testCommitTimeout() throws XAException {
        new MockClient() {
            // Do nothing, so test will timeout
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        try {
            testRes.commit(null, false);
            fail("Commit did not get rollback exception");
        } catch (XAException e) {
            assertTrue(e.errorCode == XAException.XA_RBTIMEOUT);
        }
    }

    @Test
    public void testRollback() throws XAException {
        new MockClient() {
            @Override
            public void rollback() {
                participant.setResponse(WSATParticipantState.ABORTED);
            }
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        testRes.rollback(null);
    }

    @Test
    public void testRollbackTimeout() throws XAException {
        new MockClient() {
            // Do nothing, so test will timeout
        };

        ParticipantResource testRes = new ParticipantResource(participant);
        try {
            testRes.rollback(null);
            fail("Commit did not get rollback exception");
        } catch (XAException e) {
            assertTrue(e.errorCode == XAException.XA_RBTIMEOUT);
        }
    }

    @Test
    public void testForget() throws XAException {
        ParticipantResource testRes = new ParticipantResource(participant);
        testRes.forget(null);
    }

    @Test
    public void testIsSameRM() throws XAException {
        ParticipantResource testRes1 = new ParticipantResource(participant);
        ParticipantResource testRes2 = new ParticipantResource(participant);
        assertTrue(testRes1.isSameRM(testRes2));

        InvocationHandler h = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
        };
        XAResource testRes3 = (XAResource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { XAResource.class }, h);
        assertFalse(testRes1.isSameRM(testRes3));
    }

    @Test
    public void testSetTimeout() throws XAException {
        ParticipantResource testRes1 = new ParticipantResource(participant);
        testRes1.setTransactionTimeout(123);
        assertEquals(123, testRes1.getTransactionTimeout());

        int timeout1 = 5432;
        int timeout2 = 65432;
        System.setProperty("com.ibm.ws.wsat.asyncResponseTimeout", Integer.toString(timeout1));
        WSATCoordinatorTran tran = new WSATCoordinatorTran(Utils.tranId(), timeout2);
        WSATParticipant part = new WSATParticipant(tran.getGlobalId(), "1", null);
        ParticipantResource testRes2 = new ParticipantResource(part);
        assertEquals(timeout1 / 1000, testRes2.getTransactionTimeout());
    }
}
