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
package com.ibm.ws.webcontainer.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.QName;
import com.ibm.ws.javaee.dd.common.wsclient.Handler;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.dd.common.wsclient.PortComponentRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;

/**
 *
 */
public class ServiceRefImpl extends AbstractResourceGroup implements ServiceRef {

    private String serviceInterfaceName;
    
    private QName serviceQname;
    
    private String serviceRefTypeName;
    
    private String wsdlFile;
    
    private List<HandlerChain> handlerChainList;
    
    private List<Handler> handlers;
    
    private String jaxrpcMappingFile;
    
    private List<PortComponentRef> portComponentRefs;
    
    private List<Description> descriptions;
    
    private List<DisplayName> displayName;
    
    private List<Icon> icons;
    

    public ServiceRefImpl(ServiceRef serviceRef) {
        super(serviceRef);
        this.serviceInterfaceName = serviceRef.getServiceInterfaceName();
        this.serviceQname = serviceRef.getServiceQname();
        this.serviceRefTypeName = serviceRef.getServiceRefTypeName();
        this.wsdlFile = serviceRef.getWsdlFile();
        this.handlerChainList = new ArrayList<HandlerChain>(serviceRef.getHandlerChainList());
        this.handlers = new ArrayList<Handler>(serviceRef.getHandlers());
        this.jaxrpcMappingFile = serviceRef.getJaxrpcMappingFile();
        this.portComponentRefs = new ArrayList<PortComponentRef>(serviceRef.getPortComponentRefs());
        this.descriptions = new ArrayList<Description>(serviceRef.getDescriptions());
        this.displayName = new ArrayList<DisplayName>(serviceRef.getDisplayNames());
        this.icons = new ArrayList<Icon>(serviceRef.getIcons());
    }
    
    @Override
    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    @Override
    public QName getServiceQname() {
        return serviceQname;
    }

    @Override
    public String getServiceRefTypeName() {
        return serviceRefTypeName;
    }

    @Override
    public String getWsdlFile() {
       return wsdlFile;
    }
    
    @Override
    public List<HandlerChain> getHandlerChainList() {
        return Collections.unmodifiableList(handlerChainList);
    }

    @Override
    public List<Handler> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    @Override
    public String getJaxrpcMappingFile() {
        return jaxrpcMappingFile;
    }

    @Override
    public List<PortComponentRef> getPortComponentRefs() {
        return Collections.unmodifiableList(portComponentRefs);
    }

    @Override
    public List<Description> getDescriptions() {
        return Collections.unmodifiableList(descriptions);
    }
    
    @Override
    public List<DisplayName> getDisplayNames() {
        return Collections.unmodifiableList(displayName);
    }
 
    @Override
    public List<Icon> getIcons() {
        return Collections.unmodifiableList(icons);
    }

}
