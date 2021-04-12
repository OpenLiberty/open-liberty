/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb.timers;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timer;

/**
 *
 */
@Stateless
public class ScheduledPurge {
    private static String MBEAN_TYPE = "com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean";

    @Resource
    private SessionContext sessionContext; //Used to get information about timer

    private static final String name = "ScheduledPurge";

    private int count = 0;

    /**
     * Cancels timer execution
     */
    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    /**
     * Returns count
     */
    public int getRunCount() {
        return count;
    }

    /**
     * Runs ever 1 minute. Automatically starts when application starts.
     */
    @Schedule(info = "Performing scheduled change", hour = "*", minute = "*", second = "0", persistent = true)
    public void run(Timer timer) {
        try {
            ObjectInstance bean = getMBeanObjectInstance("jdbc/derbyds");
            System.out.println("KJA1017 - Connection pool size before change: " + getPoolSize(bean));
            setPoolSize(bean, -116);
            count++;
            System.out.println("KJA1017 - Connection pool size after change: " + getPoolSize(bean));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectInstance getMBeanObjectInstance(String jndiName) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + MBEAN_TYPE + ",jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }

    private int getPoolSize(ObjectInstance bean) throws Exception {
        return Integer.parseInt((String) ManagementFactory.getPlatformMBeanServer().getAttribute(bean.getObjectName(), "size"));
    }

    private String showPoolContents(ObjectInstance bean) throws Exception {
        Object[] params = new Object[] {};
        String[] signatures = {};
        return (String) ManagementFactory.getPlatformMBeanServer().invoke(bean.getObjectName(), "showPoolContents", params, signatures);
    }

    private void setPoolSize(ObjectInstance bean, long size) throws Exception {
        Object[] params = new Object[] { size };
        String[] signatures = { long.class.getCanonicalName() };
        ManagementFactory.getPlatformMBeanServer().invoke(bean.getObjectName(), "setSize", params, signatures);
    }

}
