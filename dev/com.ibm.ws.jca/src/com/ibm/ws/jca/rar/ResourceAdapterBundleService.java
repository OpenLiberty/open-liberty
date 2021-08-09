/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.rar;

import com.ibm.wsspi.classloading.ClassLoaderIdentity;

/**
 * Provides coordination between a resource adapter and the configured
 * bundle service associated with that resource adapter.
 */
public interface ResourceAdapterBundleService {
    /**
     * Sets the classloader identity for the resource adapter on the
     * associated resource adapter bundle service.
     * 
     * This method will be called after the resource adapter bundle
     * service has been activated, but before the resource adapter
     * has been installed.
     */
    void setClassLoaderID(ClassLoaderIdentity classloaderId);
}
