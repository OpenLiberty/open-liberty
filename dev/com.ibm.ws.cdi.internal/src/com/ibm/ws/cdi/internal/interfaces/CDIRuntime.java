/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

/**
 * Holds references to all the services that CDI needs to access.
 */
public interface CDIRuntime extends CDIService {
    /**
     * @return the {@link ResourceRefConfigFactory}
     */
    public ResourceRefConfigFactory getResourceRefConfigFactory();

    /**
     * @return the {@link TransactionService}
     */
    public TransactionService getTransactionService();

    /**
     * @return the {@link SecurityServices}
     */
    public SecurityServices getSecurityServices();

    /**
     * @return an iterator through all the registered {@link WebSphereCDIExtension} services
     */
    public Iterator<ServiceAndServiceReferencePair<WebSphereCDIExtension>> getExtensionServices();

    /**
     * @return an iterator through all the registered {@link CDIExtensionMetadata} services
     */
    public Iterator<ServiceAndServiceReferencePair<CDIExtensionMetadata>> getSPIExtensionServices();

    /**
     * @return the {@link EjbEndpointService}
     */
    public EjbEndpointService getEjbEndpointService();

    /**
     * @return the {@link InjectionEngine}
     */
    public InjectionEngine getInjectionEngine();

    /**
     * Returns whether the application is running in a client application or on a server.
     *
     * @return true if we are running in a client application process, false otherwise
     */
    public boolean isClientProcess();

    /**
     * @return the {@link ScheduledExecutorService}
     */
    public ScheduledExecutorService getScheduledExecutorService();

    /**
     * @return the {@link ExecutorService}
     */
    public ExecutorService getExecutorService();

    /**
     * @return whether this archive is implicitBeanArchive scanning disabled or not
     */
    public boolean isImplicitBeanArchivesScanningDisabled(CDIArchive archive);

    /**
     * @param archive
     * @return
     */
    public boolean skipCreatingBda(CDIArchive archive);

    /**
     * Create an empty ContextBeginnerEnder.
     */
    ContextBeginnerEnder createContextBeginnerEnder();

    /**
     * Returns true if a Context started by a ContextBeginnerEnder is currently active on the current thread.
     *
     * Trying to start a context while one is already active is both unnecessary and will throw an exception
     */
    boolean isContextBeginnerEnderActive();

    /**
     * @param bundle
     * @param extra_classes
     * @param extraAnnotations
     * @param applicationBDAsVisible
     * @param extClassesOnly
     * @return
     * @throws CDIException
     */
    public ExtensionArchive getExtensionArchiveForBundle(Bundle bundle, Set<String> extra_classes, Set<String> extraAnnotations,
                                                         boolean applicationBDAsVisible,
                                                         boolean extClassesOnly,
                                                         Set<String> extraExtensionClasses) throws CDIException;

    /**
     * @return
     */
    public ProxyServices getProxyServices();

    /**
     * @param targetClass
     * @return
     */
    public BeanManager getClassBeanManager(Class<?> targetClass);

    /**
     * @param targetClass
     * @return
     */
    public WebSphereBeanDeploymentArchive getClassBeanDeploymentArchive(Class<?> targetClass);

    /**
     * @return
     */
    public MetaDataSlot getApplicationSlot();

    /**
     * Gets the bean manager for the module to which the given Container belongs.
     *
     * @param moduleMetaData the ModuleMetaData for the module
     * @return the bean manager for the module
     */
    public BeanManager getModuleBeanManager(ModuleMetaData moduleMetaData);

    /**
     * Returns whether CDI is enabled for the current application.
     *
     * @return true if CDI is enabled for the current application, otherwise false
     */
    public boolean isCurrentApplicationCDIEnabled();

    /**
     * Returns whether CDI is enabled for the given application.
     *
     * @param applicationMetaData the ApplicationMetaData for the application
     * @return true if CDI is enabled for the application, otherwise false
     * @throws CDIException if there is a problem finding the application
     */
    public boolean isApplicationCDIEnabled(ApplicationMetaData applicationMetaData);

    /**
     * Returns whether CDI is enabled for the given module.
     *
     * @param moduleMetaData the ModuleMetaData for the module
     * @return true if the module, or any module or libraries it can access, has any CDI Beans
     */
    public boolean isModuleCDIEnabled(ModuleMetaData moduleMetaData);

    /**
     * @return the cdi container
     */
    public CDIContainer getCDIContainer();

    /**
     * @return
     */
    public WebSphereCDIDeployment getCurrentDeployment();

    /**
     * @return a BeansXmlParser instance
     */
    public BeansXmlParser getBeansXmlParser();

    /**
     * @return all registered ExtensionArchiveProviders
     */
    public Collection<ExtensionArchiveProvider> getExtensionArchiveProviders();

    /**
     * @return all registered ExtensionArchiveFactories
     */
    public Collection<ExtensionArchiveFactory> getExtensionArchiveFactories();

    /**
     * @return the BuildCompatibleExtensionFinder, or {@code null} if there is not one
     */
    public BuildCompatibleExtensionFinder getBuildCompatibleExtensionFinder();

    /**
     * @return the CDIContainerEventManager, or {@code null} if there is not one
     */
    public CDIContainerEventManager getCDIContainerEventManager();

    /**
     * creates a ContextBeginnerEnder that will do the same thing as the currently
     * active ContextBeginnerEnder, or returns null if none are active.
     *
     * This is useful if you need to record which thread context classloader and
     * component metadata are currently active so you can re-establish that state
     * after a checkpoint restore.
     *
     * Remember to always close a ContextBeginnerEnder before starting a new one.
     */
    public ContextBeginnerEnder cloneActiveContextBeginnerEnder();

}
