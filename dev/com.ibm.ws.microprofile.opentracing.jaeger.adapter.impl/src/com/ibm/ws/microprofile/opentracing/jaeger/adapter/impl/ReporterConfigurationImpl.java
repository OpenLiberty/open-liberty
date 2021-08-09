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

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.ReporterConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.SenderConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerAdapterException;

public class ReporterConfigurationImpl extends AbstractJaegerAdapter<io.jaegertracing.Configuration.ReporterConfiguration> implements ReporterConfiguration {

    public ReporterConfigurationImpl() {
        super(new io.jaegertracing.Configuration.ReporterConfiguration());
    }

    @Override
    public ReporterConfiguration withLogSpans(Boolean logSpans) {
        getDelegate().withLogSpans(logSpans);
        return this;
    }

    @Override
    public ReporterConfiguration withFlushInterval(Integer flushIntervalMs) {
        getDelegate().withFlushInterval(flushIntervalMs);
        return this;
    }

    @Override
    public ReporterConfiguration withMaxQueueSize(Integer maxQueueSize) {
        getDelegate().withMaxQueueSize(maxQueueSize);
        return this;
    }

    @Override
    public ReporterConfiguration withSender(SenderConfiguration senderConfiguration) {
        SenderConfigurationImpl senderImpl = null;
        if (senderConfiguration instanceof SenderConfigurationImpl) {
            senderImpl = (SenderConfigurationImpl) senderConfiguration;
        }
        if (senderImpl != null) {
            getDelegate().withSender(senderImpl.getDelegate());
        } else {
            throw new JaegerAdapterException("senderConfiguration is not an instance of SenderConfigurationImpl");
        }
        return this;
    }
}
