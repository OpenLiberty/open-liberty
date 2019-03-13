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
package com.ibm.ws.jaxrs20.metadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * ProviderResourceInfo helps to describe the @Path or @Provider's info
 */
public class ProviderResourceInfo implements Serializable {

    /**  */
    private static final long serialVersionUID = 7712328381838880886L;

    /*
     * by default is POJO,
     * when EJB or CDI's JaxRsFactoryBeanCustomizer.onInit, change the runtimeType,
     * then if runtimeType is changed to EJB or CDI,
     * the other JaxRsFactoryBeanCustomizer will not processed this Provider or Resource
     */
    public enum RuntimeType {
        EJB, CDI, POJO, MANAGEDBEAN, IMPLICITBEAN
    }

    @Override
    public String toString() {
        String className = clazz != null ? clazz.getName() : prObject != null ? prObject.getClass().getName() : "NULL";
        return className + "|isJaxRsProvider=" + isJaxRsProvider + "|isJaxRsResource=" + isJaxRsResource;
    }

    private Class<?> clazz = null;
    private Object prObject = null;
    private RuntimeType runtimeType = RuntimeType.POJO;
    private boolean isJaxRsSingleton = false;
    private boolean isJaxRsProvider = false;
    private boolean isJaxRsResource = false;
    private final Map<String, Object> customizedProperties = new HashMap<String, Object>();

    public ProviderResourceInfo(Class<?> clazz, boolean isJaxRsResource, boolean isJaxRsProvider) {
        this.clazz = clazz;
        this.isJaxRsSingleton = false;
        this.isJaxRsProvider = isJaxRsProvider;
        this.isJaxRsResource = isJaxRsResource;
    }

    public ProviderResourceInfo(Object obj, boolean isJaxRsResource, boolean isJaxRsProvider) {
        this.prObject = obj;

        if (obj != null) {
            this.clazz = obj.getClass();
        }

        this.isJaxRsSingleton = true;
        this.isJaxRsProvider = isJaxRsProvider;
        this.isJaxRsResource = isJaxRsResource;
    }

    public String getClassName() {
        return this.clazz.getName();
    }

    public Class<?> getProviderResourceClass() {
        return this.clazz;
    }

    public Object getObject() {
        return this.prObject;
    }

    public void setObject(Object obj) {
        this.prObject = obj;
    }

    public void setRuntimeType(RuntimeType runtimeType) {
        this.runtimeType = runtimeType;
    }

    public RuntimeType getRuntimeType() {
        return this.runtimeType;
    }

    public boolean isJaxRsSingleton() {
        return this.isJaxRsSingleton;
    }

    /**
     * check if the class is provider
     * if the class is not a provider, then the class is a resource
     * if the class is a provider, the class could be a resource or not
     * 
     * @return
     */
    public boolean isJaxRsProvider() {
        return this.isJaxRsProvider;
    }

    public boolean isJaxRsResource() {
        return this.isJaxRsResource;
    }

    /**
     * put anything here, such as J2EEName
     * 
     * @param key
     * @param obj
     */
    public void putCustomizedProperty(String key, Object obj) {
        if (key == null || obj == null)
            return;

        customizedProperties.put(key, obj);
    }

    public void removeCustomizedProperty(String key) {
        if (key == null)
            return;

        customizedProperties.remove(key);
    }

    public Object getCustomizedProperty(String key) {
        if (key == null)
            return null;

        return customizedProperties.get(key);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof ProviderResourceInfo))
            return false;
        ProviderResourceInfo providerResourceInfo = (ProviderResourceInfo) object;
        return providerResourceInfo.clazz.equals(this.clazz);
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }
}
