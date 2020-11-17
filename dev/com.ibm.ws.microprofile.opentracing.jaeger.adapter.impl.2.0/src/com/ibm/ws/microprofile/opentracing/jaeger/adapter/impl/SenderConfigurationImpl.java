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

package com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl;

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.SenderConfiguration;

public class SenderConfigurationImpl extends AbstractJaegerAdapter<io.jaegertracing.Configuration.SenderConfiguration> implements SenderConfiguration {

    public SenderConfigurationImpl() {
        super(new io.jaegertracing.Configuration.SenderConfiguration());
    }

    @Override
    public SenderConfiguration withAgentHost(String agentHost) {
        getDelegate().withAgentHost(agentHost);
        return this;
    }

    @Override
    public SenderConfiguration withAgentPort(Integer agentPort) {
        getDelegate().withAgentPort(agentPort);
        return this;
    }

    @Override
    public SenderConfiguration withEndpoint(String endpoint) {
        getDelegate().withEndpoint(endpoint);
        return this;
    }

    @Override
    public SenderConfiguration withAuthToken(String authToken) {
        getDelegate().withAuthToken(authToken);
        return this;
    }

    @Override
    public SenderConfiguration withAuthUsername(String username) {
        getDelegate().withAuthUsername(username);
        return this;
    }

    @Override
    public SenderConfiguration withAuthPassword(String password) {
        getDelegate().withAuthPassword(password);
        return this;
    }



}
