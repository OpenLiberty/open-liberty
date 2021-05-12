/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.microprofile.config.ConfigProvider;
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

import io.openliberty.microprofile.openapi20.merge.MergeProcessor;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.utils.ModuleUtils;

/**
 * The ApplicationRegistry class maintains a collection of the applications that are deployed to the OpenLiberty
 * instance. It also tracks the application whose OpenAPI document is currently being returned to clients that request
 * it via the /openapi endpoint.
 * 
 * NOTE: The MP OpenAPI functionality in OpenLiberty only supports generating an OpenAPI model for a single application
 *       at a time so, if multiple applications are deployed to the OpenLiberty instance, an OpenAPI document will only
 *       be generated for the first application that is processed. Also, if an enterprise application (EAR/EBA) is
 *       deployed that contains multiple web modules, an OpenAPI document will only be generated for the first Web
 *       Module that generates an OpenAPI document.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = ApplicationRegistry.class)
public class ApplicationRegistry {

    private static final TraceComponent tc = Tr.register(ApplicationRegistry.class);

    @Reference
    private ApplicationProcessor applicationProcessor;
    
    @Reference
    private MergeDisabledAlerter mergeDisabledAlerter;
    
    // Thread safety: access to these fields must be synchronized on this
    private Map<String, ApplicationRecord> applications = new LinkedHashMap<>(); // Linked map retains order in which applications were added

    private ModuleSelectionConfig moduleSelectionConfig = ModuleSelectionConfig.fromConfig(ConfigProvider.getConfig(ApplicationRegistry.class.getClassLoader()));
    
    private OpenAPIProvider cachedProvider = null;
    
    /**
     * The addApplication method is invoked by the {@link ApplicationListener} when it is notified that an application
     * is starting. It only needs to process the application if we have not already found an application that implements
     * a JAX-RS based REST API.
     * 
     * @param newAppInfo
     *            The ApplicationInfo for the application that is starting.
     */
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
                for (OpenAPIProvider openApiProvider : openApiProviders) {
                    record.providers.add(openApiProvider);
                }
                
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
     *            The ApplicationInfo for the application that is stopping.
     */
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
                
                if (moduleSelectionConfig.useFirstModuleOnly()) {
                    if (LoggingUtils.isEventEnabled(tc)) {
                        Tr.event(this, tc, "Application Processor: Current OpenAPI application removed, looking for another application to document.");
                    }

                    // We just removed the module used for the OpenAPI document, we need to find a new module to use if there is one
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
                                                                      .map((mcci) -> (ContainerInfo) mcci)
                                                                      .filter(c -> c.getType() == Type.WEB_MODULE)
                                                                      .map(ContainerInfo::getContainer)
                                                                      .map(ModuleUtils::getWebModuleInfo)
                                                                      .filter(Objects::nonNull)
                                                                      .forEach(modules::add);
                } catch (ApplicationReadException e) {
                    // Couldn't read this application for some reason, but that means we can't have been able to include modules from it anyway.
                }
            }
            for (String unmatchedInclude : moduleSelectionConfig.findIncludesNotMatchingAnything(modules)) {
                Tr.warning(tc, MessageConstants.OPENAPI_MERGE_UNUSED_INCLUDE_CWWKO1667W, Constants.MERGE_INCLUDE_CONFIG, unmatchedInclude);
            }
        }
    }
    
    protected void unsetServerStartPhase2(ServerStartedPhase2 event) {
        
    }

    /**
     * Finds the models from all configured applications, merges them if necessary and returns them.
     * 
     * @return an {@code OpenAPIProvider} holding an OpenAPI model or {@code null} if there are no OpenAPI providers
     */
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
                OpenAPIProvider mergedProvider = MergeProcessor.mergeDocuments(providers);
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

    private static class ApplicationRecord {
        public ApplicationRecord(ApplicationInfo info) {
            this.info = info;
        }

        private ApplicationInfo info;
        private List<OpenAPIProvider> providers = new ArrayList<>();
    }

}
