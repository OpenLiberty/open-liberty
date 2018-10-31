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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.context.ApplicationContextProvider;
import com.ibm.ws.kernel.service.util.SecureAction;

/**
 * Registers this implementation as the provider of MicroProfile Concurrency.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ConcurrencyProviderImpl implements ConcurrencyProvider {
    final ApplicationContextProvider applicationContextProvider = new ApplicationContextProvider();

    private static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private final ConcurrentHashMap<ClassLoader, ConcurrencyManager> providersPerClassLoader = new ConcurrentHashMap<ClassLoader, ConcurrencyManager>();

    private ConcurrencyProviderRegistration registration;

    @Activate
    protected void activate(ComponentContext osgiComponentContext) {
        applicationContextProvider.classloaderContextProviderRef.activate(osgiComponentContext);
        applicationContextProvider.jeeMetadataContextProviderRef.activate(osgiComponentContext);
        registration = ConcurrencyProvider.register(this);
    }

    @Deactivate
    protected void deactivate(ComponentContext osgiComponentContext) {
        registration.unregister();
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
        ConcurrencyManager ccmgr = providersPerClassLoader.get(classLoader);
        if (ccmgr == null) {
            ConcurrencyManager ccmgrNew = new ConcurrencyManagerImpl(this, classLoader);
            ccmgr = providersPerClassLoader.putIfAbsent(classLoader, ccmgrNew);
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
        for (Iterator<Map.Entry<ClassLoader, ConcurrencyManager>> entries = providersPerClassLoader.entrySet().iterator(); entries.hasNext();) {
            Map.Entry<ClassLoader, ConcurrencyManager> entry = entries.next();
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

    protected void unsetClassloaderContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.classloaderContextProviderRef.unsetReference(ref);
    }

    protected void unsetJeeMetadataContextProvider(ServiceReference<com.ibm.wsspi.threadcontext.ThreadContextProvider> ref) {
        applicationContextProvider.jeeMetadataContextProviderRef.unsetReference(ref);
    }
}