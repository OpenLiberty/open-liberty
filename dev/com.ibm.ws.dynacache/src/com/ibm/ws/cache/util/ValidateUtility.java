/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.util;

import com.ibm.websphere.cache.EntryInfo;

public class ValidateUtility {

    public static boolean verify = true;

    // Called by:
    //    DistributedObjectCacheAdapter
    public static void sharingPolicy(int sharingPolicy) {
        if (verify) {
            if (sharingPolicy != EntryInfo.SHARED_PULL &&
                sharingPolicy != EntryInfo.SHARED_PUSH &&
                sharingPolicy != EntryInfo.SHARED_PUSH_PULL &&
                sharingPolicy != EntryInfo.NOT_SHARED) {
                    throw new IllegalArgumentException("sharingPolicy:"+sharingPolicy);
                }
        }
    }

    // Called by:
    //    DistributedObjectCacheAdapter
    public static void priority(int priority) {
    }

    // Called by:
    //    DistributedObjectCacheAdapter
    public static void timeToLive(int timeToLive) {
    }

    // Called by:
    //    DistributedObjectCacheAdapter
    public static void objectNotNull(Object object, String name) {
        if (verify) {
            if (object == null) {
                throw new IllegalArgumentException(name+":"+object);
            }
        }
    }

    // Called by:
    //    DistributedObjectCacheAdapter
    public static void objectNotNull(Object object1, String name1, Object object2, String name2) {
        if (verify) {
            if (object1 == null || object2 == null) {
                throw new IllegalArgumentException(name1+":"+object1+"  "+name2+":"+object2);
            }
        }
    }

}


