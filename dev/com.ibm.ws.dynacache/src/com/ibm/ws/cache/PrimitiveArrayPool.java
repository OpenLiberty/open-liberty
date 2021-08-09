
/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//------------------------------------
// Byte or long primitive array pooling used by HashtableOnDisk
//------------------------------------
public class PrimitiveArrayPool {

    private static TraceComponent tc = Tr.register(PrimitiveArrayPool.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private static boolean                  IS_UNIT_TEST    = false;

    private HashMap         pools           = null;
    private PoolConfig      poolConfig      = null;
    private long            lastScanTime    = 0;
    private String          cacheName       = null;

    //------------------------------------
    // 
    //------------------------------------
    public PrimitiveArrayPool(PoolConfig poolConfig, String cacheName) {
        final String methodName = "PrimitiveArrayPool() - CTOR";
        poolConfig.verifyConfig();
        this.poolConfig     = poolConfig;
        this.pools        = new HashMap(poolConfig.numberOfPools);
        this.cacheName    = cacheName;
        traceDebug(methodName, "cacheName=" + this.cacheName + " "  + poolConfig);
    }
    //------------------------------------

    //------------------------------------
    // Release unused pools based on Pool Configuration.
    // Must call from inside a sync(pools) block
    //------------------------------------
    private void releaseUnusedPools() {
        final String methodName = "releaseUnusedPools()";
        long currentTime = System.currentTimeMillis();
        Iterator i = pools.values().iterator();
        while (i.hasNext()) {
            ArrayList pool = (ArrayList)i.next();
            Iterator j = pool.iterator();
            while (j.hasNext()) {
                PoolEntry poolEntry = (PoolEntry)j.next();
                if (currentTime - poolEntry.getLastAccess() > poolConfig.poolEntryLife) {
                    traceDebug(methodName, "cacheName=" + this.cacheName + " unused pool entry released for type=" + poolConfig.getType() + " size=" + poolEntry.getLength());
                    j.remove();
                    poolEntry.release();
                    if (pool.size() == 0) {
                        i.remove();
                    }
                }
            }
        }
    }
    //------------------------------------


    //------------------------------------
    //  Allocate the array buffer according to the size. The buffer is being cleared.
    //------------------------------------
    public PoolEntry allocate(int size) {
        final String methodName = "allocate()";
        PoolEntry poolEntry = null;
        synchronized(pools) {
            if (pools.size() > 0) {
                ArrayList pool = (ArrayList)pools.get("" + size);
                if (pool != null && pool.size() > 0) {
                    poolEntry = (PoolEntry)pool.remove(0);
                    //traceDebug(methodName, "cacheName=" + this.cacheName + " pool entry exist for type=" + poolConfig.getType() + " size=" + size + " poolSize=" + pool.size());
                } else {
                    releaseUnusedPools();
                }
            }
        }
        if (poolEntry == null) {
            switch (poolConfig.type) {
                case PoolConfig.TYPE_LONG:
                    poolEntry = new PoolEntry(new long[size]);
                    break;
                case PoolConfig.TYPE_BYTE:
                    poolEntry = new PoolEntry(new byte[size]);
                    break;
                default:
                    throw new IllegalStateException(poolConfig.toString());
            }
            traceDebug(methodName, "cacheName=" + this.cacheName + " pool entry created for type=" + poolConfig.getType() + " size=" + size);
        }
        Object array = poolEntry.getArray();
        // clear the buffer
        if (array != null && array instanceof byte[]) {
            Arrays.fill((byte[])array, (byte)0);
        } else if (array != null && array instanceof long[]) {
            Arrays.fill((long[])array, (long)0);
        }
        return poolEntry;
    }
    //------------------------------------

    //------------------------------------
    //  Return the array buffer to pool for reuse later.
    //------------------------------------
    public boolean returnToPool(PoolEntry poolEntry) {
        final String methodName = "returnToPool()";
        boolean rc = false;
        synchronized(pools) {
            long currentTime = System.currentTimeMillis();
            if (currentTime-lastScanTime > poolConfig.scanFrequency) {
                lastScanTime=currentTime;
                releaseUnusedPools();
            }
            if (pools.size() < poolConfig.numberOfPools) {
                ArrayList pool = (ArrayList)pools.get("" + poolEntry.getLength());
                if (pool == null) {
                    pool = new ArrayList(poolConfig.poolSize);
                    pools.put("" + poolEntry.getLength(), pool);
                }
                if (pool.size() < poolConfig.poolSize) {
                    pool.add(poolEntry);
                    poolEntry.touch();
                    //traceDebug(methodName, "cacheName=" + this.cacheName + " return to pool for type=" + poolConfig.getType() + " size=" + poolEntry.getLength() + " poolSize=" + pool.size());
                    rc = true;
                } else {
                    traceDebug(methodName, "cacheName=" + this.cacheName + " the pool is full for type="+poolConfig.getType()+" size=" + poolEntry.getLength());
                    releaseUnusedPools();
                }
            } else {
                traceDebug(methodName, "cacheName=" + this.cacheName + " there are no more pool slots");
                releaseUnusedPools();
            }
        }
        return rc;
    }
    //------------------------------------

    //------------------------------------
    // Release unused pools public method
    //------------------------------------
    public void release() {
        synchronized(pools) {
            if (pools.size() > 0) {
                releaseUnusedPools();
            }
        }
    }

    //------------------------------------
    //  Trace debug method 
    //------------------------------------
    private void traceDebug(String methodName, String message){
        if (IS_UNIT_TEST) {
            System.out.println(this.getClass().getName() + "." + methodName + " " + message);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " " + message);
            }
        }
    }


    //------------------------------------
    //  Pool Configuration
    //    - array type
    //    - max number of pools
    //    - max number of arrays in each pool
    //    - pool entry life in ms
    //    - frequency to scan pool in ms
    //------------------------------------
    static class PoolConfig {
        public static final int TYPE_BYTE       = 0x01;
        public static final int TYPE_LONG       = 0x02;
        public int  type                = -1;
        public int  numberOfPools       = 0;
        public int  poolSize            = 0;
        public int  poolEntryLife       = 0;
        public int  scanFrequency       = 0;
        public String toString() {
            return  "PoolConfig@@" + hashCode()+
            " type=" + (type==TYPE_BYTE?"byte[]":type==TYPE_LONG?"long[]":"unknown") +
            " numberOfPools=" + numberOfPools+
            " poolSize=" + poolSize+
            " poolEntryLife=" + poolEntryLife+
            " scanFrequency=" + scanFrequency;
        }
        protected void verifyConfig(){
            if (numberOfPools < 0 ||
                poolSize < 0 ||
                poolEntryLife <= 0 ||
                scanFrequency < 0 ||
                type == -1 ) {
                throw new IllegalStateException("PoolConfig error "+toString());
            }
        }
        protected String getType() {
            String primitiveType = "";
            switch (type) {
                case PoolConfig.TYPE_LONG:
                    primitiveType = "long[]";
                    break;
                case PoolConfig.TYPE_BYTE:
                    primitiveType = "byte[]";
                    break;
                default:
                    throw new IllegalStateException(toString());
            }
            return primitiveType;
        }
    }
    //------------------------------------

    //------------------------------------
    //  Pool Entry which contains either byte array or long array, length and 
    //  last access time
    //------------------------------------
    static public class PoolEntry {
        private Object  primitiveArray   = null;
        private long    lastAccess  = 0;
        private int     length      = 0;
        public PoolEntry(Object primitiveArray) {
            this.primitiveArray = primitiveArray;
            if (primitiveArray instanceof byte[]) {
                length = ((byte[])primitiveArray).length;
            } else if (primitiveArray instanceof long[]) {
                length = ((long[])primitiveArray).length;
            }
        }

        public Object getArray() {
            touch();
            return primitiveArray;
        }

        protected void release() {
            primitiveArray = null;
        }

        protected long getLastAccess() {
            return lastAccess;
        }

        protected int getLength() {
            return length;
        }

        protected void touch(){
            lastAccess = System.currentTimeMillis();
        }
    }
    //------------------------------------
}

