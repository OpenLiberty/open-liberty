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
package com.ibm.ws.zos.wlm.context.internal;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.concurrent.ManagedTask;

import org.junit.Test;

import com.ibm.ws.zos.core.utils.internal.NativeUtilsImpl;
import com.ibm.ws.zos.wlm.AlreadyClassifiedException;
import com.ibm.ws.zos.wlm.Enclave;
import com.ibm.ws.zos.wlm.internal.EnclaveImpl;
import com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;

/**
 *
 */
public class WLMContextImplTest {

    // Test taskStarting and taskStopping with non-Daemon work and a pre-existing enclave
    // also there's an enclave on the dispatch thread
    @Test
    public void test1() {
        // Set up a configured zosWLMContext
        final HashMap<String, Object> zosWLMContextConfig = new HashMap<String, Object>();
        zosWLMContextConfig.put(WLMContextProviderImpl.DAEMON_TRANSACTION_CLASS, "A");
        zosWLMContextConfig.put(WLMContextProviderImpl.PROPAGATION_POLICY,
                                WLMContextProviderImpl.POLICY_PROPAGATE_OR_NEW);
        zosWLMContextConfig.put(WLMContextProviderImpl.DEFAULT_TRANSACTION_CLASS, "C");

        // The context code wants an enclave manager, so fake up one of those too
        // Some methods should get called, so have booleans so we can verify
        final class TestEnclaveManager extends EnclaveManagerImpl {

            // The context code wants an enclave, so fake one up
            final class TestEnclaveImpl extends EnclaveImpl {
                private final byte[] e_token;

                public TestEnclaveImpl(byte[] x) {
                    super(x);
                    e_token = x;
                }
            }

            private final byte[] a_token = new byte[] { 1, 1 }; // current enclave
            private final byte[] b_token = new byte[] { 2, 2 }; // enclave on thread for task-starting
            private TestEnclaveImpl a_enclave;
            private TestEnclaveImpl b_enclave;
            private boolean getCurrentEnclaveCalled = false;
            private boolean getStringTokenCalled = false;
            private boolean removeCurrentEnclaveFromThreadCalled = false;
            private boolean getEnclaveFromTokenCalled = false;
            private boolean preJoinEnclaveCalled = false;
            private boolean joinEnclaveCalled = false;
            private boolean leaveEnclaveCalled = false;
            private boolean restoreEnclaveToThreadCalled = false;

            public boolean getCurrentEnclaveCalled() {
                return getCurrentEnclaveCalled;
            }

            public boolean getStringTokenCalled() {
                return getStringTokenCalled;
            }

            public boolean removeCurrentEnclaveFromThreadCalled() {
                return removeCurrentEnclaveFromThreadCalled;
            }

            public boolean getEnclaveFromTokenCalled() {
                return getEnclaveFromTokenCalled;
            }

            public boolean preJoinEnclaveCalled() {
                return preJoinEnclaveCalled;
            }

            public boolean joinEnclaveCalled() {
                return joinEnclaveCalled;
            }

            public boolean leaveEnclaveCalled() {
                return leaveEnclaveCalled;
            }

            public boolean restoreEnclaveToThreadCalled() {
                return restoreEnclaveToThreadCalled;
            }

            @Override
            public EnclaveImpl getCurrentEnclave() {
                getCurrentEnclaveCalled = true;
                a_enclave = new TestEnclaveImpl(a_token);
                return a_enclave;
            }

            @Override
            public String getStringToken(Enclave e) {
                getStringTokenCalled = true;
                return super.getStringToken(e);
            }

            @Override
            public Enclave removeCurrentEnclaveFromThread() {
                removeCurrentEnclaveFromThreadCalled = true;
                return new TestEnclaveImpl(b_token);
            }

            @Override
            public Enclave getEnclaveFromToken(String s) {
                getEnclaveFromTokenCalled = true;
                // we're gave back the 'a' enclave during context create so return it here
                return a_enclave;
            }

            @Override
            public void preJoinEnclave(Enclave enclave) {
                preJoinEnclaveCalled = true;
            }

            @Override
            public void joinEnclave(Enclave enclave) {
                joinEnclaveCalled = true;
            }

            @Override
            public byte[] leaveEnclave(Enclave enclave) {
                leaveEnclaveCalled = true;
                return null;
            }

            @Override
            public void restoreEnclaveToThread(Enclave enclave) throws AlreadyClassifiedException {
                restoreEnclaveToThreadCalled = true;
            }
        }
        // Create our fake enclave manager
        TestEnclaveManager tem = new TestEnclaveManager();

        // Also need a fake native util class, define it and new it up
        final class TestNativeUtils extends NativeUtilsImpl {

        }
        TestNativeUtils tnu = new TestNativeUtils();

        // Create our context provider and set our fake stuff up and activate it
        WLMContextProviderImpl cpi = new WLMContextProviderImpl();
        cpi.setWlmEnclaveManager(tem);
        cpi.setNativeUtils(tnu);
        cpi.activate(new HashMap<String, Object>());

        // Drive the basic getContext (drives some code over in ContextImpl too)
        ThreadContext c = cpi.captureThreadContext(Collections.<String, String> emptyMap(), zosWLMContextConfig);

        c.taskStarting();

        c.taskStopping();

        assertEquals(true, tem.getCurrentEnclaveCalled());
        assertEquals(true, tem.getStringTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(true, tem.preJoinEnclaveCalled());
        assertEquals(true, tem.joinEnclaveCalled());
        assertEquals(true, tem.leaveEnclaveCalled());
        assertEquals(true, tem.restoreEnclaveToThreadCalled());

    }

    // Test taskStarting and taskStopping with no pre-existing enclave (and not Daemon)
    // also there's no enclave on the dispatch thread
    @Test
    public void test2() {
        // Set up properties to pass into the context provider
        HashMap<String, Object> zosWLMContextConfig = new HashMap<String, Object>();
        zosWLMContextConfig.put(WLMContextProviderImpl.DAEMON_TRANSACTION_CLASS, "A");
        zosWLMContextConfig.put(WLMContextProviderImpl.PROPAGATION_POLICY,
                                WLMContextProviderImpl.POLICY_PROPAGATE_OR_NEW);
        zosWLMContextConfig.put(WLMContextProviderImpl.DEFAULT_TRANSACTION_CLASS, "C");

        // The context code wants an enclave manager, so fake up one of those too
        // Some methods should get called, so have booleans so we can verify
        final class TestEnclaveManager extends EnclaveManagerImpl {

            // The context code wants an enclave, so fake one up
            final class TestEnclaveImpl extends EnclaveImpl {
                private final byte[] e_token;

                public TestEnclaveImpl(byte[] x) {
                    super(x);
                    e_token = x;
                }
            }

            private final byte[] b_token = new byte[] { 2, 2 }; // enclave on thread for task-starting
            private final byte[] c_token = new byte[] { 3, 3 }; // newly created enclave
            private TestEnclaveImpl a_enclave;
            private TestEnclaveImpl b_enclave;
            private TestEnclaveImpl c_enclave;
            private boolean getCurrentEnclaveCalled = false;
            private boolean getStringTokenCalled = false;
            private boolean removeCurrentEnclaveFromThreadCalled = false;
            private boolean getEnclaveFromTokenCalled = false;
            private boolean preJoinEnclaveCalled = false;
            private boolean joinEnclaveCalled = false;
            private boolean leaveEnclaveCalled = false;
            private boolean restoreEnclaveToThreadCalled = false;
            private boolean joinNewEnclaveCalled = false;

            public boolean getCurrentEnclaveCalled() {
                return getCurrentEnclaveCalled;
            }

            public boolean getStringTokenCalled() {
                return getStringTokenCalled;
            }

            public boolean removeCurrentEnclaveFromThreadCalled() {
                return removeCurrentEnclaveFromThreadCalled;
            }

            public boolean getEnclaveFromTokenCalled() {
                return getEnclaveFromTokenCalled;
            }

            public boolean preJoinEnclaveCalled() {
                return preJoinEnclaveCalled;
            }

            public boolean joinEnclaveCalled() {
                return joinEnclaveCalled;
            }

            public boolean leaveEnclaveCalled() {
                return leaveEnclaveCalled;
            }

            public boolean restoreEnclaveToThreadCalled() {
                return restoreEnclaveToThreadCalled;
            }

            public boolean joinNewEnclaveCalled() {
                return joinNewEnclaveCalled;
            }

            @Override
            public EnclaveImpl getCurrentEnclave() {
                getCurrentEnclaveCalled = true;

                return null;
            }

            @Override
            public String getStringToken(Enclave e) {
                getStringTokenCalled = true;
                return super.getStringToken(e);
            }

            @Override
            public Enclave removeCurrentEnclaveFromThread() {
                removeCurrentEnclaveFromThreadCalled = true;
                return new TestEnclaveImpl(b_token);
            }

            @Override
            public Enclave getEnclaveFromToken(String s) {
                getEnclaveFromTokenCalled = true;
                // we're gave back the 'c' enclave we created
                return c_enclave;
            }

            @Override
            public void preJoinEnclave(Enclave enclave) {
                preJoinEnclaveCalled = true;
            }

            @Override
            public void joinEnclave(Enclave enclave) {
                joinEnclaveCalled = true;
            }

            @Override
            public byte[] leaveEnclave(Enclave enclave) {
                leaveEnclaveCalled = true;
                return null;
            }

            @Override
            public void restoreEnclaveToThread(Enclave enclave) throws AlreadyClassifiedException {
                restoreEnclaveToThreadCalled = true;
            }

            @Override
            public Enclave joinNewEnclave(String tclass, long arrivalTime) {
                joinNewEnclaveCalled = true;
                c_enclave = new TestEnclaveImpl(c_token);
                return c_enclave;
            }
        }
        // Create our fake enclave manager
        TestEnclaveManager tem = new TestEnclaveManager();

        // Also need a fake native util class, define it and new it up
        final class TestNativeUtils extends NativeUtilsImpl {
            @Override
            public long getSTCK() {
                return '1';
            }

        }
        TestNativeUtils tnu = new TestNativeUtils();

        // Create our context provider and set our fake stuff up and activate it
        WLMContextProviderImpl cpi = new WLMContextProviderImpl();
        cpi.setWlmEnclaveManager(tem);
        cpi.setNativeUtils(tnu);
        cpi.activate(new HashMap<String, Object>());

        // Drive the basic getContext (drives some code over in ContextImpl too
        ThreadContext c = cpi.captureThreadContext(Collections.<String, String> emptyMap(), zosWLMContextConfig);

        c.taskStarting();

        c.taskStopping();

        assertEquals(true, tem.getCurrentEnclaveCalled());
        assertEquals(true, tem.getStringTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(false, tem.preJoinEnclaveCalled());
        assertEquals(false, tem.joinEnclaveCalled());
        assertEquals(true, tem.leaveEnclaveCalled());
        assertEquals(true, tem.restoreEnclaveToThreadCalled());
        assertEquals(true, tem.joinNewEnclaveCalled());

    }

    // Test taskStarting and taskStopping for Daemon work.  There's no preexisting enclave (not that it matters)
    // also there's no enclave on the dispatch thread
    @Test
    public void test3() {
        // Set up properties to pass into the context provider
        HashMap<String, Object> zosWLMContextConfig = new HashMap<String, Object>();
        zosWLMContextConfig.put(WLMContextProviderImpl.DAEMON_TRANSACTION_CLASS, "A");
        zosWLMContextConfig.put(WLMContextProviderImpl.PROPAGATION_POLICY,
                                WLMContextProviderImpl.POLICY_PROPAGATE_OR_NEW);
        zosWLMContextConfig.put(WLMContextProviderImpl.DEFAULT_TRANSACTION_CLASS, "C");

        // The context code wants an enclave manager, so fake up one of those too
        // Some methods should get called, so have booleans so we can verify
        final class TestEnclaveManager extends EnclaveManagerImpl {

            // The context code wants an enclave, so fake one up
            final class TestEnclaveImpl extends EnclaveImpl {
                private final byte[] e_token;

                public TestEnclaveImpl(byte[] x) {
                    super(x);
                    e_token = x;
                }
            }

            private final byte[] b_token = new byte[] { 2, 2 }; // enclave on thread for task-starting
            private final byte[] c_token = new byte[] { 3, 3 }; // newly created enclave
            private TestEnclaveImpl a_enclave;
            private TestEnclaveImpl b_enclave;
            private TestEnclaveImpl c_enclave;
            private boolean getCurrentEnclaveCalled = false;
            private boolean getStringTokenCalled = false;
            private boolean removeCurrentEnclaveFromThreadCalled = false;
            private boolean getEnclaveFromTokenCalled = false;
            private boolean preJoinEnclaveCalled = false;
            private boolean joinEnclaveCalled = false;
            private boolean leaveEnclaveCalled = false;
            private boolean restoreEnclaveToThreadCalled = false;
            private boolean joinNewEnclaveCalled = false;

            public boolean getCurrentEnclaveCalled() {
                return getCurrentEnclaveCalled;
            }

            public boolean getStringTokenCalled() {
                return getStringTokenCalled;
            }

            public boolean removeCurrentEnclaveFromThreadCalled() {
                return removeCurrentEnclaveFromThreadCalled;
            }

            public boolean getEnclaveFromTokenCalled() {
                return getEnclaveFromTokenCalled;
            }

            public boolean preJoinEnclaveCalled() {
                return preJoinEnclaveCalled;
            }

            public boolean joinEnclaveCalled() {
                return joinEnclaveCalled;
            }

            public boolean leaveEnclaveCalled() {
                return leaveEnclaveCalled;
            }

            public boolean restoreEnclaveToThreadCalled() {
                return restoreEnclaveToThreadCalled;
            }

            public boolean joinNewEnclaveCalled() {
                return joinNewEnclaveCalled;
            }

            @Override
            public EnclaveImpl getCurrentEnclave() {
                getCurrentEnclaveCalled = true;

                return null;
            }

            @Override
            public String getStringToken(Enclave e) {
                getStringTokenCalled = true;
                return super.getStringToken(e);
            }

            @Override
            public Enclave removeCurrentEnclaveFromThread() {
                removeCurrentEnclaveFromThreadCalled = true;
                return new TestEnclaveImpl(b_token);
            }

            @Override
            public Enclave getEnclaveFromToken(String s) {
                getEnclaveFromTokenCalled = true;
                // we're gave back the 'c' enclave we created
                return c_enclave;
            }

            @Override
            public void preJoinEnclave(Enclave enclave) {
                preJoinEnclaveCalled = true;
            }

            @Override
            public void joinEnclave(Enclave enclave) {
                joinEnclaveCalled = true;
            }

            @Override
            public byte[] leaveEnclave(Enclave enclave) {
                leaveEnclaveCalled = true;
                return null;
            }

            @Override
            public void restoreEnclaveToThread(Enclave enclave) throws AlreadyClassifiedException {
                restoreEnclaveToThreadCalled = true;
            }

            @Override
            public Enclave joinNewEnclave(String tclass, long arrivalTime) {
                joinNewEnclaveCalled = true;
                c_enclave = new TestEnclaveImpl(c_token);
                return c_enclave;
            }
        }
        // Create our fake enclave manager
        TestEnclaveManager tem = new TestEnclaveManager();

        // Also need a fake native util class, define it and new it up
        final class TestNativeUtils extends NativeUtilsImpl {
            @Override
            public long getSTCK() {
                return '1';
            }

        }
        TestNativeUtils tnu = new TestNativeUtils();

        // Make it daemon work
        Map<String, String> execProps = Collections.singletonMap(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());

        // Create our context provider and set our fake stuff up and activate it
        WLMContextProviderImpl cpi = new WLMContextProviderImpl();
        cpi.setWlmEnclaveManager(tem);
        cpi.setNativeUtils(tnu);
        cpi.activate(new HashMap<String, Object>());

        // Drive the basic getContext (drives some code over in ContextImpl too
        ThreadContext c = cpi.captureThreadContext(execProps, zosWLMContextConfig);

        c.taskStarting();

        c.taskStopping();

        assertEquals(true, tem.getCurrentEnclaveCalled());
        assertEquals(true, tem.getStringTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(false, tem.preJoinEnclaveCalled());
        assertEquals(false, tem.joinEnclaveCalled());
        assertEquals(true, tem.leaveEnclaveCalled());
        assertEquals(true, tem.restoreEnclaveToThreadCalled());
        assertEquals(true, tem.joinNewEnclaveCalled());

    }

    // Test taskStarting and taskStopping with non-Daemon work and a pre-existing enclave
    // also there's an enclave on the dispatch thread.  Serialize and force new Enclave
    // on taskStarting (simulating a serialize across a server start).
    @Test
    public void test1_serialize() throws Exception {
        // Set up a configured zosWLMContext
        final HashMap<String, Object> zosWLMContextConfig = new HashMap<String, Object>();
        zosWLMContextConfig.put(WLMContextProviderImpl.DAEMON_TRANSACTION_CLASS, "A");
        zosWLMContextConfig.put(WLMContextProviderImpl.PROPAGATION_POLICY,
                                WLMContextProviderImpl.POLICY_PROPAGATE_OR_NEW);
        zosWLMContextConfig.put(WLMContextProviderImpl.DEFAULT_TRANSACTION_CLASS, "C");

        // The context code wants an enclave manager, so fake up one of those too
        // Some methods should get called, so have booleans so we can verify
        final class TestEnclaveManager extends EnclaveManagerImpl {

            // The context code wants an enclave, so fake one up
            final class TestEnclaveImpl extends EnclaveImpl {
                private final byte[] e_token;

                public TestEnclaveImpl(byte[] x) {
                    super(x);
                    e_token = x;
                }
            }

            private final byte[] a_token = new byte[] { 1, 1 }; // current enclave
            private final byte[] b_token = new byte[] { 2, 2 }; // enclave on thread for task-starting
            private final byte[] c_token = new byte[] { 3, 3 }; // newly created enclave
            private TestEnclaveImpl a_enclave;
            private TestEnclaveImpl b_enclave;
            private TestEnclaveImpl c_enclave;
            private boolean getCurrentEnclaveCalled = false;
            private boolean getStringTokenCalled = false;
            private boolean removeCurrentEnclaveFromThreadCalled = false;
            private boolean getEnclaveFromTokenCalled = false;
            private boolean preJoinEnclaveCalled = false;
            private boolean joinEnclaveCalled = false;
            private boolean leaveEnclaveCalled = false;
            private boolean restoreEnclaveToThreadCalled = false;
            private boolean joinNewEnclaveCalled = false;

            public boolean getCurrentEnclaveCalled() {
                return getCurrentEnclaveCalled;
            }

            public boolean getStringTokenCalled() {
                return getStringTokenCalled;
            }

            public boolean removeCurrentEnclaveFromThreadCalled() {
                return removeCurrentEnclaveFromThreadCalled;
            }

            public boolean getEnclaveFromTokenCalled() {
                return getEnclaveFromTokenCalled;
            }

            public boolean preJoinEnclaveCalled() {
                return preJoinEnclaveCalled;
            }

            public boolean joinEnclaveCalled() {
                return joinEnclaveCalled;
            }

            public boolean leaveEnclaveCalled() {
                return leaveEnclaveCalled;
            }

            public boolean restoreEnclaveToThreadCalled() {
                return restoreEnclaveToThreadCalled;
            }

            public boolean joinNewEnclaveCalled() {
                return joinNewEnclaveCalled;
            }

            @Override
            public EnclaveImpl getCurrentEnclave() {
                getCurrentEnclaveCalled = true;
                a_enclave = new TestEnclaveImpl(a_token);
                return a_enclave;
            }

            @Override
            public String getStringToken(Enclave e) {
                getStringTokenCalled = true;
                return super.getStringToken(e);
            }

            @Override
            public Enclave removeCurrentEnclaveFromThread() {
                removeCurrentEnclaveFromThreadCalled = true;
                return new TestEnclaveImpl(b_token);
            }

            @Override
            public Enclave getEnclaveFromToken(String s) {
                getEnclaveFromTokenCalled = true;

                // Only give back "c_enclave" ...making the others "old/invalid"
                if (c_enclave != null) {
                    return c_enclave;
                }

                return null;
            }

            @Override
            public void preJoinEnclave(Enclave enclave) {
                preJoinEnclaveCalled = true;
            }

            @Override
            public void joinEnclave(Enclave enclave) {
                joinEnclaveCalled = true;
            }

            @Override
            public byte[] leaveEnclave(Enclave enclave) {
                leaveEnclaveCalled = true;
                return null;
            }

            @Override
            public void restoreEnclaveToThread(Enclave enclave) throws AlreadyClassifiedException {
                restoreEnclaveToThreadCalled = true;
            }

            @Override
            public Enclave joinNewEnclave(String tclass, long arrivalTime) {
                joinNewEnclaveCalled = true;
                c_enclave = new TestEnclaveImpl(c_token);
                return c_enclave;
            }
        }
        // Create our fake enclave manager
        TestEnclaveManager tem = new TestEnclaveManager();

        // Also need a fake native util class, define it and new it up
        final class TestNativeUtils extends NativeUtilsImpl {
            @Override
            public long getSTCK() {
                return '1';
            }

        }
        TestNativeUtils tnu = new TestNativeUtils();

        // Create our context provider and set our fake stuff up and activate it
        WLMContextProviderImpl cpi = new WLMContextProviderImpl();
        cpi.setWlmEnclaveManager(tem);
        cpi.setNativeUtils(tnu);
        cpi.activate(new HashMap<String, Object>());

        // Drive the basic getContext (drives some code over in ContextImpl too)
        ThreadContext c = cpi.captureThreadContext(Collections.<String, String> emptyMap(), zosWLMContextConfig);

        // Write the context to a stream and then read it out again
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(c);

        ThreadContextDeserializationInfo info = new ThreadContextDeserializationInfo() {
            @Override
            public String getExecutionProperty(String name) {
                return null;
            }

            @Override
            public String getMetadataIdentifier() {
                return null;
            }
        };
        ThreadContext c2 = cpi.deserializeThreadContext(info, baos.toByteArray());
        //c2.clone();

        // We're causing the "current" Enclave to not be useable...like what should happen when its brought in across a Server
        // restart.

        c2.taskStarting();

        c2.taskStopping();

        assertEquals(true, tem.getCurrentEnclaveCalled());
        assertEquals(true, tem.getStringTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(true, tem.getEnclaveFromTokenCalled());
        assertEquals(false, tem.preJoinEnclaveCalled());
        assertEquals(false, tem.joinEnclaveCalled());
        assertEquals(true, tem.leaveEnclaveCalled());
        assertEquals(true, tem.restoreEnclaveToThreadCalled());
        assertEquals(true, tem.joinNewEnclaveCalled());

    }
}
