/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.dopriv;

import java.security.PrivilegedAction;

/**
 * This class gets the context classloader while in privileged mode. Its purpose
 * is to eliminate the need to use an anonymous inner class in multiple modules
 * throughout the product, when the only privileged action required is to
 * get the context classloader on the current thread.
 */
public class GetContextClassLoaderPrivileged implements PrivilegedAction<ClassLoader> {

    @Override
    public ClassLoader run() {
        return Thread.currentThread().getContextClassLoader();
    }
}
