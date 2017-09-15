/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ejbcontainer.osgi.BeanRuntime;
import com.ibm.wsspi.anno.info.ClassInfo;

/**
 * Data for merging a bean that is not needed by on BeanInitData.
 */
public class BeanMergeData {
    private final BeanInitDataImpl initData;
    private final ModuleMergeData moduleMergeData;
    private ClassInfo classInfo;
    private boolean classNameFromAnnotation;
    private boolean typeFromAnnotation;
    private Set<String> remoteBusinessInterfaceNames;
    private Set<String> localBusinessInterfaceNames;
    private boolean bmtSet;
    private boolean startupSet;
    private boolean passivationCapableSet;

    public BeanMergeData(BeanInitDataImpl initData, ModuleMergeData moduleMergeData, ClassInfo classInfo) {
        this.initData = initData;
        this.moduleMergeData = moduleMergeData;
        this.classInfo = classInfo;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + initData.ivName + ", " + initData.ivClassName + ']';
    }

    @Trivial
    public ModuleMergeData getModuleMergeData() {
        return moduleMergeData;
    }

    @Trivial
    public BeanInitDataImpl getBeanInitData() {
        return initData;
    }

    public void setClassName(String className) {
        initData.ivClassName = className;
        moduleMergeData.addBeanClassName(className);
    }

    @Trivial
    public void setClassNameFromAnnotation(String className) {
        setClassName(className);
        classNameFromAnnotation = true;
    }

    @Trivial
    public boolean isClassNameFromAnnotation() {
        return classNameFromAnnotation;
    }

    @Trivial
    public ClassInfo getClassInfo() {
        if (classInfo == null) {
            classInfo = moduleMergeData.getInfoStore().getDelayableClassInfo(initData.ivClassName);
        }
        return classInfo;
    }

    @Trivial
    public void setType(int type, BeanRuntime beanRuntime) {
        initData.ivType = type;
        initData.beanRuntime = beanRuntime;
    }

    @Trivial
    public void setTypeFromAnnotation(int type, BeanRuntime beanRuntime) {
        setType(type, beanRuntime);
        typeFromAnnotation = true;
    }

    @Trivial
    public boolean isTypeFromAnnotation() {
        return typeFromAnnotation;
    }

    public void addRemoteBusinessInterfaceName(String interfaceName) {
        if (remoteBusinessInterfaceNames == null) {
            remoteBusinessInterfaceNames = new LinkedHashSet<String>();
        }
        remoteBusinessInterfaceNames.add(interfaceName);
    }

    @Trivial
    public Collection<String> getRemoteBusinessInterfaceNames() {
        return remoteBusinessInterfaceNames != null ? remoteBusinessInterfaceNames : Collections.<String> emptyList();
    }

    public void addLocalBusinessInterfaceName(String interfaceName) {
        if (localBusinessInterfaceNames == null) {
            localBusinessInterfaceNames = new LinkedHashSet<String>();
        }
        localBusinessInterfaceNames.add(interfaceName);
    }

    @Trivial
    public Collection<String> getLocalBusinessInterfaceNames() {
        return localBusinessInterfaceNames != null ? localBusinessInterfaceNames : Collections.<String> emptyList();
    }

    public void setBeanManagedTransaction(boolean bmt) {
        bmtSet = true;
        initData.ivBeanManagedTransaction = bmt;
    }

    @Trivial
    public boolean isSetBeanManagedTransaction() {
        return bmtSet;
    }

    public void setStartup(boolean startup) {
        startupSet = true;
        initData.ivStartup = startup;
    }

    @Trivial
    public boolean isSetStartup() {
        return startupSet;
    }

    public void setPassivationCapable(boolean passivationCapable) {
        passivationCapableSet = true;
        initData.ivPassivationCapable = passivationCapable;
    }

    @Trivial
    public boolean isSetPassivationCapable() {
        return passivationCapableSet;
    }

    public void merge() {
        if (remoteBusinessInterfaceNames != null) {
            initData.ivRemoteBusinessInterfaceNames = remoteBusinessInterfaceNames.toArray(new String[remoteBusinessInterfaceNames.size()]);
        }

        if (localBusinessInterfaceNames != null) {
            initData.ivLocalBusinessInterfaceNames = localBusinessInterfaceNames.toArray(new String[localBusinessInterfaceNames.size()]);
        }
    }
}
