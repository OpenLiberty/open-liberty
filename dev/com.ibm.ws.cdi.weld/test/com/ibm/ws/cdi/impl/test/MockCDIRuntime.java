/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl.test;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.inject.spi.BeanManager;
import javax.interceptor.InvocationContext;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.cdi.internal.interfaces.BeansXmlParser;
import com.ibm.ws.cdi.internal.interfaces.BuildCompatibleExtensionFinder;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIContainer;
import com.ibm.ws.cdi.internal.interfaces.CDIContainerEventManager;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ContextBeginnerEnder;
import com.ibm.ws.cdi.internal.interfaces.EjbEndpointService;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchive;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveFactory;
import com.ibm.ws.cdi.internal.interfaces.ExtensionArchiveProvider;
import com.ibm.ws.cdi.internal.interfaces.TransactionService;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.internal.interfaces.WeldDevelopmentMode;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;

import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

public class MockCDIRuntime implements CDIRuntime {

    @Override
    public BeanManager getCurrentBeanManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeanManager getCurrentModuleBeanManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCurrentApplicationContextID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCurrentModuleCDIEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWeldProxy(Class clazz) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWeldProxy(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<Annotation> getInterceptorBindingsFromInvocationContext(InvocationContext ic) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceRefConfigFactory getResourceRefConfigFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TransactionService getTransactionService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SecurityServices getSecurityServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<ServiceAndServiceReferencePair<WebSphereCDIExtension>> getExtensionServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<ServiceAndServiceReferencePair<CDIExtensionMetadata>> getSPIExtensionServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EjbEndpointService getEjbEndpointService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InjectionEngine getInjectionEngine() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isClientProcess() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExecutorService getExecutorService() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isImplicitBeanArchivesScanningDisabled(CDIArchive archive) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean skipCreatingBda(CDIArchive archive) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ContextBeginnerEnder createContextBeginnerEnder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isContextBeginnerEnderActive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ExtensionArchive getExtensionArchiveForBundle(Bundle bundle, Set<String> extra_classes, Set<String> extraAnnotations, boolean applicationBDAsVisible,
                                                         boolean extClassesOnly, Set<String> extraExtensionClasses) throws CDIException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProxyServices getProxyServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeanManager getClassBeanManager(Class<?> targetClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WebSphereBeanDeploymentArchive getClassBeanDeploymentArchive(Class<?> targetClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MetaDataSlot getApplicationSlot() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeanManager getModuleBeanManager(ModuleMetaData moduleMetaData) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCurrentApplicationCDIEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isApplicationCDIEnabled(ApplicationMetaData applicationMetaData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isModuleCDIEnabled(ModuleMetaData moduleMetaData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public CDIContainer getCDIContainer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WebSphereCDIDeployment getCurrentDeployment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeansXmlParser getBeansXmlParser() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ExtensionArchiveProvider> getExtensionArchiveProviders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ExtensionArchiveFactory> getExtensionArchiveFactories() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BuildCompatibleExtensionFinder getBuildCompatibleExtensionFinder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CDIContainerEventManager getCDIContainerEventManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WeldDevelopmentMode getWeldDevelopmentMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ContextBeginnerEnder cloneActiveContextBeginnerEnder() {
        // TODO Auto-generated method stub
        return null;
    }

}
