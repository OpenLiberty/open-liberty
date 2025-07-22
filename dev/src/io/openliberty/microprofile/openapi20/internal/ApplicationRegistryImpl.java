/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIAppConfigProvider;
import io.openliberty.microprofile.openapi20.internal.services.ApplicationRegistry;
import io.openliberty.microprofile.openapi20.internal.services.MergeProcessor;
import io.openliberty.microprofile.openapi20.internal.services.ModuleSelectionConfig;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.internal.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.internal.utils.ModuleUtils;

/**
 * The {@code ApplicationRegistry} maintains a collection of the applications that are deployed to the OpenLiberty
 * instance. It also tracks the application whose OpenAPI document is currently being returned to clients that request
 * it via the /openapi endpoint.
 * <p>
 * OpenAPI documentation is generated for each web module and then merged together if merging is enabled. If merging is not enabled,
 * then documentation is only generated for the first web module found.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ApplicationRegistryImpl implements ApplicationRegistry, OpenAPIAppConfigProvider.OpenAPIAppConfigListener {

    private static final TraceComponent tc = Tr.register(ApplicationRegistryImpl.class);

    private final ApplicationProcessor applicationProcessor;

    private final MergeDisabledAlerter mergeDisabledAlerter;

    private final MergeProcessor mergeProcessor;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "unbindAppConfigListener")
    public void bindAppConfigListener(OpenAPIAppConfigProvider openAPIAppConfigProvider) {
        openAPIAppConfigProvider.registerAppConfigListener(this);
    }

    public void unbindAppConfigListener(OpenAPIAppConfigProvider openAPIAppConfigProvider) {
        openAPIAppConfigProvider.unregisterAppConfigListener(this);
    }

    // Thread safety: access to these fields must be synchronized on this
    private Map<String, ApplicationRecord> applications = new LinkedHashMap<>(); // Linked map retains order in which applications were added

    private OpenAPIProvider cachedProvider = null;

    private final ModuleSelectionConfig moduleSelectionConfig;

    @Activate
    public ApplicationRegistryImpl(@Reference ApplicationProcessor applicationProcessor,
                                   @Reference MergeDisabledAlerter mergeDisabledAlerter,
                                   @Reference MergeProcessor mergeProcessor,
                                   @Reference ModuleSelectionConfig moduleSelectionConfig) {
        this.applicationProcessor = applicationProcessor;
        this.mergeDisabledAlerter = mergeDisabledAlerter;
        this.mergeProcessor = mergeProcessor;
        this.moduleSelectionConfig = moduleSelectionConfig;
    }

    /**
     * The addApplication method is invoked by the {@link ApplicationListener} when it is notified that an application
     * is starting. It only needs to process the application if we have not already found an application that implements
     * a JAX-RS based REST API.
     *
     * @param newAppInfo
     *     The ApplicationInfo for the application that is starting.
     */
    @Override
    public void addApplication(ApplicationInfo newAppInfo) {
        ApplicationRecord record = new ApplicationRecord(newAppInfo);
        synchronized (this) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Adding application started: appInfo=" + newAppInfo);
            }

            // Store the app in our collection
            applications.put(newAppInfo.getName(), record);

            OpenAPIProvider firstProvider = getFirstProvider();

            if (moduleSelectionConfig.useFirstModuleOnly() && firstProvider != null) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(this, tc, "Application Processor: useFirstModuleOnly is configured and we already have a module. Not processing. appInfo=" + newAppInfo);
                }
                mergeDisabledAlerter.setUsingMultiModulesWithoutConfig(firstProvider);
            } else {
                Collection<OpenAPIProvider> openApiProviders = applicationProcessor.processApplication(newAppInfo, moduleSelectionConfig);

                if (!openApiProviders.isEmpty()) {
                    // If the new application has any providers, invalidate the model cache
                    cachedProvider = null;
                }
                record.providers.addAll(openApiProviders);

            }
        }

        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Application Processor: Adding application ended: appInfo=" + newAppInfo);
        }
    }

    /**
     * The removeApplication method is invoked by the {@link ApplicationListener} when it is notified that an
     * application is stopping. If the application that is stopping is also the currentApp, we need to iterate over the
     * remaining applications in the collection to find the next one that implements a JAX-RS based REST API, if any.
     *
     * @param removedAppInfo
     *     The ApplicationInfo for the application that is stopping.
     */
    @Override
    public void removeApplication(ApplicationInfo removedAppInfo) {
        synchronized (this) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Removing application started: appInfo=" + removedAppInfo);
            }

            // Remove the app from our collection
            ApplicationRecord removedRecord = applications.remove(removedAppInfo.getName());

            if (!removedRecord.providers.isEmpty()) {
                // If the removed application had any providers, invalidate the provider cache
                cachedProvider = null;

                if (moduleSelectionConfig.useFirstModuleOnly() && !FrameworkState.isStopping()) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Application Processor: Current OpenAPI application removed, looking for another application to document.");
                    }

                    // We just removed the module used for the OpenAPI document and the server is not shutting down.
                    // We need to find a new module to use if there is one
                    for (ApplicationRecord app : applications.values()) {
                        Collection<OpenAPIProvider> providers = applicationProcessor.processApplication(app.info, moduleSelectionConfig);
                        if (!providers.isEmpty()) {
                            app.providers.addAll(providers);
                            break;
                        }
                    }
                }
            }

            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Removing application ended: appInfo=" + removedAppInfo);
            }
        }
    }

    /**
     * This reference should be fulfilled once the server has attempted to start all applications during startup.
     * <p>
     * At this point, check whether there are any entries in the config which don't match any deployed applications.
     *
     * @param event ignored
     */
    @FFDCIgnore(ApplicationReadException.class)
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setServerStartPhase2(ServerStartedPhase2 event) {
        synchronized (this) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Checking for unused configuration entries");
            }

            List<WebModuleInfo> modules = new ArrayList<>();
            for (ApplicationRecord record : applications.values()) {
                try {
                    applicationProcessor.getModuleClassesContainerInfos(record.info)
                                        .stream()
                                        .map(mcci -> (ContainerInfo) mcci)
                                        .filter(c -> c.getType() == Type.WEB_MODULE)
                                        .map(ContainerInfo::getContainer)
                                        .map(ModuleUtils::getWebModuleInfo)
                                        .filter(Objects::nonNull)
                                        .forEach(modules::add);
                } catch (ApplicationReadException e) {
                    // Couldn't read this application for some reason, but that means we can't have been able to include modules from it anyway.
                }
            }
            moduleSelectionConfig.sendWarningsForAppsAndModulesNotMatchingAnything(modules);
        }
    }

    protected void unsetServerStartPhase2(ServerStartedPhase2 event) {

    }

    /**
     * Finds the models from all configured applications, merges them if necessary and returns them.
     *
     * @return an {@code OpenAPIProvider} holding an OpenAPI model or {@code null} if there are no OpenAPI providers
     */
    @Override
    public OpenAPIProvider getOpenAPIProvider() {
        synchronized (this) {
            if (cachedProvider != null) {
                if (LoggingUtils.isDebugEnabled(tc)) {
                    Tr.debug(this, tc, "OpenAPIProvider retrieved from cache");
                }

                return cachedProvider;
            }

            if (LoggingUtils.isDebugEnabled(tc)) {
                Tr.debug(this, tc, "Finding OpenAPIProvider");
            }

            List<OpenAPIProvider> providers = getProvidersToMerge();
            OpenAPIProvider result;
            if (providers.isEmpty()) {
                result = null;
            } else if (providers.size() == 1) {
                result = providers.get(0);
            } else {
                OpenAPIProvider mergedProvider = mergeProcessor.mergeDocuments(providers);
                if (!mergedProvider.getMergeProblems().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String problem : mergedProvider.getMergeProblems()) {
                        sb.append("\n - ");
                        sb.append(problem);
                    }
                    Tr.warning(tc, MessageConstants.OPENAPI_MERGE_PROBLEMS_CWWKO1662W, sb.toString());
                }
                result = mergedProvider;
            }

            cachedProvider = result;
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(this, tc, "Finished creating OpenAPI provider");
            }
            return result;
        }
    }

    private List<OpenAPIProvider> getProvidersToMerge() {
        synchronized (this) {
            return applications.values().stream()
                               .flatMap(r -> r.providers.stream())
                               .collect(toList());
        }
    }

    /**
     * Thread safety: Caller must hold lock on {@code this}
     *
     * @return {@code null} if {@link #applications} contains no providers
     */
    private OpenAPIProvider getFirstProvider() {
        for (Entry<String, ApplicationRecord> entry : applications.entrySet()) {
            List<OpenAPIProvider> providers = entry.getValue().providers;
            if (!providers.isEmpty()) {
                return providers.get(0);
            }
        }
        return null;
    }

    @Override
    public void processConfigUpdate() {
        synchronized (this) {
            Map<String, ApplicationRecord> oldApps = applications;
            applications = new LinkedHashMap<>();
            for (ApplicationRecord record : oldApps.values()) {
                //Add application uses config to decide if it creates and registers any providers in ApplicationInfo
                //Rather than map from the old state to the new state when the config changes, KISS and start again.
                addApplication(record.info);
            }
            cachedProvider = null;
        }
    }

    private static class ApplicationRecord {
        public ApplicationRecord(ApplicationInfo info) {
            this.info = info;
        }

        private final ApplicationInfo info;
        private final List<OpenAPIProvider> providers = new ArrayList<>();
    }

    //This is to ensure we're called after ModuleSelectionConfigImpl
    @Override
    public int getConfigListenerPriority() {
        return 2;
    }

}
