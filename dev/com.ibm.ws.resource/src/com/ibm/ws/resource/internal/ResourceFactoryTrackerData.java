/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource.internal;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * This object holds the data associated with a ResourceFactory registration.
 */
public class ResourceFactoryTrackerData {
    private static final String JNDI_SERVICENAME = "osgi.jndi.service.name";

    /**
     * The context of the bundle that registered the ResourceFactory.
     */
    private final BundleContext context;

    /**
     * The registration of the ServiceFactory "proxy" to the ResourceFactory, or
     * null if no service is registered, either because the ResourceFactory
     * was unregistered or had invalid properties.
     */
    private ServiceRegistration<?> registration;

    /**
     * The interfaces used to registered the ServiceyFactory "proxy", or null
     * if {@link #registration} is null.
     */
    private String[] interfaces;

    public ResourceFactoryTrackerData(BundleContext context) {
        this.context = context;
    }

    /**
     * Gets an array of resource service interfaces from a {@link ResourceFactory#CREATES_OBJECT_CLASS} value,
     * possibly further refining to those listed under creates.objectClassDefault.
     * Service property creates.objectClassDefault is added as a way to indicate that a subset of classes of creates.objectClass
     * are compatible with the return type of ResourceFactory.createResource(null). This is useful for screening out
     * creates.objectClass classes that can only be accessed with a resource ref type or resource env ref type.
     *
     * @param ref service reference to a resource factory.
     * @return an array of service properties, or null if creates.compatibleClass/{@code createsObjectClass} is invalid
     */
    private String[] getServiceInterfaces(ServiceReference<ResourceFactory> ref) {
        Object createsObjectClass = ref.getProperty(ResourceFactory.CREATES_OBJECT_CLASS);
        if (createsObjectClass instanceof String[]) {
            Object createsObjectClassDefault = ref.getProperty("creates.objectClassDefault");
            if (createsObjectClassDefault instanceof String)
                return new String[] { (String) createsObjectClassDefault };
            else if (createsObjectClassDefault instanceof String[])
                return (String[]) createsObjectClassDefault;
            return (String[]) createsObjectClass;
        }
        if (createsObjectClass instanceof String) {
            return new String[] { (String) createsObjectClass };
        }
        return null;
    }

    /**
     * Gets an array of resource service properties from a {@link ResourceFactory} service reference.
     * 
     * @param ref a ResourceFactory service reference
     * @return service properties
     */
    private Dictionary<String, Object> getServiceProperties(ServiceReference<ResourceFactory> ref) {
        String[] keys = ref.getPropertyKeys();
        Dictionary<String, Object> properties = new Hashtable<String, Object>(keys.length);
        for (String key : keys) {
            if (!key.equals(ResourceFactory.JNDI_NAME) &&
                !key.equals(ResourceFactory.CREATES_OBJECT_CLASS)) {
                properties.put(key, ref.getProperty(key));
            }
        }

        properties.put(ResourceFactory.class.getName(), "true");
        properties.put(JNDI_SERVICENAME, ref.getProperty(ResourceFactory.JNDI_NAME));

        return properties;
    }

    /**
     * Register a ServiceFactory for the ResourceFactory if necessary.
     */
    public void register(ServiceReference<ResourceFactory> ref) {
        register(ref, getServiceInterfaces(ref));
    }

    private void register(final ServiceReference<ResourceFactory> ref, String[] interfaces) {
        if (interfaces != null) {
            ServiceFactory<?> factory = new ServiceFactory<Object>() {
                @Override
                public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
                    BundleContext ctx = bundle.getBundleContext();
                    if (ctx == null)
                        return null;

                    ResourceFactory factory = ctx.getService(ref);
                    try {
                        return factory.createResource(null);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                @FFDCIgnore(value = IllegalStateException.class)
                public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
                    try {
                        BundleContext ctx = bundle.getBundleContext();
                        if (ctx != null) {
                            context.ungetService(ref);
                        }
                    } catch (IllegalStateException ignored) {
                        // BundleContext is no longer valid.
                    }
                }
            };

            registration = context.registerService(interfaces, factory, getServiceProperties(ref));
            this.interfaces = interfaces;
        }
    }

    /**
     * Notification that the properties for the ResourceFactory changed.
     */
    public void modifed(ServiceReference<ResourceFactory> ref) {
        String[] newInterfaces = getServiceInterfaces(ref);
        if (!Arrays.equals(interfaces, newInterfaces)) {
            unregister();
            register(ref, newInterfaces);
        } else {
            registration.setProperties(getServiceProperties(ref));
        }
    }

    /**
     * Unregistered the ServiceFactory for the ResourceFactory if necessary.
     */
    public void unregister() {
        if (registration != null) {
            registration.unregister();
            this.registration = null;
            interfaces = null;
        }
    }
}
