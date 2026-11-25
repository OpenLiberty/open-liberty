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

import org.junit.Test;

import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.core.utils.internal.NativeUtilsImpl;
import com.ibm.ws.zos.wlm.Enclave;
import com.ibm.ws.zos.wlm.internal.EnclaveImpl;
import com.ibm.ws.zos.wlm.internal.EnclaveManagerImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;

/**
*
*/
public class WLMContextProviderImplTest {

    /**
     * Test activate and a bunch of other stuff
     */
    @Test
    public void testActivate() throws Exception {

        // Set up properties to pass into the context provider
        HashMap<String, Object> zosWLMContextConfig = new HashMap<String, Object>();
        zosWLMContextConfig.put(WLMContextProviderImpl.DAEMON_TRANSACTION_CLASS, "A");
        zosWLMContextConfig.put(WLMContextProviderImpl.PROPAGATION_POLICY,
                                WLMContextProviderImpl.POLICY_PROPAGATE_OR_NEW);
        zosWLMContextConfig.put(WLMContextProviderImpl.DEFAULT_TRANSACTION_CLASS, "C");

        // The context code wants an enclave, so fake one up
        final class TestEnclaveImpl extends EnclaveImpl {
            private final byte[] e_token;

            public TestEnclaveImpl(byte[] x) {
                super(x);
                e_token = x;
            }
        }

        // The context code wants an enclave manager, so fake up one of those too
        // Some methods should get called, so have booleans so we can verify
        final class TestEnclaveManager extends EnclaveManagerImpl {

            private final byte[] a_token = new byte[] { 1, 1 };
            private boolean getCurrentEnclaveCalled = false;
            private boolean getStringTokenCalled = false;

            public boolean getCurrentEnclaveCalled() {
                return getCurrentEnclaveCalled;
            }

            public boolean getStringTokenCalled() {
                return getStringTokenCalled;
            }

            @Override
            public EnclaveImpl getCurrentEnclave() {
                getCurrentEnclaveCalled = true;
                return new TestEnclaveImpl(a_token);
            }

            @Override
            public String getStringToken(Enclave e) {
                getStringTokenCalled = true;
                return super.getStringToken(e);
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

        // Verify the getter works
        assertEquals(null, cpi.getPrerequisites());

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
        c2.clone();

        // Check out the getter for native Utils..should be the same one we set earlier
        NativeUtils nu = cpi.getNativeUtils();
        assertEquals(nu, tnu);

        // And deactivate it
        cpi.deactivate();

        // And drive the unset methods
        cpi.unsetNativeUtils(tnu);
        cpi.unsetWlmEnclaveManager(tem);

        // Validate that our Enclave Manager got called properly
        assertEquals(true, tem.getCurrentEnclaveCalled());
        assertEquals(true, tem.getStringTokenCalled());

    }
}
