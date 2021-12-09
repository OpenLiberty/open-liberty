/*******************************************************************************
 * Copyright (c) 2018,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.contextpropagation;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ContextManagerProviderRegistration;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.microprofile.context.ApplicationContextProvider;
import com.ibm.ws.microprofile.context.CDIContextProviderHolder;
import com.ibm.ws.microprofile.context.EmptyHandleListContextProvider;
import com.ibm.ws.microprofile.context.SecurityContextProvider;
import com.ibm.ws.microprofile.context.ThreadIdentityContextProvider;
import com.ibm.ws.microprofile.context.TransactionContextProvider;
import com.ibm.ws.microprofile.context.WLMContextProvider;
import com.ibm.ws.threading.PolicyExecutorProvider;

/**
 * Registers this implementation as the provider of MicroProfile Context Propagation.
 */
@SuppressWarnings("deprecation")
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ContextManagerProviderImpl implements ApplicationStateListener, ContextManagerProvider {
    private static final TraceComponent tc = Tr.register(ContextManagerProviderImpl.class);

    final ApplicationContextProvider applicationContextProvider = new ApplicationContextProvider();
    final CDIContextProviderHolder cdiContextProvider = new CDIContextProviderHolder();
    final EmptyHandleListContextProvider emptyHandleListContextProvider = new EmptyHandleListContextProvider();
    final SecurityContextProvider securityContextProvider = new SecurityContextProvider();
    final TransactionContextProvider transactionContextProvider = new TransactionContextProvider();
    final WLMContextProvider wlmContextProvider = new WLMContextProvider();
    final ThreadIdentityContextProvider threadIdendityContextProvider = new ThreadIdentityContextProvider();

    /**
     * Key for providersPerClassLoader indicating that there is no context class loader on the thread.
     * This is needed because ConcurrentHashMap does not allow null keys.
     */
    private static final String NO_CONTEXT_CLASSLOADER = "NO_CONTEXT_CLASSLOADER";

    /**
     * Jakarta EE version if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    volatile int eeVersion;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private ServiceReference<JavaEEVersion> eeVersionRef;

    @Reference
    protected MetaDataIdentifierService metadataIdentifierService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected volatile MPConfigAccessor mpConfigAccessor;

    @Reference
    protected PolicyExecutorProvider policyExecutorProvider;

    private final ConcurrentHashMap<Object, ContextManagerImpl> providersPerClassLoader = new ConcurrentHashMap<Object, ContextManagerImpl>();

    private ContextManagerProviderRegistration registration;

    @Activate
    protected void activate(ComponentContext osgiComponentContext) {
        applicationContextProvider.classloaderContextProviderRef.activate(osgiComponentContext);
        applicationContextProvider.jeeMetadataContextProviderRef.activate(osgiComponentContext);
        cdiContextProvider.cdiContextProviderRef.activate(osgiComponentContext);
        emptyHandleListContextProvider.emptyHandleListContextProviderRef.activate(osgiComponentContext);
        securityContextProvider.securityContextProviderRef.activate(osgiComponentContext);
        threadIdendityContextProvider.threadIdentityContextProviderRef.activate(osgiComponentContext);
        transactionContextProvider.transactionContextProviderRef.activate(osgiComponentContext);
        wlmContextProvider.wlmContextProviderRef.activate(osgiComponentContext);
        registration = ContextManagerProvider.register(this);
    }

    @Override
    @Trivial
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
    }

    @Override
    @Trivial
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getName();
        for (Iterator<Map.Entry<Object, ContextManagerImpl>> it = providersPerClassLoader.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Object, ContextManagerImpl> entry = it.next();
            Object cl = entry.getKey();
            ContextManagerImpl cm = entry.getValue();
            if (!NO_CONTEXT_CLASSLOADER.equals(cl) && appName.equals(cm.appName)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "remove", cl, cm);
                it.remove();
            }
        }
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        policyExecutorProvider.shutdownNow(appInfo.getName());
    }

    @Deactivate
    protected void deactivate(ComponentContext osgiComponentContext) {
        registration.unregister();
        wlmContextProvider.wlmContextProviderRef.deactivate(osgiComponentContext);
        transactionContextProvider.transactionContextProviderRef.deactivate(osgiComponentContext);
        threadIdendityContextProvider.threadIdentityContextProviderRef.deactivate(osgiComponentContext);
        securityContextProvider.securityContextProviderRef.deactivate(osgiComponentContext);
        emptyHandleListContextProvider.emptyHandleListContextProviderRef.deactivate(osgiComponentContext);
        cdiContextProvider.cdiContextProviderRef.deactivate(osgiComponentContext);
        applicationContextProvider.jeeMetadataContextProviderRef.deactivate(osgiComponentContext);
        applicationContextProvider.classloaderContextProviderRef.deactivate(osgiComponentContext);
    }

    @Override
    public ContextManager getContextManager(ClassLoader classLoader) {
        Object key = classLoader == null ? NO_CONTEXT_CLASSLOADER : classLoader;
        ContextManagerImpl cmgr = providersPerClassLoader.get(key);
        if (cmgr == null) {
            ContextManagerImpl cmgrNew = new ContextManagerImpl(this, classLoader);
            cmgr = providersPerClassLoader.putIfAbsent(key, cmgrNew);
            if (cmgr == null)
                cmgr = cmgrNew;
        }
        return cmgr;
    }

    @Reference(service = org.eclipse.microprofile.context.spi.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.cdi.context.provider)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setCDIContextProvider(ServiceReference<org.eclipse.microprofile.context.spi.ThreadContextProvider> ref) {
        cdiContextProvider.cdiContextProviderRef.setReference(ref);
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.classloader.context.provider)")
    protected void setClassloaderContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.classloaderContextProviderRef.setReference(ref);
    }

    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
        eeVersionRef = ref;
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=io.openliberty.handlelist.context.provider)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEmptyHandleListContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        emptyHandleListContextProvider.emptyHandleListContextProviderRef.setReference(ref);
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.javaee.metadata.context.provider)")
    protected void setJeeMetadataContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.jeeMetadataContextProviderRef.setReference(ref);
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.security.context.provider)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSecurityContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        securityContextProvider.securityContextProviderRef.setReference(ref);
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.security.thread.zos.context.provider)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setThreadIdentityContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        threadIdendityContextProvider.threadIdentityContextProviderRef.setReference(ref);
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.transaction.context.provider)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setTransactionContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        transactionContextProvider.transactionContextProviderRef.setReference(ref);
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.zos.wlm.context.provider)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setWLMContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        wlmContextProvider.wlmContextProviderRef.setReference(ref);
    }

    protected void unsetCDIContextProvider(ServiceReference<org.eclipse.microprofile.context.spi.ThreadContextProvider> ref) {
        cdiContextProvider.cdiContextProviderRef.unsetReference(ref);
    }

    protected void unsetClassloaderContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.classloaderContextProviderRef.unsetReference(ref);
    }

    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            eeVersion = 0;
        }
    }

    protected void unsetEmptyHandleListContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        emptyHandleListContextProvider.emptyHandleListContextProviderRef.unsetReference(ref);
    }

    protected void unsetJeeMetadataContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.jeeMetadataContextProviderRef.unsetReference(ref);
    }

    protected void unsetSecurityContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        securityContextProvider.securityContextProviderRef.unsetReference(ref);
    }

    protected void unsetThreadIdentityContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        threadIdendityContextProvider.threadIdentityContextProviderRef.unsetReference(ref);
    }

    protected void unsetTransactionContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        transactionContextProvider.transactionContextProviderRef.unsetReference(ref);
    }

    protected void unsetWLMContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        wlmContextProvider.wlmContextProviderRef.unsetReference(ref);
    }
}