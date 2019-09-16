/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jaxws.properties.interceptor;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

public class TestConduitInterceptor extends AbstractPhaseInterceptor<Message> {
    private String testedProperties;

    public TestConduitInterceptor() {
        super(Phase.PREPARE_SEND);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Conduit conduit = message.get(Conduit.class);
        if (conduit instanceof HTTPConduit) {
            HTTPConduit http = (HTTPConduit) conduit;
            HTTPClientPolicy httpClientPolicy = http.getClient();
            AuthorizationPolicy authorizationPolicy = http.getAuthorization();
            ProxyAuthorizationPolicy proxyAuthorizationPolicy = http.getProxyAuthorization();

            String prefix = "";
            StringBuilder builder = new StringBuilder();
            if (null != httpClientPolicy) {
                builder.append(prefix);
                prefix = ",";
                builder.append("client.ConnectionTimeout=").append(httpClientPolicy.getConnectionTimeout()).append(",client.ChunkingThreshold=").append(httpClientPolicy.getChunkingThreshold());
            }

            if (null != authorizationPolicy) {
                builder.append(prefix);
                prefix = ",";
                builder.append("authorization.UserName=").append(authorizationPolicy.getUserName()).append(",authorization.Authorization=").append(authorizationPolicy.getAuthorization());
            }

            if (null != proxyAuthorizationPolicy) {
                builder.append(prefix);
                prefix = ",";
                builder.append("proxyAuthorization.UserName=").append(proxyAuthorizationPolicy.getUserName()).append(",proxyAuthorization.Authorization=").append(proxyAuthorizationPolicy.getAuthorization());
            }

            testedProperties = builder.toString();
        }
    }

    public String getTestedProperties() {
        return testedProperties;
    }

}
