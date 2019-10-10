/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.ejbcontainer.osgi.MDBRuntime;
import com.ibm.ws.ejbcontainer.osgi.ManagedBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.SessionBeanRuntime;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

/**
 * Data for merging a module that is not needed by on ModuleInitData.
 */
class ModuleMergeData {
    private boolean error = false;
    private final ModuleInitDataImpl initData;
    private final Container container;
    private AnnotationTargets_Targets annotationTargets;
    private InfoStore infoStore;
    private final Map<String, BeanMergeData> beans = new LinkedHashMap<String, BeanMergeData>();
    private final Set<String> beanClassNames = new HashSet<String>();
    private ModuleAnnotations moduleAnno;

    ModuleMergeData(ModuleInitDataImpl mid,
                    Container container,
                    AnnotationTargets_Targets annotationTargets,
                    InfoStore infoStore) {
        this.initData = mid;
        this.container = container;
        this.annotationTargets = annotationTargets;
        this.infoStore = infoStore;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + initData.ivJ2EEName + ']';
    }

    @Trivial
    boolean isEJBEnabled() {
        return initData.sessionBeanRuntime != null ||
               initData.mdbRuntime != null;
    }

    @Trivial
    SessionBeanRuntime getSessionBeanRuntime() {
        return initData.sessionBeanRuntime;
    }

    @Trivial
    MDBRuntime getMDBRuntime() {
        return initData.mdbRuntime;
    }

    @Trivial
    boolean isManagedBeanEnabled() {
        return initData.managedBeanRuntime != null;
    }

    @Trivial
    ManagedBeanRuntime getManagedBeanRuntime() {
        return initData.managedBeanRuntime;
    }

    void error() {
        error = true;
    }

    boolean hasErrors() {
        return error;
    }

    @Trivial
    Collection<BeanMergeData> getBeans() {
        return beans.values();
    }

    BeanMergeData getBeanMergeData(String name) {
        return this.beans.get(name);
    }

    private BeanMergeData createBeanMergeData(String name, ClassInfo classInfo) {
        BeanInitDataImpl bid = new BeanInitDataImpl(name, this.initData);
        BeanMergeData beanMergeData = new BeanMergeData(bid, this, classInfo);
        this.beans.put(name, beanMergeData);
        return beanMergeData;
    }

    BeanMergeData createBeanMergeDataFromAnnotation(String name, ClassInfo classInfo) {
        BeanMergeData beanMergeData = createBeanMergeData(name, classInfo);
        beanMergeData.setClassNameFromAnnotation(classInfo.getName());
        return beanMergeData;
    }

    BeanMergeData createBeanMergeDataFromXML(String name, String className) {
        BeanMergeData beanMergeData = createBeanMergeData(name, null);
        if (className != null) {
            beanMergeData.setClassName(className);
        }
        return beanMergeData;
    }

    private ModuleAnnotations getModuleAnnotations() {
        if (moduleAnno == null) {
            try {
                moduleAnno = container.adapt(ModuleAnnotations.class);
            } catch (UnableToAdaptException e) {
                throw new IllegalStateException(e);
            }
        }
        return moduleAnno;
    }

    @Trivial
    AnnotationTargets_Targets getAnnotationTargets() {
        if (annotationTargets == null) {
            try {
                annotationTargets = getModuleAnnotations().getAnnotationTargets();
            } catch (UnableToAdaptException e) {
                throw new IllegalStateException(e);
            }
        }
        return annotationTargets;
    }

    @Trivial
    public InfoStore getInfoStore() {
        if (infoStore == null) {
            try {
                infoStore = getModuleAnnotations().getInfoStore();
            } catch (UnableToAdaptException e) {
                throw new IllegalStateException(e);
            }
        }
        return infoStore;
    }

    void addBeanClassName(String className) {
        beanClassNames.add(className);
    }

    @Trivial
    public boolean containsBeanMergeDataForClass(String className) {
        return beanClassNames.contains(className);
    }
}
