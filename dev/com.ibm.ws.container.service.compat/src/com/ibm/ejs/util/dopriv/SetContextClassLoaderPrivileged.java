/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.dopriv;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This class sets the context classloader while in privileged mode. Its purpose
 * is to eliminate the need to use an anonymous inner class in multiple modules
 * throughout the product, when the only privileged action required is to
 * set the context classloader on the current thread.
 */
public class SetContextClassLoaderPrivileged implements PrivilegedAction {
    //PK83186: made these instance vars private so that this class has complete control over the 
    //setting/clearing of these variables.
    private ClassLoader oldClassLoader, newClassLoader;

    // Instance var is public to allow fast setting/getting by caller if this object is reused
    public boolean wasChanged;

    // 369927 - update ctors to take a ThreadContextAccessor
    private final ThreadContextAccessor threadContextAccessor;

    public SetContextClassLoaderPrivileged(ThreadContextAccessor threadContextAccessor) {
        this.threadContextAccessor = threadContextAccessor;
    }

    public SetContextClassLoaderPrivileged(ThreadContextAccessor threadContextAccessor, ClassLoader newCL) {
        this(threadContextAccessor);
        newClassLoader = newCL;
    }

    // 369927
    /**
     * Acquires the current thread context classloader and sets a new context
     * classloader if <code>newClassLoader</code> is different from the
     * current one. The current context classloader is set stored in
     * <code>oldClassLoader</code>. If the classloader was changed, then
     * <code>wasChanged</code> is set to <code>true</code>; otherwise, it is
     * set to <code>false</code>.
     * 
     * <p>This calls <code>AccessController.doPrivileged</code> if necessary.
     * 
     * PK83186: Changed this method such that it takes the CL which the caller
     * would like to attempt to change to. Upon method exit, this method will
     * set the old/newClassLoader variables to null in order to avoid a
     * CL leak.
     * 
     * @param cl - the ClassLoader which the caller would like to change to.
     * 
     * @return <code>oldClassLoader</code>
     */
    public ClassLoader execute(ClassLoader cl) {

        //PK83186 start
        newClassLoader = cl;

        if (threadContextAccessor.isPrivileged()) {
            cl = (ClassLoader) run();
        } else {
            cl = (ClassLoader) AccessController.doPrivileged(this);
        }

        newClassLoader = null;
        oldClassLoader = null;

        return cl;
        //PK83186 end
    }

    // 369927

    // 146064.3
    public Object run() {
        Thread currentThread = Thread.currentThread();
        oldClassLoader = threadContextAccessor.getContextClassLoader(currentThread); // 369927

        // The following tests are done in a certain order to maximize performance
        if (newClassLoader == oldClassLoader) {
            wasChanged = false;
        } else if ((newClassLoader == null && oldClassLoader != null)
                   || (newClassLoader != null &&
                   (oldClassLoader == null || !(newClassLoader.equals(oldClassLoader))))) {
            // class loaders are different, change to new one
            threadContextAccessor.setContextClassLoader(currentThread, newClassLoader); // 369927
            wasChanged = true;
        } else
            wasChanged = false;

        return oldClassLoader;
    }
} // SetContextClassLoaderPrivileged

