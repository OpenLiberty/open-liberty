/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm;

import java.security.AccessController;

/**
 *
 */
public class JcaServiceUtilities {
    /**
     * Set context classloader to the one for the resource adapter
     * 
     * @param raClassLoader
     * @return the current classloader
     */
    public ClassLoader beginContextClassLoader(ClassLoader raClassLoader) {
        return raClassLoader == null ? null
                        : AccessController.doPrivileged(new GetAndSetContextClassLoader(raClassLoader));
    }

    /**
     * Restore current context class loader saved when the context class loader was set to the one
     * for the resource adapter.
     * 
     * @param raClassLoader
     * @param previousClassLoader
     */
    public void endContextClassLoader(ClassLoader raClassLoader, ClassLoader previousClassLoader) {
        if (raClassLoader != null) {
            AccessController.doPrivileged(new GetAndSetContextClassLoader(previousClassLoader));
        }
    }

}
