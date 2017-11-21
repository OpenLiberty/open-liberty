/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.archive.AbstractCDIArchive;
import com.ibm.ws.cdi.internal.interfaces.Application;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.Resource;
import com.ibm.ws.cdi.internal.interfaces.ResourceInjectionBag;
import com.ibm.ws.runtime.metadata.MetaData;

public class OnDemandArchive extends AbstractCDIArchive implements CDIArchive {

    private final Class<?> initialClass;
    private final Application application;
    private static final Map<Object, String> bdaNameCache = new WeakHashMap<Object, String>();
    private static final String FALLBACK_BDA_NAME = "No_bundle_BDA";
    private static final String NULL_KEY = "NULL_KEY";

    public OnDemandArchive(CDIRuntime cdiRuntime, Application application, Class<?> initialClass) {
        //work out a unique archive name based on the the initial class
        super(createName(initialClass), cdiRuntime);
        this.application = application;
        this.initialClass = initialClass;
    }

    /**
     * @param initialClass
     * @return a name for this OnDemandArchive
     */
    private static String createName(Class<?> initialClass) {

        String toReturn = null;
        Object mapKey = initialClass.getClassLoader() == null ? NULL_KEY : initialClass.getClassLoader();

        if (mapKey == NULL_KEY) {
            toReturn = CDIUtils.BDA_FOR_CLASSES_LOADED_BY_ROOT_CLASSLOADER;
        } else if (bdaNameCache.containsKey(mapKey)) {
            toReturn = bdaNameCache.get(mapKey);
        } else {

            Bundle bundle = FrameworkUtil.getBundle(initialClass);

            //if the bundle is null use a default name otherwise use the bundle name and version
            if (bundle == null) {
                toReturn = FALLBACK_BDA_NAME;
                bdaNameCache.put(mapKey, toReturn);
            } else {

                StringBuilder sb = new StringBuilder();
                sb.append(CDIUtils.getSymbolicNameWithoutMinorOrMicroVersionPart(bundle.getSymbolicName()));
                sb.append("_");
                sb.append(CDIUtils.getOSGIVersionForBndName(bundle.getVersion()));

                toReturn = sb.toString();
                bdaNameCache.put(mapKey, toReturn);
            }
        }
        return toReturn;
    }

    /** {@inheritDoc} */
    @Override
    public J2EEName getJ2EEName() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArchiveType getType() {
        return ArchiveType.ON_DEMAND_LIB;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CDIException
     */
    @Override
    public ClassLoader getClassLoader() throws CDIException {
        ClassLoader classLoader = this.initialClass.getClassLoader();
        if (classLoader == null) {
            classLoader = application.getClassLoader();
        }
        return classLoader;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getClassNames() {
        return Collections.singleton(initialClass.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isModule() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Application getApplication() {
        return application;
    }

    /** {@inheritDoc} */
    @Override
    public String getClientModuleMainClass() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInjectionClassList() throws CDIException {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public MetaData getMetaData() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceInjectionBag getAllBindings() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getClientAppCallbackHandlerName() throws CDIException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getExtensionClasses() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        // this is a virtual archive so it doens't really have a path
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Set<CDIArchive> getModuleLibraryArchives() throws CDIException {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Resource getResource(String path) {
        // this is a virtual archive so it doens't really have resources
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getBeanDefiningAnnotations() throws CDIException {

        Set<String> beanDefiningAnnotations = new HashSet<String>();
        Annotation[] annotations = this.initialClass.getAnnotations();

        for (Annotation annotation : annotations) {
            if (CDIUtils.BEAN_DEFINING_META_ANNOTATIONS.contains(annotation.annotationType())) {
                beanDefiningAnnotations.add(this.initialClass.getName());
                break;
            }
        }

        return beanDefiningAnnotations;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getAnnotatedClasses(Set<String> annotations) throws CDIException {
        Set<String> annotatedClasses = new HashSet<String>();
        Annotation[] classAnnotations = this.initialClass.getAnnotations();

        for (Annotation annotation : classAnnotations) {
            if (annotations.contains(annotation.annotationType().getName())) {
                annotatedClasses.add(this.initialClass.getName());
                break;
            }
        }

        return annotatedClasses;
    }

}
