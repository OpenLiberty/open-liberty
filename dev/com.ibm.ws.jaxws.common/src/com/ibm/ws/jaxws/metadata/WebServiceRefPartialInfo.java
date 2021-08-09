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

import javax.jws.HandlerChain;
import javax.xml.namespace.QName;

/**
 * This class will serve to hold metadata for a service
 * ref that was gained from either a class annotated with
 * the @WebServiceClient or a co-located port component.
 * The information can include the WSDL location, the service
 * QName and the WSDL port.
 * 
 */
public class WebServiceRefPartialInfo implements Serializable {

    private static final long serialVersionUID = 4937176838792583301L;

    // if this represents a port component, the key will be the port component
    // name, but if it represents a class annotated with @WebServiceClient, the
    // key will be the fully qualified class name
    private String key;

    private String wsdlLocation;

    private QName serviceQName;

    private QName portQName;

    private String handlerChainDeclaringClassName;

    private HandlerChainAnnotationSer handlerChainAnnotation;

    public WebServiceRefPartialInfo() {};

    public WebServiceRefPartialInfo(String key, String wsdlLocation, QName serviceQName,
                                    QName wsdlPort, String handlerChainDeclaringClassName, HandlerChain handlerChainAnnotation) {
        this.key = key;
        this.wsdlLocation = wsdlLocation;
        this.serviceQName = serviceQName;
        this.portQName = wsdlPort;
        this.handlerChainDeclaringClassName = handlerChainDeclaringClassName;
        this.setHandlerChainAnnotation(handlerChainAnnotation);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getWsdlLocation() {
        return wsdlLocation;
    }

    public void setWsdlLocation(String wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    public QName getServiceQName() {
        return serviceQName;
    }

    public void setServiceQName(QName serviceQName) {
        this.serviceQName = serviceQName;
    }

    public QName getPortQName() {
        return portQName;
    }

    public void setPortQName(QName portQName) {
        this.portQName = portQName;
    }

    @Override
    public String toString() {
        return ("ServiceRefPartialInfo {key= " + key + ", wsdlLocation= " + wsdlLocation +
                ", serviceQName= " + serviceQName + ", portQName= " + portQName);
    }

    public HandlerChainAnnotationSer getHandlerChainAnnotation() {
        return handlerChainAnnotation;
    }

    public void setHandlerChainAnnotation(HandlerChain handlerChainAnnotation) {
        if (handlerChainAnnotation != null)
            this.handlerChainAnnotation = new HandlerChainAnnotationSer(handlerChainAnnotation);
    }

    public String getHandlerChainDeclaringClassName() {
        return handlerChainDeclaringClassName;
    }

    public void setHandlerChainDeclaringClassName(String handlerChainDeclaringClassName) {
        this.handlerChainDeclaringClassName = handlerChainDeclaringClassName;
    }
}
