/*******************************************************************************
 * Copyright (c) 2010, 2026 IBM Corporation and others.
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

package com.ibm.websphere.monitor.meters;

import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
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
    final ConcurrentMap<String, ObjectName> meterObjectNames = new ConcurrentHashMap<String, ObjectName>();
    private static final TraceComponent tc = Tr.register(MeterCollection.class);
    private static final MBeanServer mbeanServer = AccessController.doPrivileged((PrivilegedAction<MBeanServer>) () -> ManagementFactory.getPlatformMBeanServer());
    private static final int REGISTER_MXBEAN = 1;
    private static final int UNREGISTER_MXBEAN = 2;
    final String collectionName;
    Object monitor;

    public MeterCollection(String collectionName, Object monitor) {
        this.collectionName = collectionName;
        this.monitor = monitor;
    }

    public void put(String key, T meter) {
        put(key, null, meter);
    }

    /**
     * Registers a meter with a map of attributes for the metric key.
     * This method creates an ObjectName with individual properties for each attribute,
     * allowing for more flexible JMX queries.
     *
     * @param key        the unique identifier for this meter (used for internal storage)
     * @param attributes a map of attribute key-value pairs to include in the ObjectName,
     *                       or null to use the other format with a single "name" property
     * @param meter      the meter implementation to register
     */
    public void put(String key, Map<String, String> attributes, T meter) {
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (meter != null) {
                    Tr.debug(this, tc, "KEY =" + key + (attributes != null ? ", Attributes=" + attributes : "") + ", Type of Meter =" + meter.getClass().getSimpleName());
                } else {
                    Tr.debug(this, tc, "KEY =" + key + (attributes != null ? ", Attributes=" + attributes : "") + ", Type of Meter is NULL");
                }
            }
            if (meter == null) {
                return;
            }
            ObjectName objectName = null;
            if (!meters.containsValue(meter)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Calling MBean REGISTER operation for =" + key + (attributes != null ? ", Attributes=" + attributes : "") + ", Type of Meter ="
                                       + meter.getClass().getSimpleName());
                }
                //If monitor className does not exists in the filter list then there should not be any mx bean registration
                //Default behavior : If no filter is provided then all the available monitor will be registered
                if (MonitoringFrameworkExtender.groupList.size() > 0) {
                    if (!ifMonitorClassExistsInFilterGroup(monitor.getClass())) {
                        return;
                    }
                }

                // Create ObjectName based on whether attributes are provided
                if (attributes != null) {
                    objectName = MXBeanHelperWithAttributes(meter.getClass().getSimpleName(), attributes, REGISTER_MXBEAN, meter);
                    // Store the ObjectName for later unregistration
                    meterObjectNames.put(key, objectName);
                } else {
                    //USE type = meter.getClass().getSimpleName() ---> Example :If meter is ServletStats, MXBean type woould be ServletStats
                    //USE name = key ---> Example: Incase of ServletStats Key would be APPANAME.SERVLETNAME (WebSphere:type=ServletStats,name=MyBankApp.MyServlet)
                    //USE mxBeanImple as meter object ---> Example ServletStats which extends ServletStatsMXBean.
                    objectName = MXBeanHelper(meter.getClass().getSimpleName(), key, REGISTER_MXBEAN, meter);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "MBean REGISTER operation is successful. ObjectName =" + objectName);
                }
                //STORE MXBean ObjectName in Bundle MAP. If Bundle is removed, we will remove those MXBeans.
                Set<ObjectName> s = MonitoringFrameworkExtender.mxmap.get(monitor);

                if (s != null) {
                    s.add(objectName);
                }
            }
        } catch (Exception t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, t.getMessage());

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

    public synchronized ObjectName MXBeanHelper(String type, String name, int operation,
                                                Object mxBeanImpl) throws MalformedObjectNameException, NullPointerException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InstanceNotFoundException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "MXBeanHelper");
        }
        StringBuilder sb = new StringBuilder("WebSphere:");
        sb.append("type=").append(type);
        sb.append(",name=").append(name);
        ObjectName on = new ObjectName(sb.toString());
        if (operation == REGISTER_MXBEAN) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Registering MBean to platform MBean Server " + on);
            }
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    mbeanServer.registerMBean(mxBeanImpl, on);
                    return null;
                });
            } catch (PrivilegedActionException pae) {
                Throwable t = pae.getCause();
                if (t instanceof InstanceAlreadyExistsException) {
                    throw (InstanceAlreadyExistsException) t;
                } else if (t instanceof MBeanRegistrationException) {
                    throw (MBeanRegistrationException) t;
                } else if (t instanceof NotCompliantMBeanException) {
                    throw (NotCompliantMBeanException) t;
                } else {
                    throw new RuntimeException(t);
                }
            }
        } else if (operation == UNREGISTER_MXBEAN) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "UN-Registering MBean from platform MBean Server " + on);
            }
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    mbeanServer.unregisterMBean(on);
                    return null;
                });
            } catch (PrivilegedActionException pae) {
                Throwable t = pae.getCause();
                if (t instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) t;
                } else if (t instanceof MBeanRegistrationException) {
                    throw (MBeanRegistrationException) t;
                } else {
                    throw new RuntimeException(t);
                }
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "MXBeanHelper");
        }
        return on;
    }

    /**
     * Helper method to register/unregister MXBeans with attribute-based ObjectNames.
     * Creates an ObjectName with individual properties for each attribute instead of
     * a single name property.
     *
     * @param type       the MBean type
     * @param attributes map of attribute key-value pairs
     * @param operation  REGISTER_MXBEAN or UNREGISTER_MXBEAN
     * @param mxBeanImpl the MBean implementation (null for unregister)
     * @return the ObjectName that was registered/unregistered
     */
    public synchronized ObjectName MXBeanHelperWithAttributes(String type, Map<String, String> attributes, int operation,
                                                              Object mxBeanImpl) throws MalformedObjectNameException, NullPointerException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InstanceNotFoundException {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "MXBeanHelperWithAttributes");
        }
        StringBuilder sb = new StringBuilder("WebSphere:");
        sb.append("type=").append(type);

        // Add each attribute as a separate property
        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                sb.append(",").append(entry.getKey()).append("=");
                // Quote the value if it contains special characters
                String value = entry.getValue();
                if (value.contains(":") || value.contains(",") || value.contains("=") || value.contains("\"")) {
                    sb.append("\"").append(value.replace("\"", "\\\"")).append("\"");
                } else {
                    sb.append(value);
                }
            }
        }

        ObjectName on = new ObjectName(sb.toString());
        if (operation == REGISTER_MXBEAN) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Registering MBean to platform MBean Server " + on);
            }
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    mbeanServer.registerMBean(mxBeanImpl, on);
                    return null;
                });
            } catch (PrivilegedActionException pae) {
                Throwable t = pae.getCause();
                if (t instanceof InstanceAlreadyExistsException) {
                    throw (InstanceAlreadyExistsException) t;
                } else if (t instanceof MBeanRegistrationException) {
                    throw (MBeanRegistrationException) t;
                } else if (t instanceof NotCompliantMBeanException) {
                    throw (NotCompliantMBeanException) t;
                } else {
                    throw new RuntimeException(t);
                }
            }
        } else if (operation == UNREGISTER_MXBEAN) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "UN-Registering MBean from platform MBean Server " + on);
            }
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    mbeanServer.unregisterMBean(on);
                    return null;
                });
            } catch (PrivilegedActionException pae) {
                Throwable t = pae.getCause();
                if (t instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) t;
                } else if (t instanceof MBeanRegistrationException) {
                    throw (MBeanRegistrationException) t;
                } else {
                    throw new RuntimeException(t);
                }
            }
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "MXBeanHelperWithAttributes");
        }
        return on;
    }

    public T get(String key) {
        return meters.get(key);
    }

    /**
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
            mBeanImpl = meters.remove(key);
            objectName = meterObjectNames.remove(key);

            if (mBeanImpl != null) {
                if (objectName == null) {
                    try {
                        objectName = MXBeanHelper(mBeanImpl.getClass().getSimpleName(), key, UNREGISTER_MXBEAN, mBeanImpl);
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Error reconstructing ObjectName for metric: " + e.getMessage());
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "UN-Registering MBean from platform MBean Server " + objectName);
                    }
                    final ObjectName finalObjectName = objectName;
                    try {
                        AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                            mbeanServer.unregisterMBean(finalObjectName);
                            return null;
                        });
                    } catch (PrivilegedActionException pae) {
                        Throwable t = pae.getCause();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Error unregistering MBean: " + t.getMessage());
                        }
                    }
                }
            }

            //Remove from a map where we are maintaining bundle specific MBeans.
            Set<ObjectName> s = MonitoringFrameworkExtender.mxmap.get(monitor);
            if (s != null && objectName != null) {
                s.remove(objectName);
            }

        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, t.getMessage());
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