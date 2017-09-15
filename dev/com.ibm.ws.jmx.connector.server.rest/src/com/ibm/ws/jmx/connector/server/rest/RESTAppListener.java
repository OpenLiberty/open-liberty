/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.http.VirtualHostListener;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           service = { VirtualHostListener.class },
           property = { "service.vendor=IBM" })
public class RESTAppListener implements VirtualHostListener {

    private static final TraceComponent tc = Tr.register(RESTAppListener.class,
                                                         APIConstants.TRACE_GROUP,
                                                         APIConstants.TRACE_BUNDLE_CORE);

    private static final String JMX_REST_ADDRESS = "com.ibm.ws.jmx.rest.address";

    private volatile BundleContext bContext = null;
    private volatile String appURL = null;
    private volatile WsLocationAdmin locationService;
    private volatile WsResource restJMXAddressWorkareaFile;
    private volatile WsResource restJMXAddressStateFile;
    private volatile String registeredContextRoot = null;
    private volatile VirtualHost secureVirtualHost = null;
    private volatile String secureAlias = null;
    private volatile ServiceRegistration<Object> jmxEndpointRegistration = null;

    @Activate
    protected void activate(BundleContext bc) {
        bContext = bc;
    }

    @Deactivate
    protected void deactivate() {
        removeJMXAddressResource(restJMXAddressStateFile);
        removeJMXAddressResource(restJMXAddressWorkareaFile);
        if (jmxEndpointRegistration != null)
            jmxEndpointRegistration.unregister();
    }

    private void removeJMXAddressResource(WsResource remoteJMXAddressFile) {
        if (remoteJMXAddressFile != null) {
            try {
                if (!remoteJMXAddressFile.delete()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Could not delete the JMX rest connector address file.");
                    }
                    return;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Successfully deleted the JMX rest connector address file.");
                }

            } catch (SecurityException se) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Received a SecurityException while attemping to delete the JMX rest connector address file: ", se);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void contextRootAdded(String contextRoot, VirtualHost virtualHost) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Added contextRoot {0} to virtual host {1}", contextRoot, virtualHost.getName());
        }

        // Check that our app got installed
        if (contextRoot != null
            && contextRoot.contains(APIConstants.JMX_CONNECTOR_API_ROOT_PATH)
            && "default_host".equals(virtualHost.getName())) {
            registeredContextRoot = contextRoot;

            if (secureVirtualHost == virtualHost) {
                createJMXWorkAreaResourceIfChanged(virtualHost);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void contextRootRemoved(String contextRoot, VirtualHost virtualHost) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Removed contextRoot {0} from virtual host {1}", contextRoot, virtualHost.getName());
        }

        if (contextRoot != null
            && contextRoot.contains(APIConstants.JMX_CONNECTOR_API_ROOT_PATH)) {
            registeredContextRoot = null;
        }
    }

    /**
     * Set required VirtualHost. This will be called before activate.
     * The target filter will only allow the enabled default_host virtual
     * host to be bound if/when it has an SSL port available
     */
    @Reference(service = VirtualHost.class,
               target = "(&(enabled=true)(id=default_host)(httpsAlias=*))",
               policy = ReferencePolicy.STATIC,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Set vhost: ", vhost);
        }
        secureVirtualHost = vhost;
        secureAlias = props.get("httpsAlias").toString();
        createJMXWorkAreaResourceIfChanged(vhost);
    }

    protected void updatedVirtualHost(VirtualHost vhost, Map<String, Object> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Update vhost: ", vhost);
        }
        secureAlias = props.get("httpsAlias").toString();
        createJMXWorkAreaResourceIfChanged(vhost);
    }

    /** Unset required VirtualHost. This will be called after deactivate */
    protected void unsetVirtualHost(VirtualHost vhost) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Unset vhost: ", vhost);
        }
        secureVirtualHost = null;
    }

    /**
     * Called to create the work area resource. Only re-generate the file
     * if this is the first time or something in the url has changed.
     * 
     * @param appPath standard url path received from the virtual host
     */
    private synchronized void createJMXWorkAreaResourceIfChanged(VirtualHost vhost) {

        // Make sure our context root has been registered... 
        String contextRoot = registeredContextRoot;
        if (contextRoot != null) {
            // Make sure that we are dealing with the secure port before writing the file...
            String newAppURLString = vhost.getUrlString(contextRoot, true);
            if (newAppURLString.startsWith("https")) {
                // If we are dealing with the secure port, woohoo!
                String oldAppURL = appURL;
                String newAppURL = processContextRootURL(newAppURLString);

                if (oldAppURL == null || !oldAppURL.equals(newAppURL)) {
                    this.appURL = newAppURL;
                    if (restJMXAddressWorkareaFile == null) {
                        restJMXAddressWorkareaFile = createJMXWorkAreaResource(locationService);
                    } else {
                        createJmxAddressResource(restJMXAddressWorkareaFile);
                    }
                    if (restJMXAddressStateFile == null) {
                        restJMXAddressStateFile = createJMXStateResource(locationService);
                    } else {
                        createJmxAddressResource(restJMXAddressStateFile);
                    }

                    // Register or update a marker service with JMX host/port
                    Hashtable<String, Object> props = new Hashtable<String, Object>();
                    props.put("name", "JMXConnectorEndpoint");
                    props.put("jmxHost", vhost.getHostName(secureAlias));
                    props.put("jmxPort", vhost.getSecureHttpPort(secureAlias));
                    props.put("jmxAlias", secureAlias);
                    if (jmxEndpointRegistration == null)
                        jmxEndpointRegistration = bContext.registerService(Object.class, this, props);
                    else
                        jmxEndpointRegistration.setProperties(props);
                }
            }
        }
    }

    private static String processContextRootURL(String contextRootURL) {
        StringBuilder b = new StringBuilder(contextRootURL);

        if (contextRootURL.endsWith("/*")) {
            b.delete(b.length() - 2, b.length());
        } else if (contextRootURL.endsWith("/")) {
            b.deleteCharAt(b.length() - 1);
        }

        if (contextRootURL.startsWith("https")) {
            b.replace(0, "https".length(), "service:jmx:rest");
        } else if (contextRootURL.startsWith("http")) {
            b.replace(0, "http".length(), "service:jmx:rest");
        } else {
            //Assuming that the contextRoot we got is "hostname:port/app" in this case
            b.insert(0, "service:jmx:rest://");
        }

        return b.toString();
    }

    /**
     * Set the dynamic reference to the WsLocationAdmin service.
     * If the service is replaced, the new service will be set before
     * the old is removed.
     * 
     * @param locationService
     */
    @Reference(service = WsLocationAdmin.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setLocationService(WsLocationAdmin locationService) {
        this.locationService = locationService;
        if (restJMXAddressWorkareaFile == null) {
            restJMXAddressWorkareaFile = createJMXWorkAreaResource(locationService);
        }
        if (restJMXAddressStateFile == null) {
            restJMXAddressStateFile = createJMXStateResource(locationService);
        }
    }

    protected void unsetLocationService(WsLocationAdmin locationService) {
        if (this.locationService == locationService) {
            this.locationService = null;
        }
    }

    /**
     * @param locationService2
     * @return
     */
    private synchronized WsResource createJMXStateResource(WsLocationAdmin locationAdmin) {
        WsResource resource = locationAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_STATE_DIR + JMX_REST_ADDRESS);
        return createJmxAddressResource(resource);
    }

    private synchronized WsResource createJMXWorkAreaResource(WsLocationAdmin locationAdmin) {
        WsResource resource = locationAdmin.getServerWorkareaResource(JMX_REST_ADDRESS);
        return createJmxAddressResource(resource);
    }

    /**
     * @param resource
     * @return
     */
    private synchronized WsResource createJmxAddressResource(WsResource resource) {
        if (appURL != null && locationService != null) {
            try {
                if (!resource.exists()) {
                    resource.create();
                }
                OutputStream os = resource.putStream();
                os.write(appURL.getBytes("UTF-8"));
                os.flush();
                os.close();

                Tr.info(tc, "JMX_REST_ADDRESS", new Object[] { appURL });

                return resource;
            } catch (IOException ioe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received IOException while writting address to file: ", ioe);
                }
                FFDCFilter.processException(ioe, getClass().getName(), "createJMXWorkAreaResourceIO");

            } catch (SecurityException se) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received SecurityException while writting address to file: ", se);
                }
                FFDCFilter.processException(se, getClass().getName(), "createJMXWorkAreaResourceSec");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Connector address was null, so we can't write the address to a file.");
        }
        return null;
    }
}
