/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util.dopriv;

import java.security.PrivilegedAction;

import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This class gets the ThreadContextAccessor while in privileged mode. Its purpose
 * is to eliminate the need to use an anonymous inner class in multiple modules
 * throughout the product, when the only privileged action required is to
 * get the ThreadContextAccessor. This reduces product footprint.
 */
public class GetThreadContextAccessorPrivileged implements PrivilegedAction {
    /**
     * Returns a ThreadContextAccessor implementation.
     * 
     * @return <code>oldClassLoader</code>
     */
    public Object run() {
        return ThreadContextAccessor.getThreadContextAccessor();
    }

}
