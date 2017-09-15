/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.monitors.helper;

import java.lang.reflect.Method;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class ThreadPoolStatsHelper {

    private static final TraceComponent tc = Tr.register(ThreadPoolStatsHelper.class);

    private final String poolName;
    //    private Counter poolSizeDetails;
    //    private Counter activeThreadsDetails;
    //    private int poolSize;
    //    private int activeThreads;

    private Method met_getPoolSize;
    private Method met_getActiveCount;

    private final Object objThreadPoolExecutorImpl;

    /**
     * 
     */
    public ThreadPoolStatsHelper(String _pName, Object _objThreadPoolExecutorImpl) {
        this.poolName = _pName;
        this.objThreadPoolExecutorImpl = _objThreadPoolExecutorImpl;
        for (Method method : _objThreadPoolExecutorImpl.getClass().getDeclaredMethods()) {
            if (method.getName().equals("getPoolSize")) {
                met_getPoolSize = method;
            } else if (method.getName().equals("getActiveCount")) {
                met_getActiveCount = method;
            }
        }
    }

    /**
     * @return the poolName
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * @return the poolSize
     */
    public int getPoolSize() {
        Integer t = null;
        try {
            t = (Integer) met_getPoolSize.invoke(objThreadPoolExecutorImpl, null);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to Invoke getPoolSize Method on ThreadPoolExecutorImpl. Error=" + e.getMessage());
            }
        }
        if (t == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to Invoke getPoolSize Method on ThreadPoolExecutorImpl. PoolSize returned was NULL");
            }
            return -1;
        }
        return t.intValue();
    }

    /**
     * @return the activeThreads
     */
    public int getActiveThreads() {
        Integer t = null;
        try {
            t = (Integer) met_getActiveCount.invoke(objThreadPoolExecutorImpl, null);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to Invoke getActiveCount Method on ThreadPoolExecutorImpl. Error=" + e.getMessage());
            }
        }
        if (t == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to Invoke getActiveCount Method on ThreadPoolExecutorImpl. ActiveCounte returned was NULL");
            }
            return -1;
        }
        return t.intValue();
    }

}
