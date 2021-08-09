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
package com.ibm.ws.jaxrs20.ejb;

import java.util.List;
import java.util.Map;

import com.ibm.ws.ejbcontainer.EJBType;

/**
 *
 */
/**
 *
 */
public class EJBInfo {

    private final Map<String, String> methodToJNDI;

    private final Map<String, Object> ejbInstanceCache;

    private final String ejbModuleName;

    /**
     * @return the ejbModuleName
     */
    public String getEjbModuleName() {
        return ejbModuleName;
    }

    /**
     * @return the ejbInstanceCache
     */
    public Map<String, Object> getEjbInstanceCache() {
        return ejbInstanceCache;
    }

    public Map<String, String> getMethodToJNDI() {
        return methodToJNDI;
    }

    public EJBType getEjbType() {
        return ejbType;
    }

    public String getEjbClassName() {
        return ejbClassName;
    }

    public List<String> getLocalInterfaceNameList() {
        return localInterfaceNameList;
    }

    private final EJBType ejbType;

    private final String ejbClassName;

    /**
     * @return the ejbName
     */
    public String getEjbName() {
        return ejbName;
    }

    private final String ejbName;

    private final List<String> localInterfaceNameList;

    public EJBInfo(String ejbClassName, String ejbName, EJBType ejbtype, Map<String, String> methodToJNDI, Map<String, Object> ejbCache, List<String> localInterfaceNameList,
                   String ejbModuleName) {
        this.ejbClassName = ejbClassName;
        this.ejbType = ejbtype;
        this.methodToJNDI = methodToJNDI;
        this.localInterfaceNameList = localInterfaceNameList;
        this.ejbInstanceCache = ejbCache;
        this.ejbName = ejbName;
        this.ejbModuleName = ejbModuleName;

    }

}
