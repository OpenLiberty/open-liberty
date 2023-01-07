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
package test.concurrent.sim.context.zos.wlm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.concurrent.ManagedTask;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * This a fake context provider that we made up for testing purposes.
 */
@SuppressWarnings("deprecation")
public class ZWLMContextProvider implements ThreadContextProvider {
    protected void activate(Map<String, Object> properties) {
    }

    protected void deactivate() {
    }

    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        String daemonTransactionClass = (String) threadContextConfig.get("daemonTransactionClass");
        String defaultTransactionClass = (String) threadContextConfig.get("defaultTransactionClass");
        String wlm = (String) threadContextConfig.get("wlm");
        boolean isLongRunning = Boolean.parseBoolean(execProps.get(ManagedTask.LONGRUNNING_HINT));
        if (isLongRunning)
            return new ZWLMContext(daemonTransactionClass);
        else
            return new ZWLMContext(defaultTransactionClass, wlm);
    }

    /**
     * Create default context.
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        boolean isLongRunning = Boolean.parseBoolean(execProps.get(ManagedTask.LONGRUNNING_HINT));
        return new ZWLMContext(isLongRunning ? "ASYNCDMN" : "ASYNCBN");
    }

    /**
     * Deserialize context from bytes.
     */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return (ZWLMContext) in.readObject();
        } finally {
            in.close();
        }
    }

    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }
}