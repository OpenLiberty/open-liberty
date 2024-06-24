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

package org.jboss.resteasy.microprofile.client.ot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.interceptor.InvocationContext;

// effectively copied with author's permission from:
// https://github.com/apache/cxf/blob/master/rt/rs/microprofile-client/src/main/java/org/apache/cxf/microprofile/client/cdi/MPRestClientInvocationContextImpl.java
class MPRestClientInvocationContextImpl implements InvocationContext {

    private final Object target;

    private final Method method;

    private Object[] args;

    private int index;

    private final List<InterceptorInvoker> interceptorInvokers;

    private final Map<String, Object> contextData = new HashMap<>();

    private final Callable<Object> callable;
    /**
     * @param target
     * @param method
     * @param args
     * @param interceptorInvokers
     */
    MPRestClientInvocationContextImpl(Object target, Method method, Object[] args, 
                                      List<InterceptorInvoker> interceptorInvokers, 
                                      Callable<Object> callable) {
        this.target = target;
        this.method = method;
        this.args = args == null ? new Object[] {} : args;
        this.interceptorInvokers = interceptorInvokers;
        this.callable = callable;
    }

    boolean hasNextInterceptor() {
        return index < interceptorInvokers.size();
    }

    protected Object invokeNextInterceptor() throws Exception {
        int oldIndex = index;
        try {
            // Note that some FaultTolerance interceptors can cause
            // some interesting behaviors if they are invoked before
            // other interceptors. The CDIInterceptorWrapperImpl
            // intentionally orders the FT interceptor last to
            // avoid these side effects.
            return interceptorInvokers.get(index++).invoke(this);
        } finally {
            index = oldIndex;
        }
    }

    protected Object interceptorChainCompleted() throws Exception {
        return callable.call();
    }

    @Override
    public Object proceed() throws Exception {
        if (hasNextInterceptor()) {
            return invokeNextInterceptor();
        } else {
            return interceptorChainCompleted();
        }
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Constructor<?> getConstructor() {
        return null;
    }

    @Override
    public Object[] getParameters() throws IllegalStateException {
        return args;
    }

    @Override
    public void setParameters(Object[] params) throws IllegalStateException, IllegalArgumentException {
        this.args = params;
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;
    }

    @Override
    public Object getTimer() {
        return null;
    }

}
