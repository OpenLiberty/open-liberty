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

package org.apache.cxf.microprofile.client.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.microprofile.client.MicroProfileClientProviderFactory;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;

public class MPAsyncInvocationInterceptorImpl extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(MPAsyncInvocationInterceptorImpl.class);

    private final List<AsyncInvocationInterceptor> interceptors = new ArrayList<>();

    MPAsyncInvocationInterceptorImpl(Message message) {
        super(Phase.POST_MARSHAL);

        MicroProfileClientProviderFactory factory = MicroProfileClientProviderFactory.getInstance(message);
        List<ProviderInfo<Object>> aiiProviderList = 
            factory.getAsyncInvocationInterceptorFactories();

        for (ProviderInfo<Object> providerInfo: aiiProviderList) {
            AsyncInvocationInterceptor aiInterceptor = 
                ((AsyncInvocationInterceptorFactory) providerInfo.getProvider()).newInterceptor();
            interceptors.add(0, aiInterceptor); // sort in reverse order
        }
    }

    List<AsyncInvocationInterceptor> getInterceptors() {
        return interceptors;
    }

    /** {@inheritDoc}*/
    @Override
    public void handleMessage(Message message) throws Fault {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            try {
                interceptors.get(i).prepareContext();
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "ASYNC_INTERCEPTOR_EXCEPTION_PREPARE_CONTEXT", 
                        new Object[]{interceptors.get(i).getClass().getName(), t});
            }
        }
        message.getExchange().put(MPAsyncInvocationInterceptorImpl.class, this); //Liberty change
    }

}
