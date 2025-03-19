/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;
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

import io.openliberty.microprofile.openapi20.internal.cache.CacheEntry;
import io.openliberty.microprofile.openapi20.internal.cache.ConfigSerializer;
import io.openliberty.microprofile.openapi20.internal.services.ConfigFieldProvider;
import io.openliberty.microprofile.openapi20.internal.services.ModelGenerator;
import io.openliberty.microprofile.openapi20.internal.services.ModuleSelectionConfig;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionConfig;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionUsageChecker;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.IndexUtils;
import io.openliberty.microprofile.openapi20.internal.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.internal.utils.ModuleUtils;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIUtils;
import io.smallrye.openapi.api.OpenApiConfig;
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

    @Reference
    private MergeDisabledAlerter mergeDisabledAlerter;

    @Reference
    private ConfigSerializer configSerializer;

    @Reference
    private ConfigFieldProvider configFieldProvider;

    @Reference
    private ModelGenerator modelGenerator;

    @Reference
    private ValidationComponent validationComponent;

    @Reference
    private volatile List<OpenAPIVersionUsageChecker> versionUsageCheckers;

    @Reference
    private OpenAPIVersionConfig versionConfig;

    /**
     * The processApplication method processes applications that are added to the OpenLiberty instance.
     *
     * @param appInfo
     *     The ApplicationInfo for the application to be processed.
     * @return OpenAPIProvider
     * The OpenAPIProvider for the application, or null if the application is not an OAS applciation.
     */
    @FFDCIgnore(ApplicationReadException.class)
    public Collection<OpenAPIProvider> processApplication(final ApplicationInfo appInfo, ModuleSelectionConfig selectionConfig) {

        // Create the variable to return
        List<OpenAPIProvider> openAPIProviders = new ArrayList<>();

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Processing application started: appInfo=" + appInfo);
        }

        try {
            Collection<ModuleClassesContainerInfo> moduleClassesContainerInfos = getModuleClassesContainerInfos(appInfo);
            for (ModuleClassesContainerInfo moduleClassesContainerInfo : moduleClassesContainerInfos) {
                ContainerInfo containerInfo = (ContainerInfo) moduleClassesContainerInfo;
                if (containerInfo.getType() != Type.WEB_MODULE) {
                    continue;
                }

                WebModuleInfo webModuleInfo = ModuleUtils.getWebModuleInfo(containerInfo.getContainer());
                if (webModuleInfo == null) {
                    continue;
                }

                if (selectionConfig.useFirstModuleOnly() && !openAPIProviders.isEmpty()) {
                    // Note, this only checks whether we've already created a provider for a module in _this_ application,
                    // but that's sufficient because ApplicationRegistry won't even call us if it's already got a provider
                    // from another application.
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "ApplicationProcessor: Ignoring module because useFirstModuleOnly is set and we already found one. module=" + webModuleInfo.getName());
                    }
                    mergeDisabledAlerter.setUsingMultiModulesWithoutConfig(openAPIProviders.get(0));

                    break; // Break here since there's no point in looking at any further modules
                }

                if (!selectionConfig.isIncluded(webModuleInfo)) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "ApplicationProcessor: Module not included by config. app=" + appInfo.getName() + " module=" + webModuleInfo.getName() + ", config = "
                                           + selectionConfig);
                    }

                    continue;
                }

                // Process the web module
                OpenAPIProvider openAPIProvider = processWebModule(containerInfo.getContainer(), webModuleInfo, moduleClassesContainerInfo);
                if (openAPIProvider != null) {
                    Tr.info(tc, MessageConstants.OPENAPI_APPLICATION_PROCESSED, webModuleInfo.getApplicationInfo().getDeploymentName());
                    openAPIProviders.add(openAPIProvider);
                }
            }
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Processing application ended: appInfo=" + appInfo);
            }
        } catch (ApplicationReadException e) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Application Processor: Processing application ended: " + e.toString(), e);
            }
        }

        return openAPIProviders;
    }

    /**
     * Extracts the ModuleClassContainerInfos for the given application
     *
     * @param appInfo the application info
     * @return the ModuleClassContainerInfos for the modules within the application
     * @throws ApplicationReadException if there is a problem reading the application which means the ModuleClassContainerInfos can't be obtained
     */
    public Collection<ModuleClassesContainerInfo> getModuleClassesContainerInfos(ApplicationInfo appInfo) throws ApplicationReadException {
        List<ModuleClassesContainerInfo> result = new ArrayList<>();
        Container appContainer = appInfo.getContainer();
        if (appContainer == null) {
            throw new ApplicationReadException("appInfo=null");
        }

        try {
            NonPersistentCache cache = appContainer.adapt(NonPersistentCache.class);
            ApplicationClassesContainerInfo applicationClassesContainerInfo = (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
            if (applicationClassesContainerInfo == null) {
                throw new ApplicationReadException("appInfo=" + appInfo + ", appContainer=null");
            }

            result.addAll(applicationClassesContainerInfo.getModuleClassesContainerInfo());
        } catch (UnableToAdaptException e) {
            throw new ApplicationReadException("Failed to adapt NonPersistentCache: container=" + appContainer + " : \n" + e.getMessage(), e);
        }
        return result;
    }

    /**
     * The processWebModule method attempts to generate an OpenAPIProvider for the specified web module using the
     * SmallRye implementation.
     *
     * @param appContainer
     *     The Container for the web module
     * @param moduleInfo
     *     The WebModuleInfo object for the web module
     * @return OpenAPIProvider
     * The OpenAPIProvider for the web module, or null if the web module is not an OAS applciation.
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
        try (ConfigProcessor configProcessor = new ConfigProcessor(appClassloader, configFieldProvider)) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Retrieved configuration values : " + configProcessor);
            }
            OpenApiConfig config = configProcessor.getOpenAPIConfig();
            String modulePathString = appContainer.getPhysicalPath();
            Path modulePath = modulePathString == null ? null : Paths.get(modulePathString);
            Path cacheDir = getCacheDir();
            // Cache entry that we're building to store later. Null if we can't cache the result.
            CacheEntry newCacheEntry = null;
            // Cache entry we loaded from disk. Null if we can't cache the result or there's no usable cached data.
            CacheEntry loadedCacheEntry = null;

            if (modulePath != null && isWar(modulePath) && cacheDir != null) {
                // The web module is a single file. We should use the cache if possible.
                newCacheEntry = CacheEntry.createNew(moduleInfo.getApplicationInfo().getDeploymentName(), cacheDir, configSerializer);
                newCacheEntry.setConfig(config);
                newCacheEntry.addDependentFile(modulePath);

                loadedCacheEntry = CacheEntry.read(moduleInfo.getApplicationInfo().getDeploymentName(), cacheDir, configSerializer);
                if (loadedCacheEntry != null && loadedCacheEntry.modelUpToDate(newCacheEntry)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Using OpenAPI model loaded from cache");
                    }

                    openAPIModel = loadedCacheEntry.getModel();
                }
            }

            if (openAPIModel == null) {
                Index index = null;
                if (!config.scanDisable()) {
                    if (loadedCacheEntry != null && loadedCacheEntry.indexUpToDate(newCacheEntry)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Using Jandex index from cache");
                        }
                        index = loadedCacheEntry.getIndex();
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(this, tc, "Building Jandex index");
                        }
                        index = IndexUtils.getIndex(moduleInfo, moduleClassesContainerInfo, config);
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Scanning for annotations invalid for the current OpenAPI version");
                }

                Index finalIndex = index;
                versionUsageCheckers.forEach(checker -> checker.checkAnnotations(finalIndex, versionConfig.getVersion()));

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Generating OpenAPI model");
                }

                openAPIModel = generateModel(config, appContainer, appClassloader, index);
                if (openAPIModel != null && newCacheEntry != null) {
                    newCacheEntry.setIndex(index);
                    newCacheEntry.setModel(openAPIModel);
                    newCacheEntry.write();
                }
            }

            if (openAPIModel != null) {
                // Add default info to the doc if none is present
                if (openAPIModel.getInfo() == null) {
                    openAPIModel.setInfo(OASFactory.createInfo().title(Constants.DEFAULT_OPENAPI_DOC_TITLE).version(Constants.DEFAULT_OPENAPI_DOC_VERSION));
                }

                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Generated document: " + OpenAPIUtils.getOpenAPIDocument(openAPIModel, Format.JSON));
                }

                // Create the OpenAPIProvider to return
                openAPIProvider = new WebModuleOpenAPIProvider(moduleInfo, openAPIModel);

                // Validate the document if the validation property has been enabled.
                if (configProcessor.isValidating()) {
                    try {
                        if (LoggingUtils.isEventEnabled(tc)) {
                            Tr.event(tc, "Validate document");
                        }
                        validationComponent.validateAndReportErrors(openAPIModel);
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

    private OpenAPI generateModel(OpenApiConfig config, Container appContainer, ClassLoader appClassloader, Index index) {
        OpenAPI openAPIModel;

        ClassLoader tccl = classLoadingService.createThreadContextClassLoader(appClassloader);
        Object oldClassLoader = THREAD_CONTEXT_ACCESSOR.pushContextClassLoaderForUnprivileged(tccl);
        try {
            openAPIModel = modelGenerator.generateModel(config, appContainer, appClassloader, tccl, index);
        } finally {
            THREAD_CONTEXT_ACCESSOR.popContextClassLoaderForUnprivileged(oldClassLoader);
            classLoadingService.destroyThreadContextClassLoader(tccl);
        }

        if (openAPIModel == null) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "No Open API document generated");
            }
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
