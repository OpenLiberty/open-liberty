/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import org.apache.myfaces.config.annotation.DiscoverableLifecycleProvider;
import org.apache.myfaces.config.annotation.LifecycleProvider2;
import org.apache.myfaces.config.annotation.NoInjectionAnnotationLifecycleProvider;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.util.ExternalSpecifications;

import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelper;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager;

public class WebSphereAnnotationLifecycleProvider implements DiscoverableLifecycleProvider, LifecycleProvider2 {
    private static final String CLASS_NAME = WebSphereAnnotationLifecycleProvider.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    private final AnnotationHelper runtimeAnnotationHelper;
    private final AnnotationHelperManager annotationHelperManager;
    NoInjectionAnnotationLifecycleProvider nonCDIProvider;

    // Liberty 99619 - Changing to a WeakHashMap
    private final Map<Object, ManagedObjectContext> cdiContexts = Collections.synchronizedMap(new WeakHashMap<Object, ManagedObjectContext>());

    // PI30335
    private final boolean delayPostConstruct =
                    MyfacesConfig.getCurrentInstance(FacesContext.getCurrentInstance().getExternalContext()).isDelayManagedBeanPostConstruct();
    
    // TODO: use configurable property here
    private final boolean skipCDIFallback = true;

    private List<Class<? extends Annotation>> cdiAnnotationClasses = Arrays.asList(
                                                                                   javax.inject.Inject.class,
                                                                                   javax.inject.Named.class,
                                                                                   javax.inject.Scope.class,
                                                                                   javax.inject.Qualifier.class,
                                                                                   javax.inject.Singleton.class
                                                                               );


    public WebSphereAnnotationLifecycleProvider(ExternalContext externalContext) {
        annotationHelperManager = AnnotationHelperManager.getInstance((ServletContext) externalContext.getContext());
        runtimeAnnotationHelper = annotationHelperManager.getAnnotationHelper();
        nonCDIProvider = new NoInjectionAnnotationLifecycleProvider();

    }

    public WebSphereAnnotationLifecycleProvider(Context context) {
        annotationHelperManager = AnnotationHelperManager.getInstance((ServletContext) context);
        runtimeAnnotationHelper = annotationHelperManager.getAnnotationHelper();
        nonCDIProvider= new NoInjectionAnnotationLifecycleProvider();
    }

    @Override
    public Object newInstance(String className) throws InstantiationException, IllegalAccessException, InvocationTargetException, NamingException, ClassNotFoundException {
        Class<?> clazz = ClassUtils.classForName(className);
        Object object;
        ManagedObject mo;
        
        if (!shouldSkipCDIProvider(clazz)) {
            // PI30335
            mo = runtimeAnnotationHelper.inject(clazz, delayPostConstruct);
            object = mo.getObject();
            cdiContexts.put(object, mo.getContext());
        } else {
            object = nonCDIProvider.newInstance(className);
            if (!delayPostConstruct) {
                nonCDIProvider.postConstruct(object);                              
            }
        }
        return object;
    }

    @Override
    public void postConstruct(Object o) throws IllegalAccessException,
                    InvocationTargetException {
        
        // PI30335
        // doPostConstruct was delayed, so do it now
        if (!shouldSkipCDIProvider(o.getClass())) {
            if (delayPostConstruct) {
                runtimeAnnotationHelper.doDelayedPostConstruct(o);
            }
        }
        else if (delayPostConstruct) {
            nonCDIProvider.postConstruct(o);
        }
    }

    @Override
    public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {

        ManagedObjectContext mos = null;
        if (o != null) {
            runtimeAnnotationHelper.doPreDestroy(o);
            mos = cdiContexts.remove(o);
        }
        if (null != mos) {
            mos.release(); // Release the CDI creational context for this bean if it's a CDI bean.
        }

    }

    @Override
    public boolean isAvailable() {
        try {
            return annotationHelperManager != null;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
    
    /**
     * Given a Class, search for CDI class and method annotations. 
     * Return true if any of the following annotations are found:
     *  javax.inject.Inject
     *  javax.inject.Named
     *  javax.inject.Scope
     *  javax.inject.Qualifier
     *  javax.inject.Singleton
     *  
     * @param Class
     * @return true if no CDI annotations are found and the CDI fallback property is enabled
     */
    private boolean shouldSkipCDIProvider(Class<?> annotatedClass) {
        boolean skipCDIInjection = false;
        if (skipCDIFallback) {
            skipCDIInjection = true;
            if (ExternalSpecifications.isCDIAvailable(FacesContext.getCurrentInstance().getExternalContext())) {
                while (annotatedClass != null && !skipCDIInjection) {
                    for (Class<? extends Annotation> c : cdiAnnotationClasses) {
                        if (annotatedClass.isAnnotationPresent(c)) {
                            skipCDIInjection = false;
                            break;
                        }
                        for (Method method : annotatedClass.getMethods()) {
                            if (method.isAnnotationPresent(c)){
                                skipCDIInjection = false;
                                break;
                            }
                        }
                    }
                    annotatedClass = annotatedClass.getSuperclass();
                }
            }
            return skipCDIInjection;
        }
        return false;
    }
}
