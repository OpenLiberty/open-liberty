/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

//import com.ibm.ejs.ras.*;

/**
 *
 */

public class ObjectPool {

    private static final boolean DEBUG = false;
    // private static TraceComponent tc =
    // Tr.register(ObjectPool.class.getName(),"");

    private final String name;
    private final Object pool[];
    private int index;

    public ObjectPool(String name, int size) {
        pool = new Object[size];
        index = 0;
        this.name = name;
        // if (DEBUG && tc.isDebugEnabled() ) Tr.debug(tc, "Created " + name +
        // " of size " + size);
    }

    // Returns true if the object was added back to the pool
    public boolean add(Object o) {
        synchronized (pool) {
            if (index < pool.length) {
                pool[index++] = o;
                // if (DEBUG && tc.isDebugEnabled() )
                // Tr.debug(tc,"added to pool " + name + " at " + index);
                return true;
            }
        }
        // if (DEBUG && tc.isDebugEnabled() )
        // Tr.debug(tc,"pool " + name + " is full");
        return false;
    }

    public Object remove() {
        synchronized (pool) {
            if (index > 0) {
                Object o = pool[--index];
                pool[index] = null;
                // if (DEBUG && tc.isDebugEnabled() )
                // Tr.debug(tc,"removed " + index + " from pool " + name);
                return o;
            }
        }
        // if (DEBUG && tc.isDebugEnabled() )
        // Tr.debug(tc,"creating object for " + name + " pool");
        return createObject();
    }

    protected Object createObject() {
        return null;
    }

    public String getName() {
        return name;
    }

}
