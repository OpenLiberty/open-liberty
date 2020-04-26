/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.buffer.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.concurrent.ManagedTask;

import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * This a fake context provider that we made up for testing purposes.
 * It allows for propagating a per-thread character buffer.
 */
public class BufferContextProvider implements ThreadContextProvider {
    private final List<ThreadContextProvider> prerequisites = new ArrayList<ThreadContextProvider>(2);

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context component context
     */
    protected void activate(ComponentContext context) {}

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context component context
     */
    protected void deactivate(ComponentContext context) {}

    /**
     * Captures the context of the current thread or creates new thread context,
     * as determined by the execution properties and configuration of this context provider.
     * 
     * @param execProps execution properties that provide information about the contextual task.
     * @param threadContextConfig configuration for the thread context to be captured. Null if not configurable per contextService.
     * @return captured thread context.
     */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        BufferContext context = new BufferContext(
                        execProps.get(ManagedTask.IDENTITY_NAME),
                        execProps.get(WSContextService.TASK_OWNER));
        String type = execProps.get("test.buffer.context.capture");
        type = type == null ? (String) threadContextConfig.get("buffer") : type;
        if ("Default".equals(type))
            return context;
        else if ("Snapshot".equals(type) || type == null)
            return (BufferContext) context.append(BufferService.threadlocal.get().peek().buffer);
        else
            throw new UnsupportedOperationException("test.buffer.context.capture=" + type);
    }

    /**
     * Create default context.
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new BufferContext(
                        execProps.get(ManagedTask.IDENTITY_NAME),
                        execProps.get(WSContextService.TASK_OWNER));
    }

    /**
     * Deserialize context from bytes.
     */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            BufferContext context = (BufferContext) in.readObject();
            context.identityName = info.getExecutionProperty(ManagedTask.IDENTITY_NAME);
            context.taskOwner = info.getExecutionProperty(WSContextService.TASK_OWNER);
            return context;
        } finally {
            in.close();
        }
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return prerequisites;
    }

    protected void setMapContextProvider(ThreadContextProvider svc) {
        prerequisites.add(svc);
    }

    protected void setNumerationContextProvider(ThreadContextProvider svc) {
        prerequisites.add(svc);
    }

    protected void unsetMapContextProvider(ThreadContextProvider svc) {
        prerequisites.remove(svc);
    }

    protected void unsetNumerationContextProvider(ThreadContextProvider svc) {
        prerequisites.remove(svc);
    }
}