/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.HandlerInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.metadata.JaxWsServerMetaData;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.ejbcontainer.WSEJBHandlerResolver;

/**
 *
 */
public class EJBHandlerResolver implements WSEJBHandlerResolver {

    private static final TraceComponent tc = Tr.register(EJBHandlerResolver.class);

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.ejbcontainer.WSEJBHandlerResolver#retrieveJAXWSHandlers(com.ibm.websphere.csi.J2EEName)
     */
    @Override
    public List<Class<?>> retrieveJAXWSHandlers(J2EEName j2eeName) {

        JaxWsModuleMetaData jaxWsModuleMetaData = JaxWsMetaDataManager.getJaxWsModuleMetaData();
        if (jaxWsModuleMetaData == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to get the JaxWsModuleMetaData from current invocation context while querying EJBHandler");
            }
            return null;
        }

        JaxWsModuleInfo jaxWsModuleInfo = null;
        try {
            Container containerToAdapt = jaxWsModuleMetaData.getModuleContainer();
            jaxWsModuleInfo = containerToAdapt.adapt(JaxWsModuleInfo.class);
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }

        if (jaxWsModuleInfo == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to get the JaxWsModuleInfo from current module {0} while querying EJBHandler", jaxWsModuleMetaData.getModuleInfo().getName());
            }
            return null;
        }

        JaxWsServerMetaData jaxwsServerMetaData = jaxWsModuleMetaData.getServerMetaData();
        String endpointName = jaxwsServerMetaData.retrieveEndpointName(j2eeName);

        if (endpointName == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No endpoint with j2eeName {0} exists in module {1}", j2eeName, jaxWsModuleMetaData.getModuleInfo().getName());
            }
            return null;
        }

        EndpointInfo endpointInfo = jaxWsModuleInfo.getEndpointInfo(endpointName);

        if (endpointInfo == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No endpoint with endpoint name {0} exists in module {1}", endpointName, jaxWsModuleMetaData.getModuleInfo().getName());
            }
            return null;
        }

        ClassLoader appContextClassLoader = jaxWsModuleMetaData.getAppContextClassLoader();

        List<HandlerInfo> handlerInfos = endpointInfo.getHandlerChainsInfo().getAllHandlerInfos();
        List<Class<?>> handlerClasses = new ArrayList<Class<?>>(handlerInfos.size());
        for (HandlerInfo handlerInfo : handlerInfos) {
            String handlerClassName = handlerInfo.getHandlerClass();
            try {
                Class<?> handlerClass = appContextClassLoader.loadClass(handlerClassName);
                handlerClasses.add(handlerClass);
            } catch (ClassNotFoundException e) {
                Tr.warning(tc, "warn.could.not.find.handler", handlerClassName, e.getMessage());
            }
        }

        return handlerClasses;

    }

}
