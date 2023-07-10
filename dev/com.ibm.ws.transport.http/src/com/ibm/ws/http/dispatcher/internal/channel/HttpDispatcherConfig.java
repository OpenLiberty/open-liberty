/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package com.ibm.ws.http.dispatcher.internal.channel;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Configuration information set at the HTTP dispatcher channel.
 */
public class HttpDispatcherConfig {
    /** Debug variable */
    private static final TraceComponent tc = Tr.register(HttpDispatcherConfig.class);

    /** Property name for the httpEndpoint associated with this chain **/
    public static final String PROP_ENDPOINT = "httpEndpoint";
    
    public static final String PROP_USE_NETTY="useNettyTransport";
    /** ID of httpEndpoint associated w/ this chain */
    private String endpointPid = null;
    
    private boolean endpointUseNettyTransport=false;

    /**
     * Constructor.
     * 
     * @param props
     */
    public HttpDispatcherConfig(Map<Object, Object> props) {
        if (null == props) {
            // use all defaults
            return;
        }
        boolean bTrace = TraceComponent.isAnyTracingEnabled();
        Object value = props.get(PROP_ENDPOINT);
        if (null != value) {
            // this should always be set based on channel construction.. 
            this.endpointPid = (String) value;
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: endpoint pid " + this.endpointPid);
            }
        }
        value = props.get(PROP_USE_NETTY);
        if (null != value) {
            // TODO: beta fence
            this.endpointUseNettyTransport = (boolean) value;
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Config: endpoint configured to use Netty transport " + this.endpointUseNettyTransport);
            }
        }
    }

    /**
     * @return pid of httpEndpoint
     */
    public String getEndpointPid() {
        return this.endpointPid;
    }

    /**
     * @param pid pid of httpEndpoint
     */
    public void setEndpointPid(String pid) {
        this.endpointPid = pid;
    }
}
