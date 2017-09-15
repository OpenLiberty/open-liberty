/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.threading;

/**
 *
 */
public class LibertyJaxRsWorkQueueThreadContextHelper {

    private static ThreadLocal<LibertyJaxRsWorkQueueThreadContext> threadLocal = new ThreadLocal<LibertyJaxRsWorkQueueThreadContext>() {

        @Override
        protected LibertyJaxRsWorkQueueThreadContext initialValue() {
            LibertyJaxRsWorkQueueThreadContext wqtc = new LibertyJaxRsWorkQueueThreadContext();
            LibertyJaxRsWorkQueueThreadContextHelper.setThreadContext(wqtc);
            return wqtc;
        }
    };

    public static void setThreadContext(LibertyJaxRsWorkQueueThreadContext wqtc) {
        threadLocal.set(wqtc);
    }

    public static LibertyJaxRsWorkQueueThreadContext getThreadContext() {
        return threadLocal.get();
    }

    public static void destroyThreadContext() {
        threadLocal.remove();
    }
}
