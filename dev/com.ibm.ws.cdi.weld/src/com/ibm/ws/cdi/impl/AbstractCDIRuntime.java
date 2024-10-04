/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl;

import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;
import javax.interceptor.InvocationContext;

import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor.ModuleType;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.impl.weld.WebSphereEEModuleDescriptor;
import com.ibm.ws.cdi.internal.archive.liberty.RuntimeFactory;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.classloading.LibertyClassLoadingService;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * This abstract class generally holds methods which simply pass through to the CDIContainerImpl
 */
public abstract class AbstractCDIRuntime implements CDIService, CDIRuntime, CDIProvider {

    private final static ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = ThreadContextAccessor.getThreadContextAccessor();

    //Using this because it should have better performance than the getServiceWithException paradigm on CDIRuntimeImpl
    @SuppressWarnings("rawtypes")
    private static final ServiceCaller<LibertyClassLoadingService> classLoadingServiceCaller = new ServiceCaller<LibertyClassLoadingService>(AbstractCDIRuntime.class,
                                                                                                                                             LibertyClassLoadingService.class);

    private CDIContainerImpl cdiContainer;
    protected RuntimeFactory runtimeFactory;

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
    @SuppressWarnings({ "removal", "deprecation" })
    @Override
    public CDI<Object> getCDI() {
        CDI<Object> cdi = null;
        WebSphereCDIDeployment deployment = getCDIContainer().getCurrentDeployment();
        if (deployment != null) {
            cdi = deployment.getCDI();
        }

        //If we can't get CDI from the thread context metadata, fallback to matching the TCCL against apps.
        if (runtimeFactory != null && cdi == null) {
            Collection<Application> applications = runtimeFactory.getApplications();

            ClassLoader tccl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
                return THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread());
            });

            //Doing it this way for performance reasons. Saves us spinning up a lambda every loop iteration
            LibertyClassLoadingService<?> classLoadingService = classLoadingServiceCaller.current().get();
            for (Application application : applications) {
                if (application.getClassLoader().equals(tccl)
                    || classLoadingService.isThreadContextClassLoaderForAppClassLoader(tccl, application.getClassLoader())) {
                    deployment = cdiContainer.getDeployment(application);
                    //We found the correct app but it has no deployment, this happens if CDI is disabled.
                    if (deployment != null) {
                        cdi = deployment.getCDI();
                    }
                    break;
                }
            }
        }

        if (deployment == null) {
            throw new IllegalStateException("Could not find deployment");
        }

        if (cdi == null) {
            throw new IllegalStateException("Could not find CDI");
        }

        return cdi;
    }

    @Override
    public CDIContainerImpl getCDIContainer() {
        return this.cdiContainer;
    }

    @Override
    public Set<Annotation> getInterceptorBindingsFromInvocationContext(InvocationContext ic) throws IllegalArgumentException {
        return CDIUtils.getInterceptorBindingsFromInvocationContext(ic);
    }

    @Override
    public Optional<J2EEName> getModuleNameForClass(Class<?> clazz) {
        return Optional.ofNullable(getClassBeanDeploymentArchive(clazz))
                       .map(bda -> bda.getServices().get(EEModuleDescriptor.class))
                       .filter(desc -> desc.getType() == ModuleType.WEB || desc.getType() == ModuleType.EJB_JAR)
                       .filter(WebSphereEEModuleDescriptor.class::isInstance)
                       .map(WebSphereEEModuleDescriptor.class::cast)
                       .map(desc -> desc.getJ2eeName());
    }
}
