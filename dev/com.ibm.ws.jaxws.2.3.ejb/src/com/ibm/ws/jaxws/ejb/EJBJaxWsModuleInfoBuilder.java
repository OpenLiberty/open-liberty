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
package com.ibm.ws.jaxws.ejb;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
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
import com.ibm.wsspi.adaptable.module.Container;
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

    @Override
    public ExtendedModuleInfo build(ModuleMetaData moduleMetaData, Container containerToAdapt, JaxWsModuleInfo jaxWsModuleInfo) throws UnableToAdaptException {

        EndpointInfoBuilder endpointInfoBuilder = endpointInfoBuilderSRRef.getService();
        if (endpointInfoBuilder == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to locate EndpointInfoBuilder, EJB JaxWsModuleInfo builder will be skipped");
            }
            return null;
        }

        EJBEndpoints ejbEndpoints = containerToAdapt.adapt(EJBEndpoints.class);
        if (ejbEndpoints == null || ejbEndpoints.getEJBEndpoints().size() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No EJB Web Services is found");
            }
            return null;
        }

        ModuleAnnotations moduleAnnotations = AnnotationsBetaHelper.getModuleAnnotations(containerToAdapt); // throws UnableToAdaptException
        InfoStore infoStore = moduleAnnotations.getInfoStore(); // throws UnableToAdaptException

        try {
            infoStore.open(); // throws InfoStoreException
        } catch (InfoStoreException e) {
            throw new IllegalStateException(e);
        }

        try {
            JaxWsServerMetaData jaxWsServerMetaData = JaxWsMetaDataManager.getJaxWsServerMetaData(moduleMetaData);

            EndpointInfoBuilderContext endpointInfoBuilderContext = new EndpointInfoBuilderContext(infoStore, containerToAdapt);

            EJBJaxWsModuleInfoBuilderHelper.buildEjbWebServiceEndpointInfos(endpointInfoBuilder, endpointInfoBuilderContext, jaxWsServerMetaData, ejbEndpoints.getEJBEndpoints(),
                                                                            jaxWsModuleInfo);

            if (jaxWsModuleInfo.getEndpointInfos() == null || jaxWsModuleInfo.getEndpointInfos().isEmpty()) {
                return null;
            }

            JaxWsModuleInfoBuilderContext jaxWsModuleInfoBuilderContext = new JaxWsModuleInfoBuilderContext(moduleMetaData, containerToAdapt, endpointInfoBuilderContext);
            // call the extensions to extra build the jaxWsModuleInfo, eg: set context-root, security.
            for (JaxWsModuleInfoBuilderExtension extension : extensions) {
                extension.postBuild(jaxWsModuleInfoBuilderContext, jaxWsModuleInfo);
            }

            return createWebRouterModule(containerToAdapt, jaxWsModuleInfo.getContextRoot());

        } finally {
            try {
                infoStore.close();
            } catch (InfoStoreException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    protected ExtendedModuleInfo createWebRouterModule(Container containerToAdapt, String contextRoot) throws UnableToAdaptException {
        NonPersistentCache overlayCache = containerToAdapt.adapt(NonPersistentCache.class);
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
}
