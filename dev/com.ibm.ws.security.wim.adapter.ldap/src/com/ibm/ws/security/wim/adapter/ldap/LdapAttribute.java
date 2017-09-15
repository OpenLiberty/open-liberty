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
package com.ibm.ws.security.wim.adapter.ldap;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * The class which contains the information of a LDAP attribute.
 */
public class LdapAttribute {

    private String iAttrName = null;
    private String iSyntax = LdapConstants.LDAP_ATTR_SYNTAX_STRING;
    private Map<String, String> iDefaultValueMap = null;
    private Map<String, String> iDefaultAttrMap = null;
    private boolean iWIMGenerate = false;
    private Set<String> iEntityTypes = null;

    /**
     *
     */
    public LdapAttribute(String attrName) {
        iAttrName = attrName;
        iEntityTypes = new HashSet<String>();
    }

    public String getName() {
        return iAttrName;
    }

    public void setSyntax(String syntax) {
        iSyntax = syntax;
    }

    public String getSyntax() {
        return iSyntax;
    }

    public void setDefaultValue(String entityType, String value) {
        if (iDefaultValueMap == null) {
            iDefaultValueMap = new Hashtable<String, String>();
        }
        iDefaultValueMap.put(entityType, value);
    }

    public Object getDefaultValue(String entityType) {
        if ((iDefaultValueMap != null) && (iDefaultValueMap.size() > 0))
            return iDefaultValueMap.get(entityType);
        else
            return null;
    }

    public void setDefaultAttribute(String entityType, String attr) {
        if (iDefaultAttrMap == null) {
            iDefaultAttrMap = new Hashtable<String, String>();
        }
        iDefaultAttrMap.put(entityType, attr);
    }

    public String getDefaultAttribute(String entityType) {
        if ((iDefaultAttrMap != null) && (iDefaultAttrMap.size() > 0))
            return iDefaultAttrMap.get(entityType);
        else
            return null;
    }

    public void setWIMGenerate(boolean wimGen) {
        iWIMGenerate = wimGen;
    }

    public boolean isWIMGenerate() {
        return iWIMGenerate;
    }

    public Set<String> getEntityTypes() {
        return iEntityTypes;
    }

    public void addEntityType(String entityType) {
        iEntityTypes.add(entityType);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(":{");
        sb.append("iAttrName=").append(iAttrName);
        sb.append(", iDefaultAttrMap=").append(iDefaultAttrMap);
        sb.append(", iEntityTypes=").append(iEntityTypes);
        sb.append(", iSyntax=").append(iSyntax);
        sb.append(", iWIMGenerate=").append(iWIMGenerate);
        sb.append("}");
        return sb.toString();
    }
}
