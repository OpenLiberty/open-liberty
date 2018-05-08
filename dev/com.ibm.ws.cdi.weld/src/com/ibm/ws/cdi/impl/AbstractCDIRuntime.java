/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * This abstract class generally holds methods which simply pass through to the CDIContainerImpl
 */
public abstract class AbstractCDIRuntime implements CDIService, CDIRuntime, CDIProvider {

    private CDIContainerImpl cdiContainer;

    protected void start() {
        this.cdiContainer = new CDIContainerImpl(this);

        CDI.setCDIProvider(this);

        getInjectionEngine().registerInjectionMetaDataListener(this.cdiContainer);
    }

    protected void stop() {
        getInjectionEngine().unregisterInjectionMetaDataListener(this.cdiContainer);
        this.cdiContainer = null;

        CDIImpl.clear();
    }

    /** {@inheritDoc} */
    @Override
    public final BeanManager getModuleBeanManager(ModuleMetaData moduleMetaData) {
        BeanManager beanManager = getCDIContainer().getBeanManager(moduleMetaData);
        return beanManager;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public final boolean isApplicationCDIEnabled(ApplicationMetaData applicationMetaData) {
        boolean enabled = getCDIContainer().isApplicationCDIEnabled(applicationMetaData);
        return enabled;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isCurrentApplicationCDIEnabled() {
        boolean enabled = getCDIContainer().isCurrentApplicationCDIEnabled();
        return enabled;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public final boolean isModuleCDIEnabled(ModuleMetaData moduleMetaData) {
        boolean enabled = getCDIContainer().isModuleCDIEnabled(moduleMetaData);
        return enabled;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isCurrentModuleCDIEnabled() {
        boolean enabled = getCDIContainer().isCurrentModuleCDIEnabled();
        return enabled;
    }

    /**
     * Determine whether this jar is completely ignored by CDI, so no need to create bda for it. The jar will be ignored if it does not contain beans.xml and
     * in the server.xml, implicit bean archive scanning is disabled.
     *
     * @param moduleContainer the module container
     * @param type the module type
     * @return whether the jar will be ignored by CDI
     */
    @Override
    public final boolean skipCreatingBda(CDIArchive archive) {
        //only skip this if it is a leaf archive
        boolean skip = isImplicitBeanArchivesScanningDisabled(archive);
        skip = skip && (archive.getBeansXml() == null);
        skip = skip && (!(archive.getType() == ArchiveType.WEB_MODULE));
        return skip;
    }

    /** {@inheritDoc} */
    @Override
    public final BeanManager getCurrentModuleBeanManager() {
        return getCDIContainer().getCurrentModuleBeanManager();
    }

    /** {@inheritDoc} */
    @Override
    public final String getCurrentApplicationContextID() {
        return getCDIContainer().getCurrentApplicationContextID();
    }

    /** {@inheritDoc} */
    @Override
    public final WebSphereCDIDeployment getCurrentDeployment() {
        return getCDIContainer().getCurrentDeployment();
    }

    /** {@inheritDoc} */
    @Override
    public final BeanManager getClassBeanManager(Class<?> targetClass) {
        return getCDIContainer().getClassBeanManager(targetClass);
    }

    /** {@inheritDoc} */
    @Override
    public final WebSphereBeanDeploymentArchive getClassBeanDeploymentArchive(Class<?> targetClass) {
        return getCDIContainer().getClassBeanDeploymentArchive(targetClass);
    }

    /** {@inheritDoc} */
    @Override
    public final BeanManager getCurrentBeanManager() {
        return getCDIContainer().getCurrentBeanManager();
    }

    /** {@inheritDoc} */
    @Override
    public CDI<Object> getCDI() {
        CDI<Object> cdi = null;
        WebSphereCDIDeployment deployment = getCDIContainer().getCurrentDeployment();
        if (deployment != null) {
            cdi = deployment.getCDI();
        }

        return cdi;
    }

    /** {@inheritDoc} */
    @Override
    public void endContext() {
        ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        accessor.endContext();
    }

    @Override
    public CDIContainerImpl getCDIContainer() {
        return this.cdiContainer;
    }
}
