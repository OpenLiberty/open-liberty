/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.jni;

/**
 * The {@code NativeMethodManager} is responsible for managing the resolution
 * and linking of native methods to classes.
 */
public interface NativeMethodManager {

    /**
     * Register native methods from the core DLL for the specified class.
     *
     * @param clazz the class to link native methods for
     *
     * @throws UnsatisfiedLinkError if an error occurs during resolution or registration
     */
    public void registerNatives(Class<?> clazz);

    /**
     * Register native methods from the core DLL for the specified class. The
     * specified object array will be passed along to registration callback.
     *
     * @param clazz     the class to link native methods for
     * @param extraInfo extra information that will be passed to the registration hook
     *
     * @throws UnsatisfiedLinkError if an error occurs during resolution or registration
     */
    public void registerNatives(Class<?> clazz, Object[] extraInfo);

}
