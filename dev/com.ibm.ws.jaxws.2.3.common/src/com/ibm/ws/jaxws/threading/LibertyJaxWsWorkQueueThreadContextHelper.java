/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.threading;

/**
 *
 */
public class LibertyJaxWsWorkQueueThreadContextHelper {

    private static ThreadLocal<LibertyJaxWsWorkQueueThreadContext> threadLocal = new ThreadLocal<LibertyJaxWsWorkQueueThreadContext>() {

        @Override
        protected LibertyJaxWsWorkQueueThreadContext initialValue() {
            LibertyJaxWsWorkQueueThreadContext wqtc = new LibertyJaxWsWorkQueueThreadContext();
            LibertyJaxWsWorkQueueThreadContextHelper.setThreadContext(wqtc);
            return wqtc;
        }
    };

    public static void setThreadContext(LibertyJaxWsWorkQueueThreadContext wqtc) {
        threadLocal.set(wqtc);
    }

    public static LibertyJaxWsWorkQueueThreadContext getThreadContext() {
        return threadLocal.get();
    }

    public static void destroyThreadContext() {
        threadLocal.remove();
    }
}
