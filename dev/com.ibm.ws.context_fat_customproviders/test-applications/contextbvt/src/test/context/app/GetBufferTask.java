/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.context.app;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Task that returns the character buffer context for the thread where it is running
 */
public class GetBufferTask implements Callable<String>, Runnable, InvocationHandler, Serializable {
    /**  */
    private static final long serialVersionUID = 5310661956418881717L;

    // This stores results when GetBufferTask is used as a Runnable.
    static final Queue<Object> results = new ConcurrentLinkedQueue<Object>();

    @Override
    public String call() throws Exception {
        return AccessController.doPrivileged((PrivilegedExceptionAction<String>) () -> {
            BundleContext bundleContext = FrameworkUtil.getBundle(GetBufferTask.class.getClassLoader().getClass()).getBundleContext();
            ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
            final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
            try {
                return bufferSvc.toString();
            } finally {
                bundleContext.ungetService(bufferSvcRef);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            method = getClass().getMethod(method.getName(), method.getParameterTypes());
            return method.invoke(this, args);
        } catch (InvocationTargetException x) {
            Throwable t = x.getCause();
            if (t instanceof Exception)
                throw (Exception) t;
            else
                throw t;
        }
    }

    @Override
    public void run() {
        try {
            String result = call();
            results.add(result);
        } catch (Throwable x) {
            results.add(x);
        }
    }
}