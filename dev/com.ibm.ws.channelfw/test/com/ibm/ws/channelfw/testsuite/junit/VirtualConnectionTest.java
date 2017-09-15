/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.testsuite.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.ws.channelfw.internal.InboundVirtualConnection;
import com.ibm.ws.channelfw.internal.OutboundVirtualConnectionImpl;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;

/**
 * Virtual connection unit tests.
 */
public class VirtualConnectionTest {
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
     * Test the inbound VC objects.
     */
    @Test
    public void testInboundVC() {
        try {
            VirtualConnectionFactory vcf = framework.getInboundVCFactory();
            assertNotNull(vcf);

            VirtualConnection vc = null;
            try {
                vc = vcf.createConnection();
                assertNotNull(vc);
                assertNotNull(vc.getStateMap());
            } catch (Exception e) {
                fail();
            }

            InboundVirtualConnection vic = null;
            try {
                vic = (InboundVirtualConnection) vc;
            } catch (Exception e) {
                fail("Failed trying to cast vc interface");
                return;
            }

            int[] testArray = new int[10];
            for (int i = testArray.length, j = 0; j < testArray.length; j++, i--) {
                testArray[j] = i;
            }
            vic.setDiscriminatorStatus(testArray);
            int[] arrayGathered = vic.getDiscriminatorStatus();
            assertNotNull(arrayGathered);

            assertEquals(testArray, arrayGathered);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testInboundVC", t);
        }
    }

    /**
     * Test outbound VC objects.
     */
    @Test
    public void testOutboundVC() {
        try {
            VirtualConnectionFactory vcf = null;
            framework.addChannel("tcpVCTEST", TCPChannelFactory.class, null, 5);
            framework.addChain("testOutboundVC", FlowType.OUTBOUND, new String[] { "tcpVCTEST" });
            vcf = framework.getOutboundVCFactory("testOutboundVC");
            assertNotNull(vcf);
            VirtualConnection vc = vcf.createConnection();
            assertNotNull(vc);
            assertNotNull(vc.getStateMap());

            assertTrue(vc instanceof OutboundVirtualConnection);
            OutboundVirtualConnectionImpl vocImpl = (OutboundVirtualConnectionImpl) vc;
            assertNotNull(vocImpl.getApplicationLink());
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testOutboundVC", t);
        }
    }
}
