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

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This a fake thread context that we made up for testing purposes.
 * It makes a character buffer context available to each thread.
 */
public class BufferService implements Appendable {

    /**
     * Reference to the MapService.
     */
    final AtomicServiceReference<Map<String, String>> mapSvcRef = new AtomicServiceReference<Map<String, String>>("mapService");

    /**
     * Reference to the NumerationService.
     */
    final AtomicServiceReference<Object> numSvcRef = new AtomicServiceReference<Object>("numerationService");

    static ThreadLocal<Deque<BufferContext>> threadlocal = new ThreadLocal<Deque<BufferContext>>() {
        @Override
        protected Deque<BufferContext> initialValue() {
            Deque<BufferContext> stack = new LinkedList<BufferContext>();
            stack.push(new BufferContext(null, null));
            return stack;
        }
    };

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {
        mapSvcRef.activate(context);
        numSvcRef.activate(context);
    }

    /** {@inheritDoc} */
    @Override
    public Appendable append(CharSequence csq) throws IOException {
        threadlocal.get().peek().append(csq);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        threadlocal.get().peek().append(csq, start, end);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Appendable append(char c) throws IOException {
        threadlocal.get().peek().append(c);
        return this;
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context context for this component
     */
    protected void deactivate(ComponentContext context) {
        mapSvcRef.deactivate(context);
        numSvcRef.deactivate(context);
    }

    /**
     * Declarative Services method for setting the MapService reference
     * 
     * @param ref reference to the service
     */
    protected void setMapService(ServiceReference<Map<String, String>> ref) {
        mapSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the NumerationService reference
     * 
     * @param ref reference to the service
     */
    protected void setNumerationService(ServiceReference<Object> ref) {
        numSvcRef.setReference(ref);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this)) + ':'
               + threadlocal.get().peek().toString();
    }

    /**
     * Declarative Services method for unsetting the MapService reference
     * 
     * @param ref reference to the service
     */
    protected void unsetMapService(ServiceReference<Map<String, String>> ref) {
        mapSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the NumerationService reference
     * 
     * @param ref reference to the service
     */
    protected void unsetNumerationService(ServiceReference<Object> ref) {
        numSvcRef.unsetReference(ref);
    }
}
