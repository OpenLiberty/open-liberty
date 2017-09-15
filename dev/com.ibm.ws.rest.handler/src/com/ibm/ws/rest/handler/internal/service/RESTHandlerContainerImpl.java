/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.rest.handler.internal.ExtendedRESTRequestImpl;
import com.ibm.ws.rest.handler.internal.helper.HandlerPath;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap;
import com.ibm.wsspi.kernel.service.utils.ServiceAndServiceReferencePair;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTHandlerContainer;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.DefaultAuthorizationHelper;
import com.ibm.wsspi.rest.handler.helper.DefaultRoutingHelper;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerInternalError;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerJsonException;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerUnsupportedMediaType;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerUserError;
import com.ibm.wsspi.rest.handler.helper.RESTRoutingHelper;

/**
 * <p>This class gets injected with different RESTHandler implementations and holds a reference to those services. It also keeps a set
 * of rest handler registered roots for fast searching.
 * 
 * <p>The main function of this container is to be able to match an incoming URL request to its appropriate registered rest handler.
 */
@Component(service = { RESTHandlerContainer.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class RESTHandlerContainerImpl implements RESTHandlerContainer {
    private static final TraceComponent tc = Tr.register(RESTHandlerContainerImpl.class);

    static final String REST_HANDLER_REF = "restHandler";

    private final String KEY_AUTHORIZATION_HELPER = "authorizationHelper";
    private final AtomicServiceReference<DefaultAuthorizationHelper> authorizationHelperRef = new AtomicServiceReference<DefaultAuthorizationHelper>(KEY_AUTHORIZATION_HELPER);

    private final String KEY_ROUTING_HELPER = "routingHelper";
    private final AtomicServiceReference<RESTRoutingHelper> routingHelperRef = new AtomicServiceReference<RESTRoutingHelper>(KEY_ROUTING_HELPER);

    /**
     * This map holds the service references to our registered REST handlers.
     */
    private final ConcurrentServiceReferenceSetMap<String, RESTHandler> handlerMap = new ConcurrentServiceReferenceSetMap<String, RESTHandler>(REST_HANDLER_REF);

    /**
     * This Set holds the registered roots so we can access them faster than going through the services each time.
     */
    private final CopyOnWriteArraySet<HandlerPath> handlerKeys = new CopyOnWriteArraySet<HandlerPath>();

    /**
     * This object is used to synchronize between key-map related operations that need to act atomically
     */
    private final HandlerKeyMapSync handlerKeyMapSync = new HandlerKeyMapSync();

    /**
     * This class is used, rather than a simple Object, as suggested by findbugs if there's ever a deadlock (helps serviceability)
     */
    private class HandlerKeyMapSync {}

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        handlerMap.activate(context);
        authorizationHelperRef.activate(context);
        routingHelperRef.activate(context);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating RESTHandlerContainer", properties);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        handlerMap.deactivate(context);
        authorizationHelperRef.deactivate(context);
        routingHelperRef.deactivate(context);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating, reason=" + reason);
        }
    }

    /**
     * Gets a set of values for a given property
     * 
     * @param handler
     * @return
     */
    private String[] getValues(Object propValue) {
        String[] keys = null;

        if (propValue instanceof String) {
            keys = new String[] { (String) propValue };
        } else if (propValue instanceof String[]) {
            keys = (String[]) propValue;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Ignoring property with value: " + propValue);
            }
            return null;
        }

        for (String key : keys) {
            // Ensure it starts with a slash ('/')
            if (!key.startsWith("/")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Ignoring property with value: " + key + " as it does not start with slash ('/')");
                }
                return null;
            }

            // Ensure its longer than just slash ('/')
            if (key.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Ignoring proerty with value: " + key + " as it is empty");
                }
                return null;
            }
        }

        return keys;
    }

    @Reference(service = RESTRoutingHelper.class,
               name = KEY_ROUTING_HELPER,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRoutingHelper(ServiceReference<RESTRoutingHelper> routingHelper) {
        routingHelperRef.setReference(routingHelper);
    }

    protected void unsetRoutingHelper(ServiceReference<RESTRoutingHelper> routingHelper) {
        routingHelperRef.unsetReference(routingHelper);
    }

    protected RESTRoutingHelper getRoutingHelper() throws IOException {
        RESTRoutingHelper routingHelper = routingHelperRef.getService();

        if (routingHelper == null) {
            throw new IOException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", "RESTRoutingHelper"));
        }

        return routingHelper;
    }

    @Reference(service = DefaultAuthorizationHelper.class, name = KEY_AUTHORIZATION_HELPER)
    protected void setAuthorizationHelper(ServiceReference<DefaultAuthorizationHelper> authorizationHelper) {
        authorizationHelperRef.setReference(authorizationHelper);
    }

    protected void unsetAuthorizationHelper(ServiceReference<DefaultAuthorizationHelper> authorizationHelper) {
        authorizationHelperRef.unsetReference(authorizationHelper);
    }

    protected DefaultAuthorizationHelper getAuthorizationHelper() throws IOException {
        DefaultAuthorizationHelper authorizationHelper = authorizationHelperRef.getService();

        if (authorizationHelper == null) {
            throw new IOException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", "DefaultAuthorizationHelper"));
        }

        return authorizationHelper;
    }

    @Reference(service = RESTHandler.class,
               name = REST_HANDLER_REF,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.MULTIPLE,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRestHandler(ServiceReference<RESTHandler> handler) {
        final String[] rootKeys = getValues(handler.getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT));
        String[] contextRootKeys = getValues(handler.getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT));

        if (rootKeys == null) {
            return;
        }

        //If there were no context root keys explicitly defined in the REST Handler we associate with the default context root
        if (contextRootKeys == null) {
            contextRootKeys = new String[] { RESTHandler.PROPERTY_REST_HANDLER_DEFAULT_CONTEXT_ROOT };
        }

        //We now augument our root keys to contain context root information
        for (String contextRoot : contextRootKeys) {
            for (String rootKey : rootKeys) {
                rootKey = contextRoot + rootKey;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "New rootKey: " + rootKey);
                }

                //Add handler to our map
                handlerMap.putReference(rootKey, handler);

                //We want to avoid a race condition between threads checking for null services + removing the key and threads adding new services + adding the key
                synchronized (handlerKeyMapSync) {
                    //Check if this path should be hidden from top level queries
                    final boolean hidden = hasProperty(handler, RESTHandler.PROPERTY_REST_HANDLER_HIDDEN_API);

                    //Add key to our local key Set
                    handlerKeys.add(new HandlerPath(rootKey, hidden));
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Mapped root [" + rootKey + "] to handler [" + handler + "] in our container.");
                }
            }
        }

    }

    protected void unsetRestHandler(ServiceReference<RESTHandler> handler) {
        final String[] rootKeys = getValues(handler.getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT));
        String[] contextRootKeys = getValues(handler.getProperty(RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT));

        if (rootKeys == null) {
            return;
        }

        //If there were no context root keys explicitly defined in the REST Handler we associate with the default context root
        if (contextRootKeys == null) {
            contextRootKeys = new String[] { RESTHandler.PROPERTY_REST_HANDLER_DEFAULT_CONTEXT_ROOT };
        }

        //We now augument our root keys to contain context root information
        for (String contextRoot : contextRootKeys) {
            for (String rootKey : rootKeys) {
                rootKey = contextRoot + rootKey;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "New rootKey: " + rootKey);
                }

                //Remove handler from our map
                handlerMap.removeReference(rootKey, handler);

                //We want to avoid a race condition between threads checking for null services + removing the key and threads adding new services + adding the key
                synchronized (handlerKeyMapSync) {
                    //if we no longer have any services for this key, then we must remove it
                    if (handlerMap.getServices(rootKey) == null) {
                        //NOTE:  We're doing "new HandlerPath(..)" here, but it should match to a previously inserted HandlerPath
                        //because we override the hashCode and equals method in HandlerPath.  The expectation is that 99.99% of the time
                        //this method will only be called during server shutdown, so this is not a big runtime performance issue.
                        //The alternative would be to keep a map String->HandlerPath, but I believe the memory footprint of that would
                        //overweight the benefit, given the above statistic.
                        handlerKeys.remove(new HandlerPath(rootKey));
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Remove mapping from root [" + rootKey + "] to handler [" + handler + "] in our container.");
                }
            }
        }
    }

    private boolean hasProperty(ServiceReference<RESTHandler> handler, String property) {
        Object propertyObj = handler.getProperty(property);

        if (propertyObj instanceof String && "true".equalsIgnoreCase((String) propertyObj)) {
            return true;
        }

        return false;
    }

    /**
     * Small helper class to encapsulate a handler, its service reference and a handler path.
     */
    public static class HandlerInfo {
        public final RESTHandler handler;
        public final ServiceReference<RESTHandler> handlerRef;
        public final HandlerPath path;

        public HandlerInfo(RESTHandler handler, ServiceReference<RESTHandler> handlerRef) {
            this.handler = handler;
            this.handlerRef = handlerRef;
            this.path = null;
        }

        public HandlerInfo(RESTHandler handler, ServiceReference<RESTHandler> handlerRef, HandlerPath path) {
            this.handler = handler;
            this.handlerRef = handlerRef;
            this.path = path;
        }
    }

    /**
     * Try to find the appropriate RESTHandler and HandlerPath pair for the given URL. Return null if no match found.
     * May return null for the HandlerPath field if the RESTHandler matched an URL that did not contain variables.
     * 
     * @param requestURL The URL from the HTTP request. This is the URL that needs to be matched.
     */
    public HandlerInfo getHandler(String requestURL) {
        Iterator<HandlerPath> keys = handlerKeys.iterator();

        if (requestURL == null || keys == null) {
            return null;
        }

        // Check to see if we have a direct hit
        Iterator<ServiceAndServiceReferencePair<RESTHandler>> itr = handlerMap.getServicesWithReferences(requestURL);
        if (itr != null && itr.hasNext()) {

            ServiceAndServiceReferencePair<RESTHandler> handler = itr.next();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Found direct URL match: " + handler);
            }

            return new HandlerInfo(handler.getService(), handler.getServiceReference());
        }

        // If no direct match, then try to match each one. Longest match wins.
        HandlerPath bestMatchRoot = null;
        while (keys.hasNext()) {
            HandlerPath key = keys.next();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Checking HandlerPath: " + key.getRegisteredPath() + " | length: " + key.length());
            }

            if (key.matches(requestURL)) {
                if (bestMatchRoot == null || key.length() > bestMatchRoot.length()) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "New best match: " + key.getRegisteredPath());
                    }

                    bestMatchRoot = key;
                }
            }
        }

        // If we found a match...
        if (bestMatchRoot != null) {
            // Get the iterator. We MUST check hasNext first, because of how
            // the underlying implementation is written.
            itr = handlerMap.getServicesWithReferences(bestMatchRoot.getRegisteredPath());
            if (itr != null && itr.hasNext()) {

                ServiceAndServiceReferencePair<RESTHandler> handler = itr.next();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Final best handler: " + handler);
                }

                return new HandlerInfo(handler.getService(), handler.getServiceReference(), bestMatchRoot);
            }
        }
        return null;
    }

    /**
     * Return our registered keys.
     */
    @Override
    public Iterator<String> registeredKeys() {
        Iterator<HandlerPath> paths = handlerKeys.iterator();
        List<String> registeredKeys = new ArrayList<String>(handlerKeys.size());
        while (paths.hasNext()) {
            HandlerPath path = paths.next();
            if (!path.isHidden()) {
                registeredKeys.add(path.getRegisteredPath());
            }
        }

        return registeredKeys.iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.rest.handler.RESTHandlerContainer#handleRequest(java.lang.String, com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)
     */
    @Override
    @FFDCIgnore({ RESTHandlerInternalError.class, RESTHandlerUserError.class, RESTHandlerMethodNotAllowedError.class, RESTHandlerUnsupportedMediaType.class,
                 RESTHandlerJsonException.class })
    public boolean handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        final String requestURL = request.getContextPath() + request.getPath();
        final HandlerInfo handlerInfo = getHandler(requestURL);
        final boolean isRouting = DefaultRoutingHelper.containsLegacyRoutingContext(request) || DefaultRoutingHelper.containsRoutingContext(request);

        if (handlerInfo == null && !isRouting) {
            //calling proxy servlet will handle this case 
            return false;
        }

        try {
            //Security checks applicable to /ibm/api participants
            if (request.getContextPath().equals(RESTHandler.PROPERTY_REST_HANDLER_DEFAULT_CONTEXT_ROOT)) {
                //Check if the handler is doing custom security or not.
                //The first argument will be true if we're routing the call and a corresponding service wasn't present on this controller
                if (handlerInfo == null || !hasProperty(handlerInfo.handlerRef, RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY)) {
                    //This path is not performing custom security, so check for default authorization
                    if (!getAuthorizationHelper().checkAdministratorRole(request, response)) {
                        //We failed the check, so return true, since the default authorization helper would have filled up the response already.
                        return true;
                    }
                }
            }

            //If we matched a path that had variables, extended the request to contain the resolved variables
            if (handlerInfo != null && handlerInfo.path != null && handlerInfo.path.containsVariable()) {
                //When mapping variables we don't include the context root
                request = new ExtendedRESTRequestImpl(request, handlerInfo.path.mapVariables(requestURL));
            }

            //Routing special code
            if (isRouting) {
                //Check if we're doing custom routing or not
                if (handlerInfo == null || !hasProperty(handlerInfo.handlerRef, RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_ROUTING)) {
                    //This path is not doing custom routing, so use the routing helper
                    getRoutingHelper().routeRequest(request, response);
                    return true;
                }
                //...there's routing context, but matched handler wants to do custom routing, so let the request go through
            }

            try {
                //Delegate request to handler
                handlerInfo.handler.handleRequest(request, response);
            } catch (RESTHandlerInternalError ie) {
                //Handlers general internal errors and osgi errors
                response.sendError(ie.getStatusCode(), ie.getMessage());
            } catch (RESTHandlerUserError ue) {
                response.sendError(ue.getStatusCode(), ue.getMessage());
            } catch (RESTHandlerUnsupportedMediaType e) {
                response.sendError(e.getStatusCode(), e.getMessage());
            } catch (RESTHandlerMethodNotAllowedError e) {
                //A 405 response (Method Not Allowed) needs to have the response header of "Allow", to say which methods can be used
                response.setResponseHeader("Allow", e.getAllowedMethods());
                response.sendError(e.getStatusCode());
            } catch (RESTHandlerJsonException e) {
                if (e.isMessageContentJSON()) {
                    response.setStatus(e.getStatusCode());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(e.getMessage());
                } else {
                    response.sendError(e.getStatusCode(), e.getMessage());
                }
            }

        } catch (IOException ioe) {
            response.sendError(500, ioe.getMessage());
        }

        return true;
    }
}
