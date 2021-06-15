/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.MetaDataUtils;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.injectionengine.AbstractInjectionEngine;
import com.ibm.ws.injectionengine.osgi.util.OSGiJNDIEnvironmentRefBindingHelper;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appbnd.ApplicationBnd;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionMetaDataListener;
import com.ibm.wsspi.injectionengine.InjectionProcessorContextImpl;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefBindingHelper;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;
import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.injectionengine.factory.EJBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap;
import com.ibm.wsspi.resource.ResourceBindingListener;

@Component(service = {
                       OSGiInjectionEngineImpl.class,
                       InjectionEngine.class,
                       ApplicationMetaDataListener.class,
                       ModuleMetaDataListener.class,
                       ComponentMetaDataListener.class,
                       ApplicationStateListener.class },
           property = { "service.vendor=IBM" })
public class OSGiInjectionEngineImpl extends AbstractInjectionEngine implements InjectionEngine, ApplicationMetaDataListener, ModuleMetaDataListener, ComponentMetaDataListener, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(OSGiInjectionEngineImpl.class);
    private static final TraceComponent tcTWAS = Tr.register(OSGiInjectionEngineImpl.class, InjectionConfigConstants.traceString, InjectionConfigConstants.messageFile);

    private static final String REFERENCE_RESOURCE_BINDING_LISTENERS = "resourceBindingListeners";
    private static final String REFERENCE_RESOURCE_FACTORY_BUILDERS = "resourceFactoryBuilders";
    private static final String REFERENCE_EJB_LINK_REFERENCE_FACTORY = "ejbLinkReferenceFactory";

    private static final Bundle bundle = FrameworkUtil.getBundle(OSGiInjectionEngineImpl.class);
    private static final BundleContext bundleContext = bundle.getBundleContext();

    private static final String OBJECT_FACTORY_NAME = ObjectFactory.class.getName();
    private static final String OBJECT_FACTORY_INFO_NAME = ObjectFactoryInfo.class.getName();
    private static final long OBJECT_FACTORY_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private static final AtomicServiceReference<EJBLinkReferenceFactory> ejbLinkReferenceFactorySRRef = new AtomicServiceReference<EJBLinkReferenceFactory>(REFERENCE_EJB_LINK_REFERENCE_FACTORY);

    private final ConcurrentServiceReferenceSet<ResourceBindingListener> resourceBindingListeners = new ConcurrentServiceReferenceSet<ResourceBindingListener>(REFERENCE_RESOURCE_BINDING_LISTENERS);
    private final ResourceBindingListenerManager resourceBindingListenerManager = new ResourceBindingListenerManager(resourceBindingListeners);

    private final IndirectJndiLookupReferenceFactory indirectJndiLookupReferenceFactory = new IndirectJndiLookupReferenceFactoryImpl(null);
    private final IndirectJndiLookupReferenceFactoryImpl resIndirectJndiLookupReferenceFactory = new IndirectJndiLookupReferenceFactoryImpl(resourceBindingListenerManager);
    private final ResRefReferenceFactoryImpl resRefReferenceFactory = new ResRefReferenceFactoryImpl(resourceBindingListenerManager);
    private final ResAutoLinkReferenceFactory resAutoLinkReferenceFactory = new ResAutoLinkReferenceFactoryImpl(resRefReferenceFactory);
    private final EJBLinkReferenceFactory ejbLinkReferenceFactory = new EJBLinkReferenceFactoryImpl(ejbLinkReferenceFactorySRRef);

    private MetaDataSlot applicationMetaDataSlot;
    private MetaDataSlot moduleMetaDataSlot;
    private MetaDataSlot componentMetaDataSlot;

    private final ConcurrentServiceReferenceSetMap<String, ResourceFactoryBuilder> resourceFactoryBuilders = new ConcurrentServiceReferenceSetMap<String, ResourceFactoryBuilder>(REFERENCE_RESOURCE_FACTORY_BUILDERS);

    ResourceRefConfigFactory resourceRefConfigFactory;
    private ResourceRefConfig defaultResourceRefConfig;

    private final ReentrantReadWriteLock nonCompLock = new ReentrantReadWriteLock();
    private final OSGiInjectionScopeData globalScopeData = new OSGiInjectionScopeData(null, NamingConstants.JavaColonNamespace.GLOBAL, null, nonCompLock);

    private long objectFactoryWaitTime = OBJECT_FACTORY_TIMEOUT;
    private final Map<String, ServiceReference<ObjectFactory>> objectFactoryRefs = new HashMap<String, ServiceReference<ObjectFactory>>();

    public OSGiInjectionEngineImpl() {
        // initialize() must be called before processors can be registered,
        // and DS calls @Reference methods before @Activate methods.
        initialize();
    }

    @Activate
    protected void activate(ComponentContext context) {
        resourceBindingListeners.activate(context);
        resourceFactoryBuilders.activate(context);
        ejbLinkReferenceFactorySRRef.activate(context);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        resourceBindingListeners.deactivate(context);
        resourceFactoryBuilders.deactivate(context);
        ejbLinkReferenceFactorySRRef.deactivate(context);
    }

    /**
     * {@inheritDoc}
     *
     * This method has been overridden in the OSGI Injection Engine to provide
     * the java: bindings to the naming service through the injection helper. <p>
     *
     * Unlike traditional WAS, the bindings have not been bound into a physical
     * name space, instead, the bindings are used similar to EJBContext.lookup.
     */
    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    protected void processInjectionMetaData(final ComponentNameSpaceConfiguration compNSConfig,
                                            List<Class<?>> annotatedClasses) throws InjectionException {
        // OSGI Injection uses the JavaColonCompEnvMap to performing naming lookups,
        // so the map needs to be created even if the caller didn't need it.
        Map<String, InjectionBinding<?>> bindings = compNSConfig.getJavaColonCompEnvMap();
        if (bindings == null) {
            bindings = new LinkedHashMap<String, InjectionBinding<?>>();
            compNSConfig.setJavaColonCompEnvMap(bindings);
        }

        if (annotatedClasses == null && !compNSConfig.isMetaDataComplete()) {
            annotatedClasses = compNSConfig.getInjectionClasses();
        }
        if (annotatedClasses == null) {
            annotatedClasses = Collections.emptyList();
        }

        InjectionProcessorContextImpl context = (InjectionProcessorContextImpl) compNSConfig.getInjectionProcessorContext();
        context.ivSaveNonCompInjectionBindings = true;
        context.ivSavedGlobalInjectionBindings = new HashMap<Class<?>, Map<String, InjectionBinding<?>>>();
        context.ivSavedAppInjectionBindings = new HashMap<Class<?>, Map<String, InjectionBinding<?>>>();
        context.ivSavedModuleInjectionBindings = new HashMap<Class<?>, Map<String, InjectionBinding<?>>>();

        OSGiInjectionScopeData compScopeData = getCompInjectionScopeData(compNSConfig);
        ModuleMetaData mmd = compNSConfig.getModuleMetaData();
        OSGiInjectionScopeData moduleScopeData = mmd == null ? null : getInjectionScopeData(mmd);
        OSGiInjectionScopeData appScopeData = getInjectionScopeData(compNSConfig.getApplicationMetaData());
        OSGiInjectionScopeData contributorScopeData = compScopeData != null ? compScopeData : appScopeData;

        Lock writeLock = compScopeData == null ? null : compScopeData.compLock().writeLock();
        if (writeLock != null) {
            writeLock.lock();
        }
        try {
            // If the scope data already has bindings, then we're dynamically
            // updating an existing java:comp with new annotations.
            if (compScopeData != null && compScopeData.compEnvBindings != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "dynamic update to injection metadata");
                }

                // Set the (read-only) completed bindings.
                context.ivCompletedInjectionBindings = compScopeData.compEnvBindings;
            }

            // Set to the registered factory before the super call; otherwise the
            // super call will override the registered factory with a different
            // one on Client Container, which should only occur on traditional WAS.
            if (compNSConfig.getMBLinkReferenceFactory() == null) {
                compNSConfig.setMBLinkReferenceFactory(ivMBLinkRefFactory);
            }

            if (System.getSecurityManager() == null) {
                super.processInjectionMetaData(compNSConfig, annotatedClasses);
            } else {
                try {
                    final List<Class<?>> privAnnotatedClasses = annotatedClasses;
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws InjectionException {
                            processInjectionMetaDataPrivileged(compNSConfig, privAnnotatedClasses);
                            return null;
                        }
                    });
                } catch (PrivilegedActionException paex) {
                    Throwable cause = paex.getCause();
                    if (cause instanceof InjectionException) {
                        throw (InjectionException) cause;
                    }
                    throw new Error(cause);
                }
            }

            if (!bindings.isEmpty() && compScopeData == null && !isManagedBean(compNSConfig)) {
                // application.xml cannot add java:comp names.
                applicationReferenceNameError(compNSConfig, bindings.keySet());
            }

            // Now that metadata has been processed, we can update the bindings.

            Lock nonCompWriteLock = nonCompLock.writeLock();
            nonCompWriteLock.lock();
            try {
                // Verify that non-java:comp bindings won't conflict.
                if (moduleScopeData != null) {
                    moduleScopeData.validateNonCompBindings(context.ivSavedModuleInjectionBindings);
                } else if (!context.ivSavedModuleInjectionBindings.isEmpty()) {
                    // application.xml cannot add java:module names.
                    List<String> names = new ArrayList<String>();
                    for (Map<String, InjectionBinding<?>> newModuleBindings : context.ivSavedModuleInjectionBindings.values()) {
                        names.addAll(newModuleBindings.keySet());
                    }
                    applicationReferenceNameError(compNSConfig, names);
                }
                appScopeData.validateNonCompBindings(context.ivSavedAppInjectionBindings);
                globalScopeData.validateNonCompBindings(context.ivSavedGlobalInjectionBindings);

                // Finally, "commit" all new bindings.
                if (compScopeData != null) {
                    compScopeData.addCompEnvBindings(bindings);
                }
                if (moduleScopeData != null) {
                    moduleScopeData.addNonCompBindings(context.ivSavedModuleInjectionBindings, contributorScopeData);
                }
                appScopeData.addNonCompBindings(context.ivSavedAppInjectionBindings, contributorScopeData);
                globalScopeData.addNonCompBindings(context.ivSavedGlobalInjectionBindings, contributorScopeData);
            } finally {
                nonCompWriteLock.unlock();
            }
        } finally {
            if (writeLock != null) {
                writeLock.unlock();
            }
        }
    }

    private void processInjectionMetaDataPrivileged(ComponentNameSpaceConfiguration compNSConfig,
                                                    List<Class<?>> annotatedClasses) throws InjectionException {
        super.processInjectionMetaData(compNSConfig, annotatedClasses);
    }

    private void applicationReferenceNameError(ComponentNameSpaceConfiguration compNSConfig, Collection<String> names) throws InjectionConfigurationException {
        String appName = compNSConfig.getJ2EEName().getApplication();
        String anyName = null;
        for (String name : names) {
            anyName = name;
            Tr.error(tc, "APPXML_REF_NAMESPACE_CWNEN1002E", name, appName);
        }
        throw new InjectionConfigurationException(Tr.formatMessage(tc, "APPXML_REF_NAMESPACE_CWNEN1002E", anyName, appName));
    }

    @Trivial
    @Override
    public boolean isEmbeddable() {
        // This is not the embeddable EJB container.
        return false;
    }

    @Trivial
    @Override
    protected IndirectJndiLookupReferenceFactory getDefaultIndirectJndiLookupReferenceFactory() {
        return indirectJndiLookupReferenceFactory;
    }

    @Trivial
    @Override
    protected IndirectJndiLookupReferenceFactory getDefaultResIndirectJndiLookupReferenceFactory() {
        return resIndirectJndiLookupReferenceFactory;
    }

    @Trivial
    @Override
    protected ResRefReferenceFactory getDefaultResRefReferenceFactory() {
        return resRefReferenceFactory;
    }

    @Trivial
    @Override
    protected ResAutoLinkReferenceFactory getDefaultResAutoLinkReferenceFactory() {
        return resAutoLinkReferenceFactory;
    }

    @Trivial
    @Override
    protected EJBLinkReferenceFactory getDefaultEJBLinkReferenceFactory() {
        return ejbLinkReferenceFactory;
    }

    @Trivial
    @Override
    public boolean isValidationLoggable(boolean checkAppConfig) {
        return checkAppConfig;
    }

    @Trivial
    @Override
    public boolean isValidationFailable(boolean checkAppConfig) {
        return checkAppConfig;
    }

    @Override
    public OSGiInjectionScopeData getInjectionScopeData(MetaData metaData) {
        if (metaData == null) {
            return globalScopeData;
        }

        if (metaData instanceof ApplicationMetaData) {
            return getInjectionScopeData((ApplicationMetaData) metaData);
        }

        if (metaData instanceof ModuleMetaData) {
            return getInjectionScopeData((ModuleMetaData) metaData);
        }

        return getInjectionScopeData((ComponentMetaData) metaData);
    }

    private OSGiInjectionScopeData getInjectionScopeData(ApplicationMetaData amd) {
        OSGiInjectionScopeData scopeData = (OSGiInjectionScopeData) amd.getMetaData(applicationMetaDataSlot);
        if (scopeData == null) {
            scopeData = new OSGiInjectionScopeData(amd.getJ2EEName(), NamingConstants.JavaColonNamespace.APP, globalScopeData, nonCompLock);
            amd.setMetaData(applicationMetaDataSlot, scopeData);
        }
        return scopeData;
    }

    private OSGiInjectionScopeData getInjectionScopeData(ModuleMetaData mmd) {
        OSGiInjectionScopeData scopeData = (OSGiInjectionScopeData) mmd.getMetaData(moduleMetaDataSlot);
        if (scopeData == null) {
            OSGiInjectionScopeData parentScopeData = getInjectionScopeData(mmd.getApplicationMetaData());
            scopeData = new OSGiInjectionScopeData(mmd.getJ2EEName(), NamingConstants.JavaColonNamespace.MODULE, parentScopeData, nonCompLock);
            mmd.setMetaData(moduleMetaDataSlot, scopeData);
        }
        return scopeData;
    }

    private OSGiInjectionScopeData getInjectionScopeData(ComponentMetaData cmd) {
        OSGiInjectionScopeData scopeData = (OSGiInjectionScopeData) cmd.getMetaData(componentMetaDataSlot);
        if (scopeData == null) {
            OSGiInjectionScopeData parentScopeData = getInjectionScopeData(cmd.getModuleMetaData());
            scopeData = new OSGiInjectionScopeData(cmd.getJ2EEName(), NamingConstants.JavaColonNamespace.COMP, parentScopeData, null);
            cmd.setMetaData(componentMetaDataSlot, scopeData);
        }
        return scopeData;
    }

    /**
     * Returns true if the configuration is for a managed bean.
     */
    private boolean isManagedBean(ComponentNameSpaceConfiguration compNSConfig) {
        return compNSConfig.getOwningFlow() == ComponentNameSpaceConfiguration.ReferenceFlowKind.MANAGED_BEAN;
    }

    /**
     * Gets the java:comp scope data for a configuration, or null if the
     * configuration represents application.xml data or a managed bean.
     */
    private OSGiInjectionScopeData getCompInjectionScopeData(ComponentNameSpaceConfiguration compNSConfig) {
        if (isManagedBean(compNSConfig)) {
            return null;
        }

        ComponentMetaData cmd = compNSConfig.getComponentMetaData();
        if (cmd == null) {
            ModuleMetaData mmd = compNSConfig.getModuleMetaData();
            if (mmd == null) {
                return null;
            }
            return getInjectionScopeData(mmd);
        }

        return getInjectionScopeData(cmd);
    }

    private void destroyInjectionScopeData(MetaData metaData, MetaDataSlot slot) {
        OSGiInjectionScopeData scopeData = (OSGiInjectionScopeData) metaData.getMetaData(slot);
        if (scopeData != null) {
            Lock lock = scopeData.compLock().writeLock();
            lock.lock();
            try {
                Lock nonCompWriteLock = nonCompLock.writeLock();
                nonCompWriteLock.lock();
                try {
                    scopeData.destroy();
                } finally {
                    nonCompWriteLock.unlock();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Gets the injection scope data for a namespace.
     *
     * @param cmd       the component metadata, or null if null should be returned
     * @param namespace the namespace
     * @return the scope data, or null if unavailable
     */
    public OSGiInjectionScopeData getInjectionScopeData(ComponentMetaData cmd, NamingConstants.JavaColonNamespace namespace) {
        if (cmd == null) {
            return null;
        }

        if (namespace == NamingConstants.JavaColonNamespace.GLOBAL) {
            return globalScopeData;
        }

        if (namespace == NamingConstants.JavaColonNamespace.COMP || namespace == NamingConstants.JavaColonNamespace.COMP_ENV) {
            OSGiInjectionScopeData isd = (OSGiInjectionScopeData) cmd.getMetaData(componentMetaDataSlot);

            if (isd == null) {
                ModuleMetaData mmd = cmd.getModuleMetaData();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "trying module " + mmd);
                }

                // WAR java:comp is shared across all components, so the
                // bindings are stored in the module metadata.
                isd = (OSGiInjectionScopeData) cmd.getModuleMetaData().getMetaData(moduleMetaDataSlot);
                if (isd == null || !isd.isCompAllowed()) {
                    return null;
                }
            }

            return isd;
        }

        ModuleMetaData mmd = cmd.getModuleMetaData();
        if (namespace == NamingConstants.JavaColonNamespace.MODULE) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "trying " + mmd);
            }
            return (OSGiInjectionScopeData) mmd.getMetaData(moduleMetaDataSlot);
        }

        if (namespace == NamingConstants.JavaColonNamespace.APP) {
            ApplicationMetaData amd = mmd.getApplicationMetaData();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "trying " + amd);
            }
            return (OSGiInjectionScopeData) amd.getMetaData(applicationMetaDataSlot);
        }

        return null;
    }

    @Override
    public void injectClient(ComponentNameSpaceConfiguration compNSConfig) throws InjectionException {
        // We do not support a client container.
        throw new UnsupportedOperationException();
    }

    @Override
    protected void processClientInjections(ComponentNameSpaceConfiguration compNSConfig, InjectionProcessorContextImpl processorContext) throws InjectionConfigurationException {
        // We do not support a client container.
        throw new UnsupportedOperationException();
    }

    @Override
    public Reference createDefinitionReference(ComponentNameSpaceConfiguration nameSpaceConfig,
                                               InjectionScope scope,
                                               String refName,
                                               String bindingName,
                                               String type,
                                               @Sensitive Map<String, Object> properties) throws Exception {
        Reference listenerReference = createDefinitionResourceBindingListenerReference(refName, bindingName, type, properties);
        if (listenerReference != null) {
            return listenerReference;
        }

        if (bindingName != null) {
            return new IndirectReference(refName, bindingName, type, null, null, false);
        }

        return createDefinitionResourceFactoryReference(nameSpaceConfig, scope, refName, type, properties);
    }

    /**
     * Attempt to create an indirect reference for a resource definition from a
     * resource binding provider.
     *
     * @return an indirect reference object based on a binding provider, or null
     */
    private Reference createDefinitionResourceBindingListenerReference(String refName,
                                                                       String bindingName,
                                                                       String type,
                                                                       @Sensitive Map<String, Object> properties) {
        Boolean transactional = (Boolean) properties.get("transactional");
        Map<String, Object> bindingProperties = Collections.<String, Object> singletonMap("transactional", transactional == null || transactional);

        ResourceBindingImpl binding = resourceBindingListenerManager.binding(refName, bindingName, type, bindingProperties);
        if (binding != null) {
            return new IndirectReference(refName, binding.getBindingName(), type, null, binding.getBindingListenerName(), false);
        }

        return null;
    }

    @Override
    public void registerResourceFactoryBuilder(String type, ResourceFactoryBuilder builder) {
        throw new UnsupportedOperationException("ResourceFactoryBuilder implementations should be registered in the service registry with the ResourceFactory.CREATES_OBJECT_CLASS property.");
    }

    @Override
    public ResourceFactoryBuilder unregisterResourceFactoryBuilder(String type) {
        throw new UnsupportedOperationException("ResourceFactoryBuilder implementations should be registered in the service registry with the ResourceFactory.CREATES_OBJECT_CLASS property.");
    }

    @Override
    public ResourceFactoryBuilder getResourceFactoryBuilder(String type) throws InjectionException {
        Iterator<ResourceFactoryBuilder> builderIter = resourceFactoryBuilders.getServices(type);
        ResourceFactoryBuilder builder = builderIter.hasNext() ? builderIter.next() : null;
        if (builder == null) {
            throw new InjectionException(type + " definitions are not supported in this server configuration");
        }
        return builder;
    }

    /**
     * Create a reference for a resource definition from a ResourceFactory.
     *
     * @return a ResourceFactory reference object
     */
    private Reference createDefinitionResourceFactoryReference(ComponentNameSpaceConfiguration compNSConfig,
                                                               InjectionScope scope,
                                                               String refName,
                                                               String type,
                                                               @Sensitive Map<String, Object> properties) throws Exception {

        ResourceFactoryBuilder builder = getResourceFactoryBuilder(type);

        J2EEName j2eeName = compNSConfig.getJ2EEName();

        // d660700 - Based on the scope, determine the metadata for recording the
        // resulting reference, and add additional keys to the properties list.
        String appName, moduleName, compName;
        MetaData metaData;
        if (scope == InjectionScope.GLOBAL) {
            metaData = null;
            appName = moduleName = compName = null;
        } else {
            appName = j2eeName.getApplication();

            if (scope == InjectionScope.APP) {
                metaData = compNSConfig.getApplicationMetaData(); // F743-31682
                moduleName = compName = null;
            } else {
                moduleName = j2eeName.getModule();

                if (scope == InjectionScope.MODULE) {
                    metaData = compNSConfig.getModuleMetaData();
                    compName = null;
                } else {
                    compName = j2eeName.getComponent();

                    metaData = compNSConfig.getComponentMetaData();
                    if (metaData == null) {
                        // All components in a web module share the same namespace.
                        metaData = compNSConfig.getModuleMetaData();
                    }
                }
            }
        }

        if (appName == null) {
            properties.remove("application");
        } else {
            properties.put("application", appName);
        }

        if (moduleName == null) {
            properties.remove("module");
        } else {
            properties.put("module", moduleName);
        }

        if (compName == null) {
            properties.remove("component");
        } else {
            properties.put("component", compName);
        }

        properties.put("declaringApplication", j2eeName.getApplication());
        properties.put("jndiName", InjectionScope.denormalize(refName));

        ResourceFactory resourceFactory = builder.createResourceFactory(properties);
        Reference ref = new ResourceFactoryReference(type, resourceFactory, properties);

        // Save the reference in the relevant scope data so that it can be
        // destroyed when the scope is destroyed.
        getInjectionScopeData(metaData).addDefinitionReference(refName, ref);

        return ref;
    }

    @Override
    public void destroyDefinitionReference(Reference ref) throws Exception {
        // Defensive programming for FindBugs: instanceof should always succeed.
        if (ref instanceof ResourceFactoryReference) {
            ResourceFactoryReference resourceFactoryReference = (ResourceFactoryReference) ref;
            resourceFactoryReference.getResourceFactory().destroy();
        }
    }

    @Override
    public void bindJavaNameSpaceObject(ComponentNameSpaceConfiguration compNSConfig,
                                        InjectionScope scope,
                                        String name,
                                        InjectionBinding<?> binding,
                                        Object bindingObject) throws InjectionException {
        // NOTE: For this API, scope == null means java:comp/env, and COMP means
        // any other java:comp binding (e.g., java:comp/myref).
        if (scope == InjectionScope.COMP && binding != null) {
            // java:comp/env entries are handled via getJavaColonCompEnvMap,
            // and other scopes are handled via ivSaveNonCompInjectionBindings.
            // For non-env java:comp, we use this callback.
            OSGiInjectionScopeData scopeData = getCompInjectionScopeData(compNSConfig);
            if (scopeData == null) {
                if (isManagedBean(compNSConfig)) {
                    // Managed beans don't have a true component namespace.
                    return;
                }

                // application.xml cannot add java:comp names.
                applicationReferenceNameError(compNSConfig, Arrays.asList(name));
            }
            scopeData.addCompBinding(name, binding);
        }
    }

    @Override
    public Context createComponentNameSpaceContext(Object componentNameSpace) throws NamingException {
        // Nothing.  We do not support an explicit component namespace.
        return null;
    }

    @Override
    public Object createJavaNameSpace(String logicalAppName, String moduleName, String logicalModuleName, String componentName) throws NamingException {
        // Nothing.  We do not support explicit namespaces.
        return null;
    }

    @Override
    public ResourceRefConfigList createResourceRefConfigList() {
        return resourceRefConfigFactory.createResourceRefConfigList();
    }

    @Override
    public synchronized ResourceRefConfig getDefaultResourceRefConfig() {
        if (defaultResourceRefConfig == null) {
            defaultResourceRefConfig = resourceRefConfigFactory.createResourceRefConfig("default");
        }
        return defaultResourceRefConfig;
    }

    @Override
    public Object getInjectableObject(InjectionBinding<?> binding, Object targetObject, InjectionTargetContext targetContext) throws InjectionException {
        return binding.getInjectionObject(targetObject, targetContext);
    }

    @Override
    public ReferenceContext createReferenceContext(MetaData md) {
        OSGiInjectionScopeData scopeData = getInjectionScopeData(md);
        return new OSGiReferenceContextImpl(this, scopeData);
    }

    @Override
    public ReferenceContext getCommonReferenceContext(ModuleMetaData mmd) {
        OSGiInjectionScopeData scopeData = getInjectionScopeData(mmd);
        ReferenceContext rc = scopeData.ivReferenceContext;
        if (rc == null) {
            rc = new OSGiReferenceContextImpl(this, scopeData);
            scopeData.ivReferenceContext = rc;
        }

        return rc;
    }

    @Override
    public ObjectFactory getObjectFactory(final String objectFactoryClassName, Class<? extends ObjectFactory> objectFactoryClass) throws InjectionException {
        // Look for the object factory in the service registry ala JNDI.
        ObjectFactory objectFactory;
        try {
            objectFactory = AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectFactory>() {
                @Override
                public ObjectFactory run() throws InjectionException {
                    return getOSGiObjectFactory(objectFactoryClassName, objectFactoryClass);

                }
            });
        } catch (PrivilegedActionException paex) {
            Throwable cause = paex.getCause();
            if (cause instanceof InjectionException) {
                throw (InjectionException) cause;
            }
            throw new Error(cause);
        }

        if (objectFactory != null) {
            return objectFactory;
        }

        // Otherwise, if we already have the factory class (ObjectFactoryInfo),
        // then we can create it directly.
        if (objectFactoryClass != null) {
            return super.getObjectFactory(objectFactoryClassName, objectFactoryClass);
        }

        Tr.error(tcTWAS, "OBJECT_FACTORY_CLASS_FAILED_TO_LOAD_CWNEN0024E", objectFactoryClassName);
        throw new InjectionException("The injection engine failed to load the " + objectFactoryClassName + " ObjectFactory class.");
    }

    @FFDCIgnore({ InterruptedException.class })
    private ObjectFactory getOSGiObjectFactory(String objectFactoryClassName, Class<? extends ObjectFactory> objectFactoryClass) throws InjectionException {
        if (objectFactoryClassName == null) {
            // This is an internal error indicating that an InjectionBinding was
            // resolved without properly setting an injection or binding object.
            // Detect it to avoid getAllServiceReferences(null, ...), which
            // would select an arbitrary service that provides ObjectFactory.
            throw new IllegalStateException();
        }

        // Look for the requested ObjectFactory ServiceReference in the map of registered ObjectFactory
        // references. If not found and the ObjectFactory class is not available, then wait up to
        // 10 seconds for the ObjectFactory to be registered. If still not found, then disable waiting
        // and allow the injection/lookup to fail without any further delays.
        ServiceReference<ObjectFactory> ref = null;
        synchronized (objectFactoryRefs) {
            ref = objectFactoryRefs.get(objectFactoryClassName);
            if (ref == null && objectFactoryClass == null) {
                long waitTime = objectFactoryWaitTime;
                long startTime = System.currentTimeMillis();
                while (ref == null && waitTime > 0) {
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Waiting up to " + waitTime + " for " + objectFactoryClassName);
                        objectFactoryRefs.wait(waitTime);
                    } catch (InterruptedException e) {
                        // ignore; continue
                    }
                    waitTime = OBJECT_FACTORY_TIMEOUT - (System.currentTimeMillis() - startTime);
                    ref = objectFactoryRefs.get(objectFactoryClassName);
                }
                if (ref == null) {
                    objectFactoryWaitTime = 0;
                }
            }
        }

        if (ref != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Obtaining service from ref : " + ref);
            return bundleContext.getService(ref);
        }

        return null;
    }

    @Override
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) {
        OSGiInjectionScopeData scopeData = getInjectionScopeData(event.getMetaData());
        scopeData.enableDeferredReferenceData();
    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        destroyInjectionScopeData(event.getMetaData(), applicationMetaDataSlot);
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {
        if (!MetaDataUtils.copyModuleMetaDataSlot(event, moduleMetaDataSlot)) {
            OSGiInjectionScopeData scopeData = getInjectionScopeData(event.getMetaData());
            scopeData.enableDeferredReferenceData();
        }
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        destroyInjectionScopeData(event.getMetaData(), moduleMetaDataSlot);
    }

    @Override
    public void componentMetaDataCreated(MetaDataEvent<ComponentMetaData> event) {
    }

    @Override
    public void componentMetaDataDestroyed(MetaDataEvent<ComponentMetaData> event) {
        destroyInjectionScopeData(event.getMetaData(), componentMetaDataSlot);
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        if (appInfo instanceof EARApplicationInfo) {
            EARApplicationInfo earAppInfo = (EARApplicationInfo) appInfo;

            Application app;
            try {
                app = appInfo.getContainer().adapt(Application.class);
            } catch (UnableToAdaptException e) {
                throw new StateChangeException(e);
            }

            if (app != null) {
                processApplicationReferences(earAppInfo, app);
            }
        }
    }

    /**
     * Process references declared in application.xml.
     */
    private void processApplicationReferences(EARApplicationInfo appInfo, Application app) throws StateChangeException {
        Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs = new EnumMap<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>>(JNDIEnvironmentRefType.class);
        boolean anyRefs = false;
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            List<? extends JNDIEnvironmentRef> refs = refType.getRefs(app);
            allRefs.put(refType, refs);
            anyRefs |= !refs.isEmpty();
        }

        if (anyRefs) {
            ApplicationBnd appBnd;
            try {
                appBnd = appInfo.getContainer().adapt(ApplicationBnd.class);
            } catch (UnableToAdaptException e) {
                throw new StateChangeException(e);
            }

            String compNSConfigName = appInfo.getName() + " META-INF/application.xml";
            ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(compNSConfigName, ((ExtendedApplicationInfo) appInfo).getMetaData().getJ2EEName());
            compNSConfig.setClassLoader(appInfo.getApplicationClassLoader());
            compNSConfig.setApplicationMetaData(((ExtendedApplicationInfo) appInfo).getMetaData());

            JNDIEnvironmentRefType.setAllRefs(compNSConfig, allRefs);

            if (appBnd != null) {
                Map<JNDIEnvironmentRefType, Map<String, String>> allBindings = JNDIEnvironmentRefBindingHelper.createAllBindingsMap();
                Map<String, String> envEntryValues = new HashMap<String, String>();
                ResourceRefConfigList resourceRefConfigList = resourceRefConfigFactory.createResourceRefConfigList();
                OSGiJNDIEnvironmentRefBindingHelper.processBndAndExt(allBindings, envEntryValues, resourceRefConfigList, appBnd, null);
                JNDIEnvironmentRefBindingHelper.setAllBndAndExt(compNSConfig, allBindings, envEntryValues, resourceRefConfigList);
            }

            try {
                processInjectionMetaData(null, compNSConfig);
            } catch (InjectionException e) {
                throw new StateChangeException(e);
            }
        }
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
    }

    @org.osgi.service.component.annotations.Reference(name = REFERENCE_RESOURCE_BINDING_LISTENERS,
                                                      service = ResourceBindingListener.class,
                                                      cardinality = ReferenceCardinality.MULTIPLE,
                                                      policy = ReferencePolicy.DYNAMIC)
    protected void setResourceBindingListener(ServiceReference<ResourceBindingListener> reference) {
        resourceBindingListeners.addReference(reference);
    }

    protected void unsetResourceBindingListener(ServiceReference<ResourceBindingListener> reference) {
        resourceBindingListeners.removeReference(reference);
    }

    private String[] getResourceFactoryTypes(ServiceReference<ResourceFactoryBuilder> reference) {
        Object createsObjectClass = reference.getProperty(ResourceFactory.CREATES_OBJECT_CLASS);
        if (createsObjectClass instanceof String[]) {
            return (String[]) createsObjectClass;
        }
        if (createsObjectClass instanceof String) {
            return new String[] { (String) createsObjectClass };
        }
        return new String[0];
    }

    @org.osgi.service.component.annotations.Reference(name = REFERENCE_RESOURCE_FACTORY_BUILDERS,
                                                      service = ResourceFactoryBuilder.class,
                                                      cardinality = ReferenceCardinality.MULTIPLE,
                                                      policy = ReferencePolicy.DYNAMIC)
    protected void setResourceFactoryBuilder(ServiceReference<ResourceFactoryBuilder> reference) {
        for (String type : getResourceFactoryTypes(reference)) {
            resourceFactoryBuilders.putReference(type, reference);
        }
    }

    protected void unsetResourceFactoryBuilder(ServiceReference<ResourceFactoryBuilder> reference) {
        for (String type : getResourceFactoryTypes(reference)) {
            resourceFactoryBuilders.removeReference(type, reference);
        }
    }

    @org.osgi.service.component.annotations.Reference
    protected void setMetaDataSlotService(MetaDataSlotService slotService) {
        applicationMetaDataSlot = slotService.reserveMetaDataSlot(ApplicationMetaData.class);
        moduleMetaDataSlot = slotService.reserveMetaDataSlot(ModuleMetaData.class);
        componentMetaDataSlot = slotService.reserveMetaDataSlot(ComponentMetaData.class);
    }

    protected void unsetMetaDataSlotService(MetaDataSlotService slotService) {
    }

    @org.osgi.service.component.annotations.Reference
    protected void setResourceRefConfigFactory(ResourceRefConfigFactory resourceRefConfigFactory) {
        this.resourceRefConfigFactory = resourceRefConfigFactory;
    }

    protected void unsetResourceRefConfigFactory(ResourceRefConfigFactory resourceRefConfigFactory) {
    }

    @org.osgi.service.component.annotations.Reference(name = REFERENCE_EJB_LINK_REFERENCE_FACTORY,
                                                      service = EJBLinkReferenceFactory.class,
                                                      cardinality = ReferenceCardinality.OPTIONAL,
                                                      policy = ReferencePolicy.DYNAMIC)
    protected void setEJBLinkReferenceFactory(ServiceReference<EJBLinkReferenceFactory> ejbLinkReferenceFactory) {
        ejbLinkReferenceFactorySRRef.setReference(ejbLinkReferenceFactory);
    }

    protected void unsetEJBLinkReferenceFactory(ServiceReference<EJBLinkReferenceFactory> ref) {
        ejbLinkReferenceFactorySRRef.unsetReference(ref);
    }

    @org.osgi.service.component.annotations.Reference(cardinality = ReferenceCardinality.MULTIPLE,
                                                      policy = ReferencePolicy.DYNAMIC)
    @Override
    public void registerObjectFactoryInfo(ObjectFactoryInfo info) throws InjectionException {
        super.registerObjectFactoryInfo(info);
    }

    @Override
    public void unregisterObjectFactoryInfo(ObjectFactoryInfo info) throws InjectionException {
        super.unregisterObjectFactoryInfo(info);
    }

    @org.osgi.service.component.annotations.Reference(cardinality = ReferenceCardinality.MULTIPLE,
                                                      policy = ReferencePolicy.DYNAMIC)
    public void registerObjectFactory(ServiceReference<ObjectFactory> ref) throws InjectionException {
        String[] objectFactoryClassNames = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        synchronized (objectFactoryRefs) {
            for (String objectFactoryClassName : objectFactoryClassNames) {
                if (!OBJECT_FACTORY_NAME.equals(objectFactoryClassName) && !OBJECT_FACTORY_INFO_NAME.equals(objectFactoryClassName)) {
                    objectFactoryRefs.put(objectFactoryClassName, ref);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Added ObjectFactory : " + objectFactoryClassName + ", " + ref);
                    objectFactoryRefs.notifyAll();
                }
            }
        }
    }

    public void unregisterObjectFactory(ServiceReference<ObjectFactory> ref) throws InjectionException {
        synchronized (objectFactoryRefs) {
            for (Iterator<Entry<String, ServiceReference<ObjectFactory>>> iterator = objectFactoryRefs.entrySet().iterator(); iterator.hasNext();) {
                Entry<String, ServiceReference<ObjectFactory>> entry = iterator.next();
                if (ref.equals(entry.getValue())) {
                    iterator.remove();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Removed ObjectFactory : " + entry.getKey() + ", " + ref);
                }
            }
        }
    }

    @org.osgi.service.component.annotations.Reference(cardinality = ReferenceCardinality.MULTIPLE,
                                                      policy = ReferencePolicy.DYNAMIC)
    @Override
    public void registerInjectionProcessorProvider(InjectionProcessorProvider<?, ?> provider) throws InjectionException {
        super.registerInjectionProcessorProvider(provider);
    }

    @Override
    public void unregisterInjectionProcessorProvider(InjectionProcessorProvider<?, ?> provider) throws InjectionException {
        super.unregisterInjectionProcessorProvider(provider);
    }

    @org.osgi.service.component.annotations.Reference(cardinality = ReferenceCardinality.MULTIPLE,
                                                      policy = ReferencePolicy.DYNAMIC)
    @Override
    public void registerInjectionMetaDataListener(InjectionMetaDataListener listener) {
        super.registerInjectionMetaDataListener(listener);
    }

    @Override
    public void unregisterInjectionMetaDataListener(InjectionMetaDataListener listener) {
        super.unregisterInjectionMetaDataListener(listener);
    }
}
