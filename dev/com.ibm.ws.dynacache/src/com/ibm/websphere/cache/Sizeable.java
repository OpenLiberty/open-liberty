/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

/**
 * 
 * All the objects put into the cache must implement this interface
 * if the application wants to control the size of a cache instance
 * in terms of heapsize.
 * @ibm-spi
 * @ibm-api 
 * 
 */
public interface Sizeable {

    /**
     * Returns an implementation-specific size of the object.
     * 
     * @return estimated size of <code>object</code>
     * @ibm-spi
     * @ibm-api  
     * 
     */
    public long getObjectSize();
}
