/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import com.ibm.ws.cache.intf.DCache;

/**
 * Extend the public CacheAdminMBean to add a method which relies on an internal class.
 * It isn't clear this is actually being used by anyone, but for now we want to
 * both hold onto it and keep it out of the actual MBean interface.
 */
public interface CacheAdmin extends com.ibm.websphere.cache.CacheAdminMBean {

    /**
     * Retrieves the names of the available cache statistics.
     * 
     * @return The names of the available cache statistics.
     */
    public abstract String[] getCacheStatisticNames(DCache cache);

}