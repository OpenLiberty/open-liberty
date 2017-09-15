/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.PackageAdmin;

import com.ibm.websphere.monitor.MonitorManager;
import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

// TODO: Java 2 Security
// TODO: Lazy instantiation of the monitor - requires passing config / metadata
public class MonitoringFrameworkExtender implements SynchronousBundleListener {

    public final static String MONITORING_COMPONENT_BUNDLE_HEADER = "Liberty-Monitoring-Components";
    public static final ArrayList<String> groupList = new ArrayList<String>();
    Map<Long, Set<MonitorObject>> processedBundles = new HashMap<Long, Set<MonitorObject>>();
    public static final ConcurrentMap<Object, Set<ObjectName>> mxmap = new ConcurrentHashMap<Object, Set<ObjectName>>();
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    public final ConcurrentMap<Object, MonitorObject> objectMap = new ConcurrentHashMap<Object, MonitorObject>();
    private final static String MONITORING_GROUP_FILTER = "filter";
    private final static String MONITORING_TRADITIONALPMI = "enableTraditionalPMI";
    private volatile boolean isDeactivated = false;
    MonitorManager monitorManager;

    PackageAdmin packageAdmin;

    BundleContext bundleContext;

    private static final TraceComponent tc = Tr.register(MonitoringFrameworkExtender.class);

    protected synchronized void activate(BundleContext bundleContext, Map<String, Object> properties) {
        this.bundleContext = bundleContext;
        Boolean val = (Boolean) properties.get(MONITORING_TRADITIONALPMI);
        if (val != null && val) {
            PmiRegistry.enable();
        } else {
            PmiRegistry.disable();
        }
        String filter = (String) properties.get(MONITORING_GROUP_FILTER);
        if (filter != null) {
            StringTokenizer st = new StringTokenizer(filter, ",");
            while (st.hasMoreTokens()) {
                groupList.add(st.nextToken());
            }
        }
        bundleContext.addBundleListener(this);
        for (Bundle bundle : bundleContext.getBundles()) {
            activateMonitors(bundle);
        }
    }

    protected synchronized void deactivate() {
        isDeactivated = true;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isDeactivated in deactivate() is", isDeactivated);
        }
        bundleContext.removeBundleListener(this);
        for (Bundle bundle : bundleContext.getBundles()) {
            deactivateMonitors(bundle);
        }
        this.bundleContext = null;
    }

    protected synchronized void setMonitorManager(MonitorManager monitorManager) {
        this.monitorManager = monitorManager;
    }

    protected synchronized void unsetMonitorManager(MonitorManager monitorManager) {
        if (monitorManager == this.monitorManager) {
            this.monitorManager = null;
        }
    }

    protected void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }

    protected void unsetPackageAdmin(PackageAdmin packageAdmin) {
        if (packageAdmin == this.packageAdmin) {
            this.packageAdmin = null;
        }
    }

    @Override
    public synchronized void bundleChanged(BundleEvent event) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "isDeactivated in bundleChanged is", isDeactivated);
        }
        if (!isDeactivated) {
            switch (event.getType()) {
                case BundleEvent.LAZY_ACTIVATION:
                case BundleEvent.STARTED:
                    activateMonitors(event.getBundle());
                    break;
                case BundleEvent.STOPPING:
                case BundleEvent.STOPPED:
                    deactivateMonitors(event.getBundle());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @param bundle
     *            Starting point for activation/processing/registration/filtering process of any bundle which is eligible for monitoring.This will be
     *            decided by scanning each bundle for bundle header "Liberty-Monitoring-Components".
     */
    synchronized void activateMonitors(Bundle bundle) {
        Long bundleId = bundle.getBundleId();
        if (processedBundles.containsKey(bundleId)) {
            return;
        }
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
            return;
        }
        if (bundle.getHeaders("").get(MONITORING_COMPONENT_BUNDLE_HEADER) == null) {
            return;
        }
        if (bundle.getBundleContext() == null) {
            return;
        }

        //Defect # 62098
        //We are seeing Server stop operating is somehow invoking activate() for this class,
        //which calls registerMonitor to fail.
        //Adding following check.
        //If server is being stop, don't register monitors.
        if (FrameworkState.isStopping()) {
            return;
        }
        Set<MonitorObject> bundleMonitors = new HashSet<MonitorObject>();
        processedBundles.put(bundleId, bundleMonitors);
        for (Class<?> clazz : MonitoringUtility.loadMonitoringClasses(bundle)) {
            boolean filterGroup = true;
            Monitor groups = clazz.getAnnotation(Monitor.class);
            if (!(groupList.size() == 0)) {
                for (String group : groups.group()) {
                    filterGroup = groupList.contains(group);
                }
            }
            for (int i = 0; i < clazz.getMethods().length; i++) {
                Annotation anno = ((clazz.getMethods()[i]).getAnnotation(ProbeSite.class));
                if (anno != null) {
                    String temp = ((ProbeSite) anno).clazz();
                    if (monitorManager != null) {
                        monitorManager.updateNonExcludedClassesSet(temp); //RTCD 89497
                    }
                }
            }
            Object monitor = constructMonitor(clazz);
            if (monitor != null) {
                MonitorObject mObject = new MonitorObject(bundleId, groups, monitor, clazz);
                objectMap.put(monitor, mObject);
                bundleMonitors.add(mObject);
                if (!filterGroup) {
                    continue;
                }
                processANDregister((objectMap.get(monitor)).getMonitor(), objectMap.get(monitor));
            }
        }
    }

    /**
     * @param monitor
     *            Initate calls for processing the Monitor Object( which includes generating ObectNames) and registering of the object.
     */
    private void processANDregister(Object monitor, MonitorObject mob) {
        // TODO Auto-generated method stub
        try {
            Set<ObjectName> mxbeanset = new HashSet<ObjectName>();
            mxmap.put(monitor, mxbeanset);
            processMeters(monitor);
            if (monitorManager != null) {
                monitorManager.registerMonitor(monitor);
                mob.setMonitor(monitor);
                mob.setRegistered(true);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Registration Successfull for object", monitor);
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Monitor Manager is NULL");
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "processANDregister exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "processANDregister:Exception");
            }
        }
    }

    /**
     * @param clazz
     * @return
     *         Constructs Object for the respective Monitorable classes.
     */
    protected Object constructMonitor(Class<?> clazz) {
        Object monitor = null;
        for (Constructor<?> ctor : ReflectionHelper.getConstructors(clazz)) {
            // TODO: Filter for compatible constructors and use most flexible
            Class<?>[] parameterTypes = ctor.getParameterTypes();
            if (parameterTypes.length == 0) {
                monitor = ReflectionHelper.newInstance(ctor);
            } else if (parameterTypes.length == 1) {
                Class<?> argType = parameterTypes[0];
                if (argType.equals(MonitorManager.class)) {
                    monitor = ReflectionHelper.newInstance(ctor, monitorManager);
                }
            }
        }
        return monitor;
    }

    /**
     * @param instance
     *            Takes care of creating ObjectNames for Monitor which has MeterType Ex:JVM and later add it to the mxMap set which
     *            maintains the MBean objectNames.
     */
    private void processMeters(Object instance) {
        try {
            Set<Field> publishedFields = new HashSet<Field>();
            Class<?> clazz = instance.getClass();
            for (Field f : clazz.getDeclaredFields()) {
                PublishedMetric publishedMetric = f.getAnnotation(PublishedMetric.class);
                if (publishedMetric == null) {
                    continue;
                }
                f.setAccessible(true);
                //If Stats extends Meter, create MXBean with specified type
                //WebSphere:type=<ClassSimleName>
                if (Meter.class.isAssignableFrom(f.getType())) {
                    Object o = f.get(instance);
                    if (o != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "need to create MBEAN for " + o);
                        }
                        publishedFields.add(f);
                        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
                        StringBuilder sb = new StringBuilder("WebSphere:");
                        sb.append("type=").append(o.getClass().getSimpleName());
                        ObjectName objectName = new ObjectName(sb.toString());
                        mbeanServer.registerMBean(o, objectName);
                        Set<ObjectName> temp = mxmap.get(instance);
                        if (temp != null) {
                            temp.add(objectName);
                        } else {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "ObjectName returned from mxMap is null");
                            }
                        }
                    }
                } else if (MeterCollection.class.isAssignableFrom(f.getType())) {
                    //If Stats extends MeterCollection, create MXBean with specified type
                    //WebSphere:type=<ClassSimleName>,name=<instanceName>
                    //This is Handled in MeterCollection, as during initialisation, we don't know all instances.
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, e.getMessage());
            }
        }
    }

    /**
     * @param bundle
     *            Checks to see if the bundle being deactivated is Monitoring bundle ,if so unregister the PerfMBean which
     *            is registered if traditional PMI is enabled .Later initiate call for unregisterMonitor.
     */
    synchronized void deactivateMonitors(Bundle bundle) {
        if (bundle.getSymbolicName().equals("com.ibm.ws.monitor")) {
            //deregister MBean
            try {
                MBeanServer mServer = ManagementFactory.getPlatformMBeanServer();
                mServer.unregisterMBean(new ObjectName(PmiConstants.MBEAN_NAME));
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unregisterung MBean Failed due to ", e.getMessage());
                }
            }

        }
        unregisterMonitor(bundle.getBundleId(), null);
    }

    /**
     * @param bundleId
     * @param monitor
     *            This method is used to unregister the monitor and cleansup the Mbeans associated with those monitors.
     *            The call to this method happens during deactivation of any bundle(Which includes monitor bundles) or
     *            from the modified method.
     */
    public synchronized void unregisterMonitor(Long bundleId, MonitorObject monitor) {
        if (monitor != null) {
            if (mbeanServer == null)
                mbeanServer = ManagementFactory.getPlatformMBeanServer();

            Set<ObjectName> onameSet = mxmap.remove(monitor.getMonitor());
            unregisterMbeans(onameSet);
            monitorManager.unregisterMonitor(monitor.getMonitor());
            monitor.setRegistered(false);
            //(processedBundles.get(bundleId)).remove(monitor);
        } else {
            Set<MonitorObject> bundleMonitors = processedBundles.remove(bundleId);
            if (bundleMonitors == null) {
                return;
            }

            for (MonitorObject o : bundleMonitors) {
                Set<ObjectName> mbeanonameset = mxmap.remove(o.getMonitor());
                unregisterMbeans(mbeanonameset);
                monitorManager.unregisterMonitor(o.getMonitor());
                o.setRegistered(false);
            }
        }

    }

    /**
     * @param context ComponentContext
     * @param newProperties
     * @throws Exception
     *             This method is called when ever there is change in server.xml.In Here we will be looking for the list of monitor's which needs
     *             to be instrumented.If groupMonitoring is specified and there is no input ex: groupMonitoring="" we assume that all the monitors
     *             needs to be instrumented.If atleast one is preset as part of groupMonitoring i.e, groupMonitoring="WebContainer,JVM,ThreadPool"
     *             then those present will be instrumented.Please note that rest all monitors which are nor present in groupMonitoring will be
     *             unregistered and respective Mbeans are removed from the MBeanServer.
     */
    protected void modified(ComponentContext context, Map<String, Object> newProperties) throws Exception {
        try {
            groupList.clear();
            String filter = (String) newProperties.get(MONITORING_GROUP_FILTER);
            if (!(filter.length() > 0)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Enabling All:Started");
                }
                Set<Object> monitorkeys = objectMap.keySet();
                Iterator monitorIterator = monitorkeys.iterator();
                while (monitorIterator.hasNext()) {
                    Object monobj = monitorIterator.next();
                    MonitorObject mob = objectMap.get(monobj);
                    if (!(mob.getRegistered())) {
                        Object regMon = constructMonitor(mob.getClazz());
                        processANDregister(regMon, mob);
                    }
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Enabling All:Completed");
                }
                return;
            }
            StringTokenizer st = new StringTokenizer(filter, ",");
            while (st.hasMoreTokens()) {
                groupList.add(st.nextToken());
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Enabling those Monitors specified in groupMonitoring and " +
                             "disabling those which are not specified and are already being monitored");
            }
            Set<Object> monitorkeys = objectMap.keySet();
            Iterator monitorIterator = monitorkeys.iterator();
            if (groupList.size() > 0) {
                while (monitorIterator.hasNext()) {
                    Object monobj = monitorIterator.next();
                    MonitorObject mob = objectMap.get(monobj);
                    Monitor groups = mob.getGroups();
                    for (String group : groups.group()) {
                        if (!(groupList.contains(group))) {
                            unregisterMonitor(mob.getBundleId(), mob);
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "Unregistered " + group);
                            }
                        } else {
                            if (!(mob.getRegistered())) {
                                Object regMon = constructMonitor(mob.getClazz());
                                processANDregister(regMon, mob);
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Registered " + group);
                                }
                            }
                        }
                    }
                }
            } else {
                while (monitorIterator.hasNext()) {
                    Object monobj = monitorIterator.next();
                    MonitorObject mob = objectMap.get(monobj);
                    if (mob.getRegistered()) {
                        unregisterMonitor(mob.getBundleId(), mob);
                    }
                }
            }
        } catch (Exception e) {

            e.getMessage();
        }
    }

    /**
     * @param onameset
     *            Used to Unregister Mbeans and takes set as Input.This set contains ObjectNames of the Mbeans which needs to be unregistered.
     */
    private void unregisterMbeans(Set<ObjectName> onameset) {
        if (mbeanServer != null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        if (onameset == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning from here is onameset is null");
            }
            return;
        } else {
            for (ObjectName on : onameset) {
                try {
                    mbeanServer.unregisterMBean(on);
                } catch (MBeanRegistrationException e) {
                    FFDCFilter.processException(e, getClass().getSimpleName(), "deactivateMonitors:MBeanRegistrationException");
                } catch (InstanceNotFoundException e) {
                    FFDCFilter.processException(e, getClass().getSimpleName(), "deactivateMonitors:InstanceNotFoundException");
                }
            }
        }
    }
}