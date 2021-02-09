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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATCoordinatorTran;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATFault;
import com.ibm.ws.wsat.service.WSATFaultException;
import com.ibm.ws.wsat.test.MockClient;
import com.ibm.ws.wsat.test.MockLoader;
import com.ibm.ws.wsat.test.MockProxy;
import com.ibm.ws.wsat.test.Utils;
import com.ibm.ws.wsat.tm.impl.ParticipantFactoryService;

/**
 * WS-Coord activation and registration service
 */
public class RegistrationImplTest {

    private final RegistrationImpl service = RegistrationImpl.getInstance();

    private EndpointReferenceType epr;
    private final TestRemoteTranMgr remoteMgr = new TestRemoteTranMgr();

    private EndpointReferenceType partEpr;

    @Before
    public void setup() throws Exception {
        Utils.setTranService("remoteTranMgr", remoteMgr);
        Utils.setTranService("clService", new MockLoader());
        Utils.setTranService("syncRegistry", new MockProxy());

        epr = new EndpointReferenceType();
        AttributedURIType uri = new AttributedURIType();
        uri.setValue("http://www.example.com/endpoint");
        epr.setAddress(uri);
        epr.setReferenceParameters(new ReferenceParametersType());

        ProtocolImpl.getInstance().setCoordinatorEndpoint(epr);
        ProtocolImpl.getInstance().setParticipantEndpoint(epr);

        new MockClient();
    }

    @Test
    public void testGetEndpointReference() throws WSATException {
        String tranId = Utils.tranId();
        service.setRegistrationEndpoint(epr);
        EndpointReferenceType epr1 = service.getRegistrationEndpoint(tranId);
        assertEquals(1, epr1.getReferenceParameters().getAny().size());
        Object rp = epr1.getReferenceParameters().getAny().get(0);
        assertTrue(rp instanceof JAXBElement<?>);
        assertEquals(tranId, ((JAXBElement<String>) rp).getValue());
    }

    @Test
    public void testActivateCoordinator() throws WSATException {
        String tranId = Utils.tranId();
        WSATContext ctx = service.activate(tranId, 1000, false);
        assertNotNull(ctx);
        assertNotNull(WSATTransaction.getCoordTran(tranId));

        assertEquals(ctx.getId(), tranId);
        assertNotNull(ctx.getRegistration());
        assertEquals(1000, ctx.getExpires());
    }

    @Test
    public void testActivateCoordTimeout() throws WSATException {
        try {
            service.activate(Utils.tranId(), -1000, false);
            fail("Expected timeout");
        } catch (WSATException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("CWLIB0203E"));
        }
    }

    @Test
    public void testActivateParticipant() throws WSATException {
        String tranId = Utils.tranId();
        WSATContext ctx = service.activate(tranId, 1000, false);
        assertNotNull(ctx);
        assertNotNull(WSATTransaction.getTran(tranId));

        assertEquals(ctx.getId(), tranId);
        assertNotNull(ctx.getRegistration());
        assertEquals(1000, ctx.getExpires());
    }

    @Test
    public void testRegister() throws WSATException {
        String tranId = Utils.tranId();
        WSATCoordinatorTran tran = new WSATCoordinatorTran(tranId, 1000);
        tran.setCoordinator(epr);
        WSATTransaction.putTran(tran);
        EndpointReferenceType epr1 = service.register(tranId, epr);
        assertNotNull(epr1);

        assertTrue(remoteMgr.registerRemote);
        assertEquals("(" + Constants.WS_FACTORY_PART + ")", remoteMgr.factoryFilter);
        assertTrue(remoteMgr.info instanceof ArrayList);
        assertTrue(ParticipantFactoryService.deserialize(remoteMgr.info) instanceof WSATParticipant);
        assertEquals(tranId, remoteMgr.remoteId);
    }

    @Test
    public void testRegisterNoCoord() throws WSATException {
        try {
            service.register(Utils.tranId(), epr);
            fail("No coordinator tran");
        } catch (WSATFaultException e) {
            WSATFault fault = e.getFault();
            assertEquals("Sender", fault.getCode());
            assertEquals("CannotRegisterParticipant", fault.getSubcode().getLocalPart());
            assertTrue(fault.getDetail().startsWith("CWLIB0201E"));
        }
    }

    @Test
    public void testRegisterParticipantTest() throws WSATException {
        new MockClient() {
            @Override
            public EndpointReferenceType register(EndpointReferenceType participant) throws WSATException {
                partEpr = participant;
                return epr;
            }
        };

        String tranId = Utils.tranId();
        WSATTransaction tran = new WSATTransaction(tranId, 1000);
        service.registerParticipant(tranId, tran);
        assertNotNull(partEpr);

        assertTrue(remoteMgr.registerRecovery);
        assertEquals("(" + Constants.WS_FACTORY_COORD + ")", remoteMgr.factoryFilter);
        assertTrue(remoteMgr.info instanceof ArrayList);
        assertTrue(ParticipantFactoryService.deserialize(remoteMgr.info) instanceof WSATCoordinator);
        assertEquals(tranId, remoteMgr.remoteId);
    }

    /*
     * Mock Transaction services for these tests
     */

    public static class TestRemoteTranMgr extends MockProxy {

        boolean registerRemote = false;
        boolean registerRecovery = false;
        String factoryFilter;
        Serializable info;
        String remoteId;

        public boolean registerRemoteParticipant(String xaResFactoryFilter, Serializable xaResInfo, String globalId) {
            registerRemote = true;
            factoryFilter = xaResFactoryFilter;
            info = xaResInfo;
            remoteId = globalId;
            return true;
        }

        public boolean registerRecoveryCoordinator(String recovFactoryFilter, Serializable recovInfo, String globalId) {
            registerRecovery = true;
            factoryFilter = recovFactoryFilter;
            info = recovInfo;
            remoteId = globalId;
            return true;
        }
    }
}
