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

import java.awt.Container;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleType;
import com.ibm.ws.jaxws.metadata.JaxWsServerMetaData;
import com.ibm.ws.jaxws.metadata.builder.AbstractJaxWsModuleInfoBuilder;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilder;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.ws.jaxws.metadata.builder.JaxWsModuleInfoBuilderContext;
import com.ibm.ws.jaxws.metadata.builder.JaxWsModuleInfoBuilderExtension;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.ws.jaxws.support.JaxWsWebContainerManager;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Adaptable;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class EJBJaxWsModuleInfoBuilder extends AbstractJaxWsModuleInfoBuilder {

    private static final TraceComponent tc = Tr.register(EJBJaxWsModuleInfoBuilder.class);

    private final AtomicServiceReference<JaxWsWebContainerManager> jaxWsWebContainerManagerRef = new AtomicServiceReference<JaxWsWebContainerManager>("jaxWsWebContainerManager");

    public EJBJaxWsModuleInfoBuilder() {
        super(JaxWsModuleType.EJB);
    }

    public ExtendedModuleInfo build(ModuleMetaData moduleMetaData, Container containerToAdapt, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {

        EndpointInfoBuilder endpointInfoBuilder = endpointInfoBuilderSRRef.getService();
        if (endpointInfoBuilder == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to locate EndpointInfoBuilder, EJB JaxWsModuleInfo builder will be skipped");
            }
            return null;
        }

        EJBEndpoints ejbEndpoints = ((Adaptable) containerToAdapt).adapt(EJBEndpoints.class);
        if (ejbEndpoints == null || ejbEndpoints.getEJBEndpoints().size() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No EJB Web Services is found");
            }
            return null;
        }

        InfoStore infoStore = ((Adaptable) containerToAdapt).adapt(ModuleAnnotations.class).getInfoStore();
        try {
            infoStore.open();
            JaxWsServerMetaData jaxWsServerMetaData = JaxWsMetaDataManager.getJaxWsServerMetaData(moduleMetaData);

            EndpointInfoBuilderContext endpointInfoBuilderContext = new EndpointInfoBuilderContext(infoStore, (com.ibm.wsspi.adaptable.module.Container) containerToAdapt);

            EJBJaxWsModuleInfoBuilderHelper.buildEjbWebServiceEndpointInfos(endpointInfoBuilder, endpointInfoBuilderContext, jaxWsServerMetaData, ejbEndpoints.getEJBEndpoints(),
                                                                            jaxWsModuleInfo);

            if (jaxWsModuleInfo.getEndpointInfos() == null || jaxWsModuleInfo.getEndpointInfos().isEmpty()) {
                return null;
            }

            JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext = new JaxWsModuleInfoBuilderContext(moduleMetaData, (com.ibm.wsspi.adaptable.module.Container) containerToAdapt, endpointInfoBuilderContext);
            // call the extensions to extra build the jaxWsModuleInfo, eg: set context-root, security.
            for (JaxWsModuleInfoBuilderExtension extension : extensions) {
                extension.postBuild(jaxWsModuleInfoBuilderContext, jaxWsModuleInfo);
            }

            return createWebRouterModule(containerToAdapt, jaxWsModuleInfo.getContextRoot());

        } catch (InfoStoreException e) {
            throw new IllegalStateException(e);
        } finally {
            if (infoStore != null) {
                try {
                    infoStore.close();
                } catch (InfoStoreException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    protected ExtendedModuleInfo createWebRouterModule(Container containerToAdapt, String contextRoot) throws UnableToAdaptException {
        NonPersistentCache overlayCache = ((Adaptable) containerToAdapt).adapt(NonPersistentCache.class);
        ExtendedModuleInfo ejbModuleInfo = (ExtendedModuleInfo) overlayCache.getFromCache(EJBModuleInfo.class);
        JaxWsWebContainerManager jaxWsWebContainerManager = jaxWsWebContainerManagerRef.getService();
        if (jaxWsWebContainerManager == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No JaxWsWebContainerManager service is located, unable to publish the EJB endpoints to web container");
            }
            return null;
        }
        return jaxWsWebContainerManager.createWebModuleInfo(ejbModuleInfo, contextRoot);
    }

    @Override
    protected void activate(ComponentContext cc) {
        super.activate(cc);
        jaxWsWebContainerManagerRef.activate(cc);
    }

    @Override
    protected void deactivate(ComponentContext cc) {
        jaxWsWebContainerManagerRef.deactivate(cc);
        super.deactivate(cc);
    }

    protected void setJaxWsWebContainerManager(ServiceReference<JaxWsWebContainerManager> ref) {
        jaxWsWebContainerManagerRef.setReference(ref);
    }

    protected void unsetJaxWsWebContainerManager(ServiceReference<JaxWsWebContainerManager> ref) {
        jaxWsWebContainerManagerRef.unsetReference(ref);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.metadata.builder.JaxWsModuleInfoBuilder#build(com.ibm.ws.runtime.metadata.ModuleMetaData, com.ibm.wsspi.adaptable.module.Container,
     * com.ibm.ws.jaxws.metadata.JaxWsModuleInfo)
     */
    @Override
    public ExtendedModuleInfo build(ModuleMetaData moduleMetaData, com.ibm.wsspi.adaptable.module.Container containerToAdapt,
                                    JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {
        // TODO Auto-generated method stub
        return null;
    }
}
