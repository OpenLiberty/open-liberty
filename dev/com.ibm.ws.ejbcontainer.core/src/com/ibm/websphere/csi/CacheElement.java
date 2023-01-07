/*******************************************************************************
 * Copyright (c) 2001, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * CacheElement instances are what are stored in the EJBCache
 * implementation and are returned when enumerating the contents
 * of the EJBCache.
 * 
 * @see EJBCache
 */

public interface CacheElement {

    /**
     * Return the object associated with the CacheElement.
     */

    public Object getObject();

    /**
     * Return the key associated with the CacheElement.
     */

    public Object getKey();

} // CacheElement
