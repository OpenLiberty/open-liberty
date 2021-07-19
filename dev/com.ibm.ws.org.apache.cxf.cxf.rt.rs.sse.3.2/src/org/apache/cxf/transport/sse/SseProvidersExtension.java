/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.sse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusCreationListener;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.sse.SseContextProvider;
import org.apache.cxf.jaxrs.sse.SseEventSinkContextProvider;
import org.apache.cxf.jaxrs.sse.interceptor.SseInterceptor;

public class SseProvidersExtension implements BusCreationListener {

    private static final String BUS_PROVIDERS = "org.apache.cxf.jaxrs.bus.providers";

    @Override
    public void busCreated(Bus bus) {
        Object providers = bus.getProperty(BUS_PROVIDERS);
        
        final List<?> sseProviders = 
            Arrays.asList(
                new SseContextProvider(), 
                new SseEventSinkContextProvider()
            );
        
        if (providers instanceof List) {
            final List<?> existing = new ArrayList<>((List<?>)providers);
            existing.addAll(CastUtils.cast(sseProviders));
            bus.setProperty(BUS_PROVIDERS, existing);
        } else {
            bus.setProperty(BUS_PROVIDERS, sseProviders);
        }
        
        bus.getInInterceptors().add(new SseInterceptor());
    }
}
