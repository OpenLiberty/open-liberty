/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common.wsclient;

import java.util.List;

import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.QName;
import com.ibm.ws.javaee.dd.common.ResourceGroup;

/**
 * Represents &lt;service-ref>.
 */
public interface ServiceRef
                extends ResourceGroup, DescriptionGroup
{
    /**
     * @return &lt;service-interface>
     */
    String getServiceInterfaceName();

    /**
     * @return &lt;service-ref-type>, or null if unspecified
     */
    String getServiceRefTypeName();

    /**
     * @return &lt;wsdl-file>, or null if unspecified
     */
    String getWsdlFile();

    /**
     * @return &lt;jaxrpc-mapping-file>, or null if unspecified
     */
    String getJaxrpcMappingFile();

    /**
     * @return &lt;service-qname>, or null if unspecified
     */
    QName getServiceQname();

    /**
     * @return &lt;port-component-ref> as a read-only list
     */
    List<PortComponentRef> getPortComponentRefs();

    /**
     * @return &lt;handler> as a read-only list
     */
    List<Handler> getHandlers();

    /**
     * @return &lt;handler-chain> in &lt;handler-chains> as a read-only list
     */
    List<HandlerChain> getHandlerChainList();
}
