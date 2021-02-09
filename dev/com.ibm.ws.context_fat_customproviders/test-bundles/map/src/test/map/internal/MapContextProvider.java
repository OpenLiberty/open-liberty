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
package test.map.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * This a fake context provider that we made up for testing purposes.
 * It allows for propagating a per-thread "map" context
 * (which is just a java.util.HashMap)
 */
public class MapContextProvider implements ThreadContextProvider {
    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(Map<String, Object> properties) {}

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     */
    protected void deactivate() {}

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
        String type = execProps.get("test.map.context.capture");
        type = type == null ? (String) threadContextConfig.get("map") : type;
        if ("Empty".equals(type))
            return new MapContext();
        else if ("Snapshot".equals(type) || type == null)
            return MapService.threadlocal.get().peek().clone();
        else
            throw new UnsupportedOperationException("map=" + type.length());
    }

    /**
     * Create default context.
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new MapContext();
    }

    /**
     * Deserialize context from bytes.
     */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return (MapContext) in.readObject();
        } finally {
            in.close();
        }
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }
}