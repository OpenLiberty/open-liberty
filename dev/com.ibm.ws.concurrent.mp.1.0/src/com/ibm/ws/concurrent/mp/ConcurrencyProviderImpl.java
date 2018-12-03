/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp;

import java.security.AccessController;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.concurrent.spi.ConcurrencyManager;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyManagerBuilder;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProviderRegistration;
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.context.ApplicationContextProvider;
import com.ibm.ws.concurrent.mp.context.SecurityContextProvider;
import com.ibm.ws.concurrent.mp.context.TransactionContextProvider;
import com.ibm.ws.concurrent.mp.context.WLMContextProvider;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.threading.PolicyExecutorProvider;

/**
 * Registers this implementation as the provider of MicroProfile Concurrency.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ConcurrencyProviderImpl implements ConcurrencyProvider {
    final ApplicationContextProvider applicationContextProvider = new ApplicationContextProvider();
    final SecurityContextProvider securityContextProvider = new SecurityContextProvider();
    final TransactionContextProvider transactionContextProvider = new TransactionContextProvider();
    final WLMContextProvider wlmContextProvider = new WLMContextProvider();

    /**
     * Key for providersPerClassLoader indicating that there is no context class loader on the thread.
     * This is needed because ConcurrentHashMap does not allow null keys.
     */
    private static final String NO_CONTEXT_CLASSLOADER = "NO_CONTEXT_CLASSLOADER";

    private static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    @Reference
    protected MetaDataIdentifierService metadataIdentifierService;

    @Reference
    protected PolicyExecutorProvider policyExecutorProvider;

    private final ConcurrentHashMap<Object, ConcurrencyManager> providersPerClassLoader = new ConcurrentHashMap<Object, ConcurrencyManager>();

    private ConcurrencyProviderRegistration registration;

    @Activate
    protected void activate(ComponentContext osgiComponentContext) {
        applicationContextProvider.classloaderContextProviderRef.activate(osgiComponentContext);
        applicationContextProvider.jeeMetadataContextProviderRef.activate(osgiComponentContext);
        securityContextProvider.securityContextProviderRef.activate(osgiComponentContext);
        securityContextProvider.threadIdentityContextProviderRef.activate(osgiComponentContext);
        transactionContextProvider.transactionContextProviderRef.activate(osgiComponentContext);
        wlmContextProvider.wlmContextProviderRef.activate(osgiComponentContext);
        registration = ConcurrencyProvider.register(this);
    }

    @Deactivate
    protected void deactivate(ComponentContext osgiComponentContext) {
        registration.unregister();
        wlmContextProvider.wlmContextProviderRef.deactivate(osgiComponentContext);
        transactionContextProvider.transactionContextProviderRef.deactivate(osgiComponentContext);
        securityContextProvider.threadIdentityContextProviderRef.deactivate(osgiComponentContext);
        securityContextProvider.securityContextProviderRef.deactivate(osgiComponentContext);
        applicationContextProvider.jeeMetadataContextProviderRef.deactivate(osgiComponentContext);
        applicationContextProvider.classloaderContextProviderRef.deactivate(osgiComponentContext);
    }

    @Override
    @Trivial
    public ConcurrencyManager getConcurrencyManager() {
        return getConcurrencyManager(priv.getContextClassLoader());
    }

    @Override
    public ConcurrencyManager getConcurrencyManager(ClassLoader classLoader) {
        Object key = classLoader == null ? NO_CONTEXT_CLASSLOADER : classLoader;
        ConcurrencyManager ccmgr = providersPerClassLoader.get(key);
        if (ccmgr == null) {
            ConcurrencyManager ccmgrNew = new ConcurrencyManagerImpl(this, classLoader);
            ccmgr = providersPerClassLoader.putIfAbsent(key, ccmgrNew);
            if (ccmgr == null)
                ccmgr = ccmgrNew;
        }
        return ccmgr;
    }

    @Override
    public ConcurrencyManagerBuilder getConcurrencyManagerBuilder() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void registerConcurrencyManager(ConcurrencyManager manager, ClassLoader classLoader) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void releaseConcurrencyManager(ConcurrencyManager manager) {
        // This is inefficient. Does the spec need to require it?
        // The container, which already knows the class loader,
        // can instead directly remove the entry based on the key.
        for (Iterator<Map.Entry<Object, ConcurrencyManager>> entries = providersPerClassLoader.entrySet().iterator(); entries.hasNext();) {
            Map.Entry<Object, ConcurrencyManager> entry = entries.next();
            if (manager.equals(entry.getValue()))
                entries.remove();
        }
    }

    @Reference(service = com.ibm.wsspi.threadcontext.ThreadContextProvider.class,
               target = "(component.name=com.ibm.ws.classloader.context.provider)")
    protected void setClassloaderContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.classloaderContextProviderRef.setReference(ref);
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
        securityContextProvider.threadIdentityContextProviderRef.setReference(ref);
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

    protected void unsetClassloaderContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.classloaderContextProviderRef.unsetReference(ref);
    }

    protected void unsetJeeMetadataContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.jeeMetadataContextProviderRef.unsetReference(ref);
    }

    protected void unsetSecurityContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        securityContextProvider.securityContextProviderRef.unsetReference(ref);
    }

    protected void unsetThreadIdentityContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        securityContextProvider.threadIdentityContextProviderRef.unsetReference(ref);
    }

    protected void unsetTransactionContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        transactionContextProvider.transactionContextProviderRef.unsetReference(ref);
    }

    protected void unsetWLMContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        wlmContextProvider.wlmContextProviderRef.unsetReference(ref);
    }
}