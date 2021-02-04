/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw.objectpool;

/**
 * Interface for Object Pool usage.
 */
// TODO make this use Generics
public interface ObjectPool {
    /**
     * Get an object from the pool for usage.
     * 
     * @return Object
     */
    Object get();

    /**
     * Put an object into the pool.
     * 
     * @param o
     * @return object that wouldn't fit in the pool (may not be the same as the
     *         one passed in!)
     */
    Object put(Object o);

}
