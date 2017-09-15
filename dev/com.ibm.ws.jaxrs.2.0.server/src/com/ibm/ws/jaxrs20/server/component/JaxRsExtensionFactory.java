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
package com.ibm.ws.jaxrs20.server.component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.EndpointPublisher;
import com.ibm.ws.jaxrs20.endpoint.JaxRsPublisherContext;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.ws.jaxrs20.server.deprecated.JaxRsExtensionProcessor;
import com.ibm.ws.jaxrs20.server.deprecated.JaxRsWebAppConfigurator;
import com.ibm.ws.jaxrs20.server.internal.JaxRsServerConstants;
import com.ibm.ws.jaxrs20.support.JaxRsMetaDataManager;
import com.ibm.ws.jaxrs20.utils.JaxRsUtils;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

@Component(name = "com.ibm.ws.jaxrs20.server.extensionFactory", immediate = true, property = { "service.vendor=IBM" })
public class JaxRsExtensionFactory implements ExtensionFactory {

    private static final TraceComponent tc = Tr.register(JaxRsExtensionFactory.class);
    private final AtomicServiceReference<EndpointPublisher> endpointPublisherSR = new AtomicServiceReference<EndpointPublisher>("WebEndpointPublisher");

    private final Set<JaxRsWebAppConfigurator> jaxRsWebAppConfigurators = new CopyOnWriteArraySet<JaxRsWebAppConfigurator>();

    @Reference(name = "jaxRsWebAppConfigurator", service = JaxRsWebAppConfigurator.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setJaxRsWebAppConfigurator(JaxRsWebAppConfigurator configurator) {
        jaxRsWebAppConfigurators.add(configurator);
    }

    protected void unsetJaxRsWebAppConfigurator(JaxRsWebAppConfigurator configurator) {
        jaxRsWebAppConfigurators.remove(configurator);
    }

    @Reference(name = "WebEndpointPublisher", service = EndpointPublisher.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setWebEndpointPublisher(ServiceReference<EndpointPublisher> publisher) {
        endpointPublisherSR.setReference(publisher);
    }

    protected void unsetWebEndpointPublisher(ServiceReference<EndpointPublisher> publisher) {
        endpointPublisherSR.unsetReference(publisher);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        endpointPublisherSR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        endpointPublisherSR.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public ExtensionProcessor createExtensionProcessor(IServletContext servletContext) throws Exception {

        WebModuleMetaData moduleMetaData = ((WebAppConfigExtended) (servletContext.getWebAppConfig())).getMetaData();
        JaxRsModuleMetaData jaxRsModuleMetaData = JaxRsMetaDataManager.getJaxRsModuleMetaData(moduleMetaData);

        //If jaxrs-2.0 feature is enabled while the server is on the running status, WebContainer service may receive the JaxRsExtensionFactory registration
        //service before the started applications are removed, at this time, no JaxRsModuleMeta was stored in the application metadata
        //So, now we just return null, as the application will be restarted as it is configured in the jaxrs feature file
        if (jaxRsModuleMetaData == null) {
            return null;
        }

        //Add WebAppInjectionInterceptor to JaxWsInstanceManager
        //   jaxRsModuleMetaData.getJaxRsInstanceManager().addInterceptor(new WebAppInjectionInstanceInterceptor(servletContext));

        //Get JaxRsModuleInfo
        NonPersistentCache overlayCache = servletContext.getModuleContainer().adapt(NonPersistentCache.class);
        JaxRsModuleInfo jaxRsModuleInfo = (JaxRsModuleInfo) overlayCache.getFromCache(JaxRsModuleInfo.class);

        //No WebService Implementation is found and just return null to indicate no interest on the request processing
        if (jaxRsModuleInfo == null || jaxRsModuleInfo.endpointInfoSize() == 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No JAX-RS service is found in the web module, will not create web service processor");
            }
            return null;
        }

        Container publisherModuleContainer = servletContext.getModuleContainer();
        JaxRsPublisherContext publisherContext = new JaxRsPublisherContext(jaxRsModuleMetaData, publisherModuleContainer, JaxRsUtils.getWebModuleInfo(publisherModuleContainer));

        publisherContext.setAttribute(JaxRsServerConstants.SERVLET_CONTEXT, servletContext);

//        WebApp webApp = (WebApp) servletContext;
//        publisherContext.setAttribute(JaxRsWebContainerConstants.NAMESPACE_COLLABORATOR, webApp.getCollaboratorHelper().getWebAppNameSpaceCollaborator());

        publisherContext.setAttribute(JaxRsConstants.ENDPOINT_INFO_BUILDER_CONTEXT, new EndpointInfoBuilderContext(
                        servletContext.getModuleContainer().adapt(WebAnnotations.class).getInfoStore(),
                        servletContext.getModuleContainer()
                                      ));
        //Add collaborator to publisherContext, IBMRestServlet can get it later
        WebApp webApp = (WebApp) servletContext;
        publisherContext.setAttribute(JaxRsConstants.COLLABORATOR, webApp.getCollaboratorHelper().getWebAppNameSpaceCollaborator());

        // get endpoint publisher and do publish
//        EndpointPublisher endpointPublisher = getEndpointPublisher(JaxRsConstants.WEB_ENDPOINT_PUBLISHER_TYPE);
        EndpointPublisher endpointPublisher = endpointPublisherSR.getServiceWithException();
        for (EndpointInfo endpointInfo : jaxRsModuleInfo.getEndpointInfos()) {
            endpointPublisher.publish(endpointInfo, publisherContext);
        }

        for (JaxRsWebAppConfigurator jaxRsWebAppConfigurator : jaxRsWebAppConfigurators) {
            jaxRsWebAppConfigurator.configure(jaxRsModuleInfo, servletContext.getWebAppConfig());
        }

        return new JaxRsExtensionProcessor(servletContext);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public List getPatternList() {
        return Collections.emptyList();
    }
}
