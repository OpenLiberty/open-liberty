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

package com.ibm.ws.microprofile.opentracing.jaeger.adapter;

import java.util.Map;

public interface Configuration extends JaegerAdapter {

    public io.opentracing.Tracer getTracer();
    
    public JaegerTracer.Builder getTracerBuilder();
    
    public Configuration withServiceName(String serviceName);
    
    public Configuration withReporter(ReporterConfiguration reporterConfig);
    
    public Configuration withSampler(SamplerConfiguration samplerConfig);
    
    public Configuration withTracerTags(Map<String, String> tracerTags);
    
    public Configuration withCodec(CodecConfiguration codecConfig);
    
    public interface SamplerConfiguration {

        public SamplerConfiguration withType(String type);
        
        public SamplerConfiguration withParam(Number param);
        
        public SamplerConfiguration withManagerHostPort(String managerHostPort);
    }
    
    public interface SenderConfiguration {
        
        public SenderConfiguration withAgentHost(String agentHost);

        public SenderConfiguration withAgentPort(Integer agentPort);
        
        public SenderConfiguration withEndpoint(String endpoint);
        
        public SenderConfiguration withAuthToken(String authToken);
        
        public SenderConfiguration withAuthUsername(String username);
        
        public SenderConfiguration withAuthPassword(String password);
    }
    
    public interface ReporterConfiguration {
        
        public ReporterConfiguration withLogSpans(Boolean logSpans);
        
        public ReporterConfiguration withFlushInterval(Integer flushIntervalMs);
        
        public ReporterConfiguration withMaxQueueSize(Integer maxQueueSize);
        
        public ReporterConfiguration withSender(SenderConfiguration senderConfiguration);
    }
    
    public interface CodecConfiguration {
        
        public CodecConfiguration withPropagation(Propagation propagation);
        
    }
    
    public enum Propagation {
        JAEGER,
        B3
    }
    
}
