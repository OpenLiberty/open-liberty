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
package org.apache.cxf.jaxrs.client.spec;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.client.AbstractClient;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.utils.HttpUtils;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.jaxrs21.clientconfig.JAXRSClientConstants;

public class InvocationBuilderImpl implements Invocation.Builder {
    private static final String PROPERTY_KEY = "jaxrs.filter.properties";

    private final WebClient webClient;
    private final SyncInvoker sync;
    private final Configuration config;

    public InvocationBuilderImpl(WebClient webClient,
                                 Configuration config) {
        this.webClient = webClient;
        this.sync = webClient.sync();
        this.config = config;
    }

    public WebClient getWebClient() {
        return this.webClient;
    }

    @Override
    public Response delete() {
        return sync.delete();
    }

    @Override
    public <T> T delete(Class<T> cls) {
        return sync.delete(cls);
    }

    @Override
    public <T> T delete(GenericType<T> type) {
        return sync.delete(type);
    }

    @Override
    public Response get() {
        return sync.get();
    }

    @Override
    public <T> T get(Class<T> cls) {
        return sync.get(cls);
    }

    @Override
    public <T> T get(GenericType<T> type) {
        return sync.get(type);
    }

    @Override
    public Response head() {
        return sync.head();
    }

    @Override
    public Response method(String method) {
        return sync.method(method);
    }

    @Override
    public <T> T method(String method, Class<T> cls) {
        return sync.method(method, cls);
    }

    @Override
    public <T> T method(String method, GenericType<T> type) {
        return sync.method(method, type);
    }

    @Override
    public Response method(String method, Entity<?> entity) {
        return sync.method(method, entity);
    }

    @Override
    public <T> T method(String method, Entity<?> entity, Class<T> cls) {
        return sync.method(method, entity, cls);
    }

    @Override
    public <T> T method(String method, Entity<?> entity, GenericType<T> type) {
        return sync.method(method, entity, type);
    }

    @Override
    public Response options() {
        return sync.options();
    }

    @Override
    public <T> T options(Class<T> cls) {
        return sync.options(cls);
    }

    @Override
    public <T> T options(GenericType<T> type) {
        return sync.options(type);
    }

    @Override
    public Response post(Entity<?> entity) {
        return sync.post(entity);
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> cls) {
        return sync.post(entity, cls);
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> type) {
        return sync.post(entity, type);
    }

    @Override
    public Response put(Entity<?> entity) {
        return sync.put(entity);
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> cls) {
        return sync.put(entity, cls);
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> type) {
        return sync.put(entity, type);
    }

    @Override
    public Response trace() {
        return sync.trace();
    }

    @Override
    public <T> T trace(Class<T> cls) {
        return sync.trace(cls);
    }

    @Override
    public <T> T trace(GenericType<T> type) {
        return sync.trace(type);
    }

    @Override
    public Builder accept(String... types) {
        webClient.accept(types);
        return this;
    }

    @Override
    public Builder accept(MediaType... types) {
        webClient.accept(types);
        return this;
    }

    @Override
    public Builder acceptEncoding(String... enc) {
        webClient.acceptEncoding(enc);
        return this;

    }

    @Override
    public Builder acceptLanguage(Locale... lang) {
        for (Locale l : lang) {
            webClient.acceptLanguage(HttpUtils.toHttpLanguage(l));
        }
        return this;
    }

    @Override
    public Builder acceptLanguage(String... lang) {
        webClient.acceptLanguage(lang);
        return this;
    }

    @Override
    public Builder cacheControl(CacheControl control) {
        webClient.header(HttpHeaders.CACHE_CONTROL, control.toString());
        return this;
    }

    @Override
    public Builder cookie(Cookie cookie) {
        webClient.cookie(cookie);
        return this;
    }

    @Override
    public Builder cookie(String name, String value) {
        webClient.header(HttpHeaders.COOKIE, name + "=" + value);
        return this;
    }

    @Override
    public Builder header(String name, Object value) {
        RuntimeDelegate rd = HttpUtils.getOtherRuntimeDelegate();
        doSetHeader(rd, name, value);
        return this;
    }

    @Override
    public Builder headers(MultivaluedMap<String, Object> headers) {
        webClient.removeAllHeaders();
        if (headers != null) {
            RuntimeDelegate rd = HttpUtils.getOtherRuntimeDelegate();
            for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
                for (Object value : entry.getValue()) {
                    doSetHeader(rd, entry.getKey(), value);
                }
            }
        }
        return this;
    }

    private void doSetHeader(RuntimeDelegate rd, String name, Object value) {
        HeaderDelegate<Object> hd = HttpUtils.getHeaderDelegate(rd, value);
        if (hd != null) {
            value = hd.toString(value);
        }

        // If value is null then all current headers of the same name should be removed
        if (value == null) {
            webClient.replaceHeader(name, value);
        } else {
            webClient.header(name, value);
        }
    }

    @Override
    public Builder property(String name, @Sensitive Object value) {
        Map<String, Object> contextProps = WebClient.getConfig(webClient).getRequestContext();
        Map<String, Object> filterProps = CastUtils.cast((Map<?, ?>) contextProps.get(PROPERTY_KEY));
        if (filterProps == null) {
            filterProps = new HashMap<>();
            contextProps.put(PROPERTY_KEY, filterProps);
        }
        if (value == null) {
            filterProps.remove(name);
        } else {
            // Liberty change start need to convert proxy password to ProtectedString
            if (JAXRSClientConstants.PROXY_PASSWORD.equals(name) && value != null &&
                !(value instanceof ProtectedString)) {
                value = new ProtectedString(value.toString().toCharArray());
            } // Liberty change end
            filterProps.put(name, value);
        }
        return this;
    }

    @Override
    public AsyncInvoker async() {
        return webClient.async();
    }

    @Override
    public Invocation build(String method) {
        return new InvocationImpl(method);
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        return new InvocationImpl(method, entity);
    }

    @Override
    public Invocation buildDelete() {
        return build(HttpMethod.DELETE);
    }

    @Override
    public Invocation buildGet() {
        return build(HttpMethod.GET);
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return build(HttpMethod.POST, entity);
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return build(HttpMethod.PUT, entity);
    }

    private class InvocationImpl implements Invocation {

        private final Invocation.Builder invBuilder;
        private final String httpMethod;
        private final Entity<?> entity;

        InvocationImpl(String httpMethod) {
            this(httpMethod, null);
        }

        InvocationImpl(String httpMethod, Entity<?> entity) {
            this.invBuilder = InvocationBuilderImpl.this;
            this.httpMethod = httpMethod;
            this.entity = entity;
        }

        @Override
        public Response invoke() {
            return invBuilder.method(httpMethod, entity);
        }

        @Override
        public <T> T invoke(Class<T> cls) {
            return invBuilder.method(httpMethod, entity, cls);
        }

        @Override
        public <T> T invoke(GenericType<T> type) {
            return invBuilder.method(httpMethod, entity, type);
        }

        @Override
        public Invocation property(String name, Object value) {
            invBuilder.property(name, value);
            return this;
        }

        @Override
        public Future<Response> submit() {
            return invBuilder.async().method(httpMethod, entity);
        }

        @Override
        public <T> Future<T> submit(Class<T> cls) {
            return invBuilder.async().method(httpMethod, entity, cls);
        }

        @Override
        public <T> Future<T> submit(GenericType<T> type) {
            return invBuilder.async().method(httpMethod, entity, type);
        }

        @Override
        public <T> Future<T> submit(InvocationCallback<T> callback) {
            return invBuilder.async().method(httpMethod, entity, callback);
        }
    }

    @Override
    public CompletionStageRxInvoker rx() {
        return webClient.rx(getConfiguredExecutorService());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <T extends RxInvoker> T rx(Class<T> rxCls) {
        return webClient.rx(rxCls, getConfiguredExecutorService());
    }

    private ExecutorService getConfiguredExecutorService() {
        return (ExecutorService)config.getProperty(AbstractClient.EXECUTOR_SERVICE_PROPERTY);
    }

}
