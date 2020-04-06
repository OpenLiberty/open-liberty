/*******************************************************************************
 * Copyright (c) 2015,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.management.j2ee.internal;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
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
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
@Component
public class JCAMBeanRuntime {

    //////////////////////////////Logs & Trace variables //////////////////////////////
    private static final TraceComponent tc = Tr.register(JCAMBeanRuntime.class, "WAS.j2c");
    private static final String className = "JCAMBeanRuntime :";
    private static int counterSetConnectionFactory = 0;
    private static int counterSetResourceAdapter = 0;
    private static int counterUnsetConnectionFactory = 0;
    private static int counterUnsetResourceAdapter = 0;

    //////////////////////////////MBeans Lists variables //////////////////////////////
    protected final ConcurrentHashMap<String, ResourceAdapterMBeanImpl> resourceAdapters = new ConcurrentHashMap<String, ResourceAdapterMBeanImpl>();
    protected final ConcurrentHashMap<String, ResourceAdapterModuleMBeanImpl> resourceAdapterModules = new ConcurrentHashMap<String, ResourceAdapterModuleMBeanImpl>();
    protected final ConcurrentHashMap<String, JCAResourceMBeanImpl> jcaResources = new ConcurrentHashMap<String, JCAResourceMBeanImpl>();
    protected final ConcurrentHashMap<String, JCAConnectionFactoryMBeanImpl> jcaConnectionFactorys = new ConcurrentHashMap<String, JCAConnectionFactoryMBeanImpl>();
    protected final ConcurrentHashMap<String, JCAManagedConnectionFactoryMBeanImpl> jcaManagedConnectionFactorys = new ConcurrentHashMap<String, JCAManagedConnectionFactoryMBeanImpl>();

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    @Activate
    protected void activate(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "JCAMBeanRuntime activated", properties);
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     */
    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "JCAMBeanRuntime deactivated, reason=" + reason);
    }

    @Reference(service = ResourceFactory.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(component.name=com.ibm.ws.jca.connectionFactory.supertype)")
    protected void setConnectionFactory(ServiceReference<ResourceFactory> ref) {
        final String methodName = "setConnectionFactory";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JCAMBeanRuntime.counterSetConnectionFactory);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");
            final String id = name.contains("]/") ? null : (String) ref.getProperty("id");
            final String jndiName = (String) ref.getProperty("jndiName");
            final String mcf = (String) ref.getProperty("properties.0.managedconnectionfactory-class");
            final String resourceAdapter = (String) ref.getProperty("properties.0.resourceAdapterConfig.id");
            final String jcaResourcName = resourceAdapter + "/JCAResource";
            final BundleContext bndCtx = getBundleContext(FrameworkUtil.getBundle(getClass()));
            final String server = bndCtx.getProperty(WsLocationConstants.LOC_SERVER_NAME);

            //Create & register JCAResourceMBean if it doesn't exist already
            // need to only create one of these for each resource adapter, changing the key from name to resourceAdapter
            // since we are changing the name, its likely a test will fail.
            JCAResourceMBeanImpl rmbean = jcaResources.get(jcaResourcName);
            if (rmbean == null) {
                rmbean = new JCAResourceMBeanImpl(jcaResourcName, resourceAdapter, server);
                rmbean.register(bndCtx);
                JCAResourceMBeanImpl rmbeanTemp = jcaResources.putIfAbsent(jcaResourcName, rmbean);
                if (rmbeanTemp != null) {
                    // This mean some other thread beat us to it. However this should not happen because currently only
                    // this synchronized block is doing the put on the ConcurrentHashMap.
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "JCAResourceMBean: Unexpected put for the key: " + rmbean.getName() + " happened in " + className + methodName);
                }
            }

            //Create & register JCAConnectionFactoryMBean
            JCAConnectionFactoryMBeanImpl cfmbean = new JCAConnectionFactoryMBeanImpl(id, jndiName, name, server, mcf, resourceAdapter);
            cfmbean.register(bndCtx);
            JCAConnectionFactoryMBeanImpl cfmbeanTemp = jcaConnectionFactorys.putIfAbsent(name, cfmbean);
            if (cfmbeanTemp != null) {
                // This mean some other thread beat us to it. However this should not happen because currently only
                // this synchronized block is doing the put on the ConcurrentHashMap.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "JCAConnectionFactoryMBean: Unexpected put for the key: " + cfmbean.getName() + " happened in " + className + methodName);
            }
            //Update JCAResourceMBeanImpl children list to include the new child JCAConnectionFactoryMBeanImpl
            rmbean.setConnectionFactoryChild(cfmbean.getName(), cfmbean);

            //Create & register JCAManagedConnectionFactoryMBean
            JCAManagedConnectionFactoryMBeanImpl mcfmbean = new JCAManagedConnectionFactoryMBeanImpl(name, server);
            mcfmbean.register(bndCtx);
            JCAManagedConnectionFactoryMBeanImpl mcfmbeanTemp = jcaManagedConnectionFactorys.putIfAbsent(name, mcfmbean);
            if (mcfmbeanTemp != null) {
                // This mean some other thread beat us to it. However this should not happen because currently only
                // this synchronized block is doing the put on the ConcurrentHashMap.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "JCAManagedConnectionFactoryMBean: Unexpected put for the key: " + mcfmbean.getName() + " happened in " + className + methodName);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, "Normal Exit");
    }

    @SuppressWarnings("unchecked")
    @Reference(service = ApplicationRecycleContext.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(component.name=com.ibm.ws.jca.resourceAdapter.properties)")
    // Temporary work around ibm:extends behavior. In the future will only need target = (component.name=com.ibm.ws.jca.resourceAdapter.properties)
    protected void setResourceAdapter(ServiceReference<ApplicationRecycleContext> ref) {
        final String methodName = "setResourceAdapter";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JCAMBeanRuntime.counterSetResourceAdapter);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");
            final String moduleName = (String) ref.getProperty("module-name");
            if (moduleName == null) { // Means we don't have enough information to build any MBean
                if (trace && tc.isEntryEnabled())
                    Tr.exit(tc, methodName, "Short Exit. No MBeans were created.");
                return;
            }
            final String id = (String) ref.getProperty("id");
            final int appNameLength = id.length() - moduleName.length() - 1;
            final String appName = appNameLength > 0 ? id.substring(0, appNameLength) : null;
            final BundleContext bndCtx = getBundleContext(FrameworkUtil.getBundle(getClass()));
            final String server = bndCtx.getProperty(WsLocationConstants.LOC_SERVER_NAME);
            final String serverLocation = bndCtx.getProperty(WsLocationConstants.LOC_SERVER_CONFIG_DIR);

            String rarFileName = null;
            Collection<URL> urls = null;
            URL fullPathToRAR = null;
            ServiceReference<?>[] rasRef = null;
            try {
                rasRef = getServiceReferences(bndCtx, "com.ibm.ws.jca.internal.ResourceAdapterService", (String) ref.getProperty("raDDPath"));

                String rasRefId;
                for (int i = 0; i < rasRef.length; i++) {
                    rasRefId = (String) rasRef[i].getProperty("id");
                    if (rasRefId != null && rasRefId.equals(id)) {
                        rarFileName = (String) rasRef[i].getProperty("rarFileName");
                        urls = (Collection<URL>) rasRef[i].getProperty("urls");
                    }
                }

            } catch (InvalidSyntaxException e) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "ResourceAdapterModuleMBean: Failed to obtain the name of the RAR file happened in " + className + methodName, e);
            }

            //Find the right URL presenting the full path to the RA
            if (urls != null && !urls.isEmpty()) {
                for (URL urlItem : urls) {
                    String path = urlItem.getPath();
                    if (path.endsWith(rarFileName)) {
                        fullPathToRAR = urlItem;
                        break;
                    }
                }
                // workaround: If we didn't find an exact match, see if any paths contain the RA id
                if (fullPathToRAR == null) {
                    for (URL urlItem : urls) {
                        String path = urlItem.getPath();
                        if (path.contains(rarFileName)) {
                            fullPathToRAR = urlItem;
                            break;
                        }
                    }
                }
            } else if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "ResourceAdapterModuleMBean: Failed to obtain the full path to the RAR file because the URLs Collection is null or empty."
                             + " Used server: " + serverLocation
                             + " This happened in " + className + methodName);

            //Create & register ResourceAdapterModuleMBean
            ResourceAdapterModuleMBeanImpl ramMBean = resourceAdapterModules.get(name);
            if (ramMBean == null) {
                ramMBean = new ResourceAdapterModuleMBeanImpl(id, name, server, appName, fullPathToRAR);
                ramMBean.register(bndCtx);
                ResourceAdapterModuleMBeanImpl rammbeanTemp = resourceAdapterModules.putIfAbsent(name, ramMBean);
                if (rammbeanTemp != null) {
                    // This mean some other thread beat us to it. However this should not happen because currently only
                    // this synchronized block is doing the put on the ConcurrentHashMap.
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "ResourceAdapterModuleMBean: Unexpected put for the key: " + ramMBean.getName() + " happened in " + className + methodName);
                }
            }

            //Create & register ResourceAdapterMBean
            ResourceAdapterMBeanImpl rambean = new ResourceAdapterMBeanImpl(id, name, server, appName, moduleName, this);
            rambean.register(bndCtx);
            ResourceAdapterMBeanImpl rambeanTemp = resourceAdapters.putIfAbsent(name, rambean);
            if (rambeanTemp != null) {
                // This mean some other thread beat us to it. However this should not happen because currently only
                // this synchronized block is doing the put on the ConcurrentHashMap.
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "ResourceAdapterMBean: Unexpected put for the key: " + rambean.getName() + " happened in " + className + methodName);
            }

            //Update ResourceAdapterModuleMBeanImpl children list to include the new child ResourceAdapterMBeanImpl
            ramMBean.setResourceAdapterChild(rambean.getName(), rambean);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName, "Normal Exit");
    }

    protected synchronized void unsetConnectionFactory(ServiceReference<ResourceFactory> ref) {
        final String methodName = "unsetConnectionFactory";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JCAMBeanRuntime.counterUnsetConnectionFactory);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");
            final String resourceAdapter = (String) ref.getProperty("properties.0.resourceAdapterConfig.id");
            final String jcaResourcName = resourceAdapter + "/JCAResource";

            //Get a reference to the JCAResourceMBeanImpl needed to be unregistered
            JCAResourceMBeanImpl rmbean = jcaResources.get(jcaResourcName);
            //Get a reference to the JCAConnectionFactoryMBeanImpl needed to be unregistered
            JCAConnectionFactoryMBeanImpl cfmbean = jcaConnectionFactorys.get(name);
            //Get a reference to the JCAManagedConnectionFactoryMBeanImpl needed to be unregistered
            JCAManagedConnectionFactoryMBeanImpl mcfmbean = jcaManagedConnectionFactorys.get(name);

            //Remove the parent-child relation
            rmbean.removeConnectionFactoryChild(cfmbean.getName());
            //Update the ConcurrentHashMap
            this.jcaConnectionFactorys.remove(cfmbean.getName());
            //Unregister JCAConnectionFactoryMBean
            cfmbean.unregister();

            //Unregister JCAResourceMBeanImpl only if it has no more children.
            if (rmbean.getConnectionFactoryChildrenCount() == 0) {
                //Update the ConcurrentHashMap
                this.jcaResources.remove(rmbean.getName());
                rmbean.unregister();
            }

            //Update the ConcurrentHashMap
            this.jcaManagedConnectionFactorys.remove(mcfmbean.getName());
            //Unregister JCAManagedConnectionFactoryMBean
            mcfmbean.unregister();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

    protected void unsetResourceAdapter(ServiceReference<ApplicationRecycleContext> ref) {
        final String methodName = "unsetResourceAdapter";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, "Entry Count: " + ++JCAMBeanRuntime.counterUnsetResourceAdapter);

        synchronized (this) {
            //////////////////////Variables from ServiceReference<ResourceFactory> //////////////////////
            final String name = (String) ref.getProperty("config.displayId");

            //Get a reference to the ResourceAdapterMBeanImpl needed to be unregistered
            ResourceAdapterMBeanImpl raMBean = resourceAdapters.get(name);
            //Get a reference to the ResourceAdapterModuleMBeanImpl needed to be unregistered
            ResourceAdapterModuleMBeanImpl ramMBean = resourceAdapterModules.get(name);

            if (raMBean == null || ramMBean == null) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(tc, methodName, "Short Exit. No MBeans were unregistered.");
                return;
            } else {
                //Update the ConcurrentHashMap
                this.resourceAdapters.remove(raMBean.getName());
                //Unregister ResourceAdapterMBean
                raMBean.unregister();

                //Update the ConcurrentHashMap
                this.resourceAdapterModules.remove(ramMBean.getName());
                //Unregister ResourceAdapterModuleMBean
                ramMBean.unregister();
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }

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
