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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;

import io.openliberty.microprofile.openapi20.utils.LoggingUtils;

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

    // Thread safety: access to these fields must be synchronized on this
    private Map<String, ApplicationRecord> applications = new TreeMap<>();

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
        }

        // Process the application
        OpenAPIProvider openApiProvider = applicationProcessor.processApplication(newAppInfo);
        if (openApiProvider != null) {
            record.providers.add(openApiProvider);
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
            applications.remove(removedAppInfo.getName());

            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Removing application ended: appInfo=" + removedAppInfo);
            }
        }
    }

    /**
     * The getCurrentOpenAPIProvider method returns the OpenAPIProvider for the current application, if any.
     * 
     * @return OpenAPIProvider
     *         The current OpenAPIProvider.
     */
    public OpenAPIProvider getCurrentOpenAPIProvider() {
        synchronized (this) {
            for (ApplicationRecord record : applications.values()) {
                if (!record.providers.isEmpty()) {
                    return record.providers.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Merges the models from all configured applications and returns them.
     * 
     * @return an {@code OpenAPIProvider} holding a merged OpenAPI model or {@code null} if there are no OpenAPI models to merge
     */
    public OpenAPIProvider getMergedOpenAPIProvider() {
        List<OpenAPIProvider> providers = getProvidersToMerge();
        if (providers.isEmpty()) {
            return null;
        }
        
        return providers.get(0);
    }

    private List<OpenAPIProvider> getProvidersToMerge() {
        synchronized (this) {
            return applications.values().stream()
                               .flatMap(r -> r.providers.stream())
                               .collect(toList());
        }
    }

    private static class ApplicationRecord {
        public ApplicationRecord(ApplicationInfo info) {
            this.info = info;
        }

        @SuppressWarnings("unused")
        private ApplicationInfo info;
        private List<OpenAPIProvider> providers = new ArrayList<>();
    }

}
