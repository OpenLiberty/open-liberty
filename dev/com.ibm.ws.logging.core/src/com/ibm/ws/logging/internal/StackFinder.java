/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Utility class for interacting with the current stack.
 */
public class StackFinder extends SecurityManager {
    private static class StackFinderSingleton {
        static final StackFinder instance = AccessController.doPrivileged(new PrivilegedAction<StackFinder>()
            {
                public StackFinder run()
                {
                    return new StackFinder();
                }
            });
    }

    /**
     * Get the singleton instance of this class.
     * 
     * @return StackFinder
     */
    public static StackFinder getInstance() {
        return StackFinderSingleton.instance;
    }

    /**
     * Find the caller on the current stack, ignoring the immediate trace
     * component classes.
     * 
     * @return Class<Object>
     */
    @SuppressWarnings( { "unchecked" })
    public Class<Object> getCaller() {
        Class<Object> aClass = null;

        // Walk the stack backwards to find the calling class: don't
        // want to use Class.forName, because we want the class as loaded
        // by it's original classloader
        Class<Object> stack[] = (Class<Object>[]) this.getClassContext();
        for (Class<Object> bClass : stack) {
            // Find the first class in the stack that _isn't_ Tr or StackFinder,
            // etc. Use the name rather than the class instance (to also work across
            // classloaders, should that happen)
            String name = bClass.getName();
            if (!name.endsWith("ras.Tr") && // ejs or websphere
                !name.endsWith("ras.TraceNLS") &&
                !name.endsWith("internal.StackFinder") &&
                !name.endsWith("internal.TraceNLSResolver") &&
                !name.endsWith("internal.WsLogger")) {
                aClass = bClass;
                break;
            }
        }

        return aClass;
    }

    /**
     * Find the caller on the current stack, ignoring the immediate trace
     * component classes.
     * 
     * @return Class<Object>
     */
    public Class<Object> getCaller(String name) {
        // Try walking the stack until we find the class with the name passed in
        Class<Object> aClass = matchCaller(name);

        if (aClass == null) {
            // If we couldn't find the class by the name passed in,
            // find the first non-ras caller
            aClass = getCaller();
        }

        return aClass;
    }

    /**
     * Return the class if the given classname is found on the stack.
     * 
     * @param fragment
     * @return boolean
     */
    @SuppressWarnings("unchecked")
    public Class<Object> matchCaller(String className) {
        // Walk the stack backwards to find the calling class: don't
        // want to use Class.forName, because we want the class as loaded
        // by it's original classloader
        Class<Object> stack[] = (Class<Object>[]) this.getClassContext();
        for (Class<Object> bClass : stack) {
            // See if any class in the stack contains the following string
            if (bClass.getName().equals(className))
                return bClass;
        }

        return null;
    }

    /**
     * Check whether the current stack contains the provided fragment.
     * 
     * @param fragment
     * @return boolean
     */
    @SuppressWarnings("unchecked")
    public boolean callstackContains(String fragment) {
        // Walk the stack backwards to find the calling class: don't
        // want to use Class.forName, because we want the class as loaded
        // by it's original classloader
        Class<Object> stack[] = (Class<Object>[]) this.getClassContext();
        for (Class<?> bClass : stack) {
            // See if any class in the stack contains the following string
            if (bClass.getName().contains(fragment))
                return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    /**
     * Returns the first bundle we can find in the call stack. Only
     * active bundles (those with a bundle context) will be returned.
     */
    public Bundle getTopBundleFromCallStack() {
        // Walk the stack backwards to find the calling class
        Class<Object> stack[] = (Class<Object>[]) this.getClassContext();
        for (Class<?> bClass : stack) {
            // See if any class in the stack contains the following string
            Bundle bundle = FrameworkUtil.getBundle(bClass);
            if (bundle != null && bundle.getBundleContext() != null) {
                return bundle;
            }
        }
        return null;
    }

}
