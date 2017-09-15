/*******************************************************************************
 * Copyright (c) 1998, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.util;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

//477704 mcasile imports for FFDC facade
// import com.ibm.ws.ffdc.FFDCFilter;
// Alex import com.ibm.ffdc.Ffdc;
//Alex import static com.ibm.ffdc.Manager.Ffdc;

/**
 * WSThreadLocal provide a ThreadLocal implementation that is performance
 * friendly to WebSphere thread pools. Note that instances are *not*
 * cleared on each thread dispatch. Thus, any given thread may have
 * dirty data from a previous thread activation. Thus all uses of
 * this class must use an overwrite policy.
 * 
 */
public class WSThreadLocal<T> extends ThreadLocal<T> {

    private static TraceComponent tc = Tr.register(WSThreadLocal.class, "Runtime", "com.ibm.ws.runtime.runtime");

    private static int count = 0;

    private int index;

    // D247418 -Every new instance of WSThreadLocal causes a new context type
    // "slot" (holder, memory location) to be created for every worker thread
    // in every thread pool. After a context slot is created, it persists for
    // the life of the server, that is, any worker thread can use this slot
    // to hold a particular context type once it has been created.
    // Components typically misuse WSThreadLocal by repeatedly creating a
    // new instance of WSThreadLocals to assiciate the same context type to a
    // worker thread. Misusing WSThreadLocal will lead to an OutOfMemoryError.
    // as more and more unecessary "slots" are allocated for all threads.
    // Under normal usage, there should be a small (<100), finite number of
    // context slots necessary for any deployment, and this number will be reached
    // as the server exercises applications. Suspect misuse when this number
    // grows large while serving applications. The diagnostics identify
    // which component(s) periodically obtain new instances of WSThreadLocals
    // after the number of context slots has reached a generous upper threashold.

    private final static int SUSPECTED_MISUSAGE_THRESHOLD = 200;

    public WSThreadLocal() {
        // D529674
        synchronized (WSThreadLocal.class) {
            index = count++;
        }
        // D247418+D252781
        if ((count % SUSPECTED_MISUSAGE_THRESHOLD) == 0) { // Frequency cycle = threshold
            Exception e = new Exception("WSThreadLocal: instance count = " + count + " ; potential memory leak; verify usage.");
            // Alex Ffdc.log(e, this, this.getClass().getName(), "50", this); //
            // 477704
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "WSThreadLocal", e);
            }
        }
    }

    public T get() {
        Thread thread = Thread.currentThread();
        if (thread instanceof ThreadPool.Worker) {
            ThreadPool.Worker worker = (ThreadPool.Worker) thread;
            Object[] wsLocals = getThreadLocals(worker);
            Object result = wsLocals[index];
            if (result == null) {
                result = initialValue();
                // D655858: Reacquire the thread local array after calling
                // initialValue(). That method might result in the creation and use of
                // additional WSThreadLocal, which will reallocate wsThreadLocals. In
                // that case, this method's wsLocals array is stale.
                getThreadLocals(worker)[index] = result;
            }
            return (T) result;
        } else {
            return super.get();
        }
    }

    public void set(T value) {
        Thread thread = Thread.currentThread();
        if (thread instanceof ThreadPool.Worker) {
            Object[] wsLocals = getThreadLocals((ThreadPool.Worker) thread);
            wsLocals[index] = value;
        } else {
            super.set(value);
        }
    }

    // D398396: Java 5 added the remove() method. The javadoc for this
    // explains that it may allow recovery of memory (but this implementation
    // doesn't) and that it resets the value to its initial value
    public void remove() {
        Thread thread = Thread.currentThread();
        if (thread instanceof ThreadPool.Worker) {
            Object[] wsLocals = getThreadLocals((ThreadPool.Worker) thread);
            wsLocals[index] = null;
        } else {
            super.remove();
        }
    }

    private Object[] getThreadLocals(ThreadPool.Worker worker) {
        Object[] locals = worker.wsThreadLocals;
        int curCount = count;
        if (locals.length != curCount) {
            Object[] newLocals = new Object[curCount];
            System.arraycopy(locals, 0, newLocals, 0, locals.length);
            locals = newLocals;
            worker.wsThreadLocals = locals;
        }

        return locals;
    }
}
