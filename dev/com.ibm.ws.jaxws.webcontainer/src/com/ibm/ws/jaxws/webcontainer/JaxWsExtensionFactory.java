/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.webcontainer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.jaxws.JaxWsConstants;
import com.ibm.ws.jaxws.endpoint.EndpointPublisher;
import com.ibm.ws.jaxws.endpoint.EndpointPublisherManager;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleInfo;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.ws.jaxws.support.JaxWsMetaDataManager;
import com.ibm.ws.jaxws.utils.JaxWsUtils;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.anno.info.InfoStore;

public class JaxWsExtensionFactory implements ExtensionFactory {

    private static final TraceComponent tc = Tr.register(JaxWsExtensionFactory.class);

    private final AtomicServiceReference<EndpointPublisherManager> endpointPublisherManagerRef = new AtomicServiceReference<EndpointPublisherManager>("endpointPublisherManager");

    private final Set<JaxWsWebAppConfigurator> jaxWsWebAppConfigurators = new CopyOnWriteArraySet<JaxWsWebAppConfigurator>();

    public void setJaxWsWebAppConfigurator(JaxWsWebAppConfigurator configurator) {
        jaxWsWebAppConfigurators.add(configurator);
    }

    public void unsetJaxWsWebAppConfigurator(JaxWsWebAppConfigurator configurator) {
        jaxWsWebAppConfigurators.remove(configurator);
    }

    public void setEndpointPublisherManager(ServiceReference<EndpointPublisherManager> ref) {
        endpointPublisherManagerRef.setReference(ref);
    }

    public void unsetEndpointPublisherManager(ServiceReference<EndpointPublisherManager> ref) {
        endpointPublisherManagerRef.setReference(null);
    }

    protected void activate(ComponentContext cc) {
        endpointPublisherManagerRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        endpointPublisherManagerRef.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public ExtensionProcessor createExtensionProcessor(IServletContext servletContext) throws Exception {

        WebModuleMetaData moduleMetaData = ((WebAppConfigExtended) (servletContext.getWebAppConfig())).getMetaData();
        JaxWsModuleMetaData jaxWsModuleMetaData = JaxWsMetaDataManager.getJaxWsModuleMetaData(moduleMetaData);

        //If jaxws-2.2 feature is enabled while the server is on the running status, WebContainer service may receive the JaxWsExtensionFactory registration
        //service before the started applications are removed, at this time, no JaxWsModuleMeta was stored in the application metadata
        //So, now we just return null, as the application will be restarted as it is configured in the jaxws feature file
        if (jaxWsModuleMetaData == null) {
            return null;
        }

        //Add WebAppInjectionInterceptor to JaxWsInstanceManager
        jaxWsModuleMetaData.getJaxWsInstanceManager().addInterceptor(new WebAppInjectionInstanceInterceptor(servletContext));

        //Get JaxWsModuleInfo
        JaxWsModuleInfo jaxWsModuleInfo = servletContext.getModuleContainer().adapt(JaxWsModuleInfo.class);

        //No WebService Implementation is found and just return null to indicate no interest on the request processing
        if (jaxWsModuleInfo == null || jaxWsModuleInfo.endpointInfoSize() == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No web service implementation bean is found in the web module, will not create web service processor");
            }
            return null;
        }

        Container publisherModuleContainer = servletContext.getModuleContainer();
        JaxWsPublisherContext publisherContext = new JaxWsPublisherContext(jaxWsModuleMetaData, publisherModuleContainer, JaxWsUtils.getWebModuleInfo(publisherModuleContainer));

        publisherContext.setAttribute(JaxWsWebContainerConstants.SERVLET_CONTEXT, servletContext);

        WebApp webApp = (WebApp) servletContext;
        publisherContext.setAttribute(JaxWsWebContainerConstants.NAMESPACE_COLLABORATOR, webApp.getCollaboratorHelper().getWebAppNameSpaceCollaborator());

        WebAnnotations webAnnotations = AnnotationsBetaHelper.getWebAnnotations(servletContext.getModuleContainer());
        InfoStore infoStore = webAnnotations.getInfoStore();

        publisherContext.setAttribute(
            JaxWsConstants.ENDPOINT_INFO_BUILDER_CONTEXT,
            new EndpointInfoBuilderContext(infoStore, servletContext.getModuleContainer()));

        // get endpoint publisher and do publish
        EndpointPublisher endpointPublisher = endpointPublisherManagerRef.getServiceWithException().getEndpointPublisher(JaxWsConstants.WEB_ENDPOINT_PUBLISHER_TYPE);

        for (EndpointInfo endpointInfo : jaxWsModuleInfo.getEndpointInfos()) {
            endpointPublisher.publish(endpointInfo, publisherContext);
        }

        for (JaxWsWebAppConfigurator jaxWsWebAppConfigurator : jaxWsWebAppConfigurators) {
            jaxWsWebAppConfigurator.configure(jaxWsModuleInfo, servletContext.getWebAppConfig());
        }

        return new JaxWsExtensionProcessor(servletContext);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public List getPatternList() {
        return Collections.emptyList();
    }
}
