/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
abstract class AbstractMetaData {

    private final ConcurrentHashMap<String, Boolean> executableConstrainedCache = new ConcurrentHashMap<String, Boolean>();

    /**
     * Check whether an executable is constrained.
     *
     * @return is the executable constrained. null indicates the executable is not in the cache.
     */
    public Boolean isExecutableConstrained(String executableName) {
        return executableConstrainedCache.get(executableName);
    }

    /**
     * Add an executable to the constrained executables cache.
     *
     * @param executableName the toString representation of an Executable
     * @param isConstrained  is this executable constrained, taking into account constraints and applicable executable types
     */
    public void addExecutableToConstrainedCache(String executableName, Boolean isConstrained) {
        executableConstrainedCache.put(executableName, isConstrained);
    }

    /**
     * Get the URI that represents the current module. By default this will be null.
     * Bean Validation 2.0 and greater should override this default behavior.
     *
     * @return the module URI
     */
    public String getModuleUri() {
        return null;
    }
}
