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

import java.util.HashMap;
import java.util.Map;

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
    private Map<String, ApplicationInfo> applications = new HashMap<>();
    private ApplicationInfo currentApp = null;
    
    // Thread safety: writes to this field must be synchronized on this
    private volatile OpenAPIProvider currentProvider = null;

    /**
     * The addApplication method is invoked by the {@link ApplicationListener} when it is notified that an application
     * is starting. It only needs to process the application if we have not already found an application that implements
     * a JAX-RS based REST API. 
     * 
     * @param newAppInfo
     *           The ApplicationInfo for the application that is starting.
     */
    public void addApplication(ApplicationInfo newAppInfo) {
        synchronized (this) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Adding application started: appInfo=" + newAppInfo);
            }

            // Store the app in our collection
            applications.put(newAppInfo.getName(), newAppInfo);

            // Process the application... this will only scan the application if we do not have a current application
            processApplication(newAppInfo);

            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Adding application ended: appInfo=" + newAppInfo);
            }
        }
    }
    
    /**
     * The removeApplication method is invoked by the {@link ApplicationListener} when it is notified that an
     * application is stopping.  If the application that is stopping is also the currentApp, we need to iterate over the
     * remaining applications in the collection to find the next one that implements a JAX-RS based REST API, if any.
     * 
     * @param removedAppInfo
     *           The ApplicationInfo for the application that is stopping.
     */
    public void removeApplication(ApplicationInfo removedAppInfo) {
        synchronized(this) {
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Removing application started: appInfo=" + removedAppInfo);
            }
            
            // Remove the app from our collection
            applications.remove(removedAppInfo.getName());
            
            // Check to see if the application being remove is the currentApp
            if (currentApp != null && currentApp.getName().equals(removedAppInfo.getName())) {
                
                // The currentApp is being removed... see if any of the other applications implement a JAX-RS REST API
                currentApp = null;
                currentProvider = null;
                for (ApplicationInfo appInfo : applications.values()) {
                    processApplication(appInfo);
                    if (currentApp != null) {
                        break;
                    }
                } // FOR
            }
            
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Application Processor: Removing application ended: appInfo=" + removedAppInfo);
            }
        }
    }

    /**
     * The getCurrentOpenAPIProvider method returns the OpenAPIProvider for the current application, if any.
     * 
     * @return OpenAPIProvider
     *          The current OpenAPIProvider.
     */
    public OpenAPIProvider getCurrentOpenAPIProvider() {
        return currentProvider;
    }

    /**
     * The processApplication method checks to see if we have already found an application that implements
     * a JAX-RS based REST API. If not, it attempts to proces the specified application, it it exposes a JAX-RS REST
     * API, it sets it as the currentApp. 
     *
     * @param appInfo
     *          The ApplicationInfo for the application to be processed.
     */
    private void processApplication(ApplicationInfo appInfo) {
        // Only scan the application if we do not have a current application
        if (currentApp == null) {
            currentProvider = applicationProcessor.processApplication(appInfo);
            if (currentProvider != null) {
                currentApp = appInfo;
            }
        }
    }

}
