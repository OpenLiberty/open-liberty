/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb;

import java.util.List;
import java.util.Set;

import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.EndpointType;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsServerMetaData;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilder;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class EJBJaxWsModuleInfoBuilderHelper {
    /**
     * build Web Service endpoint infos
     * 
     * @param endpointInfoBuilder
     * @param ctx
     * @param jaxWsServerMetaData
     * @param ejbEndpoints
     * @param jaxWsModuleInfo
     * @return
     * @throws UnableToAdaptException
     */
    static void buildEjbWebServiceEndpointInfos(EndpointInfoBuilder endpointInfoBuilder, EndpointInfoBuilderContext ctx, JaxWsServerMetaData jaxWsServerMetaData,
                                                   List<EJBEndpoint> ejbEndpoints, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {
        Set<String> presentedServices = jaxWsModuleInfo.getEndpointImplBeanClassNames();

        for (EJBEndpoint ejbEndpoint : ejbEndpoints) {
            if (!ejbEndpoint.isWebService()) {
                continue;
            }
            if (presentedServices.contains(ejbEndpoint.getClassName())) {
                continue;
            }

            String ejbName = ejbEndpoint.getJ2EEName().getComponent();
            ctx.addContextEnv(JaxWsConstants.ENV_ATTRIBUTE_ENDPOINT_BEAN_NAME, ejbName);
            EndpointInfo endpointInfo = endpointInfoBuilder.build(ctx, ejbEndpoint.getClassName(), EndpointType.EJB);
            if (endpointInfo != null) {
                jaxWsModuleInfo.addEndpointInfo(ejbEndpoint.getName(), endpointInfo);
                jaxWsServerMetaData.putEndpointNameAndJ2EENameEntry(ejbName, ejbEndpoint.getJ2EEName());
            }
        }
    }
}
