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

import java.util.Map;

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerAdapterException;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerTracer.Builder;

import io.opentracing.Tracer;

public class ConfigurationImpl extends AbstractJaegerAdapter<io.jaegertracing.Configuration> implements Configuration {
    
    public ConfigurationImpl(String serviceName) {
        super(new io.jaegertracing.Configuration(serviceName));
    }

    @Override
    public Tracer getTracer() {
        return getDelegate().getTracer();
    }

    @Override
    public Configuration withServiceName(String serviceName) {
        getDelegate().withServiceName(serviceName);
        return this;
    }

    @Override
    public Configuration withReporter(ReporterConfiguration reporterConfiguration) {
        ReporterConfigurationImpl reporterImpl = null;
        if (reporterConfiguration instanceof ReporterConfigurationImpl) {
            reporterImpl = (ReporterConfigurationImpl) reporterConfiguration;
        }
        if (reporterImpl != null) {
            getDelegate().withReporter(reporterImpl.getDelegate());
        } else {
            throw new JaegerAdapterException("reporterConfiguration is not an instance of ReporterConfigurationImpl");
        }
        return this;
    }

    @Override
    public Configuration withSampler(SamplerConfiguration samplerConfiguration) {
        SamplerConfigurationImpl samplerImpl = null;
        if (samplerConfiguration instanceof SamplerConfigurationImpl) {
            samplerImpl = (SamplerConfigurationImpl) samplerConfiguration;
        }
        if (samplerImpl != null) {
            getDelegate().withSampler(samplerImpl.getDelegate());
        } else {
            throw new JaegerAdapterException("samplerConfiguration is not an instance of SamplerConfigurationImpl");
        }
        return this;
    }

    @Override
    public Configuration withTracerTags(Map<String, String> tracerTags) {
        getDelegate().withTracerTags(tracerTags);
        return this;
    }

    @Override
    public Configuration withCodec(CodecConfiguration codecConfig) {
        CodecConfigurationImpl codecImpl = null;
        if (codecConfig instanceof CodecConfigurationImpl) {
            codecImpl = (CodecConfigurationImpl) codecConfig;
        }
        if (codecImpl != null) {
            getDelegate().withCodec(codecImpl.getDelegate());
        } else {
            throw new JaegerAdapterException("CodecConfiguration is not an instance of CodecConfigurationImpl");
        }
        return this;
    }

    @Override
    public Builder getTracerBuilder() {
        getDelegate().getTracerBuilder();
        return null;  //TODO FW
    }
}
