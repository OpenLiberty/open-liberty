/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.restfulWS.client.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.restfulWS.client.ClientAsyncTaskWrapper;

public class ThreadContextAsyncTaskWrapper implements ClientAsyncTaskWrapper {

    private static final ComponentMetaDataAccessorImpl cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

    private static ClassLoader getThreadContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        }

        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private static void setThreadContextClassLoader(ClassLoader threadCL) {
        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(threadCL);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Thread.currentThread().setContextClassLoader(threadCL);
                    return null;
                }
            });
        }

    }

    @Override
    public Runnable wrap(Runnable r) {
        ClassLoader cl = getThreadContextClassLoader();
        ComponentMetaData cmd = cmdAccessor.getComponentMetaData();
        return new Runnable() {
            @Override
            public void run() {
                ClassLoader origCL = getThreadContextClassLoader();
                try {
                    setThreadContextClassLoader(cl);
                    if (cmd != null) {
                        cmdAccessor.beginContext(cmd);
                    }
                    r.run();
                } finally {
                    if (cmd != null) {
                        cmdAccessor.endContext();
                    }
                    setThreadContextClassLoader(origCL);
                }
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> c) {
        ClassLoader cl = getThreadContextClassLoader();
        ComponentMetaData cmd = cmdAccessor.getComponentMetaData();
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                ClassLoader origCL = getThreadContextClassLoader();
                try {
                    setThreadContextClassLoader(cl);
                    if (cmd != null) {
                        cmdAccessor.beginContext(cmd);
                    }
                    return c.call();
                } finally {
                    if (cmd != null) {
                        cmdAccessor.endContext();
                    }
                    setThreadContextClassLoader(origCL);
                }
            }
        };
    }

    @Override
    public <T> Supplier<T> wrap(Supplier<T> s) {
        ClassLoader cl = getThreadContextClassLoader();
        ComponentMetaData cmd = cmdAccessor.getComponentMetaData();
        return new Supplier<T>() {
            @Override
            public T get() {
                ClassLoader origCL = getThreadContextClassLoader();
                try {
                    setThreadContextClassLoader(cl);
                    if (cmd != null) {
                        cmdAccessor.beginContext(cmd);
                    }
                    return s.get();
                } finally {
                    if (cmd != null) {
                        cmdAccessor.endContext();
                    }
                    setThreadContextClassLoader(origCL);
                }
            }
        };
    }

}
