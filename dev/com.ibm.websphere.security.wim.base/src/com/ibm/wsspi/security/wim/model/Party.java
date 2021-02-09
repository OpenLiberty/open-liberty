/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//

package com.ibm.wsspi.security.wim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Party complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Party">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}RolePlayer">
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The Party object extends the RolePlayer object, and represents a Party which is extended by Person,
 * Group, OrgContainer and LoginAccount.
 * 
 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Party")
@XmlSeeAlso({
             Group.class,
             OrgContainer.class,
             Person.class,
             LoginAccount.class
})
@Trivial
public class Party
                extends RolePlayer
{

    private static List mandatoryProperties = null;
    private static List transientProperties = null;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;

    static {
        setMandatoryPropertyNames();
        setTransientPropertyNames();
        getTransientProperties();
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    @Override
    public Object get(String propName) {
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "Party";
    }

    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList();
    }

    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList();
        transientProperties.addAll(RolePlayer.getTransientProperties());
    }

    @Override
    public boolean isMandatory(String propName) {
        if (mandatoryProperties == null) {
            setMandatoryPropertyNames();
        }
        if (mandatoryProperties.contains(propName)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isPersistentProperty(String propName) {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        if (transientProperties.contains(propName)) {
            return false;
        } else {
            return true;
        }
    }

    protected static List getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return transientProperties;
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.addAll(RolePlayer.getPropertyNames("RolePlayer"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        } else {
            return super.getDataType(propName);
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
        superTypeList.add("RolePlayer");
        superTypeList.add("Entity");
    }

    @Override
    public ArrayList getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
        subTypeList.add("Group");
        subTypeList.add("OrgContainer");
        subTypeList.add("LoginAccount");
        subTypeList.add("Person");
        subTypeList.add("PersonAccount");
    }

    public static HashSet getSubTypes() {
        if (subTypeList == null) {
            setSubTypes();
        }
        return subTypeList;
    }

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
