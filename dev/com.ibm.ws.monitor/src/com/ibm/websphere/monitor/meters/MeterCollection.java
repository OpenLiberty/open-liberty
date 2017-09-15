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

package com.ibm.websphere.monitor.meters;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.monitor.internal.MonitoringFrameworkExtender;

public final class MeterCollection<T> {

    final ConcurrentMap<String, T> meters = new ConcurrentHashMap<String, T>();
    private final TraceComponent tc = Tr.register(MeterCollection.class);
    private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    private static final int REGISTER_MXBEAN = 1;
    private static final int UNREGISTER_MXBEAN = 2;
    final String collectionName;
    Object monitor;

    public MeterCollection(String collectionName, Object monitor) {
        this.collectionName = collectionName;
        this.monitor = monitor;
    }

    public void put(String key, T meter) {
        try {
            if (tc.isDebugEnabled()) {
                if (meter != null) {
                    Tr.debug(tc, "KEY =" + key + ",. Type of Meter =" + meter.getClass().getSimpleName());
                } else {
                    Tr.debug(tc, "KEY =" + key + ",. Type of Meter is NULL");
                }
            }
            if (meter == null) {
                return;
            }
            ObjectName objectName = null;
            if (!meters.containsValue(meter)) {
                //USE type = meter.getClass().getSimpleName() ---> Example :If meter is ServletStats, MXBean type woould be ServletStats
                //USE name = key ---> Example: Incase of ServletStats Key would be APPANAME.SERVLETNAME (WebSphere:type=ServletStats,name=MyBankApp.MyServlet)
                //USE mxBeanImple as meter object ---> Example ServletStats which extends ServletStatsMXBean.
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling MBean REGISTER operation for =" + key + ",. Type of Meter =" + meter.getClass().getSimpleName());
                }
                //If monitor className does not exists in the filter list then there should not be any mx bean registration
                //Default behavior : If no filter is provided then all the available monitor will be registered 
                if (MonitoringFrameworkExtender.groupList.size() > 0) {
                    if (!ifMonitorClassExistsInFilterGroup(monitor.getClass())) {
                        return;
                    }
                }
                objectName = MXBeanHelper(meter.getClass().getSimpleName(), key, REGISTER_MXBEAN, meter);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "MBean REGISTER operation is successful. ObjectName =" + objectName);
                }
                //STORE MXBean ObjectName in Bundle MAP. If Bundle is removed, we will remove those MXBeans.
                Set<ObjectName> s = MonitoringFrameworkExtender.mxmap.get(monitor);

                if (s != null) {
                    s.add(objectName);
                }
            }
        } catch (Exception t) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, t.getMessage());

            }
        }
        meters.put(key, meter);
    }

    private static boolean ifMonitorClassExistsInFilterGroup(Class monitorClassName) {
        boolean filterExits = false;
        //fetch the group of the monitorClassName
        Monitor groups = (Monitor) monitorClassName.getAnnotation(Monitor.class);
        String[] group = groups.group();
        //Now check if the monitorClassName group exists in the available filter group
        for (int i = 0; i < group.length; i++) {
            if (MonitoringFrameworkExtender.groupList.contains(group[i]) == true) {
                filterExits = true;
                break;
            }
        }
        return filterExits;
    }

    public synchronized ObjectName MXBeanHelper(String type, String name, int operation, Object mxBeanImpl) throws MalformedObjectNameException, NullPointerException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InstanceNotFoundException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "MXBeanHelper");
        }
        StringBuilder sb = new StringBuilder("WebSphere:");
        sb.append("type=").append(type);
        sb.append(",name=").append(name);
        ObjectName on = new ObjectName(sb.toString());
        if (operation == REGISTER_MXBEAN) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Registering MBean to platform MBean Server " + on);
            }
            mbeanServer.registerMBean(mxBeanImpl, on);
        } else if (operation == UNREGISTER_MXBEAN) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "UN-Registering MBean from platform MBean Server " + on);
            }
            mbeanServer.unregisterMBean(on);
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "MXBeanHelper");
        }
        return on;
    }

    public T get(String key) {
        return meters.get(key);
    }

    /**
     * remove
     * 
     * 
     * There are 3 objectives for this method
     * 1) Remove it from concurrent map, meters.
     * 2) Un-Register MXBean for specified Type of Meter (e.g. ServletStats, ThreadPoolStats, etc)
     * 3) Remove MXBean in a list of Bundle specific MBeans, so when a bundle is removed, we will clean all MXBeans for it.
     * 
     * 
     * @param key
     * 
     */

    public void remove(String key) {
        T mBeanImpl = null;
        ObjectName objectName = null;
        try {
            //Get mBeanImpl Object from meters map
            if ((mBeanImpl = meters.remove(key)) != null) {
                //Un-Register MXBean for specified Type of Meter (e.g. ServletStats, ThreadPoolStats, etc)
                objectName = MXBeanHelper(mBeanImpl.getClass().getSimpleName(), key, UNREGISTER_MXBEAN, null);
            }

            //Remove from a map where we are maintaining bundle specific MBeans.
            Set<ObjectName> s = MonitoringFrameworkExtender.mxmap.get(monitor);
            if (s != null && objectName != null) {
                s.remove(objectName);
            }

        } catch (Throwable t) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, t.getMessage());
            }
        }
    }

    String getCollectionName() {
        return collectionName;
    }

    Set<String> getKeys() {
        return meters.keySet();
    }

}