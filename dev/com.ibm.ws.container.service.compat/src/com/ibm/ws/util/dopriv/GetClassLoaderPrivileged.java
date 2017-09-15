/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

/**
 * This class gets a classloader of a class while in privileged mode. <p>
 *
 * Its purpose is to eliminate the need to use an anonymous inner class in
 * multiple modules throughout the product, when the only privileged action
 * required is to get the classloader of a class. <p>
 */
public class GetClassLoaderPrivileged implements PrivilegedAction<ClassLoader> {
    // Instance vars are public to allow fast setting/getting by caller if
    // this object is reused

    /**
     * Set this field to the Class you wish to invoke getClassLoader() on,
     * or pass the Class on the constructor.
     **/
    public Class<?> ivClass;

    /**
     * Constructs an instance of GetClassLoaderPrivileged that may be used
     * to invoke getClassLoader on the specified Class.
     **/
    public GetClassLoaderPrivileged(Class<?> clazz) {
        ivClass = clazz;
    }

    /**
     * PrivilegedAction run() method.
     **/
    @Override
    public ClassLoader run() {
        return ivClass.getClassLoader();
    }

} // GetClassLoaderPrivileged
