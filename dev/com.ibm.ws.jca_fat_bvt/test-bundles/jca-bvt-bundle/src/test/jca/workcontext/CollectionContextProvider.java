/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.workcontext;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.resource.spi.work.WorkContextLifecycleListener;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.jca.JCAContextProvider;

/**
 * This a fake thread context provider that we made up for testing purposes.
 * It makes a java.util.Collection available to each thread.
 * The JCA WorkManager can be used to make a thread run with a particular "collectionContext".
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = "type=test.jca.workcontext.CollectionContext")
public class CollectionContextProvider extends AbstractCollection<String> implements JCAContextProvider, ThreadContextProvider, Collection<String> {

    static ThreadLocal<Deque<CollectionThreadContext>> threadlocal = new ThreadLocal<Deque<CollectionThreadContext>>() {
        @Override
        protected Deque<CollectionThreadContext> initialValue() {
            Deque<CollectionThreadContext> stack = new LinkedList<CollectionThreadContext>();
            stack.push(new CollectionThreadContext());
            return stack;
        }
    };

    protected void activate(ComponentContext componentContext) {}

    /**
     * @see java.util.AbstractCollection#add(java.lang.String)
     */
    @Override
    public boolean add(String element) {
        return threadlocal.get().peek().add(element);
    }

    protected void deactivate(ComponentContext componentContext) {}

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#captureThreadContext(java.util.Map, org.osgi.framework.ServiceReference)
     */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        throw new UnsupportedOperationException("captureThreadContext");
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#createDefaultThreadContext(java.util.Map)
     */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        String identityName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
        return new CollectionThreadContext(Collections.<String> emptySet(), null, identityName);
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#deserializeThreadContext(java.util.Map, byte[])
     */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        throw new NotSerializableException();
    }

    /**
     * @see com.ibm.wsspi.threadcontext.jca.JCAContextProvider#getInflowContext(java.lang.Object,java.util.Map)
     */
    @Override
    public ThreadContext getInflowContext(Object jcaContext, Map<String, String> execProps) {
        WorkContextLifecycleListener listener = jcaContext instanceof WorkContextLifecycleListener ? (WorkContextLifecycleListener) jcaContext : null;
        String identityName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
        return new CollectionThreadContext(((CollectionContext) jcaContext).getCollection(), listener, identityName);
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }

    /**
     * @see java.util.AbstractCollection#iterator()
     */
    @Override
    public Iterator<String> iterator() {
        return threadlocal.get().peek().iterator();
    }

    /**
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public int size() {
        return threadlocal.get().peek().size();
    }
}
