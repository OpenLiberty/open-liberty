/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.connmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.connmgmt.ConnectionHandle.ConnectionHandleFactory;

/**
 */
public class ConnectionHandleTest {
    private static SharedOutputManager outputMgr;
    private static final ChannelFramework framework = ChannelFrameworkFactory.getChannelFramework();

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * Verify exception paths
     */
    @Test
    public void testConnectionHandle() {
        try {
            VirtualConnectionFactory vcf = framework.getInboundVCFactory();
            assertNotNull(vcf);

            VirtualConnection vc;
            byte[] flatHandle;
            ConnectionHandle handle, first, second;

            vc = vcf.createConnection();
            assertNotNull(vc);

            handle = ConnectionHandle.getConnectionHandle(vc);
            flatHandle = handle.getBytes();
            first = new ConnectionHandle(flatHandle);
            assertTrue("Handle and first should be the same", first.equals(handle));

            // first & second should not be equal because their classes aren't the same
            // (ConnectionHandle vs. DummyConnectionHandle)
            second = new DummyConnectionHandle(first); // force re-use of connId & seqNum
            assertFalse("Handles of different classes should not be equal", first.equals(second));

            DummyConnectionHandle.DummyConnectionHandleFactory factory = new DummyConnectionHandle.DummyConnectionHandleFactory();
            second = factory.createConnectionHandle(first); // re-use connID & seqNum, different factory ID
            assertFalse("Handles with different factory ids should not be equal", first.equals(second));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testConnectionHandle", t);
        }
    }

    /**
     * Verify exception paths
     */
    @Test
    public void testConnectionHandleFactory() {
        try {
            long nxtId, nxtSeq;
            ConnectionHandle handle;
            ConnectionHandleFactory factory = ConnectionHandle.factory;

            Field nxtConnId = ConnectionHandleFactory.class.getDeclaredField("nextConnectionId");
            Field nxtSeqId = ConnectionHandleFactory.class.getDeclaredField("sharedSequenceNum");
            Field.setAccessible(new Field[] { nxtConnId, nxtSeqId }, true);

            ((AtomicLong) nxtConnId.get(factory)).set(Long.MAX_VALUE - 1);

            handle = factory.createConnectionHandle();
            nxtId = ((AtomicLong) nxtConnId.get(factory)).get();
            nxtSeq = ((AtomicInteger) nxtSeqId.get(factory)).get();
            assertEquals("Current value of connID should be MAX: " + handle.connID, handle.connID, Long.MAX_VALUE);
            assertEquals("Current value of nxtConnId should be MIN: " + nxtId, nxtId, Long.MIN_VALUE);
            assertEquals("Current value of seqId should be 1: " + handle.seqNum, handle.seqNum, 1);
            assertEquals("Current value of nxtSeqId should be 1: " + nxtSeq, nxtSeq, 1);

            ((AtomicLong) nxtConnId.get(factory)).set(Long.MAX_VALUE - 1);
            ((AtomicInteger) nxtSeqId.get(factory)).set(Integer.MAX_VALUE - 1);

            handle = factory.createConnectionHandle();
            nxtId = ((AtomicLong) nxtConnId.get(factory)).get();
            nxtSeq = ((AtomicInteger) nxtSeqId.get(factory)).get();
            assertEquals("Current value of connID should be MAX: " + handle.connID, handle.connID, Long.MAX_VALUE);
            assertEquals("Current value of nxtConnId should be MIN: " + nxtId, nxtId, Long.MIN_VALUE);
            assertEquals("Current value of seqId should be MAX: " + handle.seqNum, handle.seqNum, Integer.MAX_VALUE);
            assertEquals("Current value of nxtSeqId should be MIN: " + nxtSeq, nxtSeq, Integer.MIN_VALUE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testConnectionHandleFactory", t);
        }
    }

    /**
     * Verify connection types
     */
    @Test
    public void testConnectionTypes() {
        final String m = "testConnectionTypes";
        VirtualConnectionFactory vcf;

        ConnectionHandle handle;
        VirtualConnection vc;

        try {
            vcf = framework.getInboundVCFactory();
            assertNotNull(vcf);

            vc = vcf.createConnection();
            assertNotNull(vc);

            // w/o pre-specification, default type should be null (TCPChannel assigns default type, not VC)
            handle = ConnectionHandle.getConnectionHandle(vc);
            assertEquals(m + "1: Default inbound type should be present", null, handle.getConnectionType());
            assertTrue(m + "1a: With no handle in VC, connection should be inbound", handle.isInbound());
            assertFalse(m + "1b: With no handle in VC, connection should be inbound", handle.isOutbound());

            // Clear previous connection handle and connection type
            vc.getStateMap().remove(ConnectionHandle.CONNECTION_HANDLE_VC_KEY);
            vc.getStateMap().remove(ConnectionType.CONNECTION_TYPE_VC_KEY);

            // Should not be able to set Outbound ConnectionType on Inbound VC
            try {
                ConnectionType.setVCConnectionType(vc, ConnectionType.OUTBOUND);
                fail("Missed IllegalStateException for setting OUTBOUND type on inbound connection");
            } catch (Exception e) {
                // Expected exception
            }

            // Clear previous connection handle and connection type
            vc.getStateMap().remove(ConnectionHandle.CONNECTION_HANDLE_VC_KEY);
            vc.getStateMap().remove(ConnectionType.CONNECTION_TYPE_VC_KEY);

            // Should be able to assign internal type to inbound connection
            ConnectionType.setVCConnectionType(vc, ConnectionType.INTERNAL_CR_SR);
            handle = ConnectionHandle.getConnectionHandle(vc);
            assertEquals(m + "2: Should have provided connection type", ConnectionType.INTERNAL_CR_SR, handle.getConnectionType());
            assertFalse(m + "2a: Internal connection is neither inbound nor outbound", handle.isInbound());
            assertFalse(m + "2b: With no handle in VC, connection should be inbound", handle.isOutbound());

            // Clear previous connection handle and connection type
            vc.getStateMap().remove(ConnectionHandle.CONNECTION_HANDLE_VC_KEY);
            vc.getStateMap().remove(ConnectionType.CONNECTION_TYPE_VC_KEY);

            framework.addChannel("connHandleTCP", TCPChannelFactory.class, null, 5);
            framework.addChain("connHandleChain", FlowType.OUTBOUND, new String[] { "connHandleTCP" });
            vcf = framework.getOutboundVCFactory("connHandleChain");
            assertNotNull(vcf);

            vc = vcf.createConnection();
            assertNotNull(vc);

            // w/o pre-specification, TCP channel sets default type
            handle = ConnectionHandle.getConnectionHandle(vc);
            assertEquals(m + "3: Default outbound type should be present", ConnectionType.OUTBOUND, handle.getConnectionType());
            assertFalse(m + "3a: Default handle in VC, connection should be outbound", handle.isInbound());
            assertTrue(m + "3b: Default handle in VC, connection should be outbound", handle.isOutbound());

            // Clear previous connection handle and connection type
            vc.getStateMap().remove(ConnectionHandle.CONNECTION_HANDLE_VC_KEY);
            vc.getStateMap().remove(ConnectionType.CONNECTION_TYPE_VC_KEY);

            // Should not be able to set inbound ConnectionType on outbound VC
            try {
                ConnectionType.setVCConnectionType(vc, ConnectionType.INBOUND);
                fail("Missed IllegalStateException for setting INBOUND type on outbound connection");
            } catch (Exception e) {
                // Expected exception
            }

            // Pre-set an outbound connection type
            ConnectionType.setVCConnectionType(vc, ConnectionType.OUTBOUND_CR_TO_REMOTE);
            handle = ConnectionHandle.getConnectionHandle(vc);
            assertEquals(m + "4: Should have provided connection type", ConnectionType.OUTBOUND_CR_TO_REMOTE, handle.getConnectionType());
            assertFalse(m + "4a: Connection should be outbound", handle.isInbound());
            assertTrue(m + "4b: Connection should be outbound", handle.isOutbound());

            // Clear previous connection handle and connection type
            vc.getStateMap().remove(ConnectionHandle.CONNECTION_HANDLE_VC_KEY);
            vc.getStateMap().remove(ConnectionType.CONNECTION_TYPE_VC_KEY);

            // Should be able to set internal type on an outbound connection
            ConnectionType.setVCConnectionType(vc, ConnectionType.INTERNAL_CR_SR);

            // Internal connections are neither inbound nor outbound
            handle = ConnectionHandle.getConnectionHandle(vc);
            assertEquals(m + "5: Internal connection type", ConnectionType.INTERNAL_CR_SR, handle.getConnectionType());
            assertFalse(m + "5a: Connection should not be inbound", handle.isInbound());
            assertFalse(m + "5b: Connection should not be outbound", handle.isOutbound());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    static class DummyConnectionHandle extends ConnectionHandle {
        /**
         * This will create a DummyConnectionHandle with all of the same
         * values (connId, seqNum, & factory) as the original handle.
         * When compared, the two handles should not be equal: different classes
         */
        protected DummyConnectionHandle(ConnectionHandle handle) {
            super(handle.connID, handle.seqNum, handle.connHandleCreatorId);
        }

        static class DummyConnectionHandleFactory extends ConnectionHandleFactory {
            /**
             * Create a connection handle object.
             * 
             * @param handle
             * @return ConnectionHandle
             */
            public ConnectionHandle createConnectionHandle(ConnectionHandle handle) {
                return new ConnectionHandle(handle.connID, handle.seqNum, (byte) 0x02);
            }
        }
    }

    final static String digits = "0123456789abcdef";

    /**
     * Converts a byte array to a hexadecimal string.
     * 
     * @param b
     * @return String
     */
    public static String toHexString(byte[] b) {
        StringBuffer result = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            result.append(digits.charAt((b[i] >> 4) & 0xf));
            result.append(digits.charAt(b[i] & 0xf));
        }
        return (result.toString());
    }
}