/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.handlelist.context.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * This pseudo-context provider ensures that each contextual task runs with its own
 * initially empty HandleList.
 */
@Component(service = ThreadContextProvider.class,
           name = "io.openliberty.handlelist.context.provider",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "alwaysCaptureThreadContext:Boolean=true")
@SuppressWarnings("deprecation")
public class EmptyHandleListContextProvider implements ThreadContextProvider {
    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        return new EmptyHandleListContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return new EmptyHandleListContext();
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, @Sensitive byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        try {
            return (EmptyHandleListContext) in.readObject();
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
