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

package com.ibm.ws.monitor.internal.collectors;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class CpuTimeCollector {

    private final static ThreadMXBean threadBean = getThreadMXBean();
    private static final TraceComponent tc = Tr.register(CpuTimeCollector.class);

    long previous;

    long current;

    public long getPrevious() {
        return previous;
    }

    public long getCurrent() {
        return current;
    }

    public long getElapsed() {
        return current - previous;
    }

    public void begin() {
        if (threadBean == null)
            return;

        previous = current;
        current = threadBean.getCurrentThreadCpuTime();
    }

    public void end() {
        if (threadBean == null)
            return;

        previous = current;
        current = threadBean.getCurrentThreadCpuTime();
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
