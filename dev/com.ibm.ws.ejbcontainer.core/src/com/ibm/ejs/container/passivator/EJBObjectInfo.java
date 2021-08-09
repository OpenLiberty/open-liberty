/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.passivator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ejs.util.Util;

/**
 * The EJBObjectInfo object provides information about Stateful EJB objects
 * and it's corresponding interceptors when they are passivated and activated.
 * For serializable objects, it just contains the object itself. For non-serializable
 * objects, it contains the class name and information about each of its fields,
 * including the field values. Each of the fields must either be serializable or
 * be replaceble by the NewOutputStream. This object is serializable itself and
 * it is assumed that is will be processed by the NewOutputStream and NewInputStream.
 */
class EJBObjectInfo implements Serializable {
    private static final long serialVersionUID = 7712587930606979632L;

    /**
     * Is the object that this info is representing serializable?
     */
    private boolean ivSerializable = false;

    // The following field is used if the object is serializable
    private Object ivSerializableObject = null;

    // The following fields are used if the object is not serializable
    private String ivClassName = null;
    private Map<String, List<FieldInfo>> ivFieldInfoMap = null; // d460047

    /**
     * Inner class to hold field information. This is expected to be
     * accessed by Stateful Passivator.
     */
    static class FieldInfo implements Serializable {
        private static final long serialVersionUID = -6936032901508808602L;

        String name;
        Object value;
    }

    String getClassName() {
        return ivClassName;
    }

    void setClassName(String className) {
        this.ivClassName = className;
    }

    // d460047
    Map<String, List<FieldInfo>> getFieldInfoMap() {
        return ivFieldInfoMap;
    }

    // d460047
    /**
     * Adds the className and fieldInfoList to the ivFieldInfoMap.
     * 
     * @param className
     * @param fieldInfoList
     */
    void addFieldInfo(String className, List<FieldInfo> fieldInfoList) {
        if (ivFieldInfoMap == null) {
            ivFieldInfoMap = new HashMap<String, List<FieldInfo>>();
        }
        ivFieldInfoMap.put(className, fieldInfoList);
    }

    boolean isSerializable() {
        return ivSerializable;
    }

    void setSerializable(boolean serializable) {
        this.ivSerializable = serializable;
    }

    Object getSerializableObject() {
        return ivSerializableObject;
    }

    void setSerializableObject(Object serializableObject) {
        this.ivSerializableObject = serializableObject;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("EJBObjectInfo");
        sb.append("\tserializable=" + ivSerializable);
        if (ivSerializable) {
            if (ivSerializableObject != null) {
                sb.append(" className=" + ivSerializableObject.getClass().getName());
            }
        }
        else {
            if (ivClassName != null) {
                sb.append(" className=" + ivClassName);
            }
            if (ivFieldInfoMap != null) {
                sb.append("\n\tfieldInfoMap");
                Set<String> classNames = ivFieldInfoMap.keySet();
                for (String className : classNames) {
                    sb.append("\n\t\tclassName=" + className);
                    List<FieldInfo> fieldInfoList = ivFieldInfoMap.get(className);
                    if (fieldInfoList != null) {
                        for (FieldInfo fieldInfo : fieldInfoList) {
                            sb.append("\n\t\t\tfieldInfo=" + fieldInfo.name + " - " + Util.identity(fieldInfo.value)); // RTC97224
                        }
                    }

                }
            }
        }

        return sb.toString();
    }
}
