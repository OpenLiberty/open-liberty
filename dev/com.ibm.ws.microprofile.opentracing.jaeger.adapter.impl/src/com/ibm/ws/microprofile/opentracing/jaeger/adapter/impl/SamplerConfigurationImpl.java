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

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.SamplerConfiguration;

public class SamplerConfigurationImpl extends AbstractJaegerAdapter<io.jaegertracing.Configuration.SamplerConfiguration> implements SamplerConfiguration {

    public SamplerConfigurationImpl() {
        super(new io.jaegertracing.Configuration.SamplerConfiguration());
    }

    @Override
    public SamplerConfiguration withType(String type) {
        getDelegate().withType(type);
        return this;
    }

    @Override
    public SamplerConfiguration withParam(Number param) {
        getDelegate().withParam(param);
        return this;
    }

    @Override
    public SamplerConfiguration withManagerHostPort(String managerHostPort) {
        getDelegate().withManagerHostPort(managerHostPort);
        return this;
    }

}
