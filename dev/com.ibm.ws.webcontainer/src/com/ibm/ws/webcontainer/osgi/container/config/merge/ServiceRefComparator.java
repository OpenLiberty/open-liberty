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
package com.ibm.ws.webcontainer.osgi.container.config.merge;

import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;

/**
 *
 */
public class ServiceRefComparator extends ResourceGroupComparator<ServiceRef> {

    @Override
    public boolean compare(ServiceRef o1, ServiceRef o2) {
        if (!super.compare(o1, o2)) {
            return false;
        }
        
        if (o1.getServiceInterfaceName() == null) {
            if (o2.getServiceInterfaceName() != null)
                return false;
        } else if (!o1.getServiceInterfaceName().equals(o2.getServiceInterfaceName())) {
            return false;
        }
        
        if (o1.getServiceQname() == null) {
            if (o2.getServiceQname() != null)
                return false;
        } else if (!o1.getServiceQname().equals(o2.getServiceQname())) {
            return false;
        }

        if (o1.getServiceRefTypeName() == null) {
            if (o2.getServiceRefTypeName() != null)
                return false;
        } else if (!o1.getServiceRefTypeName().equals(o2.getServiceRefTypeName())) {
            return false;
        }
        
        if (o1.getWsdlFile() == null) {
            if (o2.getWsdlFile() != null)
                return false;
        } else if (!o1.getWsdlFile().equals(o2.getWsdlFile())) {
            return false;
        }
        
        if (o1.getHandlerChainList() == null) {
            if (o2.getHandlerChainList() != null)
                return false;
        } else if (!o1.getHandlerChainList().equals(o2.getHandlerChainList())) {
            return false;
        }
        
        if (o1.getHandlers() == null) {
            if (o2.getHandlers() != null)
                return false;
        } else if (!o1.getHandlers().equals(o2.getHandlers())) {
            return false;
        }
        
        if (o1.getPortComponentRefs() == null) {
            if (o2.getPortComponentRefs() != null)
                return false;
        } else if (!o1.getPortComponentRefs().equals(o2.getPortComponentRefs())) {
            return false;
        }
        
        if (o1.getJaxrpcMappingFile() == null) {
            if (o2.getJaxrpcMappingFile() != null)
                return false;
        } else if (!o1.getJaxrpcMappingFile().equals(o2.getJaxrpcMappingFile())) {
            return false;
        }
        
        return compareDescriptions(o1.getDescriptions(), o2.getDescriptions());
    }

}
