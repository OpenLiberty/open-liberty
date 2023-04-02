/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.sim.context.zos.thread;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * This a fake context provider that we made up for testing purposes.
 * It allows for propagating the thread name.
 */
public class ThreadNameContextProvider implements ThreadContextProvider {
    private final List<ThreadContextProvider> prereqs = new ArrayList<ThreadContextProvider>(2);

    protected void activate(Map<String, Object> properties) {
    }

    protected void deactivate() {
    }

    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return new ThreadNameContext(Thread.currentThread().getName());
    }

    /**
     * Create default context.
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new ThreadNameContext(ThreadNameContext.CLEARED);
    }

    /**
     * Deserialize context from bytes.
     */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return (ThreadNameContext) in.readObject();
        } finally {
            in.close();
        }
    }

    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return prereqs;
    }

    protected void setJeeMetadataContextProvider(ThreadContextProvider svc) {
        prereqs.add(svc);
    }

    protected void setSecurityContextProvider(ThreadContextProvider svc) {
        prereqs.add(svc);
    }

    protected void unsetJeeMetadataContextProvider(ThreadContextProvider svc) {
        prereqs.remove(svc);
    }

    protected void unsetSecurityContextProvider(ThreadContextProvider svc) {
        prereqs.remove(svc);
    }
}