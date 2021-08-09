/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threadContext;

import com.ibm.ejs.util.FastStack;
import com.ibm.ws.util.WSThreadLocal; //133207.2

public class ThreadContextImpl<T> extends WSThreadLocal<ThreadContext<T>> implements ThreadContext<T> {
    /**
     * Optional iniatial object to push onto stack when initialValue method
     * gets called. Push occurs only if a non-null value is passed to CTOR.
     * Note, most users use the default CTOR (zero argument CTOR), so there
     * will be nothing on stack as an initial value.
     */
    private T ivInitialStackEntry = null; //122727

    /**
     * Default Contructor that constructs a ThreadContextImpl
     * that will contain an empty stack.
     * <ul>
     * <li><b>Post-condition:</b> getContext() == null
     * </ul>
     */
    public ThreadContextImpl() //122727
    {}

    /**
     * Construct a ThreadContextImpl object and use a specified object
     * as the initial context data to be pushed onto the stack. Note,
     * a corresponding endContext never occurs for this initial entry.
     * 
     * <ul>
     * <li><b>Post-condition:</b> getContext() == initialStackEntry
     * </ul>
     * 
     * @param a non-null reference to object to be implicitly pushed
     *            onto ThreadContextImpl stack. Note, this same reference
     *            is pushed onto stack for all threads, so only use this
     *            method to pass a singleton object. If each thread needs
     *            a separate object, then do not use this method. Instead,
     *            ask for a new CTOR that handles this new requirement.
     * 
     */
    public ThreadContextImpl(T initialStackEntry) //122727
    {
        ivInitialStackEntry = initialStackEntry;
    }

    @Override
    protected ThreadContext<T> initialValue() {
        // Get stack object and push initial entry onto stack if one was provided.  
        FastStackThreadContextAdapter<T> stack = new FastStackThreadContextAdapter<T>();
        if (ivInitialStackEntry != null) //122727
        {
            stack.push(ivInitialStackEntry); //122727 
        }

        return stack;
    }

    public T beginContext(T object) {
        return get().beginContext(object); // d646139.1
    }

    public T endContext() {
        return get().endContext(); // d646139.1
    }

    public T getContext() {
        return get().getContext(); // d646139.1
    }

    public int getContextIndex() {
        return get().getContextIndex(); // d646139.1
    }

    /**
     * Adapt the FastStack class to the ThreadContext interface.
     */
    private static class FastStackThreadContextAdapter<T> extends FastStack<T> implements ThreadContext<T> // d646139.1
    {
        public T beginContext(T object) {
            T oldObj = peek();
            push(object);
            return oldObj;
        }

        public T endContext() {
            return pop();
        }

        public T getContext() {
            return peek();
        }

        public int getContextIndex() {
            return getTopOfStackIndex();
        }
    }
}
