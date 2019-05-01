/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21.cdi.internal;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jpa.management.JPAEMFPropertyProvider;

import com.ibm.wsspi.classloading.ClassLoadingService;

/**
 * Class to provide properties specific to CDI to the JPA container.
 */
@Component
public class CDIJPAEMFPropertyProviderImpl implements JPAEMFPropertyProvider, HibernateNotifier {
    private static final TraceComponent tc = Tr.register(CDIJPAEMFPropertyProviderImpl.class);
    private static final ClassLoader CLASSLOADER = BeanManager.class.getClassLoader();
    /**
     * Spec-defined property used for passing an instance of the BeanManager to
     * to the JPA Provider for injection into entity listeners.
     */
    private static final String CDI_BEANMANAGER = "javax.persistence.bean.manager";

    private CDIService cdiService;
    private Set<IBMHibernateExtendedBeanManager> extendedBeanManagers = new HashSet<IBMHibernateExtendedBeanManager>();

    @Reference
    protected ClassLoadingService classLoadingService;

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public void updateProperties(Map<String, Object> props, ClassLoader applicationClassLoader) {
        if (cdiService != null) {
            Class[] beanManagerInterfaces = null;
            InvocationHandler invocationHandler = null;
            ClassLoader classLoader = CLASSLOADER;
            try {
                ClassLoader unifiedClassLoader = unify(CLASSLOADER, applicationClassLoader);

                Class<?> extendedBeanManagerInterface = Class.forName("org.hibernate.resource.beans.container.spi.ExtendedBeanManager",
                                                                         true, unifiedClassLoader);
                Class<?> depreciatedExtendedBeanManagerInterface = Class.forName("org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager",
                                                                         true, unifiedClassLoader); //A bug in hibernate means we need to implement this interface too for now. 
                IBMHibernateExtendedBeanManager extendedBeanManager = new IBMHibernateExtendedBeanManager(unifiedClassLoader);
                extendedBeanManagers.add(extendedBeanManager);
                invocationHandler = new BeanManagerInvocationHandler(cdiService, extendedBeanManager);
                beanManagerInterfaces = new Class<?>[] { extendedBeanManagerInterface, depreciatedExtendedBeanManagerInterface, BeanManager.class };
                classLoader = unifiedClassLoader;
            } catch (ClassNotFoundException e) {
                invocationHandler = new BeanManagerInvocationHandler(cdiService);
                beanManagerInterfaces = new Class<?>[] { BeanManager.class };
            }

            Object beanManager = Proxy.newProxyInstance(classLoader,
                                                        beanManagerInterfaces,
                                                        invocationHandler);
            props.put(CDI_BEANMANAGER, beanManager);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "updateProperties setting {0}={1}", CDI_BEANMANAGER, beanManager);
            }
        }

    }
    
    public void notifyHibernateAfterBeanDiscovery(BeanManager beanManager) {
        for (IBMHibernateExtendedBeanManager extendedBeanManager : extendedBeanManagers) {
            //We check which is the correct bean manager inside IBMHibernateExtendedBeanManager as that will have access to the underlying bean manager. 
            extendedBeanManager.notifyHibernateAfterBeanDiscovery(beanManager);
        }
    }

    public void notifyHibernateBeforeShutdown(BeanManager beanManager) {
        Iterator<IBMHibernateExtendedBeanManager> it = extendedBeanManagers.iterator();
        while (it.hasNext()) {
            IBMHibernateExtendedBeanManager extendedBeanManager = it.next();
            //We check which is the correct bean manager inside IBMHibernateExtendedBeanManager as that will have access to the underlying bean manager. 
            if (extendedBeanManager.notifyHibernateBeforeShutdown(beanManager)) {
                it.remove();
            }
        }
    }

    private ClassLoader unify(final ClassLoader parent, final ClassLoader child) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return classLoadingService.unify(parent, child);
            }
        });
    }

    @Reference
    protected void setCDIService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    protected void unsetCDIService(CDIService cdiService) {
        this.cdiService = null;
    }
}
