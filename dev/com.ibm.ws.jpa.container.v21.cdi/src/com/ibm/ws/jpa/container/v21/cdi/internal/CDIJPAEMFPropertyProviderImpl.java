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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jpa.management.JPAEMFPropertyProvider;

import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
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
    private Map<String,IBMHibernateExtendedBeanManager> extendedBeanManagers = new HashMap<String,IBMHibernateExtendedBeanManager>();

    @Reference
    protected ClassLoadingService classLoadingService;
    @Reference
    protected ClassLoaderIdentifierService classLoaderIdentifierService;

    private final static String ENABLE_HIBERNATE_COMPATIBILITY = "com.ibm.websphere.jpa.hibernate-cdi-compatibility";
    @SuppressWarnings("unchecked")
    private static final boolean hibernateEnabled = Boolean.parseBoolean((String) AccessController.doPrivileged(new SystemGetPropertyPrivileged(ENABLE_HIBERNATE_COMPATIBILITY, "false")));

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public void updateProperties(Map<String, Object> props, ClassLoader applicationClassLoader) {
        if (cdiService != null) {
            Class[] beanManagerInterfaces = null;
            InvocationHandler invocationHandler = null;
            ClassLoader classLoader = CLASSLOADER;

            if (hibernateEnabled) {
                //In this try block we search for Hibernate classes, if they are not on the classlodaer we set up the default non-hibernate invocationHandler in the catch block.
                try {

                    String baseClassLoaderId = getBaseClassLoaderId(applicationClassLoader);

                    ClassLoader unifiedClassLoader = unify(CLASSLOADER, applicationClassLoader);

                    Class<?> extendedBeanManagerInterface = Class.forName("org.hibernate.resource.beans.container.spi.ExtendedBeanManager",
                                                                             true, unifiedClassLoader);
                    Class<?> depreciatedExtendedBeanManagerInterface = Class.forName("org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager",
                                                                             true, unifiedClassLoader); //A bug in hibernate means we need to implement this interface too for now. 

                    //Since extended bean managers only handle CDI lifecycle events, which are scoped to the whole ear we only need one.
                    IBMHibernateExtendedBeanManager extendedBeanManager = null;
                    if (extendedBeanManagers.containsKey(baseClassLoaderId)) {
                        extendedBeanManager = extendedBeanManagers.get(baseClassLoaderId);
                    } else {
                        extendedBeanManager = new IBMHibernateExtendedBeanManager(unifiedClassLoader, baseClassLoaderId);
                        extendedBeanManagers.put(baseClassLoaderId, extendedBeanManager);
                    }

                    invocationHandler = new BeanManagerInvocationHandler(cdiService, extendedBeanManager);
                    beanManagerInterfaces = new Class<?>[] { extendedBeanManagerInterface, depreciatedExtendedBeanManagerInterface, BeanManager.class };
                    classLoader = unifiedClassLoader;
                } catch (ClassNotFoundException e) {
                    invocationHandler = new BeanManagerInvocationHandler(cdiService);
                    beanManagerInterfaces = new Class<?>[] { BeanManager.class };
                }
            } else {
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
    
    public void notifyHibernateAfterBeanDiscovery(BeanManager beanManager, ClassLoader classLoader) {
        String baseClassLoaderId = getBaseClassLoaderId(classLoader);
        for (IBMHibernateExtendedBeanManager extendedBeanManager : extendedBeanManagers.values()) {
            //We check which is the correct bean manager inside IBMHibernateExtendedBeanManager. 
            extendedBeanManager.notifyHibernateAfterBeanDiscovery(baseClassLoaderId, beanManager);
        }
    }

    public void notifyHibernateBeforeShutdown(BeanManager beanManager) {
        Iterator<Map.Entry<String,IBMHibernateExtendedBeanManager>> it = extendedBeanManagers.entrySet().iterator();
        while (it.hasNext()) {
            IBMHibernateExtendedBeanManager extendedBeanManager = it.next().getValue();
            //We check which is the correct bean manager inside IBMHibernateExtendedBeanManager. 
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

    private String getBaseClassLoaderId(ClassLoader applicationClassLoader) {
        while (applicationClassLoader != null) {
            String id = classLoaderIdentifierService.getClassLoaderIdentifier(applicationClassLoader);
            if (id.startsWith("EARApplication")) {
                return id;
            }

            ClassLoader parent = applicationClassLoader.getParent();
            String parentId = classLoaderIdentifierService.getClassLoaderIdentifier(parent) == null ? null : classLoaderIdentifierService.getClassLoaderIdentifier(parent);
            if (parent == null || parentId == null || parentId.equals("Shared Library:global")) {
            	return id;
            } else {
                applicationClassLoader = parent;
            }
        }
        return null;
    }

    @Reference
    protected void setCDIService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    protected void unsetCDIService(CDIService cdiService) {
        this.cdiService = null;
    }
}
