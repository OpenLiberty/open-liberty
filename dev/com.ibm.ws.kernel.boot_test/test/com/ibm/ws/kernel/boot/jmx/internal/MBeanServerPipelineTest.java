/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.jmx.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerFactory;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.kernel.boot.jmx.service.MBeanServerForwarderDelegate;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;

/**
 *
 */
public class MBeanServerPipelineTest {

    private static final String MBEAN_SERVER_BUILDER_PROPERTY = "javax.management.builder.initial";
    private static final String MBEAN_SERVER_BUILDER_CLASS = PlatformMBeanServerBuilder.class.getName();

    public static final class MBeanServerPipelineHolder implements PlatformMBeanServerBuilderListener {

        private MBeanServerPipeline pipeline;

        @Override
        public void platformMBeanServerCreated(MBeanServerPipeline pipeline) {
            this.pipeline = pipeline;
        }

        public MBeanServerPipeline getMBeanServerPipeline() {
            return pipeline;
        }
    }

    public static class BasicFilter extends MBeanServerForwarderDelegate {

        private final int priority;

        public BasicFilter(int priority) {
            this.priority = priority;
        }

        @Override
        public final int getPriority() {
            return priority;
        }
    }

    public static class PriorityRecorder extends BasicFilter {

        private final List<Integer> priorities;

        public PriorityRecorder(int priority, List<Integer> priorities) {
            super(priority);
            this.priorities = priorities;
        }

        @Override
        public Integer getMBeanCount() {
            priorities.add(getPriority());
            return super.getMBeanCount();
        }
    }

    protected PlatformMBeanServer mBeanServer;
    protected MBeanServerPipeline mBeanServerPipeline;

    @Before
    public void setUp() throws Exception {
        System.setProperty(MBEAN_SERVER_BUILDER_PROPERTY, MBEAN_SERVER_BUILDER_CLASS);
        mBeanServer = (PlatformMBeanServer) MBeanServerFactory.newMBeanServer();
        MBeanServerPipelineHolder pipelineHolder = new MBeanServerPipelineHolder();
        mBeanServer.invokePlatformMBeanServerCreated(pipelineHolder);
        mBeanServerPipeline = pipelineHolder.getMBeanServerPipeline();
    }

    @Test
    public void testInsert() {
        final BasicFilter f1 = new BasicFilter(5);
        final BasicFilter f2 = new BasicFilter(-1);

        assertTrue("Excpected insert of BasicFilter(5) succeeded.", mBeanServerPipeline.insert(f1));
        assertFalse("Excpected insert of BasicFilter(5) failed.", mBeanServerPipeline.insert(f1));
        assertFalse("Excpected insert of BasicFilter(-1) failed.", mBeanServerPipeline.insert(f2));
        assertFalse("Excpected insert of null failed.", mBeanServerPipeline.insert(null));
    }

    @Test
    public void testRemove() {
        final BasicFilter f1 = new BasicFilter(42);
        final BasicFilter f2 = new BasicFilter(8);
        final BasicFilter f3 = new BasicFilter(13);

        assertFalse("Excpected remove of BasicFilter(42) failed.", mBeanServerPipeline.remove(f1));

        mBeanServerPipeline.insert(f1);
        mBeanServerPipeline.insert(f2);
        mBeanServerPipeline.insert(f3);

        assertTrue("Excpected remove of BasicFilter(42) succeeded.", mBeanServerPipeline.remove(f1));
        assertTrue("Excpected remove of BasicFilter(8) succeeded.", mBeanServerPipeline.remove(f2));
        assertTrue("Excpected remove of BasicFilter(13) succeeded.", mBeanServerPipeline.remove(f3));
        assertFalse("Excpected remove of BasicFilter(42) failed.", mBeanServerPipeline.remove(f1));
    }

    @Test
    public void testContains() {
        final BasicFilter f1 = new BasicFilter(7);
        final BasicFilter f2 = new BasicFilter(13);
        final BasicFilter f3 = new BasicFilter(9);
        final BasicFilter f4 = new BasicFilter(4);

        assertFalse("Excpected pipeline does not contain BasicFilter(7).", mBeanServerPipeline.contains(f1));
        assertFalse("Excpected pipeline does not contain null.", mBeanServerPipeline.contains(null));

        mBeanServerPipeline.insert(f1);
        mBeanServerPipeline.insert(f2);
        mBeanServerPipeline.insert(f3);

        assertTrue("Excpected pipeline contains BasicFilter(7).", mBeanServerPipeline.contains(f1));
        assertTrue("Excpected pipeline contains BasicFilter(13).", mBeanServerPipeline.contains(f2));
        assertTrue("Excpected pipeline contains BasicFilter(9).", mBeanServerPipeline.contains(f3));
        assertFalse("Excpected pipeline does not contain BasicFilter(4).", mBeanServerPipeline.contains(f4));

        mBeanServerPipeline.remove(f1);
        mBeanServerPipeline.remove(f2);
        mBeanServerPipeline.remove(f3);

        assertFalse("Excpected pipeline does not contain BasicFilter(7).", mBeanServerPipeline.contains(f1));
        assertFalse("Excpected pipeline does not contain BasicFilter(13).", mBeanServerPipeline.contains(f2));
        assertFalse("Excpected pipeline does not contain BasicFilter(9).", mBeanServerPipeline.contains(f3));
    }

    @Test
    public void testPipelineOrder() {
        final List<Integer> priorities = new ArrayList<Integer>();
        final PriorityRecorder f1 = new PriorityRecorder(7, priorities);
        final PriorityRecorder f2 = new PriorityRecorder(13, priorities);
        final PriorityRecorder f3 = new PriorityRecorder(9, priorities);
        final PriorityRecorder f4 = new PriorityRecorder(4, priorities);
        final PriorityRecorder f5 = new PriorityRecorder(15, priorities);
        final PriorityRecorder f6 = new PriorityRecorder(6, priorities);
        final PriorityRecorder f7 = new PriorityRecorder(12, priorities);
        final PriorityRecorder f8 = new PriorityRecorder(8, priorities);

        final int mBeanCount1 = mBeanServer.getMBeanCount();

        mBeanServerPipeline.insert(f1);
        mBeanServerPipeline.insert(f2);
        mBeanServerPipeline.insert(f3);
        mBeanServerPipeline.insert(f4);
        mBeanServerPipeline.insert(f5);
        mBeanServerPipeline.insert(f6);
        mBeanServerPipeline.insert(f7);
        mBeanServerPipeline.insert(f8);

        final int mBeanCount2 = mBeanServer.getMBeanCount();
        assertEquals("Expected MBean counts to be equal.", mBeanCount1, mBeanCount2);

        final int length = priorities.size();
        assertEquals("Expected priorities.size() == 8.", 8, length);

        for (int i = 0; i < length - 1; ++i) {
            int t0 = priorities.get(i);
            int t1 = priorities.get(i + 1);
            assertTrue("Expected '" + t0 + "' >= '" + t1 + "'.", t0 >= t1);
        }
    }
}
