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
package test.numeration.internal;

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
 * It allows for propagating a per-thread numeration system context
 * (which is just an int representing the radix)
 */
public class NumerationContextProvider implements ThreadContextProvider {
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

    /** {@inheritDoc} */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        // Snapshot of current thread context
        NumerationContext context = NumerationServiceImpl.threadlocal.get().peek().clone();

        // Customize based on execution properties and configured numerationContext service properties.

        Object radix = execProps.get("test.numeration.context.radix");
        radix = radix == null ? threadContextConfig.get("radix") : radix;
        if (radix != null)
            context.radix = radix instanceof Integer ? (Integer) radix : Integer.parseInt((String) radix);

        Object upperCase = execProps.get("test.numeration.context.upperCase");
        upperCase = upperCase == null ? upperCase = threadContextConfig.get("upperCase") : upperCase;
        if (upperCase != null)
            context.upperCase = upperCase instanceof Boolean ? (Boolean) upperCase : Boolean.parseBoolean((String) upperCase);

        return context;
    }

    /**
     * Create default context.
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        NumerationContext context = new NumerationContext();

        Object radix = execProps.get("test.numeration.context.radix");
        if (radix != null)
            context.radix = radix instanceof Integer ? (Integer) radix : Integer.parseInt((String) radix);

        Object upperCase = execProps.get("test.numeration.context.upperCase");
        if (upperCase != null)
            context.upperCase = upperCase instanceof Boolean ? (Boolean) upperCase : Boolean.parseBoolean((String) upperCase);

        return context;
    }

    /**
     * Deserialize context from bytes.
     */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return (NumerationContext) in.readObject();
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