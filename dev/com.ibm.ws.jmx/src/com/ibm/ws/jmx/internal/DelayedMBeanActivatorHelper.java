/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.internal;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jmx.PlatformMBeanService;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           service = { PlatformMBeanService.class },
           property = { "service.vendor=IBM" })
public final class DelayedMBeanActivatorHelper implements PlatformMBeanService, ServiceTrackerCustomizer<Object, ServiceReference<?>> {

    private volatile DelayedMBeanActivator mDelayedMBeanActivator;
    private final ConcurrentHashMap<ServiceReference<?>, ObjectName> mBeanMap;

    private ServiceTracker<Object, ServiceReference<?>> mbeanTracker;
    private MBeanServerPipeline pipeline;

    public DelayedMBeanActivatorHelper() {
        mBeanMap = new ConcurrentHashMap<ServiceReference<?>, ObjectName>();
    }

    /**
     * DS method to activate this component.
     *
     * @param compContext
     */
    protected void activate(ComponentContext compContext) {
        BundleContext ctx = compContext.getBundleContext();
        mDelayedMBeanActivator = new DelayedMBeanActivator(ctx);
        pipeline.insert(mDelayedMBeanActivator);
        try {
            mbeanTracker = new ServiceTracker<Object, ServiceReference<?>>(ctx, ctx.createFilter("(jmx.objectname=*)"), this);
            mbeanTracker.open(true);
        } catch (InvalidSyntaxException ise) {

        }
    }

    /**
     * DS method to deactivate this component.
     *
     * @param compContext
     */
    protected void deactivate(ComponentContext compContext) {
        if (mbeanTracker != null) {
            mbeanTracker.close();
        }
        pipeline.remove(mDelayedMBeanActivator);
    }

    @Override
    public MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Sets the reference to a dynamic MBean.
     */
    protected void setMBean(ServiceReference<?> ref) {
        // Use "jmx.objectname" for determining the ObjectName to be consistent with Apache Aries.
        Object jmxObjectName = ref.getProperty("jmx.objectname");
        if (jmxObjectName instanceof String) {
            try {
                // Construct an ObjectName from the "jmx.objectname" property and register the MBean.
                ObjectName name = new ObjectName((String) jmxObjectName);
                setServiceReferenceInternal(ref, name);
            } catch (MalformedObjectNameException e) {
                // One of our MBeans had a bad ObjectName.
                // TODO: trace, FFDC?
            }
        } else {
            // REVISIT: "jmx.objectname" was not specified or was not in
            // the right format. Ignoring this MBean for now since we don't
            // have an ObjectName. Possible that this MBean is a
            // javax.management.MBeanRegistration, do we want to try to
            // register it that way?
        }
    }

    /**
     * Remove the reference to the dynamic MBean.
     */
    protected void unsetMBean(ServiceReference<?> ref) {
        unsetServiceReferenceInternal(ref);
    }

    /**
     * Adds the DelayedMBeanActivator to the platform MBeanServer pipeline.
     */
    @Reference
    protected void setMBeanServerPipeline(MBeanServerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Removes the DelayedMBeanActivator to the platform MBeanServer pipeline.
     */
    protected void unsetMBeanServerPipeline(MBeanServerPipeline pipeline) {
        this.pipeline = null;
    }

    // Helper methods
    private void setServiceReferenceInternal(ServiceReference<?> ref, ObjectName name) {
        if (mBeanMap.put(ref, name) != null) {
            // ignore
        }
        // Only register the MBeans here if the activate() method has already been called on this component. TODO remove comment, no longer valid
        if (!mDelayedMBeanActivator.registerDelayedMBean(ref, name)) {
            // Collision: If we get here it means that more than one of our MBeans has the same ObjectName.
            // This shouldn't happen.
            // TODO: trace, FFDC?
        }
    }

    @FFDCIgnore(InstanceNotFoundException.class)
    private void unsetServiceReferenceInternal(ServiceReference<?> ref) {
        final ObjectName name = mBeanMap.remove(ref);
        if (name != null) {
            try {
                mDelayedMBeanActivator.unregisterMBean(name);
            } catch (InstanceNotFoundException e) {
                // Unexpected. The delayed registration failed or someone else removed this MBean.
                // TODO: trace
            } catch (MBeanRegistrationException e) {
                // TODO: trace, FFDC?
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public ServiceReference<?> addingService(ServiceReference<Object> reference) {
        setMBean(reference);
        return reference;
    }

    /** {@inheritDoc} */
    @Override
    public void modifiedService(ServiceReference<Object> reference, ServiceReference<?> service) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removedService(ServiceReference<Object> reference, ServiceReference<?> service) {
        unsetMBean(reference);

    }
}
