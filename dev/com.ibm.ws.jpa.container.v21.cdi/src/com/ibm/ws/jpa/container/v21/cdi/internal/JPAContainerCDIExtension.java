/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21.cdi.internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
import com.ibm.ws.cdi.CDIService;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class)
public class JPAContainerCDIExtension implements Extension, WebSphereCDIExtension {
	
    //This is not actually used since weld will create a new instance of this class seperate from the one OSGI has populated. 
    //But this stays so that OSGI will manage the extensions lifecycle. 
    @Reference
    protected HibernateNotifier notUsed;

    WeakReference<ClassLoader> classLoader = null;

    private final static String ENABLE_HIBERNATE_COMPATIBILITY = "com.ibm.websphere.jpa.hibernate-cdi-compatibility";
    @SuppressWarnings("unchecked")
    private static final boolean hibernateEnabled = Boolean.parseBoolean((String) AccessController.doPrivileged(new SystemGetPropertyPrivileged(ENABLE_HIBERNATE_COMPATIBILITY, "false")));

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
        //ClassLoaders are used to match events to the correct extended bean manager instance as they are the only data
        //unique enough to serve as an id available at the point the extended bean manager is created. 
        //As AfterDeploymentValidation events do not have any reference to the classLoader, we get it here and remember it
        if (hibernateEnabled) {
            ClassLoader cl = extractClassLoaderFromEvent(event);
            if (cl == null) {
                throw new IllegalStateException("Unable to find a classloader which can map the correct hibernate ExtendedBeanManager");
            }
            classLoader = new WeakReference<ClassLoader>(cl);
        }
    }

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager manager) {
        if (hibernateEnabled) {
            HibernateNotifier hibernateNotifier = getPropertyProvideder();
            hibernateNotifier.notifyHibernateAfterBeanDiscovery(manager, classLoader.get());
            classLoader.clear();
        }
    }
    
    public void beforeShutdown(@Observes BeforeShutdown event, BeanManager manager) {
        if (hibernateEnabled) {
    	    HibernateNotifier hibernateNotifier = getPropertyProvideder();
    	    hibernateNotifier.notifyHibernateBeforeShutdown(manager);
        }
    }

    private ClassLoader extractClassLoaderFromEvent(final AfterBeanDiscovery event) {
        if (event != null && event.getClass().getName().equals("org.jboss.weld.bootstrap.events.AfterBeanDiscoveryImpl")) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    try {
                        Method getDeployment = event.getClass().getSuperclass().getDeclaredMethod("getDeployment");
                        getDeployment.setAccessible(true);
                        Object maybeDeployment = getDeployment.invoke(event);
                        if (maybeDeployment.getClass().getSimpleName().contains("WebSphereCDIDeployment")) {
                            Method getClassLoader = maybeDeployment.getClass().getDeclaredMethod("getClassLoader");
                            getClassLoader.setAccessible(true);
                            Object maybeClassLoader = getClassLoader.invoke(maybeDeployment);
                            if (maybeClassLoader instanceof ClassLoader) {
                                return (ClassLoader) maybeClassLoader;
                            }  else {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    } catch (Throwable t) {
                        return null;
                    }
                }
            });
        } else {
            return null;
        }    
    }
 
    private HibernateNotifier getPropertyProvideder() {
        final Bundle bundle = FrameworkUtil.getBundle(HibernateNotifier.class);
        HibernateNotifier hibernateNotifier = AccessController.doPrivileged(new PrivilegedAction<HibernateNotifier>() {
            @Override
            public HibernateNotifier run() {
                BundleContext bCtx = bundle.getBundleContext();
                ServiceReference<HibernateNotifier> svcRef = bCtx.getServiceReference(HibernateNotifier.class);
                return svcRef == null ? null : bCtx.getService(svcRef);
            }
        });
        if (hibernateNotifier == null) {
            throw new IllegalStateException("Failed to get the HibernateNotifier.");
        }
        return hibernateNotifier;
    }
}
