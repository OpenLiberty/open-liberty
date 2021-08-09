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

package com.ibm.ws.monitor.internal.boot.templates;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;

public final class ProbeUtils {

    private final static ThreadMXBean threadBean = getThreadMXBean();

    public static final long nanoTime() {
        return System.nanoTime();
    }

    public static final long elapsedNanoTime(long startTime) {
        return nanoTime() - startTime;
    }

    public static final long cpuTime() {
        if (threadBean != null) {
            return threadBean.getCurrentThreadCpuTime();
        }
        return -1L;
    }

    public static final long elapsedCpuTime(long startTime) {
        return cpuTime() - startTime;
    }

    public static final long userTime() {
        if (threadBean != null) {
            return threadBean.getCurrentThreadUserTime();
        }
        return -1L;
    }

    public static final long elapsedUserTime(long startTime) {
        return userTime() - startTime;
    }

    private static ThreadMXBean getThreadMXBean() {
        return AccessController.doPrivileged(new PrivilegedAction<ThreadMXBean>() {
            public ThreadMXBean run() {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                try {
                    if (bean != null && !bean.isCurrentThreadCpuTimeSupported()) {
                        bean = null;
                    }
                    if (bean != null && !bean.isThreadCpuTimeEnabled()) {
                        bean.setThreadCpuTimeEnabled(true);
                    }
                } catch (Throwable t) {
                    bean = null;
                }
                return bean;
            }
        });
    }
}
