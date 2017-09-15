/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.container.config;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.ServletConfiguratorHelperFactory;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.AdapterFactoryService;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

@Component(service = ContainerAdapter.class, immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {"service.vendor=IBM",
                       "toType=com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration" } )
public class WebAppConfigurationAdapter implements ContainerAdapter<WebAppConfiguration>
{
    @SuppressWarnings("unused")
    private static final TraceComponent tc =
        Tr.register(WebAppConfigurationAdapter.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    // Service refs ... resolved using values from the bindings files.
    
    private final AtomicServiceReference<ResourceRefConfigFactory> resourceRefConfigFactorySRRef =
        new AtomicServiceReference<ResourceRefConfigFactory>("resourceRefConfigFactory");

    private final ConcurrentServiceReferenceSet<ServletConfiguratorHelperFactory> servletConfiguratorHelperFactories =
        new ConcurrentServiceReferenceSet<ServletConfiguratorHelperFactory>("servletConfiguratorHelperFactory");

    //

    public WebAppConfigurationAdapter() {
        // Deliberately empty
    }

    /**
     * Obtain a web application configuration from a container.
     * 
     * Processing is through {@link WebAppConfigurator}: Create a configurator,
     * install helpers in the configurator using {@link WebAppConfigurator#addHelper(ServletConfiguratorHelper)},
     * then process the configuration using {@link WebAppConfigurator#configure()}.
     * 
     * The configurator places the web application configuration in the overlay cache.
     * After processing the configuration, retrieve and return that configuration.
     * 
     * Throw an exception if the web application configuration was not saved to the cache.
     * 
     * The adapt call usually causes one or two side effects: The web application
     * configuration is stored to the overlay cache, and web module annotations may
     * be stored to the overlay cache.
     * 
     * @param root The parent of the target container.  Currently unused.
     * @param rootOverlay The root overlay container.  Currently unused.
     * @param artifactContainer The artifact container underly the target container.
     *     Currently unused.
     * @param containerToAdapt The container for which to obtain a web application configuration.
     * 
     * @return WebAppConfiguration The web application configuration for the container.
     * 
     * @throws UnableToAdaptException Thrown in case of an error during processing, or
     *     if the configuration was not made available by processing.
     */
    @Override
    public WebAppConfiguration adapt(Container root,
                                     OverlayContainer rootOverlay,
                                     ArtifactContainer artifactContainer,
                                     Container containerToAdapt) throws UnableToAdaptException {
    
        NonPersistentCache overlayCache = containerToAdapt.adapt(NonPersistentCache.class);

        WebModuleInfo moduleInfo = (WebModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
        if(moduleInfo == null){
            throw new UnableToAdaptException("Container is not a Web Module");
        }
        
        WebAppConfigurator webAppConfigurator =
            new WebAppConfigurator(containerToAdapt, overlayCache,
                                   resourceRefConfigFactorySRRef.getService());
        
        for (ServletConfiguratorHelperFactory configHelperFactory : servletConfiguratorHelperFactories.services()) {
            ServletConfiguratorHelper configHelper = configHelperFactory.createConfiguratorHelper(webAppConfigurator);
            webAppConfigurator.addHelper(configHelper);
        }
        webAppConfigurator.configure();

        WebAppConfiguration appConfig = (WebAppConfiguration) overlayCache.getFromCache(WebAppConfig.class);
        if (appConfig == null) {
            throw new UnableToAdaptException("WebAppConfiguration=null");
        }
        return appConfig;
    }

    /**
     * DS method to activate this component.
     */
    @Activate
    protected void activate(ComponentContext context)
    {
        resourceRefConfigFactorySRRef.activate(context);
        servletConfiguratorHelperFactories.activate(context);
    }

    /**
     * DS method to deactivate this component.
     */
    @Deactivate
    protected void deactivate(ComponentContext context)
    {
        resourceRefConfigFactorySRRef.deactivate(context);
        servletConfiguratorHelperFactories.deactivate(context);
    }

    @Reference(name = "resourceRefConfigFactory", service = ResourceRefConfigFactory.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setResourceRefConfigFactory(ServiceReference<ResourceRefConfigFactory> ref)
    {
        resourceRefConfigFactorySRRef.setReference(ref);
    }

    protected void unsetResourceRefConfigFactory(ServiceReference<ResourceRefConfigFactory> ref)
    {
        resourceRefConfigFactorySRRef.unsetReference(ref);
    }

    @Reference(name = "servletConfiguratorHelperFactory", service = ServletConfiguratorHelperFactory.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setServletConfiguratorHelperFactory(ServiceReference<ServletConfiguratorHelperFactory> ref)
    {
        servletConfiguratorHelperFactories.addReference(ref);
    }

    protected void unsetServletConfiguratorHelperFactory(ServiceReference<ServletConfiguratorHelperFactory> ref)
    {
        servletConfiguratorHelperFactories.removeReference(ref);
    }

    @Reference(service = AdapterFactoryService.class,
               target = "(&" +
                            "(containerToType=com.ibm.ws.container.service.annotations.WebAnnotations)" +
                            "(containerToType=com.ibm.ws.container.service.config.WebFragmentsInfo)" +
                            "(containerToType=com.ibm.ws.javaee.dd.web.WebApp)" +
                            "(containerToType=com.ibm.ws.javaee.dd.web.WebFragment)" +
                            "(containerToType=com.ibm.ws.javaee.dd.webbnd.WebBnd)" +
                            "(containerToType=com.ibm.ws.javaee.dd.webext.WebExt)" +
                        ")")
    protected void setAdaptorFactoryService(AdapterFactoryService afs) { }
    protected void unsetAdaptorFactoryService(AdapterFactoryService afs) { }
}
