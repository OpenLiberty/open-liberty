/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.service;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.internal.BootstrapContextImpl;
import com.ibm.ws.jca.internal.ResourceAdapterMetaData;
import com.ibm.ws.jca.internal.Utils;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Resource factory for administered objects.
 */
//as documentation only at this point:
//@Component(pid="com.ibm.ws.jca.adminObject.supertype")
//also ???
//@Component(pid="com.ibm.ws.jca.connectionFactory.supertype", service=AdminObjectService.class.getName())
public class AdminObjectService implements ResourceFactory, ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(AdminObjectService.class);

    public static final String JNDI_NAME = "jndiName";
    public static final String ADMIN_OBJECT_PID = "com.ibm.ws.jca.adminObject.supertype";
    public static final String ADMIN_OBJECT = "adminObject";
    /**
     * Prefix for flattened config properties.
     */
    private static final String CONFIG_PROPS_PREFIX = "properties.0.";

    /**
     * Length of prefix for flattened config properties.
     */
    private static final int CONFIG_PROPS_PREFIX_LENGTH = CONFIG_PROPS_PREFIX.length();

    /**
     * Name of admin object implementation class.
     */
    private String adminObjectImplClassName;

    /**
     * Set of names of applications that have accessed this administered object. The set supports concurrent modifications.
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Reference to the resource adapter bootstrap context.
     */
    private final AtomicServiceReference<BootstrapContextImpl> bootstrapContextRef = new AtomicServiceReference<BootstrapContextImpl>("bootstrapContext");

    /**
     * Name of the administered object.
     * The name is the jndiName if specified, otherwise the config id.
     */
    private String name;

    private String id;

    private String jndiName;

    /**
     * Service properties for this instance. Includes the config properties.
     */
    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context DeclarativeService defined/populated component context
     */
    @Trivial
    protected void activate(ComponentContext context) throws Exception {
        Dictionary<String, ?> props = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", props);

        adminObjectImplClassName = (String) props.get(CONFIG_PROPS_PREFIX + "adminobject-class");
        name = (String) props.get("config.id");
        id = (String) props.get("id");
        jndiName = (String) props.get("jndiName");

        for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            if (key.length() > CONFIG_PROPS_PREFIX_LENGTH && key.charAt(CONFIG_PROPS_PREFIX_LENGTH - 1) == '.' && key.startsWith(CONFIG_PROPS_PREFIX)) {
                String propName = key.substring(CONFIG_PROPS_PREFIX_LENGTH);
                if (propName.indexOf('.') < 0 && propName.indexOf('-') < 0)
                    properties.put(propName, props.get(key));
            }
        }

        bootstrapContextRef.activate(context);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /** {@inheritDoc} */
    @Override
    public Object createResource(ResourceInfo refInfo) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createResource", refInfo);
        try {
            BootstrapContextImpl bootstrapContext = bootstrapContextRef.getServiceWithException();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "loading", adminObjectImplClassName);
            Class<?> adminObjectClass = bootstrapContext.loadClass(adminObjectImplClassName);
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            String currentApp = null;
            ResourceAdapterMetaData metadata = bootstrapContext.getResourceAdapterMetaData();
            // cData is null when its not in an application thread
            if (cData != null && cData != metadata) {
                currentApp = cData.getJ2EEName().getApplication();
                applications.add(cData.getJ2EEName().getApplication());
            }
            String adapterName = bootstrapContext.getResourceAdapterName();
            if (metadata != null && metadata.isEmbedded() && cData != metadata) { // Metadata is null for SIB/WMQ. No check needed if called from activationSpec
                String embeddedApp = metadata.getJ2EEName().getApplication();
                Utils.checkAccessibility(name, adapterName, embeddedApp, currentApp, false);
            }
            Object adminObject = adminObjectClass.getConstructor().newInstance();
            bootstrapContext.configure(adminObject, name, properties, null, null, null);
            return adminObject;
        } catch (Exception x) {
            throw x;
        } catch (Error x) {
            throw x;
        } finally {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createResource");
        }
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext context) {
        bootstrapContextRef.deactivate(context);
    }

    @Override
    public ApplicationRecycleContext getContext() {
        ApplicationRecycleContext context = bootstrapContextRef.getService();
        if (context != null) {
            return context;
        }
        return null;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        return members;
    }

    /**
     * Declarative Services method for setting the BootstrapContext reference
     * 
     * @param ref reference to the service
     */
    protected void setBootstrapContext(ServiceReference<BootstrapContextImpl> ref) {
        bootstrapContextRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the BootstrapContext reference
     * 
     * @param ref reference to the service
     */
    protected void unsetBootstrapContext(ServiceReference<BootstrapContextImpl> ref) {
        bootstrapContextRef.unsetReference(ref);
    }

    public String getId() {
        return id;
    }

    public String getJndiName() {
        return jndiName;
    }
}
