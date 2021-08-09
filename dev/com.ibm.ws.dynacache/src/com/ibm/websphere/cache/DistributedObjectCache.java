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

import com.ibm.websphere.cache.DistributedNioMap;


/**
 * Abstract class implementing
 * DistributedMap and DistributedNioMap.
 * 
 * When doing a JNDI lookup on a DistributedMap
 * or a DistributedNioMap, the actual object
 * type returned from the lookup is
 * DistributedObjectCache.  If you absolutly
 * do not know map type to be returned from the
 * JNDI lookup, use the getMapType() to verify
 * the type.  Otherwise, you can directly cast
 * to DistributedMap or DistributedNioMap.
 * 
 * @see DistributedMap
 * @see DistributedNioMap
 * @since v6.0
 * @ibm-api 
 */
public abstract class DistributedObjectCache implements DistributedNioMap, DistributedMap {
    
    /**
     * The underlying map represented by this
     * DistributedObjectCache is of type DistributedMap.
     * 
     * @see DistributedMap
     * @since v6.0
     * @ibm-api 
     */
    public static final int    TYPE_DISTRIBUTED_MAP             = 0x01;
    
    
    /**
     * The underlying map represented by this
     * DistributedObjectCache is of type DistributedLockingMap.
     * 
     * @see DistributedMap
     * @since v6.0
     * @ibm-api 
     * @deprecated 
     * TYPE_DISTRIBUTED_LOCKING_MAP is no longer used.
     */
    public static final int    TYPE_DISTRIBUTED_LOCKING_MAP     = 0x02;


    /**
     * The underlying map represented by this
     * DistributedObjectCache is of type DistributedNioMap.
     * 
     * @see DistributedMap
     * @since v6.0
     * @ibm-api 
     */
    public static final int    TYPE_DISTRIBUTED_NIO_MAP         = 0x03;


    /**
     * Returns the underlying map type for this
     * DistribuedObjectCache.
     * 
     * @return mapType
     *         <br>TYPE_DISTRIBUTED_MAP
     *         <br>TYPE_DISTRIBUTED_NIO_MAP
     * @see DistributedMap
     * @see DistributedNioMap
     * @since v6.0
     * @ibm-api 
     */
    abstract public int getMapType();

}



