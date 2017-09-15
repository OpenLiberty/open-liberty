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
package com.ibm.ws.jdbc.management.j2ee.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.library.LibraryChangeListener;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 * 
 * OSGi methods (activate/deactivate) should be protected.
 */
@Component(immediate = true)
public class JDBCMBeanRuntime {

    ////////////////////////////// Logs & Trace variables //////////////////////////////
    private static final String TRACE_GROUP = "RRA";
    private static final TraceComponent tc = Tr.register(JDBCMBeanRuntime.class, TRACE_GROUP);
    private static final String className = "JDBCMBeanRuntime :";
    private static int counterActivate = 0;
    private static int counterDeactivate = 0;
    private static int counterSetDataSource = 0;
    private static int counterSetJdbcDriver = 0;
    private static int counterUnsetDataSource = 0;
    private static int counterUnsetJdbcDriver = 0;
    private static int counterUpdatedDataSource = 0;

    ////////////////////////////// MBeans Lists variables //////////////////////////////
    private final ConcurrentHashMap<String, JDBCDataSourceMBeanImpl> dsMBeanList = new ConcurrentHashMap<String, JDBCDataSourceMBeanImpl>();
    private final ConcurrentHashMap<String, JDBCResourceMBeanImpl> rsMBeanList = new ConcurrentHashMap<String, JDBCResourceMBeanImpl>();
    private final ConcurrentHashMap<String, JDBCDriverMBeanImpl> drMBeanList = new ConcurrentHashMap<String, JDBCDriverMBeanImpl>();

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    @Activate
    protected void activate(Map<String, Object> properties) {
        final String methodName = "activate";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JDBCMBeanRuntime.counterActivate);
        if (trace && tc.isEventEnabled())
            Tr.event(tc, className + methodName, properties);
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, "Normal Exit");

    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     */
    @Deactivate
    protected void deactivate(int reason) {
        final String methodName = "deactivate";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JDBCMBeanRuntime.counterDeactivate);
        if (trace && tc.isEventEnabled())
            Tr.event(tc, className + methodName + ", reason=" + reason);
        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, "Normal Exit");
    }

    @Reference(service = ResourceFactory.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(component.name=com.ibm.ws.jdbc.dataSource)")
    protected void setDataSource(ServiceReference<ResourceFactory> ref) {
        final String methodName = "setDataSource";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JDBCMBeanRuntime.counterSetDataSource);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");
            final String jdbcResourcename = name + "/JDBCResource";
            final String id = name.contains("]/dataSource[") ? null : (String) ref.getProperty("id");
            final String jndiName = (String) ref.getProperty("jndiName");
            final BundleContext bndCtx = getBundleContext(FrameworkUtil.getBundle(getClass()));
            String JDBCDriver = null;

            try {
                ServiceReference<?>[] refs2 = getServiceReferences(bndCtx, "com.ibm.ws.jdbc.internal.JDBCDriverService", (String) ref.getProperty("driver.target"));
                JDBCDriver = (String) refs2[0].getProperty("id"); // TODO - need to handle differently since we may not find one.
            } catch (InvalidSyntaxException e) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, className + methodName + ": Unexpected Exception: " + e);
            }

            /////////////////////////////// Variables from BundleContext ///////////////////////////////
            final String J2EEServer = bndCtx.getProperty(WsLocationConstants.LOC_SERVER_NAME);

            //Create & register JDBCResourceMbean if it doesn't exist already
            JDBCResourceMBeanImpl rsMBean = rsMBeanList.get(jdbcResourcename);
            if (rsMBean == null) {
                rsMBean = new JDBCResourceMBeanImpl(id, jndiName, jdbcResourcename, J2EEServer);
                rsMBean.register(bndCtx);
                JDBCResourceMBeanImpl rsMBeanTemp = this.rsMBeanList.putIfAbsent(rsMBean.getName(), rsMBean);
                if (rsMBeanTemp != null) {
                    // This mean some other thread beat us to it. However this should not happen because currently only
                    // this synchronized block is doing the put on the ConcurrentHashMap.
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "JDBCResourceMBean: Unexpected put for the key: " + rsMBean.getName() + " happened in " + className + methodName);
                }
            }

            /////////////////////////// Variables from JDBCResourceMBeanImpl ///////////////////////////
            final String JDBCResource = rsMBean.getName();

            //Create & register JDBCDataSourceMbean
            final JDBCDataSourceMBeanImpl dsMBean = new JDBCDataSourceMBeanImpl(id, jndiName, name, J2EEServer, JDBCResource, JDBCDriver);
            dsMBean.register(bndCtx);
            JDBCDataSourceMBeanImpl dsMBeanTemp = this.dsMBeanList.putIfAbsent(dsMBean.getName(), dsMBean);
            if (dsMBeanTemp != null) {
                // This mean some other thread beat us to it. However this should not happen because currently only
                // this synchronized block is doing the put on the ConcurrentHashMap.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "JDBCDataSourceMBean: Unexpected put for the key: " + dsMBean.getName() + " happened in " + className + methodName);
            }
            //Update JDBCResourceMBeanImpl children list to include the new child JDBCDataSourceMBeanImpl
            rsMBean.setDataSourceChild(dsMBean.getName(), dsMBean);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, "Normal Exit");
    }

    @Reference(service = LibraryChangeListener.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(component.name=com.ibm.ws.jdbc.jdbcDriver)")
    protected void setJdbcDriver(ServiceReference<LibraryChangeListener> ref) {
        final String methodName = "setJdbcDriver";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JDBCMBeanRuntime.counterSetJdbcDriver);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");
            final String id = name.contains("]/jdbcDriver[") ? null : (String) ref.getProperty("id");
            final BundleContext bndCtx = getBundleContext(FrameworkUtil.getBundle(getClass()));

            /////////////////////////////// Variables from BundleContext ///////////////////////////////
            final String J2EEServer = bndCtx.getProperty(WsLocationConstants.LOC_SERVER_NAME);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "TODO: Need to register mbeans for jdbcDriver id=" + id + " name=" + name);

            // Create JDBCDriverMBeanImpl
            JDBCDriverMBeanImpl drMBean = new JDBCDriverMBeanImpl(id, name, J2EEServer);

            // Register JDBCDriverMBeanImpl
            drMBean.register(bndCtx);
            JDBCDriverMBeanImpl drMBeanTemp = this.drMBeanList.putIfAbsent(drMBean.getName(), drMBean);
            if (drMBeanTemp != null) {
                // This mean some other thread beat us to it. However this should not happen because currently only
                // this synchronized block is doing the put on the ConcurrentHashMap.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "JDBCDriverMBean: Unexpected put for the key: " + drMBean.getName() + " happened in " + className + methodName);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, "Normal Exit");
    }

    protected void unsetDataSource(ServiceReference<ResourceFactory> ref) {
        final String methodName = "unsetDataSource";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JDBCMBeanRuntime.counterUnsetDataSource);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");

            //Get a reference to the JDBCDataSourceMBeanImpl needed to be unregistered
            JDBCDataSourceMBeanImpl dsMBean = this.dsMBeanList.get(name);
            //Get a reference to the JDBCResourceMBeanImpl needed to be unregistered
            JDBCResourceMBeanImpl rsMBean = this.rsMBeanList.get(dsMBean.getJDBCResource());

            //Remove the parent-child relation
            rsMBean.removeDataSourceChild(dsMBean.getName());
            //Update the ConcurrentHashMap
            this.dsMBeanList.remove(dsMBean.getName());
            //Unregister JDBCDataSourceMBeanImpl
            dsMBean.unregister();
            //Unregister JDBCResourceMBeanImpl only if it has no more children.
            if (rsMBean.getDataSourceChildrenCount() == 0) {
                //Update the ConcurrentHashMap
                this.rsMBeanList.remove(rsMBean.getName());
                //Unregister JDBCResourceMBeanImpl
                rsMBean.unregister();
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    protected void unsetJdbcDriver(ServiceReference<LibraryChangeListener> ref) {
        final String methodName = "unsetJdbcDriver";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JDBCMBeanRuntime.counterUnsetJdbcDriver);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");

            //Get a reference to the JDBCDriverMBeanImpl need to be unregistered
            JDBCDriverMBeanImpl drMBean = this.drMBeanList.get(name);
            //Update the ConcurrentHashMap
            this.drMBeanList.remove(drMBean.getName());
            //Unregister JDBCDriverMBeanImpl
            drMBean.unregister();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    protected void updatedDataSource(ServiceReference<ResourceFactory> ref) {
        final String methodName = "updatedDataSource";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JDBCMBeanRuntime.counterUpdatedDataSource);
        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");
            final String jdbcResourcename = name + "/JDBCResource";
            final String id = name.contains("]/dataSource[") ? null : (String) ref.getProperty("id");
            final String jndiName = (String) ref.getProperty("jndiName");
            final BundleContext bndCtx = getBundleContext(FrameworkUtil.getBundle(getClass()));
            String JDBCDriver = null;

            try {
                ServiceReference<?>[] refs2 = getServiceReferences(bndCtx, "com.ibm.ws.jdbc.internal.JDBCDriverService", (String) ref.getProperty("driver.target"));
                JDBCDriver = (String) refs2[0].getProperty("id"); // TODO - need to handle differently since we may not find one.
            } catch (InvalidSyntaxException e) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, className + methodName + ": Unexpected Exception: " + e);
            }

            //Part 1: Unregister the effected MBeans

            //Get a reference to the JDBCDataSourceMBeanImpl needed to be unregistered
            JDBCDataSourceMBeanImpl dsMBeanOld = this.dsMBeanList.get(name);
            //Get a reference to the JDBCResourceMBeanImpl needed to be unregistered
            JDBCResourceMBeanImpl rsMBeanOld = this.rsMBeanList.get(dsMBeanOld.getJDBCResource());

            //Remove the parent-child relation
            rsMBeanOld.removeDataSourceChild(dsMBeanOld.getName());
            //Update the ConcurrentHashMap
            this.dsMBeanList.remove(dsMBeanOld.getName());
            //Unregister JDBCDataSourceMBeanImpl
            dsMBeanOld.unregister();
            //Unregister JDBCResourceMBeanImpl only if it has no more children.
            if (rsMBeanOld.getDataSourceChildrenCount() == 0) {
                //Update the ConcurrentHashMap
                this.rsMBeanList.remove(rsMBeanOld.getName());
                //Unregister JDBCResourceMBeanImpl 
                rsMBeanOld.unregister();
            }

            //Part 2: Register the effected MBeans with the new info

            /////////////////////////////// Variables from BundleContext ///////////////////////////////
            final String J2EEServer = bndCtx.getProperty(WsLocationConstants.LOC_SERVER_NAME);

            //Create & register JDBCResourceMbean if it doesn't exist already
            JDBCResourceMBeanImpl rsMBeanNew = rsMBeanList.get(jdbcResourcename);
            if (rsMBeanNew == null) {
                rsMBeanNew = new JDBCResourceMBeanImpl(id, jndiName, jdbcResourcename, J2EEServer);
                rsMBeanNew.register(bndCtx);
                JDBCResourceMBeanImpl rsMBeanTemp = this.rsMBeanList.putIfAbsent(rsMBeanNew.getName(), rsMBeanNew);
                if (rsMBeanTemp != null) {
                    // This mean some other thread beat us to it. However this should not happen because currently only
                    // this synchronized block is doing the put on the ConcurrentHashMap.
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "JDBCResourceMBean: Unexpected put for the key: " + rsMBeanNew.getName() + " happened in " + className + methodName);
                }
            }

            /////////////////////////// Variables from JDBCResourceMBeanImpl ///////////////////////////
            final String JDBCResource = rsMBeanNew.getName();

            //Create & register JDBCDataSourceMbean
            final JDBCDataSourceMBeanImpl dsMBeanNew = new JDBCDataSourceMBeanImpl(id, jndiName, name, J2EEServer, JDBCResource, JDBCDriver);
            dsMBeanNew.register(bndCtx);
            JDBCDataSourceMBeanImpl dsMBeanTemp = this.dsMBeanList.putIfAbsent(dsMBeanNew.getName(), dsMBeanNew);
            if (dsMBeanTemp != null) {
                // This mean some other thread beat us to it. However this should not happen because currently only
                // this synchronized block is doing the put on the ConcurrentHashMap.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "JDBCDataSourceMBean: Unexpected put for the key: " + dsMBeanNew.getName() + " happened in " + className + methodName);
            }
            //Update JDBCResourceMBeanImpl children list to include the new child JDBCDataSourceMBeanImpl
            rsMBeanNew.setDataSourceChild(dsMBeanNew.getName(), dsMBeanNew);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, "Normal Exit");
    }

    // no need for an updatedJdbcDriver because changes will not impact the mbean

    private static BundleContext getBundleContext(final Bundle bundle) {
        if (System.getSecurityManager() == null)
            return bundle.getBundleContext();
        else
            return AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
                @Override
                public BundleContext run() {
                    return bundle.getBundleContext();
                }
            });
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private static ServiceReference<?>[] getServiceReferences(final BundleContext bCtx, final String clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceReference<?>[]>() {
                    @Override
                    public ServiceReference<?>[] run() throws InvalidSyntaxException {
                        return bCtx.getServiceReferences(clazz, filter);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

}
