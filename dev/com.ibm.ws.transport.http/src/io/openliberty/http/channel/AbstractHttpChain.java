/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.http.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.ibm.websphere.channelfw.EndPointMgr;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.ws.http.internal.HttpServiceConstants;

import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

public abstract class AbstractHttpChain implements Chain {

    protected volatile ChainConfiguration config;
    private volatile boolean enabled;
    private volatile boolean https;
    private final AtomicReference<ChainState> state = new AtomicReference<>(ChainState.UNINITIALIZED);
    private HttpEndpointImpl endpoint;

    protected String endpointName;
    protected String tcpName;
    protected String sslName;
    protected String httpName;
    protected String dispatcherName;
    protected String chainName;
    protected EndPointMgr endpointManager;

    protected String host;
    protected int port = -1;


    public AbstractHttpChain(HttpEndpointImpl endpoint, boolean isHttps) {
        this.endpoint = endpoint;
        this.https = isHttps;
    }

    @Override
    public final void disable() {
        enabled = false;

    }

    @Override
    public final void enable() {
        enabled = true;

    }

    public final boolean enabled(){
        return enabled;
    }

    @Override
    public final AtomicReference<ChainState> state() {
        return this.state;
    }

    @Override
    public final ChainConfiguration config() {
        return config;
    }

    @Override
    public final boolean isHttps() {
        return this.https;
    }

    public final HttpEndpointImpl endpoint(){
        return this.endpoint;
    }

    @Override
    public final int activePort() {
        return port;
    }

    @Override
    public final String activeHost(){
        return host;
    }

    protected final void handleStartupError(Exception exception, ChainConfiguration configuration){
        
        //Delegate to HttpEndpoint, something like endpoint().handleStartupError(e);
        if(endpoint().onError() == OnError.FAIL){
            endpoint().shutdownFramework();
        }
        else{
            String topic = endpoint().getEventTopic() + HttpServiceConstants.ENDPOINT_FAILED;
            postEvent(topic, configuration, exception);
            this.port = -1;
        }
    }

    protected final void postEvent(String topic, ChainConfiguration configuration, Exception exception){
        Map<String, Object> eventProps = new HashMap<String, Object>(4);

        eventProps.put(HttpServiceConstants.ENDPOINT_NAME, endpointName);
        eventProps.put(HttpServiceConstants.ENDPOINT_ACTIVE_PORT, port);
        eventProps.put(HttpServiceConstants.ENDPOINT_CONFIG_HOST, config.getHost());
        eventProps.put(HttpServiceConstants.ENDPOINT_CONFIG_PORT, config.port());
        eventProps.put(HttpServiceConstants.ENDPOINT_IS_HTTPS, isHttps());

        if(exception != null){
            eventProps.put(HttpServiceConstants.ENDPOINT_EXCEPTION, exception.toString());
        }

        EventAdmin engine = endpoint.getEventAdmin();
        if(engine != null){
            Event event = new Event(topic, eventProps);
            engine.postEvent(event);
        }
    }

    public abstract void stop();

    public abstract void update(String host);

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[@=" + System.identityHashCode(this)
               + ",enabled=" + enabled
               + ",state=" + (state != null ? state.get() : "null")
               + ",chainName=" + chainName
               + ",config=" + config + "]";

    }

}
