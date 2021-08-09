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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.Serializable;

import javax.transaction.Status;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.test.MockClient;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;

/**
 *
 */
public class HandlerImplTest {

    private HandlerImpl handler;

    private TestLocalTranMgr tranMgr;
    private TestUOWMgr uowMgr;
    private TestRemoteTranMgr remoteMgr;

    private EndpointReferenceType epr;
    private EndpointReferenceType coordEpr;

    @Before
    public void setUp() throws Exception {
        handler = new HandlerImpl();
        tranMgr = new TestLocalTranMgr();
        uowMgr = new TestUOWMgr();
        remoteMgr = new TestRemoteTranMgr();

        Utils.setTranService("localTranMgr", tranMgr);
        Utils.setTranService("remoteTranMgr", remoteMgr);
        Utils.setTranService("uowManager", uowMgr);
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());

        epr = new EndpointReferenceType();
        AttributedURIType uri = new AttributedURIType();
        uri.setValue("http://www.example.com/endpoint");
        epr.setAddress(uri);

        RegistrationImpl.getInstance().setRegistrationEndpoint(epr);
        ProtocolImpl.getInstance().setCoordinatorEndpoint(epr);
        ProtocolImpl.getInstance().setParticipantEndpoint(epr);

        new MockClient();
    }

    @After
    public void afterTest() {
        tranMgr = null;
        uowMgr = null;
        remoteMgr = null;
    }

    @Test
    public void testClientRequestNoTran() throws WSATException {
        tranMgr.status = Status.STATUS_UNKNOWN;
        WSATContext ctx = handler.clientRequest();
        assertNull(ctx);
    }

    @Test
    public void testClientRequestNewTran() throws WSATException {
        WSATContext ctx = handler.clientRequest();
        assertNotNull(ctx);
        assertFalse(remoteMgr.remoteRegister);

        assertEquals(remoteMgr.globalId, ctx.getId());
        assertTrue(ctx.getRegistration() instanceof EndpointReferenceType);
        assertTrue(ctx.getExpires() > 0 && ctx.getExpires() <= 5000);
    }

    @Test
    public void testClientRequestPrevTran() throws WSATException {
        WSATContext ctx = handler.clientRequest();
        assertNotNull(ctx);
        assertFalse(remoteMgr.remoteRegister);

        remoteMgr.remoteRegister = false;
        WSATContext ctx2 = handler.clientRequest();
        assertNotNull(ctx2);
        assertFalse(remoteMgr.remoteRegister);
        assertEquals(ctx.getId(), ctx2.getId());
        assertEquals(ctx.getExpires(), ctx2.getExpires());
        assertEquals(ctx.getRegistration(), ctx2.getRegistration());
    }

    @Test
    public void testClientRequestExpiredTran() throws WSATException {
        uowMgr.expiry = -1000;
        try {
            handler.clientRequest();
            fail("Expected WSATException");
        } catch (WSATException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("CWLIB0203E"));
        }
    }

    @Test
    public void testClientResonse() throws WSATException {
        handler.clientRequest();
        handler.clientResponse();
        assertEquals(remoteMgr.globalId, remoteMgr.unexportId);
    }

    @Test
    public void testClientResponseNoRequest() throws WSATException {
        handler.clientResponse();
        assertNull(remoteMgr.unexportId);
    }

    @Test
    public void testClientFault() throws WSATException {
        handler.clientRequest();
        handler.clientFault();
        assertEquals(remoteMgr.globalId, remoteMgr.unexportId);
    }

    @Test
    public void testServerRequestNewTran() throws WSATException {
        new MockClient() {
            @Override
            public EndpointReferenceType register(EndpointReferenceType epr) {
                coordEpr = epr;
                return null;
            }
        };

        String tranId = Utils.tranId();
        handler.serverRequest(tranId, epr, 1000);
        assertEquals(tranId, remoteMgr.importId);
        assertEquals(1, remoteMgr.importTimeout);
        assertNotNull(coordEpr);
        assertTrue(remoteMgr.recoveryRegister);
    }

    @Test
    public void testServerRequestPrevTran() throws WSATException {
        new MockClient() {
            @Override
            public EndpointReferenceType register(EndpointReferenceType epr) {
                coordEpr = epr;
                return null;
            }
        };

        String tranId = Utils.tranId();
        handler.serverRequest(tranId, epr, 1000);
        assertEquals(tranId, remoteMgr.importId);
        assertEquals(1, remoteMgr.importTimeout);
        coordEpr = null;

        remoteMgr.importId = tranId;
        remoteMgr.recoveryRegister = false;
        handler.serverRequest(tranId, epr, 1000);
        assertNull(coordEpr);
        assertFalse(remoteMgr.recoveryRegister);
    }

    @Test
    public void testServerResponse() throws WSATException {
        String tranId = Utils.tranId();
        handler.serverRequest(tranId, epr, 1000);
        handler.serverResponse();
        assertEquals(remoteMgr.unimportId, tranId);
        assertFalse(tranMgr.rollback);
    }

    @Test
    public void testServerResponseNoRequest() throws WSATException {
        assertNull(remoteMgr.unimportId);
        handler.serverResponse();
        assertNull(remoteMgr.unimportId);
    }

    @Test
    public void testServerFault() throws WSATException {
        String tranId = Utils.tranId();
        handler.serverRequest(tranId, epr, 1000);
        handler.serverFault();
        assertEquals(remoteMgr.unimportId, tranId);
        assertFalse(tranMgr.rollback);
    }

    /*
     * Mock Transaction services for these tests
     */

    public static class TestLocalTranMgr extends MockProxy {

        int status = Status.STATUS_ACTIVE;
        boolean rollback = false;

        public int getStatus() {
            return status;
        }

        public void setRollbackOnly() {
            rollback = true;
        }
    }

    public static class TestUOWMgr extends MockProxy {

        long expiry = 2000;

        public long getUOWExpiration() {
            return System.currentTimeMillis() + expiry;
        }
    }

    public static class TestRemoteTranMgr extends MockProxy {

        String globalId = Utils.tranId();
        boolean remoteRegister = false;
        boolean recoveryRegister = false;
        int importTimeout;
        String importId;
        String unexportId;
        String unimportId;

        public String exportTransaction() {
            return globalId;
        }

        public void unexportTransaction(String id) {
            unexportId = id;
        }

        public String getGlobalId() {
            return globalId;
        }

        public boolean importTransaction(String id, int timeout) {
            boolean created = (importId == null);
            importId = id;
            importTimeout = timeout;
            return created;
        }

        public void unimportTransaction(String id) {
            unimportId = id;
        }

        public boolean registerRemoteParticipant(String xaResFactoryFilter, Serializable xaResInfo, String globalId) {
            remoteRegister = true;
            return true;
        }

        public boolean registerRecoveryCoordinator(String recovFactoryFilter, Serializable recovInfo, String globalId) {
            recoveryRegister = true;
            return true;
        }
    }
}
