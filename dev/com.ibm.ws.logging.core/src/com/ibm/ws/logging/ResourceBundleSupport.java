/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.logging;

import com.ibm.websphere.ras.TraceComponent;

/**
 * A mix-in interface to be used to support providing a Class to get a ClassLoader when trying to 
 * resolve a ResourceBundle.  This interface is important when a null Class object is passed to
 * the TraceComponent constructor.  See OSGiTraceComponent class in TrOSGiLogForwarder for where
 * this interface is primarily implemented.
 */
public interface ResourceBundleSupport {

    Class<?> getClassForResourceBundle();

    static Class<?> getTraceClassForResourceBundle(TraceComponent tc) {
        Class<?> resolveClass = tc.getTraceClass();
        // If getTraceClass() returns null, check to see if the TraceComponent is subclassed and
        // implements ResourceBundleSupport, call the method on that interface to get the Class
        // to be used to resolve the ResourceBundle.
        if (resolveClass == null && tc instanceof ResourceBundleSupport) {
            resolveClass = ((ResourceBundleSupport) tc).getClassForResourceBundle();
        }
        return resolveClass;
    }
}
