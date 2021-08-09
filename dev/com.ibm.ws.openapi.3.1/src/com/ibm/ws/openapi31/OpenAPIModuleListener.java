/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.http.VirtualHostListener;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

@Component(service = { ModuleMetaDataListener.class,
                       VirtualHostListener.class }, immediate = true, property = { "service.vendor=IBM" })

public class OpenAPIModuleListener implements ModuleMetaDataListener, VirtualHostListener {

    private static final TraceComponent tc = Tr.register(OpenAPIModuleListener.class);

    /**
     * Context used to register new APIProviders for Web modules
     */
    private ComponentContext context;

    private static final String KEY_EXECUTOR_SERVICE_REF = "executorService";
    private final AtomicServiceReference<ScheduledExecutorService> executorServiceRef = new AtomicServiceReference<ScheduledExecutorService>(KEY_EXECUTOR_SERVICE_REF);

    /**
     * Maps context roots to their corresponding web provider objects
     */
    private final ConcurrentHashMap<String, OpenAPIWebProvider> webAPIProviders = new ConcurrentHashMap<String, OpenAPIWebProvider>();

    /**
     * List of internal IBM context roots to ignore.
     *
     */
    private static final List<String> contextRootsToIgnore = new ArrayList<String>();

    static {
        //RESTHandlers will implement APIProvider themselves
        contextRootsToIgnore.add("/ibm/api");

        //UI explorer, not REST
        contextRootsToIgnore.add("/ibm/api/explorer");

        //The plan is to move the JMX connector into a rest handler, so it is also providing its own APIProvider
        contextRootsToIgnore.add("/IBMJMXConnectorREST");
    }

    private ScheduledFuture<?> scheduledWabProcessor;

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        this.context = context;
        executorServiceRef.activate(context);

        //Kick off a thread that will process any WABs that started before this bundle.  We retry with a fixed delay
        //to catch cases where this is run before the container service has had a chance to register our class as the adapter
        //for OpenAPIWebProvider, in which case container.adapt returns null.
        scheduledWabProcessor = getExecutorService().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    processWABs();
                } catch (InvalidSyntaxException e) {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Exception while processing WABs: " + e.getMessage());
                    }
                    throw new RuntimeException(e);
                }
            }
        }, 2, 1, TimeUnit.SECONDS);

        //We will cancel our scheduler from within the processWABs method once we looped and processed all existing WABs, but
        //if for some reason that processing doesn't ever finish this is a fail-safe thread that will kill the scheduler after 1 minute
        getExecutorService().schedule(new Runnable() {
            @Override
            public void run() {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Running fail-safe routine to cleanup scheduler");
                }
                cancelScheduler();
            }
        }, 60, TimeUnit.SECONDS);
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        executorServiceRef.deactivate(context);
        this.context = null;
    }

    @Reference(name = KEY_EXECUTOR_SERVICE_REF, service = ScheduledExecutorService.class)
    protected void setExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        executorServiceRef.setReference(ref);
    }

    protected void unsetExecutorService(ServiceReference<ScheduledExecutorService> ref) {
        executorServiceRef.unsetReference(ref);
    }

    /**
     * Get an instance of the ScheduledExecutorService service.
     *
     * @return the service
     * @throws IllegalStateException when the service returns as null
     */
    private ScheduledExecutorService getExecutorService() {
        ScheduledExecutorService service = executorServiceRef.getService();

        if (service == null) {
            throw new IllegalStateException(OpenAPIUtils.getOsgiServiceErrorMessage(this.getClass(), "ScheduledExecutorService"));
        }

        return service;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        if (event.getMetaData() instanceof WebModuleMetaData) {
            //Fetch context root of added module
            final String contextRoot = ((WebModuleMetaData) event.getMetaData()).getConfiguration().getContextRoot();

            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "WebModule metadata created for" +
                             " module " + event.getMetaData().getJ2EEName() +
                             " | context root " + contextRoot +
                             " | in app " + event.getMetaData().getJ2EEName().getApplication());
            }
            processModule(contextRoot, event.getContainer());
        }
    }

    private boolean processModule(String contextRoot, Container container) {
        //Ensure it is not in the ignored list
        for (String ignore : contextRootsToIgnore) {
            if (contextRoot.equals(ignore)) {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Matched ignored root: " + ignore);
                }
                return true;
            }
        }

        //Adapt into our web provider obj
        OpenAPIWebProvider webProvider = null;
        try {
            webProvider = container.adapt(OpenAPIWebProvider.class);
        } catch (UnableToAdaptException e) {
            Tr.event(tc, "Failed to adapt module: " + contextRoot);
            return true;
        }
        if (webProvider == null) {
            return true;
        }

        //Add our new web provider obj into the map, so we can add its full URL and register it into DS as an APIProvider later on
        OpenAPIWebProvider oldWebProvider = webAPIProviders.putIfAbsent(contextRoot, webProvider);

        if (oldWebProvider != null) {
            webProvider = oldWebProvider;
        }

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Added into the map {" + contextRoot + "," + webProvider + "}");
        }

        return true;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        if (event.getMetaData() instanceof WebModuleMetaData) {

            final String contextRoot = ((WebModuleMetaData) event.getMetaData()).getConfiguration().getContextRoot();
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "WebModule metadata destroyed for" +
                             " module " + event.getMetaData().getJ2EEName() +
                             " | context root " + contextRoot +
                             " | in app " + event.getMetaData().getJ2EEName().getApplication());
            }

            //Remove from internal map and unregister corresponding APIProvider service
            //NOTE: We don't need to iterate through the ignored list, because our Map will return null if this context root was ignored during creation
            OpenAPIWebProvider webProvider = webAPIProviders.remove(contextRoot);

            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Removed from map {" + contextRoot + "," + webProvider + "}");
            }

            if (webProvider != null) {
                webProvider.setEnabled(false);
            }

        }
    }

    private OpenAPIWebProvider getWebProviderFromRoot(String contextRoot) {
        //Fetched our corresponding web provider
        return webAPIProviders.get(contextRoot);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.VirtualHostListener#contextRootAdded(java.lang.String, com.ibm.wsspi.http.VirtualHost)
     */
    @Override
    public void contextRootAdded(String contextRoot, VirtualHost virtualHost) {
        //Remove any trailing / or /* characters
        contextRoot = OpenAPIUtils.normalizeContextRoot(contextRoot);

        OpenAPIWebProvider webProvider = getWebProviderFromRoot(contextRoot);

        if (webProvider != null) {
            //Build full URL string
            String moduleURL = virtualHost.getUrlString(contextRoot, false);

            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "About to update moduleURL: " + moduleURL);
            }

            //Update the web provider obj to contain a full URL
            webProvider.setModuleURL(moduleURL.endsWith("/") ? moduleURL.substring(0, moduleURL.length() - 1) : moduleURL);
            webProvider.setModuleURL(contextRoot);

            if (!"default_host".equals(virtualHost.getName())) {
                List<String> hosts = new ArrayList<String>();
                hosts.add(moduleURL.substring(0, moduleURL.lastIndexOf(contextRoot)));
                String securedModuleURL = virtualHost.getUrlString(contextRoot, true);//get secured URL if exists
                String securedHost = securedModuleURL.substring(0, securedModuleURL.lastIndexOf(contextRoot));
                if (!hosts.contains(securedHost)) {
                    hosts.add(securedHost);
                }
                webProvider.setNonDefaultHosts(hosts);
            }

            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "No existing configuration found, just enable the web provider obj.");
            }
            //No specific configuration for this module, so just register it in DS
            webProvider.setEnabled(true);

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.VirtualHostListener#contextRootRemoved(java.lang.String, com.ibm.wsspi.http.VirtualHost)
     */
    @Override
    public void contextRootRemoved(String contextRoot, VirtualHost virtualHost) {
        //Don't need to do anything here, since we will unregister our relevant context roots during the "moduleMetaDataDestroyed" method
    }

    protected void processWABs() throws InvalidSyntaxException {
        //This can be called after deactivate, if the server is shutdown right after starting (ie: in a test environment)
        if (context == null) {
            return;
        }

        //Fetch all WABs
        @SuppressWarnings("unchecked")
        ServiceReference<Bundle>[] refs = (ServiceReference<Bundle>[]) context.getBundleContext().getServiceReferences(Bundle.class.getName(),
                                                                                                                       "(installed.wab.contextRoot=*)");

        if (refs != null) {
            for (ServiceReference<Bundle> ref : refs) {
                //Fetch the WAB artifacts that we need.  We don't have to actually fetch the service itself
                final String contextRoot = (String) ref.getProperty("installed.wab.contextRoot");
                final Container container = (Container) ref.getProperty("installed.wab.container");

                //If this WAB started after our bundle, then we might have processed it through the "moduleMetaDataCreated", which means we don't need
                //to process it here
                OpenAPIWebProvider webProvider = getWebProviderFromRoot(contextRoot);

                if (webProvider == null) {
                    //We haven't processed this WAB yet
                    boolean processedModule = processModule(contextRoot, container);

                    if (!processedModule) {
                        //We don't yet have the necessary services to process this.  The scheduler will call us again to retry
                        return;
                    }

                    //Check again, as the code above will have created a web provider obj IF the WAB is not on the ignore list..
                    webProvider = getWebProviderFromRoot(contextRoot);

                    if (webProvider != null) {
                        //We can't be sure that a call to contextRootAdded will be made for this WAB (it depends on timing), so we set the raw contextRoot
                        webProvider.setModuleURL(contextRoot);
                        webProvider.setEnabled(true);
                    }
                }
            }
        }

        //We have processed all WABs, so we can now cancel our scheduler
        getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                if (OpenAPIUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Finished processing WABs, so cleaning up scheduler");
                }
                cancelScheduler();
            }
        });
    }

    private void cancelScheduler() {
        //Need to check if it's cancelled first because this will be called by our fail-safe scheduled cleaner and by the processingWAB method
        if (!scheduledWabProcessor.isCancelled()) {
            scheduledWabProcessor.cancel(true);
        }
    }
}
