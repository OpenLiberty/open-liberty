/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;

import io.openliberty.microprofile.openapi20.cache.CacheEntry;
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
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = ApplicationProcessor.class)
public class ApplicationProcessor {

    private static final TraceComponent tc = Tr.register(ApplicationProcessor.class);
    
    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());
    
    @Reference
    private ClassLoadingService classLoadingService;

    /**
     * The processApplication method processes applications that are added to the OpenLiberty instance.
     * 
     * @param appInfo
     *            The ApplicationInfo for the application to be processed.
     * @return OpenAPIProvider
     *         The OpenAPIProvider for the application, or null if the application is not an OAS applciation.
     */
    @FFDCIgnore(UnableToAdaptException.class)
    public OpenAPIProvider processApplication(final ApplicationInfo appInfo) {

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
                    ApplicationClassesContainerInfo applicationClassesContainerInfo = (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
                    if (applicationClassesContainerInfo != null) {
                        
                        for (ModuleClassesContainerInfo moduleClassesContainerInfo : applicationClassesContainerInfo.getModuleClassesContainerInfo()) {
                            ContainerInfo containerInfo = (ContainerInfo) moduleClassesContainerInfo;
                            if (containerInfo.getType() != Type.WEB_MODULE) {
                                continue;
                            }
                            
                            WebModuleInfo webModuleInfo = ModuleUtils.getWebModuleInfo(containerInfo.getContainer());
                            if (webModuleInfo == null) {
                                continue;
                            }
                            
                            // Process the web module
                            openAPIProvider = processWebModule(containerInfo.getContainer(), webModuleInfo, moduleClassesContainerInfo);
                            if (openAPIProvider != null) {
                                Tr.info(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSED, webModuleInfo.getApplicationInfo().getDeploymentName());
                                break;
                            }
                        }
                        if (LoggingUtils.isEventEnabled(tc)) {
                            Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo);
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
     *         The OpenAPIProvider for the web module, or null if the web module is not an OAS applciation.
     */
    private OpenAPIProvider processWebModule(final Container appContainer, final WebModuleInfo moduleInfo, final ModuleClassesContainerInfo moduleClassesContainerInfo) {

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
            CacheEntry newCacheEntry = null;
            OpenApiConfig config = configProcessor.getOpenAPIConfig();
            String modulePathString = appContainer.getPhysicalPath();
            Path modulePath = modulePathString == null ? null : Paths.get(modulePathString);
            Path cacheDir = getCacheDir();

            if (modulePath != null && isWar(modulePath) && cacheDir != null) {
                // The web module is a single file. We should use the cache if possible.
                newCacheEntry = CacheEntry.createNew(moduleInfo.getApplicationInfo().getDeploymentName(), cacheDir);
                newCacheEntry.setConfig(config);
                newCacheEntry.addDependentFile(modulePath);

                CacheEntry loadedCacheEntry = CacheEntry.read(moduleInfo.getApplicationInfo().getDeploymentName(), cacheDir);
                if (loadedCacheEntry != null && loadedCacheEntry.isUpToDateWith(newCacheEntry)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Using OpenAPI model loaded from cache");
                    }

                    openAPIModel = loadedCacheEntry.getModel();
                }
            }

            if (openAPIModel == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Generating OpenAPI model");
                }

                openAPIModel = generateModel(config, appContainer, moduleInfo, moduleClassesContainerInfo, appClassloader);
                if (openAPIModel != null && newCacheEntry != null) {
                    newCacheEntry.setModel(openAPIModel);
                    newCacheEntry.write();
                }
            }

            if (openAPIModel != null) {
                // Add default info to the doc if none is present
                if (openAPIModel.getInfo() == null) {
                    openAPIModel.setInfo(new InfoImpl().title(Constants.DEFAULT_OPENAPI_DOC_TITLE).version(Constants.DEFAULT_OPENAPI_DOC_VERSION));
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
                    Tr.event(tc, "WebModule: Processing ended : Not an OAS application : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot="
                                 + moduleInfo.getContextRoot());
                }
            }
        } catch (Exception e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                final String message = String.format("Failed to process application %s: %s", moduleInfo.getApplicationInfo().getDeploymentName(), e.getMessage());
                Tr.event(tc, "Failed to process application: " + message);
            }
            Tr.error(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSING_ERROR, moduleInfo.getApplicationInfo().getDeploymentName(), e.toString());
        }

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "WebModule: Processing ended : deploymentName=" + moduleInfo.getApplicationInfo().getDeploymentName() + " : contextRoot=" + moduleInfo.getContextRoot());
        }
        return openAPIProvider;
    }

    private OpenAPI generateModel(OpenApiConfig config, Container appContainer, WebModuleInfo moduleInfo, ModuleClassesContainerInfo moduleClassesContainerInfo, ClassLoader appClassloader) {
        OpenAPI openAPIModel;
        OpenApiDocument.INSTANCE.reset();
        OpenApiDocument.INSTANCE.config(config);
        
        ClassLoader tccl = classLoadingService.createThreadContextClassLoader(appClassloader);
        Object oldClassLoader = THREAD_CONTEXT_ACCESSOR.pushContextClassLoaderForUnprivileged(tccl);
        try {
            OpenApiStaticFile staticFile = StaticFileProcessor.getOpenAPIFile(appContainer);
            OpenApiDocument.INSTANCE.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(staticFile));
            OpenApiDocument.INSTANCE.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, IndexUtils.getIndexView(moduleInfo, moduleClassesContainerInfo, config)));
            OpenApiDocument.INSTANCE.modelFromReader(OpenApiProcessor.modelFromReader(config, appClassloader));
            OpenApiDocument.INSTANCE.filter(OpenApiProcessor.getFilter(config, appClassloader));
            OpenApiDocument.INSTANCE.initialize();

            openAPIModel = OpenApiDocument.INSTANCE.get();

            /*
             * We need to determine whether the scanned application is an OAS application at all. In order to do
             * this we can check two things:
             * 
             * 1) Whether a static file was found in the application.
             * 2) Whether the generated OpenAPI model object is a just the default generated by the SmallRye
             * implementation.
             */
            if (staticFile == null && OpenAPIUtils.isDefaultOpenApiModel(openAPIModel)) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Default Open API document generated");
                }
                openAPIModel = null;
            }

        } finally {
            THREAD_CONTEXT_ACCESSOR.popContextClassLoaderForUnprivileged(oldClassLoader);
            classLoadingService.destroyThreadContextClassLoader(tccl);
        }

        return openAPIModel;
    }

    private static boolean isWar(Path path) {
        return path.getFileName().toString().endsWith(".war") && Files.isRegularFile(path);
    }

    private static Path getCacheDir() {
        Bundle bundle = FrameworkUtil.getBundle(ApplicationProcessor.class);
        if (bundle == null) {
            // Not in OSGi, shouldn't happen at runtime
            return null;
        }

        File cacheFile = bundle.getDataFile("");
        if (cacheFile == null) {
            // Shouldn't happen
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "No support from OSGi for caching");
            }
            return null;
        }

        return cacheFile.toPath();
    }

}
