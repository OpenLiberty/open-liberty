/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Dummy implementation of {@link WSContextService} and supporting classes with functionality required for unit tests
 */
public class DummyContextService implements WSContextService {

    @Override
    public ThreadContextDescriptor captureThreadContext(Map<String, String> executionProperties, Map<String, ?>... additionalThreadContextConfig) {
        // TODO Auto-generated method stub
        return new DummyThreadContextDescriptor();
    }

    @Override
    public <T> T createContextualProxy(ThreadContextDescriptor threadContextDescriptor, T instance, Class<T> intf) {
        throw new UnsupportedOperationException();
    }

    public static class DummyThreadContextDescriptor implements ThreadContextDescriptor {

        @Override
        public Map<String, String> getExecutionProperties() {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] serialize() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(String providerName, ThreadContext context) {
            // Do nothing
        }

        @Override
        public ArrayList<ThreadContext> taskStarting() throws RejectedExecutionException {
            return new ArrayList<>();
        }

        @Override
        public void taskStopping(ArrayList<ThreadContext> threadContext) {
            // Do nothing
        }

        @Override
        public ThreadContextDescriptor clone() {
            throw new UnsupportedOperationException();
        }

    }

}
