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
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This a fake thread context that we made up for testing purposes.
 * It represents a per-thread character buffer.
 */
public class BufferContext implements Appendable, ThreadContext {
    /**  */
    private static final long serialVersionUID = 335599617873975364L;

    final StringBuilder buffer;

    transient String identityName;

    transient String taskOwner;

    /**
     * Construct a default context - an empty character buffer
     * 
     * @param identityName name of the contextual task
     * @param taskOwner owner of the contextual task
     */
    public BufferContext(String identityName, String taskOwner) {
        buffer = new StringBuilder();
        this.identityName = identityName;
        this.taskOwner = taskOwner;
    }

    /** {@inheritDoc} */
    @Override
    public Appendable append(CharSequence csq) {
        buffer.append(csq);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        buffer.append(csq, start, end);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Appendable append(char c) throws IOException {
        // Provide some way for test cases to clear the buffer so they don't interfere with eachother 
        if (c == '\u001B')
            buffer.delete(0, buffer.length());
        else
            buffer.append(c);
        return this;
    }

    @Override
    public BufferContext clone() {
        BufferContext b = new BufferContext(identityName, taskOwner);
        b.buffer.append(buffer);
        return b;
    }

    /**
     * <p>Establishes context on the current thread.
     * When this method is used, expect that context will later be removed and restored
     * to its previous state via operationStopping.
     * 
     * <p>This method should fail if the context cannot be established on the thread.
     * In the event of failure, any partially applied context must be removed before this method returns.
     * 
     * <p>This implementation relies on map service and numeration service already being propagated,
     * (as specified in the buffer.bnd file), and uses them to append some information about the
     * contextual operation that is starting. The information is formatted like this,
     * <pre>
     * [task owner] is running [task name] on thread [thread ID in numeration system] with map=[contents of map]
     * </pre>
     * 
     * @throws RejectedExecutionException if the context provider isn't available.
     */
    @Override
    public void taskStarting() throws RejectedExecutionException {

        BufferService.threadlocal.get().push(this.clone());

        // Append to the buffer some information about the operation being started.
        // We rely on the "map" service already being propagated because we append the contents of the map.
        // We rely on the "numeration" service because we append the thread ID represented in the numeration system.
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                BundleContext bundleContext = FrameworkUtil.getBundle(BufferService.class).getBundleContext();
                ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
                BufferService bufferSvc = (BufferService) bundleContext.getService(bufferSvcRef);
                try {
                    Map<String, String> map = bufferSvc.mapSvcRef.getServiceWithException();
                    long threadID = Thread.currentThread().getId();
                    Object numSvc = bufferSvc.numSvcRef.getServiceWithException();
                    Method NumerationService_toString = numSvc.getClass().getMethod("toString", long.class);
                    String threadIDStr = (String) NumerationService_toString.invoke(numSvc, threadID);

                    String message = "\r\n" + taskOwner + " is running " + identityName +
                                     " on thread " + threadIDStr + " with map=" + map + "\r\n";

                    BufferService.threadlocal.get().peek().append(message);
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                } finally {
                    bundleContext.ungetService(bufferSvcRef);
                }
                return null;
            }
        }); 
    }

    /**
     * Restore the thread to its previous state from before the most recently applied context.
     */
    @Override
    public void taskStopping() {

        // Remove most recent, which restores the previous
        BufferService.threadlocal.get().pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(this)) + ':'
               + buffer.toString();
    }
}