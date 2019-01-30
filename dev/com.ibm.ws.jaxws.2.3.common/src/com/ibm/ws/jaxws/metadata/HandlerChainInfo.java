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

import javax.xml.namespace.QName;

/**
 *
 */
public class HandlerChainInfo implements Serializable {

    private static final long serialVersionUID = -637871918385157230L;

    private QName serviceNamePattern;

    private QName portNamePattern;

    private final List<String> protocolBindings = new ArrayList<String>();

    private final List<HandlerInfo> handlers = new ArrayList<HandlerInfo>();

    public List<String> getProtocolBindings() {
        return Collections.unmodifiableList(protocolBindings);
    }

    public boolean removeProtocolBinding(String protocolBinding) {
        return protocolBindings.remove(protocolBinding);
    }

    public boolean addProtocolBinding(String protocolBinding) {
        return protocolBindings.add(protocolBinding);
    }

    public boolean addProtocolBindings(List<String> protocolBindings) {
        return protocolBindings.addAll(protocolBindings);
    }

    public List<HandlerInfo> getHandlerInfos() {
        return Collections.unmodifiableList(handlers);
    }

    public boolean removeHandlerInfo(HandlerInfo handlerInfo) {
        return handlers.remove(handlerInfo);
    }

    public boolean addHandlerInfo(HandlerInfo handlerInfo) {
        return handlers.add(handlerInfo);
    }

    /**
     * @return the serviceNamePattern
     */
    public QName getServiceNamePattern() {
        return serviceNamePattern;
    }

    /**
     * @param serviceNamePattern the serviceNamePattern to set
     */
    public void setServiceNamePattern(QName serviceNamePattern) {
        this.serviceNamePattern = serviceNamePattern;
    }

    /**
     * @return the portNamePattern
     */
    public QName getPortNamePattern() {
        return portNamePattern;
    }

    /**
     * @param portNamePattern the portNamePattern to set
     */
    public void setPortNamePattern(QName portNamePattern) {
        this.portNamePattern = portNamePattern;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "HandlerChainInfo [serviceNamePattern=" + serviceNamePattern + ", portNamePattern=" + portNamePattern + ", protocolBindings=" + protocolBindings + ", handlers="
               + handlers + "]";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((handlers == null) ? 0 : handlers.hashCode());
        result = prime * result + ((portNamePattern == null) ? 0 : portNamePattern.hashCode());
        result = prime * result + ((protocolBindings == null) ? 0 : protocolBindings.hashCode());
        result = prime * result + ((serviceNamePattern == null) ? 0 : serviceNamePattern.hashCode());
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
        HandlerChainInfo other = (HandlerChainInfo) obj;
        if (handlers == null) {
            if (other.handlers != null)
                return false;
        } else if (!handlers.equals(other.handlers))
            return false;
        if (portNamePattern == null) {
            if (other.portNamePattern != null)
                return false;
        } else if (!portNamePattern.equals(other.portNamePattern))
            return false;
        if (protocolBindings == null) {
            if (other.protocolBindings != null)
                return false;
        } else if (!protocolBindings.equals(other.protocolBindings))
            return false;
        if (serviceNamePattern == null) {
            if (other.serviceNamePattern != null)
                return false;
        } else if (!serviceNamePattern.equals(other.serviceNamePattern))
            return false;
        return true;
    }

}
