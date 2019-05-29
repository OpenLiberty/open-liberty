/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class HandlerInfo implements Serializable {

    /**  */
    private static final long serialVersionUID = 938574497711221393L;

    private String id;

    private String handlerName;

    private String handlerClass;

    private final List<String> soapRoles = new ArrayList<String>();

    private final List<ParamValueInfo> initParams = new ArrayList<ParamValueInfo>();

    private final List<XsdQNameInfo> soapHeaders = new ArrayList<XsdQNameInfo>();

    private final List<String> portNames = new ArrayList<String>();

    public boolean addPortName(String portName) {
        return portNames.add(portName);
    }

    public boolean removePortName(String portName) {
        return portNames.remove(portName);
    }

    /**
     * @return the portNames
     */
    public List<String> getPortNames() {
        return portNames;
    }

    /**
     * @return the handlerName
     */
    public String getHandlerName() {
        return handlerName;
    }

    /**
     * @param handlerName the handlerName to set
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    /**
     * @return the handlerClass
     */
    public String getHandlerClass() {
        return handlerClass;
    }

    /**
     * @param handlerClass the handlerClass to set
     */
    public void setHandlerClass(String handlerClass) {
        this.handlerClass = handlerClass;
    }

    public List<String> getSoapRoles() {
        return Collections.unmodifiableList(soapRoles);
    }

    public boolean addSoapRole(String soapRole) {
        return soapRoles.add(soapRole);
    }

    public boolean removeSoapRole(String soapRole) {
        return soapRoles.remove(soapRole);
    }

    /**
     * @return the soapHeaders
     */
    public List<XsdQNameInfo> getSoapHeaders() {
        return Collections.unmodifiableList(soapHeaders);
    }

    public boolean addSoapHeader(XsdQNameInfo soapHeader) {
        return soapHeaders.add(soapHeader);
    }

    public boolean removeSoapHeader(XsdQNameInfo soapHeader) {
        return soapHeaders.remove(soapHeader);
    }

    /**
     * @return the initParam
     */
    public List<ParamValueInfo> getInitParam() {
        return initParams;
    }

    public boolean addInitParam(ParamValueInfo paramValue) {
        return initParams.add(paramValue);
    }

    public boolean removeInitParam(ParamValueInfo paramValue) {
        return initParams.remove(paramValue);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "HandlerInfo [id=" + id + ", handlerName=" + handlerName + ", handlerClass=" + handlerClass + ", soapRoles=" + soapRoles + ", initParams=" + initParams
               + ", soapHeaders=" + soapHeaders + "]";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((handlerClass == null) ? 0 : handlerClass.hashCode());
        result = prime * result + ((handlerName == null) ? 0 : handlerName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((initParams == null) ? 0 : initParams.hashCode());
        result = prime * result + ((soapHeaders == null) ? 0 : soapHeaders.hashCode());
        result = prime * result + ((soapRoles == null) ? 0 : soapRoles.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HandlerInfo other = (HandlerInfo) obj;
        if (handlerClass == null) {
            if (other.handlerClass != null)
                return false;
        } else if (!handlerClass.equals(other.handlerClass))
            return false;
        if (handlerName == null) {
            if (other.handlerName != null)
                return false;
        } else if (!handlerName.equals(other.handlerName))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (initParams == null) {
            if (other.initParams != null)
                return false;
        } else if (!initParams.equals(other.initParams))
            return false;
        if (soapHeaders == null) {
            if (other.soapHeaders != null)
                return false;
        } else if (!soapHeaders.equals(other.soapHeaders))
            return false;
        if (soapRoles == null) {
            if (other.soapRoles != null)
                return false;
        } else if (!soapRoles.equals(other.soapRoles))
            return false;
        return true;
    }
}
