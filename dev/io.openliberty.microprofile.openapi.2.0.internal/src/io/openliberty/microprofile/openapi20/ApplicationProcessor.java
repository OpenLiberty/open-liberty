/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.IndexUtils;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.utils.ModuleUtils;
import io.openliberty.microprofile.openapi20.utils.OpenAPIUtils;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.api.models.info.InfoImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

/**
 * The ApplicationProcessor class processes an application that has been deployed to the OpenLiberty instance in order
 * to generate an OpenAPI model. 
 */
public class ApplicationProcessor {

    private static final TraceComponent tc = Tr.register(ApplicationProcessor.class);

    /**
     * The processApplication method processes applications that are added to the OpenLiberty instance. 
     * 
     * @param appInfo
     *            The ApplicationInfo for the application to be processed.
     * @return OpenAPIProvider
     *            The OpenAPIProvider for the application, or null if the application is not an OAS applciation.
     */
    @FFDCIgnore(UnableToAdaptException.class)
    public static OpenAPIProvider processApplication(final ApplicationInfo appInfo) {
        
        // Create the variable to return
        OpenAPIProvider openAPIProvider = null;

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Processing application started: appInfo=" + appInfo);
        }
            
        // Make sure that we have valid application info
        if (appInfo != null) {
            
            // Get the container for the application
            Container appContainer = appInfo.getContainer();
            if (appContainer != null) {
                
                // Check for app classes, if it is not there then the app manager is not in control of this app
                try {
                    NonPersistentCache cache = appContainer.adapt(NonPersistentCache.class);
                    ApplicationClassesContainerInfo applicationClassesContainerInfo =
                        (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
                    if (applicationClassesContainerInfo != null) {

                        // Check to see if the deployed application is an EAR/EBA
                        if (appInfo instanceof EARApplicationInfo) {
                            /*
                             * Iterate over the entries in the application. An Enterprise Application can contain
                             * various types of module, including Web modules. We need to attempt to retrieve the
                             * WebModuleInfo for each entry and, if there is WebModuleInfo, process it. If this
                             * results in an OpenAPI document being generated, we do not process any more entries
                             * because we only generate a single OpenAPI document... even if the application
                             * contains multiple web modules.
                             */
                            for (Entry entry : appContainer) {
                                try {
                                    // Attempt to adapt the entry to a container
                                    Container container = entry.adapt(Container.class);
                                    if (container != null) {
                                        
                                        // Attempt to retrieve WebModuleInfo for the container
                                        WebModuleInfo webModuleInfo = ModuleUtils.getWebModuleInfo(container);
                                        if (webModuleInfo != null) {
                                            
                                            // Process the web module
                                            openAPIProvider = processWebModule(container, webModuleInfo);
                                            if (openAPIProvider != null) {
                                                Tr.info(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSED, webModuleInfo.getApplicationInfo().getDeploymentName());
                                                break;
                                            }
                                        }
                                    }
                                } catch (UnableToAdaptException e) {
                                    // Unable to adapt... log it and move on
                                    if (LoggingUtils.isEventEnabled(tc)) {
                                        Tr.event(tc, "Failed to adapt entry: entry=" + entry + " : \n" + e.getMessage());
                                    }
                                }
                            } // FOR
                        } else {
                            // Not an Enterprise Application... attempt to get the WebModuleInfo
                            WebModuleInfo webModuleInfo = ModuleUtils.getWebModuleInfo(appContainer);
                            
                            // Make sure that we have a valid web module.  If we do, process it.
                            if (webModuleInfo != null) {
                                openAPIProvider = processWebModule(appContainer, webModuleInfo);
                                if (openAPIProvider != null) {
                                    Tr.info(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSED, webModuleInfo.getApplicationInfo().getDeploymentName());
                                }
                                
                                if (LoggingUtils.isEventEnabled(tc)) {
                                    Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo);
                                }
                            } else {
                                if (LoggingUtils.isEventEnabled(tc)) {
                                    Tr.event(tc, "Application Processor: Processing application ended: moduleInfo=null : appInfo=" + appInfo);
                                }
                            }
                        }
                    } else {
                        // No application classes... the app manager is not in control of this ap
                        if (LoggingUtils.isEventEnabled(tc)) {
                            Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo + ", applicationClassesContainerInfo=null");
                        }
                    }
                } catch (UnableToAdaptException e) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Failed to adapt NonPersistentCache: container=" + appContainer + " : \n" + e.getMessage());
                    }
                }
            } else {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo + ", appContainer=null");
                }
            }
        } else {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Processing application ended: appInfo=null");
            }
        }
        
        return openAPIProvider;
    }

    /**
     * The processWebModule method attempts to generate an OpenAPIProvider for the specified web module using the
     * SmallRye implementation.
     * 
     * @param appContainer
     *            The Container for the web module
     * @param moduleInfo
     *            The WebModuleInfo object for the web module
     * @return OpenAPIProvider
     *            The OpenAPIProvider for the web module, or null if the web module is not an OAS applciation.
     */
    private static OpenAPIProvider processWebModule(final Container appContainer, final WebModuleInfo moduleInfo) {
        
        // Create the variable to return
        OpenAPIProvider openAPIProvider = null;
        
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "WebModule: Processing started : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot=" + moduleInfo.getContextRoot());
        }
        ClassLoader appClassloader = moduleInfo.getClassLoader();
        OpenAPI openAPIModel = null;
        
        // Read and process the MicroProfile config. Try with resources will close the ConfigProcessor when done.
        try (ConfigProcessor configProcessor = new ConfigProcessor(appClassloader)) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Retrieved configuration values : " + configProcessor);
            }
            
            try {
                OpenApiConfig config = configProcessor.getOpenAPIConfig();
                OpenApiDocument.INSTANCE.reset();
                OpenApiDocument.INSTANCE.config(config);
                OpenApiStaticFile staticFile = StaticFileProcessor.getOpenAPIFile(appContainer);
                OpenApiDocument.INSTANCE.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(staticFile));
                OpenApiDocument.INSTANCE.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, IndexUtils.getIndexView(moduleInfo, config)));
                OpenApiDocument.INSTANCE.modelFromReader(OpenApiProcessor.modelFromReader(config, appClassloader));
                OpenApiDocument.INSTANCE.filter(OpenApiProcessor.getFilter(config, appClassloader));
                OpenApiDocument.INSTANCE.initialize();
                
                openAPIModel =  OpenApiDocument.INSTANCE.get();
                
                /*
                 * We need to determine whether the scanned application is an OAS application at all. In order to do
                 * this we can check two things:
                 * 
                 *     1) Whether a static file was found in the application.
                 *     2) Whether the generated OpenAPI model object is a just the default generated by the SmallRye
                 *        implementation.
                 */
                if (staticFile == null && OpenAPIUtils.isDefaultOpenApiModel(openAPIModel)) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Default Open API document generated");
                    }
                    openAPIModel = null;
                }

                if (openAPIModel != null) {
                    // Add default info to the doc if none is present
                    if (openAPIModel.getInfo() == null) {
                        openAPIModel.setInfo(new InfoImpl().title(Constants.DEFAULT_OPENAPI_DOC_TITLE)
                                .version(Constants.DEFAULT_OPENAPI_DOC_VERSION));
                    }

                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Generated document: " + OpenAPIUtils.getOpenAPIDocument(openAPIModel, Format.JSON));
                    }
                    
                    // Create the OpenAPIProvider to return
                    openAPIProvider = new WebModuleOpenAPIProvider(moduleInfo, openAPIModel, OpenAPIUtils.containsServersDefinition(openAPIModel));

                    // Validate the document if the validation property has been enabled.
                    if (configProcessor.isValidating()) {
                        try {
                            if (LoggingUtils.isEventEnabled(tc)) {
                                Tr.event(tc, "Validate document");
                            }
                            OpenAPIUtils.validateDocument(openAPIModel);
                        } catch (Throwable e) {
                            if (LoggingUtils.isEventEnabled(tc)) {
                                Tr.event(tc, "Failed to call OASValidator: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "WebModule: Processing ended : Not an OAS application : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot=" + moduleInfo.getContextRoot());
                    }
                }
            } catch (Exception e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    final String message = String.format("Failed to process application %s: %s", moduleInfo.getApplicationInfo().getDeploymentName(), e.getMessage());
                    Tr.event(tc, "Failed to process application: " + message);
                }
                Tr.error(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSING_ERROR, moduleInfo.getApplicationInfo().getDeploymentName());
            }
        }

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "WebModule: Processing ended : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot=" + moduleInfo.getContextRoot());
        }
        return openAPIProvider;
    }

    private ApplicationProcessor() {
        // This class is not meant to be instantiated.
    }
}
