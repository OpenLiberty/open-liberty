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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.channelfw.internal.ChannelDataImpl;
import com.ibm.ws.channelfw.internal.InboundVirtualConnection;
import com.ibm.ws.channelfw.internal.InboundVirtualConnectionFactoryImpl;
import com.ibm.ws.channelfw.internal.discrim.DiscriminationProcessImpl;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.base.InboundProtocolLink;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;

/**
 * Test case for the various discrimination objects.
 */
@SuppressWarnings("unused")
public class DiscriminationTest {
    private static SharedOutputManager outputMgr;

    protected boolean channelCheck = false;
    protected int noCheckCount = 0;
    protected int maybeCheckCount = 0;
    protected Class<?> commonCorrectClass = Integer.class;
    protected Class<?> commonWrongClass = String.class;
    private final Discriminator discA = new SimpleTestDiscriminator(idA);
    private final Discriminator discB = new SimpleTestDiscriminator(idB);
    private final Discriminator discC = new SimpleTestDiscriminator(idC);
    private final Discriminator discD = new SimpleTestDiscriminator(idD);
    private final String channelNameA = "A";
    private static final int idA = 1;
    private static final int idB = 2;
    private static final int idC = 3;
    private static final int idD = 4;
    private static final int idE = 5;

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
     * Test simple discrimination.
     */
    @Test
    public void testChannelLink() {
        try {
            DiscriminationProcessImpl dp = new DiscriminationProcessImpl(Integer.class, channelNameA);
            // Single discriminator test
            SimpleTestDiscriminator testA = new SimpleTestDiscriminator(idC);
            testA.setChannel(new SimpleChannel(new SimpleCCa(), testA));
            try {
                dp.addDiscriminator(testA, idC);
            } catch (DiscriminationProcessException e) {
                fail("adddiscrim failure: " + e.getMessage());
            }
            dp.start();
            ConnectionLink myLink = new SimpleLink();
            int i = dp.discriminate(null, myLink, "MyName");
            assertEquals(DiscriminationProcess.SUCCESS, i);
            assertTrue(myLink.getApplicationCallback() instanceof SimpleLinka);
            assertEquals(DiscriminationProcess.FAILURE, dp.discriminate(null, myLink, "NotReallyMyName"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testChannelLink", t);
        }
    }

    /**
     * Test a discrimination failure.
     */
    @Test
    public void testFailure() {
        try {
            DiscriminationProcessImpl dp = new DiscriminationProcessImpl(Integer.class, channelNameA);
            dp.start();
            ConnectionLink myLink = new SimpleLink();
            assertEquals(DiscriminationProcess.FAILURE, dp.discriminate(null, "hello world", myLink));
            assertEquals(DiscriminationProcess.FAILURE, dp.discriminate(null, myLink, "hello world"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testFailure", t);
        }
    }

    /**
     * Test discrimination runtime permutations.
     */
    @Test
    public void testRuntime() {
        try {
            DiscriminationProcessImpl dp = new DiscriminationProcessImpl(Integer.class, channelNameA);
            // Single discriminator test
            SimpleNoDiscriminator noA = new SimpleNoDiscriminator(idC);
            noA.setChannel(new SimpleChannelCheck(new SimpleCC(), noA, this));
            dp.addDiscriminator(noA, idC);
            // test running discrimination when dp not started
            try {
                assertEquals(DiscriminationProcess.FAILURE,
                             dp.discriminate(null, new Integer(5), new SimpleLink()));
            } catch (DiscriminationProcessException e) {
                fail("discrim failure: " + e.getMessage());
            }
            dp.start();
            // Even though this discriminator always returns no...we should get a success response
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(null, new Integer(5), new SimpleLink()));
            assertFalse(channelCheck);
            // two "NO"s in a process should still return yes
            dp = new DiscriminationProcessImpl(commonCorrectClass, dp);
            SimpleNoDiscriminator noB = new SimpleNoDiscriminator(idA);
            noB.setChannel(new SimpleChannel(new SimpleCC(), noB));
            dp.addDiscriminator(noB, idA);
            dp.start();
            VirtualConnectionFactory vcf = new InboundVirtualConnectionFactoryImpl();
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(vcf.createConnection(), new Integer(5), new SimpleLink()));
            // single maybe, 2 nos
            dp = new DiscriminationProcessImpl(commonCorrectClass, dp);
            SimpleMaybeDiscriminator maybeA = new SimpleMaybeDiscriminator(idB);
            maybeA.setChannel(new SimpleChannel(new SimpleCC(), maybeA));
            dp.addDiscriminator(maybeA, idB);
            dp.start();
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(vcf.createConnection(), new Integer(5), new SimpleLink()));
            // 2 maybes, 2 nos
            dp = new DiscriminationProcessImpl(commonCorrectClass, dp);
            SimpleMaybeDiscriminator maybeB = new SimpleMaybeDiscriminator(idD);
            maybeB.setChannel(new SimpleChannel(new SimpleCC(), maybeB));
            dp.addDiscriminator(maybeB, idD);
            dp.start();
            assertEquals(DiscriminationProcess.AGAIN,
                         dp.discriminate(vcf.createConnection(), new Integer(5), new SimpleLink()));
            // 2 maybes, 2 nos, 1 yes
            dp = new DiscriminationProcessImpl(commonCorrectClass, dp);
            SimpleYesDiscriminator yesA = new SimpleYesDiscriminator(idE);
            yesA.setChannel(new SimpleChannel(new SimpleCC(), yesA));
            dp.addDiscriminator(yesA, idE);
            dp.start();
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(vcf.createConnection(), new Integer(5), new SimpleLink()));
            // 2 Maybes, 2 nos, 1 test should test the channel well.
            dp = new DiscriminationProcessImpl(commonCorrectClass, dp);
            // remove last yes
            dp.removeDiscriminator(yesA);
            yesA = null;
            SimpleTestDiscriminator testC = new SimpleTestDiscriminator(idC);
            testC.setChannel(new SimpleChannel(new SimpleCC(), testC));
            dp.addDiscriminator(testC, idC);
            SimpleTestDiscriminator testE = new SimpleTestDiscriminator(idE);
            testE.setChannel(new SimpleChannelCheck(new SimpleCC(), testE, this));
            dp.addDiscriminator(testE, idE);
            dp.start();
            // test number 10: testA should fire
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(vcf.createConnection(), new Integer(idC), new SimpleLink()));
            // test number 11: should return AGAIN
            assertEquals(DiscriminationProcess.AGAIN,
                         dp.discriminate(vcf.createConnection(), new Integer(idD), new SimpleLink()));
            // test number 12: testB should fire
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(vcf.createConnection(), new Integer(idE), new SimpleLink()));
            assertFalse(channelCheck);
            InboundVirtualConnection vc = (InboundVirtualConnection) vcf.createConnection();
            // Check state insertion to VC
            assertEquals(DiscriminationProcess.AGAIN,
                         dp.discriminate(vc, new Integer(idA), new SimpleLink()));
            assertNotNull(vc.getDiscriminatorStatus());
            // Check state info cleared after VC finishes
            // there was one idA and one idB. the only idB is a MAYBE always discriminator
            assertEquals(Discriminator.MAYBE, vc.getDiscriminatorStatus()[1]);
            // still again because "NO" state is remembered
            assertEquals(DiscriminationProcess.AGAIN,
                         dp.discriminate(vc, new Integer(idE), new SimpleLink()));
            assertNotNull(vc.getDiscriminatorStatus());
            // this should not set state
            vc = (InboundVirtualConnection) vcf.createConnection();
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(vc, new Integer(idE), new SimpleLink()));
            assertNull(vc.getDiscriminatorStatus());
            // Check to see algorithm uses states
            // 2 maybes, 2 nos, 2 tests
            dp = new DiscriminationProcessImpl(commonCorrectClass, dp);
            SimpleNoCheckDiscriminator noCheck = new SimpleNoCheckDiscriminator(idB, this);
            noCheck.setChannel(new SimpleChannel(new SimpleCC(), noCheck));
            dp.addDiscriminator(noCheck, idB);
            SimpleMaybeCheckDiscriminator maybeCheck = new SimpleMaybeCheckDiscriminator(idE, this);
            maybeCheck.setChannel(new SimpleChannel(new SimpleCC(), maybeCheck));
            dp.addDiscriminator(maybeCheck, idE);
            dp.start();
            vc = (InboundVirtualConnection) vcf.createConnection();
            assertEquals(DiscriminationProcess.AGAIN,
                         dp.discriminate(vc, new Integer(idA), new SimpleLink()));
            assertEquals(1, maybeCheckCount);
            assertEquals(1, noCheckCount);
            assertEquals(DiscriminationProcess.AGAIN,
                         dp.discriminate(vc, new Integer(idA), new SimpleLink()));
            assertEquals(2, maybeCheckCount);
            assertEquals(1, noCheckCount);
            assertEquals(DiscriminationProcess.SUCCESS,
                         dp.discriminate(vc, new Integer(idA), new SimpleLink()));
            assertEquals(1, noCheckCount);
            assertNull(vc.getDiscriminatorStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testRuntime", t);
        }
    }

    /**
     * Test discrimination processes changing on the fly.
     */
    @Test
    public void testChangingDP() {
        try {
            DiscriminationProcessImpl dp = new DiscriminationProcessImpl(Integer.class, channelNameA);
            SimpleMaybeDiscriminator maybeA = new SimpleMaybeDiscriminator(idA);
            maybeA.setChannel(new SimpleChannel(new SimpleCC(), maybeA));
            dp.addDiscriminator(maybeA, idA);
            SimpleNoDiscriminator noB = new SimpleNoDiscriminator(idB);
            noB.setChannel(new SimpleChannel(new SimpleCC(), noB));
            dp.addDiscriminator(noB, idB);
            SimpleMaybeDiscriminator maybeC = new SimpleMaybeDiscriminator(idC);
            maybeC.setChannel(new SimpleChannel(new SimpleCC(), maybeC));
            dp.addDiscriminator(maybeC, idC);
            dp.start();
            VirtualConnectionFactory vcf = new InboundVirtualConnectionFactoryImpl();
            VirtualConnection vc = vcf.createConnection();
            ConnectionLink cl = new SimpleLink();
            // maybe - no - maybe
            assertEquals(DiscriminationProcess.AGAIN, dp.discriminate(vc, vc, cl));
            DiscriminationProcessImpl newdp = new DiscriminationProcessImpl(Integer.class, dp);
            newdp.removeDiscriminator(noB);
            SimpleYesDiscriminator yesB = new SimpleYesDiscriminator(idB);
            yesB.setChannel(new SimpleChannel(new SimpleCC(), yesB));
            newdp.addDiscriminator(yesB, idB);
            newdp.start();
            // maybe - yes - maybe
            assertEquals(DiscriminationProcess.SUCCESS, newdp.discriminate(vc, vc, cl));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testChangingDP", t);
        }
    }

    /**
     * Test discrimination config.
     */
    @Test
    public void testConfig() {
        try {
            DiscriminationProcessImpl dp = new DiscriminationProcessImpl(Integer.class, channelNameA);
            assertNotNull(dp);
            dp.addDiscriminator(discA, idA);
            // check to see if the discriminator was added
            List<Discriminator> al = dp.getDiscriminators();
            assertNotNull(al);
            Discriminator shouldBeA = al.get(0);
            assertNotNull(shouldBeA);
            assertEquals(idA, shouldBeA.getWeight());
            // add a second discriminator
            dp.addDiscriminator(discB, idB);
            // check to see if the discriminator was added
            al = dp.getDiscriminators();
            assertNotNull(al);
            Discriminator shouldBeB = al.get(1);
            assertNotNull(shouldBeB);
            assertEquals(idB, shouldBeB.getWeight());
            // test add with already there discriminator
            dp.addDiscriminator(discA, idA);
            // check to see if the discriminator was added
            al = dp.getDiscriminators();
            assertNotNull(al);
            assertEquals(2, al.size());
            // test add illegal discriminator weight
            try {
                dp.addDiscriminator(discC, -1);
                fail("negative weight should have thrown exception");
            } catch (DiscriminationProcessException e) {
                // nothing
            }
            // check to see if the discriminator was added
            al = dp.getDiscriminators();
            assertEquals(2, al.size());
            // Test removing a discriminator
            dp.removeDiscriminator(discA);
            // check to see if the discriminator was added
            al = dp.getDiscriminators();
            assertEquals(1, al.size());
            // make sure we still have discB
            assertEquals(idB, al.get(0).getWeight());
            // start the discrimination Process
            dp.start();
            // Test adding discriminators to running DP
            try {
                dp.addDiscriminator(discC, idC);
                fail("adding discrim to running dp should have failed");
            } catch (DiscriminationProcessException e) {
                // nothing
            }
            // Check to see if this discriminator still made it in
            al = dp.getDiscriminators();
            assertFalse(al.contains(discC));
            // removing while running
            try {
                dp.removeDiscriminator(discB);
                fail("removing discrim from running dp should have failed");
            } catch (DiscriminationProcessException e) {
                // nothing
            }
            al = dp.getDiscriminators();
            assertTrue(al.contains(discB));
            // create a new DP from a running Dp
            DiscriminationProcessImpl newDP = new DiscriminationProcessImpl(commonCorrectClass, dp);
            assertNotNull(newDP);
            // Check to see if this discriminator still made it in
            al = newDP.getDiscriminators();
            assertTrue(al.contains(discB));
            newDP.addDiscriminator(discD, idD);
            // check to see if the discriminator was added
            al = newDP.getDiscriminators();
            assertTrue(al.contains(discD));
            // test incoherent class
            Discriminator incoherentDisc = new SimpleIncoherentDiscriminator(10);
            try {
                newDP.addDiscriminator(incoherentDisc, 10);
                fail("adding incoherent discrim should have failed");
            } catch (ClassCastException e) {
                // expected failure
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testConfig", t);
        }
    }

    private class SimpleTestDiscriminator implements Discriminator {
        private int id = 0;
        private Channel c = null;

        protected SimpleTestDiscriminator(int id) {
            this.id = id;
            c = new SimpleChannel(new SimpleCC(), this);
        }

        @Override
        public int discriminate(VirtualConnection vc, Object data) {
            if (((Integer) data).intValue() == id) {
                return Discriminator.YES;
            }
            return Discriminator.NO;
        }

        protected void setChannel(Channel c) {
            this.c = c;
        }

        @Override
        public Class<?> getDiscriminatoryDataType() {
            return commonCorrectClass;
        }

        @Override
        public Channel getChannel() {
            return c;
        }

        @Override
        public int getWeight() {
            return id;
        }

        @Override
        public void cleanUpState(VirtualConnection vc) {
            // Nothing to clean up.
        }
    }

    private class SimpleYesDiscriminator implements Discriminator {
        private int id = 0;
        private Channel c = null;

        protected SimpleYesDiscriminator(int id) {
            this.id = id;
        }

        protected void setChannel(Channel c) {
            this.c = c;
        }

        @Override
        public int discriminate(VirtualConnection vc, Object data) {
            return Discriminator.YES;
        }

        @Override
        public Class<?> getDiscriminatoryDataType() {
            return commonCorrectClass;
        }

        @Override
        public Channel getChannel() {
            return c;
        }

        @Override
        public int getWeight() {
            return id;
        }

        @Override
        public void cleanUpState(VirtualConnection vc) {
            // Nothing to clean up.
        }
    }

    private class SimpleMaybeDiscriminator implements Discriminator {
        private Channel c = null;
        private int id = 0;

        protected SimpleMaybeDiscriminator(int id) {
            this.id = id;
        }

        protected void setChannel(Channel c) {
            this.c = c;
        }

        @Override
        public int discriminate(VirtualConnection vc, Object data) {
            return Discriminator.MAYBE;
        }

        @Override
        public Class<?> getDiscriminatoryDataType() {
            return commonCorrectClass;
        }

        @Override
        public Channel getChannel() {
            return c;
        }

        @Override
        public int getWeight() {
            return id;
        }

        @Override
        public void cleanUpState(VirtualConnection vc) {
            // Nothing to clean up.
        }
    }

    private class SimpleNoDiscriminator implements Discriminator {
        private int id = 0;
        private Channel c = null;

        protected SimpleNoDiscriminator(int id) {
            this.id = id;
        }

        protected void setChannel(Channel c) {
            this.c = c;
        }

        @Override
        public int discriminate(VirtualConnection vc, Object data) {
            return Discriminator.NO;
        }

        @Override
        public Class<?> getDiscriminatoryDataType() {
            return commonCorrectClass;
        }

        @Override
        public Channel getChannel() {
            return c;
        }

        @Override
        public int getWeight() {
            return id;
        }

        @Override
        public void cleanUpState(VirtualConnection vc) {
            // Nothing to clean up.
        }
    }

    private class SimpleIncoherentDiscriminator implements Discriminator {
        private int id = 0;

        protected SimpleIncoherentDiscriminator(int id) {
            this.id = id;
        }

        @Override
        public int discriminate(VirtualConnection vc, Object data) {
            return Discriminator.MAYBE;
        }

        @Override
        public Class<?> getDiscriminatoryDataType() {
            return commonWrongClass;
        }

        @Override
        public Channel getChannel() {
            return null;
        }

        @Override
        public int getWeight() {
            return id;
        }

        @Override
        public void cleanUpState(VirtualConnection vc) {
            // Nothing to clean up.
        }
    }

    private class SimpleChannel implements InboundChannel {
        private Discriminator d = null;
        /** Channel configuration object */
        private ChannelData config = null;
        /** DiscriminationProcess */
        private DiscriminationProcess discriminationProcess = null;

        protected SimpleChannel(ChannelData cc, Discriminator d) {
            this.d = d;
            this.config = cc;
        }

        @Override
        public ConnectionLink getConnectionLink(VirtualConnection vc) {
            if (this.config instanceof SimpleCCa) {
                return new SimpleLinka();
            }
            return new SimpleLink();
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#start()
         */
        @Override
        public void start() {
            // nothing
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#stop(int)
         */
        @Override
        public void stop(long millisec) {
            // nothing
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#init()
         */
        @Override
        public void init() {
            // nothing
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#destroy()
         */
        @Override
        public void destroy() {
            // nothing
        }

        @Override
        public Class<?> getApplicationInterface() {
            return null;
        }

        @Override
        public Class<?> getDeviceInterface() {
            return null;
        }

        @Override
        public void update(ChannelData cc) {
            // nothing
        }

        @Override
        public Discriminator getDiscriminator() {
            return d;
        }

        @Override
        public Class<?> getDiscriminatoryType() {
            return null;
        }

        /*
         * @see com.ibm.wsspi.channelfw.Channel#getName()
         */
        @Override
        public String getName() {
            return this.config.getName();
        }

        /*
         * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
         */
        @Override
        public final DiscriminationProcess getDiscriminationProcess() {
            return this.discriminationProcess;
        }

        /*
         * @see com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(com.ibm.wsspi.channelfw.DiscriminationProcess)
         */
        @Override
        public final void setDiscriminationProcess(DiscriminationProcess dp) {
            this.discriminationProcess = dp;
        }

    }

    private class SimpleLink extends InboundProtocolLink {
        protected SimpleLink() {
            // nothing
        }

        @Override
        public Object getChannelAccessor() {
            return null;
        }

        @Override
        public void ready(VirtualConnection inVC) {
            // nothing
        }
    }

    private class SimpleLinka extends InboundProtocolLink {
        protected SimpleLinka() {
            // nothing
        }

        @Override
        public Object getChannelAccessor() {
            return null;
        }

        @Override
        public void ready(VirtualConnection inVC) {
            // nothing
        }
    }

    private class SimpleCC implements ChannelData {

        /** Serialization ID string */
        private static final long serialVersionUID = -7465768116533968082L;

        protected SimpleCC() {
            super();
        }

        @Override
        public Class<?> getFactoryType() {
            return null;
        }

        @Override
        public Map<Object, Object> getPropertyBag() {
            return null;
        }

        @Override
        public int getDiscriminatorWeight() {
            return 0;
        }

        @Override
        public String getName() {
            return "otherChannel";
        }

        @Override
        public boolean isInbound() {
            return true;
        }

        @Override
        public boolean isOutbound() {
            return false;
        }

        @Override
        public String getExternalName() {
            return "otherChannel";
        }
    }

    private class SimpleCCa extends SimpleCC {

        /** Serialization ID string */
        private static final long serialVersionUID = 2217472880870185689L;

        protected SimpleCCa() {
            super();
        }

        @Override
        public String getName() {
            return "MyName" + ChannelDataImpl.CHILD_STRING + "0";
        }
    }

    private class SimpleChannelCheck implements InboundChannel {
        private Discriminator d = null;
        private DiscriminationTest dt = null;
        /** Channel configuration object */
        private ChannelData config = null;
        /** DiscriminationProcess */
        private DiscriminationProcess discriminationProcess = null;

        protected SimpleChannelCheck(ChannelData cc, Discriminator d, DiscriminationTest dt) {
            this.d = d;
            this.dt = dt;
            this.config = cc;
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#start()
         */
        @Override
        public void start() {
            // nothing
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#stop(int)
         */
        @Override
        public void stop(long millisec) {
            // nothing
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#init()
         */
        @Override
        public void init() {
            // nothing
        }

        /**
         * @see com.ibm.wsspi.channelfw.Channel#destroy()
         */
        @Override
        public void destroy() {
            // nothing
        }

        @Override
        public ConnectionLink getConnectionLink(VirtualConnection vc) {
            return new SimpleLinkCheck(dt);
        }

        @Override
        public Class<?> getApplicationInterface() {
            return null;
        }

        @Override
        public Class<?> getDeviceInterface() {
            return null;
        }

        @Override
        public void update(ChannelData cc) {
            // nothing
        }

        @Override
        public Discriminator getDiscriminator() {
            return d;
        }

        @Override
        public Class<?> getDiscriminatoryType() {
            return null;
        }

        /*
         * @see com.ibm.wsspi.channelfw.Channel#getName()
         */
        @Override
        public String getName() {
            return this.config.getName();
        }

        /*
         * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
         */
        @Override
        public final DiscriminationProcess getDiscriminationProcess() {
            return this.discriminationProcess;
        }

        /*
         * @see com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(com.ibm.wsspi.channelfw.DiscriminationProcess)
         */
        @Override
        public final void setDiscriminationProcess(DiscriminationProcess dp) {
            this.discriminationProcess = dp;
        }
    }

    private class SimpleLinkCheck extends InboundProtocolLink {
        private DiscriminationTest dt = null;

        protected SimpleLinkCheck(DiscriminationTest dt) {
            this.dt = dt;
        }

        @Override
        public Object getChannelAccessor() {
            return null;
        }

        @Override
        public void ready(VirtualConnection inVC) {
            dt.channelCheck = true;
        }
    }

    private class SimpleMaybeCheckDiscriminator implements Discriminator {
        private int id = 0;
        private DiscriminationTest dt = null;
        private Channel c = null;

        protected SimpleMaybeCheckDiscriminator(int id, DiscriminationTest dt) {
            this.id = id;
            this.dt = dt;
        }

        protected void setChannel(Channel c) {
            this.c = c;
        }

        @Override
        public int discriminate(VirtualConnection vc, Object data) {
            dt.maybeCheckCount++;
            if (dt.maybeCheckCount > 2) {
                return Discriminator.YES;
            }
            return Discriminator.MAYBE;
        }

        @Override
        public Class<?> getDiscriminatoryDataType() {
            return commonCorrectClass;
        }

        @Override
        public Channel getChannel() {
            return c;
        }

        @Override
        public int getWeight() {
            return id;
        }

        @Override
        public void cleanUpState(VirtualConnection vc) {
            // Nothing to clean up.
        }
    }

    private class SimpleNoCheckDiscriminator implements Discriminator {
        private int id = 0;
        private DiscriminationTest dt = null;
        private Channel c = null;

        protected SimpleNoCheckDiscriminator(int id, DiscriminationTest dt) {
            this.id = id;
            this.dt = dt;
        }

        protected void setChannel(Channel c) {
            this.c = c;
        }

        @Override
        public int discriminate(VirtualConnection vc, Object data) {
            dt.noCheckCount++;
            return Discriminator.NO;
        }

        @Override
        public Class<?> getDiscriminatoryDataType() {
            return commonCorrectClass;
        }

        @Override
        public Channel getChannel() {
            return c;
        }

        @Override
        public int getWeight() {
            return id;
        }

        @Override
        public void cleanUpState(VirtualConnection vc) {
            // Nothing to clean up.
        }
    }
}
