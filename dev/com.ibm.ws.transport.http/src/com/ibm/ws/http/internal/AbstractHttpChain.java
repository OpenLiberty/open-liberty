/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.http.internal;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * Common configuration and endpoint notifications for HTTP transport chains.
 * Each transport owns its lifecycle state and synchronization.
 */
public abstract class AbstractHttpChain {
    private static final TraceComponent tc = Tr.register(AbstractHttpChain.class);

    public enum ChainState {
        UNINITIALIZED(0, "UNINITIALIZED"),
        DESTROYED(1, "DESTROYED"),
        INITIALIZED(2, "INITIALIZED"),
        STOPPED(3, "STOPPED"),
        QUIESCED(4, "QUIESCED"),
        STARTED(5, "STARTED"),
        STARTING(6, "STARTING"),
        STOPPING(7, "STOPPING");

        public final int val;
        final String name;

        @Trivial
        ChainState(int val, String name) {
            this.val = val;
            this.name = "name";
        }

        @Trivial
        public static final String printState(int state) {
            switch (state) {
                case 0:
                    return "UNINITIALIZED";
                case 1:
                    return "DESTROYED";
                case 2:
                    return "INITIALIZED";
                case 3:
                    return "STOPPED";
                case 4:
                    return "QUIESCED";
                case 5:
                    return "STARTED";
                case  6:
                    return "STARTING";
                case  7:
                    return "STOPPING";
            }
            return "UNKNOWN";
        }
    }

    protected final HttpEndpointImpl owner;
    protected final boolean isHttps;
    protected String endpointName;
    protected String chainName;
    protected EndPointMgr endpointMgr;

    /**
     * A snapshot of the configuration (collection of properties objects) last used
     * for a start/update operation.
     */
    protected volatile ActiveConfiguration currentConfig = null;

    protected AbstractHttpChain(HttpEndpointImpl owner, boolean isHttps) {
        this.owner = owner;
        this.isHttps = isHttps;
    }

    public HttpEndpointImpl getOwner() {
        return this.owner;
    }

    public boolean isHttps() {
        return this.isHttps;
    }

    public abstract void enable();

    public abstract void disable();

    public abstract void update(String resolvedHostName);

    public abstract void stop();

    public abstract int getActivePort();

    public abstract int getChainState();

    public void handleStartupError(Exception e, ActiveConfiguration cfg) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Error starting chain " + chainName, this, e);
        }

        if (owner.onError() == OnError.FAIL) {
            // Stop the server if something bad happened starting the chain
            owner.shutdownFramework();
        } else {
            // Post an endpoint failed to start event to anyone listening
            String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_FAILED;
            postEvent(topic, cfg, e);

            // TODO: schedule a task to try again later..
        }
    }

    /**
     * Publish an event relating to a chain starting/stopping with the
     * given properties set about the chain.
     */
    protected void postEvent(String t, ActiveConfiguration c, Exception e) {
        Map<String, Object> eventProps = new HashMap<String, Object>(4);

        eventProps.put(HttpServiceConstants.ENDPOINT_NAME, endpointName);
        eventProps.put(HttpServiceConstants.ENDPOINT_ACTIVE_PORT, c.activePort);
        eventProps.put(HttpServiceConstants.ENDPOINT_CONFIG_HOST, c.configHost);
        eventProps.put(HttpServiceConstants.ENDPOINT_CONFIG_PORT, c.configPort);
        eventProps.put(HttpServiceConstants.ENDPOINT_IS_HTTPS, isHttps);

        if (e != null) {
            eventProps.put(HttpServiceConstants.ENDPOINT_EXCEPTION, e.toString());
        }

        EventAdmin engine = owner.getEventAdmin();
        if (engine != null) {
            Event event = new Event(t, eventProps);
            engine.postEvent(event);
        }
    }

    public static final class ActiveConfiguration {
        final boolean isHttps;
        public final int configPort;
        public final String configHost;
        final String resolvedHost;

        final Map<String, Object> tcpOptions;
        final Map<String, Object> sslOptions;
        final Map<String, Object> httpOptions;
        final Map<String, Object> remoteIp;
        final Map<String, Object> compression;
        final Map<String, Object> samesite;
        final Map<String, Object> headers;
        final Map<String, Object> endpointOptions;

        volatile int activePort = -1;
        public boolean validConfiguration = false;

        public ActiveConfiguration(boolean isHttps,
                                   Map<String, Object> tcp,
                                   Map<String, Object> ssl,
                                   Map<String, Object> http,
                                   Map<String, Object> remoteIp,
                                   Map<String, Object> compression,
                                   Map<String, Object> samesite,
                                   Map<String, Object> headers,
                                   Map<String, Object> endpoint,
                                   String resolvedHostName) {
            this.isHttps = isHttps;
            tcpOptions = tcp;
            sslOptions = ssl;
            httpOptions = http;
            this.remoteIp = remoteIp;
            this.compression = compression;
            this.samesite = samesite;
            this.headers = headers;
            endpointOptions = endpoint;

            String attribute = isHttps ? "httpsPort" : "httpPort";
            configPort = MetatypeUtils.parseInteger(HttpServiceConstants.ENPOINT_FPID_ALIAS, attribute,
                                                    endpointOptions.get(attribute),
                                                    -1);
            configHost = (String) endpointOptions.get("host");
            resolvedHost = resolvedHostName;
        }

        /**
         * Reset the active port to -1 (not actively listening)
         */
        public void clearActivePort() {
            activePort = -1;
        }

        public String getResolvedHost() {
            return resolvedHost;
        }

        public int getConfigPort() {
            return configPort;
        }

        public String getEndpointPID() {
            return (String) endpointOptions.get(Constants.SERVICE_PID);
        }

        /**
         * @return true if the ActiveConfiguration contains the required
         *         configuration to start the http chains. The base http
         *         chain needs both tcp and http options. The https chain
         *         additionally needs ssl options.
         */
        @Trivial
        public boolean complete() {
            if (tcpOptions == null || httpOptions == null)
                return false;

            if (isHttps && sslOptions == null)
                return false;

            return true;
        }

        /**
         * Check to see if all of the maps are the same as they
         * were the last time: ConfigurationAdmin returns unmodifiable
         * maps: if the map instances are the same, there have been no
         * updates.
         */
        public boolean unchanged(ActiveConfiguration other) {
            if (other == null)
                return false;

            // Only look at ssl options if this is an https chain
            if (isHttps) {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions &&
                       sslOptions == other.sslOptions &&
                       httpOptions == other.httpOptions &&
                       remoteIp == other.remoteIp &&
                       compression == other.compression &&
                       samesite == other.samesite &&
                       headers == other.headers &&
                       !endpointChanged(other);
            } else {
                return configHost.equals(other.configHost) &&
                       configPort == other.configPort &&
                       tcpOptions == other.tcpOptions &&
                       httpOptions == other.httpOptions &&
                       remoteIp == other.remoteIp &&
                       compression == other.compression &&
                       samesite == other.samesite &&
                       headers == other.headers &&
                       !endpointChanged(other);
            }
        }

        protected boolean tcpChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            return !configHost.equals(other.configHost) ||
                   configPort != other.configPort ||
                   tcpOptions != other.tcpOptions;
        }

        protected boolean sslChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            return sslOptions != other.sslOptions;
        }

        protected boolean httpChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            return (httpOptions != other.httpOptions) || (remoteIp != other.remoteIp) || (compression != other.compression) || (samesite != other.samesite)
                   || (headers != other.headers);

        }

        protected boolean endpointChanged(ActiveConfiguration other) {
            if (other == null)
                return true;

            // Instance equality doesn't work for this one, because the endpoint options
            // are the httpEndpoint's service properties, and they will change for reasons
            // that shouldn't cause a chain to restart
            return !endpointOptions.get(Constants.SERVICE_PID).equals(other.endpointOptions.get(Constants.SERVICE_PID));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                   + "[host=" + configHost
                   + ",resolvedHost=" + resolvedHost
                   + ",port=" + configPort
                   + ",listening=" + activePort
                   + ",complete=" + complete()
                   + ",tcpOptions=" + System.identityHashCode(tcpOptions)
                   + ",httpOptions=" + System.identityHashCode(httpOptions)
                   + ",remoteIp=" + System.identityHashCode(remoteIp)
                   + ",compression=" + System.identityHashCode(compression)
                   + ",samesite=" + System.identityHashCode(samesite)
                   + ",headers=" + System.identityHashCode(headers)
                   + ",sslOptions=" + (isHttps ? System.identityHashCode(sslOptions) : "0")
                   + ",endpointOptions=" + endpointOptions.get(Constants.SERVICE_PID)
                   + "]";
        }
    }

}
