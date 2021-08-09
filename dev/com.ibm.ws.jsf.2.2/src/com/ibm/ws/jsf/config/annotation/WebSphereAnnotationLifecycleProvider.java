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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
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
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;

import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelper;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager;

public class WebSphereAnnotationLifecycleProvider implements DiscoverableLifecycleProvider, LifecycleProvider2 {
    private static final String CLASS_NAME = WebSphereAnnotationLifecycleProvider.class.getName();
    private static final Logger logger = Logger.getLogger(CLASS_NAME);

    private final AnnotationHelper runtimeAnnotationHelper;
    private final AnnotationHelperManager annotationHelperManager;

    // Liberty 99619 - Changing to a WeakHashMap
    private final Map<Object, ManagedObjectContext> cdiContexts = Collections.synchronizedMap(new WeakHashMap<Object, ManagedObjectContext>());

    // PI30335
    private final boolean delayPostConstruct =
                    MyfacesConfig.getCurrentInstance(FacesContext.getCurrentInstance().getExternalContext()).isDelayManagedBeanPostConstruct();

    public WebSphereAnnotationLifecycleProvider(ExternalContext externalContext) {
        annotationHelperManager = AnnotationHelperManager.getInstance((ServletContext) externalContext.getContext());
        runtimeAnnotationHelper = annotationHelperManager.getAnnotationHelper();
    }

    public WebSphereAnnotationLifecycleProvider(Context context) {
        annotationHelperManager = AnnotationHelperManager.getInstance((ServletContext) context);
        runtimeAnnotationHelper = annotationHelperManager.getAnnotationHelper();
    }

    @Override
    public Object newInstance(String className) throws InstantiationException, IllegalAccessException, InvocationTargetException, NamingException, ClassNotFoundException {
        Class<?> clazz = ClassUtils.classForName(className);
        Object object;
        ManagedObject mo;
        // PI30335
        mo = runtimeAnnotationHelper.inject(clazz, delayPostConstruct);
        object = mo.getObject();

        cdiContexts.put(object, mo.getContext());

        return object;
    }

    @Override
    public void postConstruct(Object o) throws IllegalAccessException,
                    InvocationTargetException {
        // PI30335
        // doPostConstruct was delayed, so do it now
        if (delayPostConstruct) {
            runtimeAnnotationHelper.doDelayedPostConstruct(o);
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
}
