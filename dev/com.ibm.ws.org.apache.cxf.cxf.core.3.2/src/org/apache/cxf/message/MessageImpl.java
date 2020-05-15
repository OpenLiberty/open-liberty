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

package org.apache.cxf.message;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.https.CertConstraints;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class MessageImpl extends StringMapImpl implements Message {
    private static final long serialVersionUID = -3020763696429459865L;

    private Exchange exchange;
    private String id;
    private InterceptorChain interceptorChain;

    // array of Class<T>/T pairs for contents
    private Object[] contents = new Object[20];
    private int index;

    //Liberty code change start
    private static int contentType = 0;
    private static int protoHeaders = 1;
    private static int queryString = 2;
    private static int httpRequest = 3;
    private static int httpResponse = 4;
    private static int pathToMatchSlash = 5;
    private static int httpRequestMethod = 6;
    private static int interceptorProviders = 7;
    private static int templateParameters = 8;
    private static int accept = 9;
    private static int continuationProvider = 10;
    private static int destination = 11;
    private static int opStack = 12;
    private static int wsdlDescription = 13;
    private static int wsdlInterface = 14;
    private static int wsdlOperation = 15;
    private static int wsdlPort = 16;
    private static int wsdlService = 17;
    private static int requestUrl = 18;
    private static int requestUri = 19;
    private static int pathInfo = 20;
    private static int basePath = 21;
    private static int fixedParamOrder = 22;
    private static int inInterceptors = 23;
    private static int outInterceptors = 24;
    private static int responseCode = 25;
    private static int attachments = 26;
    private static int encoding = 27;
    private static int httpContext = 28;
    private static int httpConfig = 29;
    private static int httpContextMatchStrategy = 30;
    private static int httpBasePath = 31;
    private static int asyncPostDispatch = 32;
    private static int securityContext = 33;
    private static int authorizationPolicy = 34;
    private static int certConstraints = 35;
    private static int serviceRedirection = 36;
    private static int httpServletResponse = 37;
    private static int resourceMethod = 38;
    private static int oneWayRequest = 39;
    private static int asyncResponse = 40;
    private static int threadContextSwitched = 41;
    private static int cacheInputProperty = 42;
    private static int previousMessage = 43;
    private static int responseHeadersCopied = 44;
    private static int sseEventSink = 45;
    private static int requestorRole = 46;
    private static int partialResponse = 47;
    private static int emptyPartialResponse = 48;
    private static int endpointAddress = 49;
    private static int inboundMessage = 50;
    private static int TOTAL = 51;
    private Object[] propertyValues = new Object[TOTAL];
    
    private static final String REQUEST_PATH_TO_MATCH_SLASH = "path_to_match_slash";
    private static final String TEMPLATE_PARAMETERS = "jaxrs.template.parameters";
    private static final String CONTINUATION_PROVIDER = ContinuationProvider.class.getName();
    private static final String DESTINATION = Destination.class.getName();
    private static final String OP_RES_INFO_STACK = "org.apache.cxf.jaxrs.model.OperationResourceInfoStack";
    private static final String HTTP_BASE_PATH = "http.base.path";
    private static final String SECURITY_CONTEXT = SecurityContext.class.getName();
    private static final String AUTHORIZATION_POLICY = AuthorizationPolicy.class.getName();
    private static final String CERT_CONSTRAINTS = CertConstraints.class.getName();
    private static final String HTTP_SERVLET_RESPONSE = HttpServletResponse.class.getName();
    private static final String RESOURCE_METHOD = "org.apache.cxf.resource.method";
    private static final String ASYNC_RESPONSE = "javax.ws.rs.container.AsyncResponse";
    private static final String SSE_EVENT_SINK = "javax.ws.rs.sse.SseEventSink";
    private static final Map<String, Integer> KEYMAP;
    private static final String[] propertyNames = { CONTENT_TYPE, PROTOCOL_HEADERS, QUERY_STRING, AbstractHTTPDestination.HTTP_REQUEST,
                    AbstractHTTPDestination.HTTP_RESPONSE, REQUEST_PATH_TO_MATCH_SLASH, HTTP_REQUEST_METHOD, INTERCEPTOR_PROVIDERS,
                    TEMPLATE_PARAMETERS, ACCEPT_CONTENT_TYPE, CONTINUATION_PROVIDER, DESTINATION, OP_RES_INFO_STACK, WSDL_DESCRIPTION,
                    WSDL_INTERFACE, WSDL_OPERATION, WSDL_PORT, WSDL_SERVICE, REQUEST_URL, REQUEST_URI, PATH_INFO, BASE_PATH,
                    FIXED_PARAMETER_ORDER, IN_INTERCEPTORS, OUT_INTERCEPTORS, RESPONSE_CODE, ATTACHMENTS, ENCODING,
                    AbstractHTTPDestination.HTTP_CONTEXT, AbstractHTTPDestination.HTTP_CONFIG, AbstractHTTPDestination.HTTP_CONTEXT_MATCH_STRATEGY,
                    HTTP_BASE_PATH, ASYNC_POST_RESPONSE_DISPATCH, SECURITY_CONTEXT, AUTHORIZATION_POLICY, CERT_CONSTRAINTS,
                    AbstractHTTPDestination.SERVICE_REDIRECTION, HTTP_SERVLET_RESPONSE, RESOURCE_METHOD, ONE_WAY_REQUEST, ASYNC_RESPONSE,
                    THREAD_CONTEXT_SWITCHED, OutgoingChainInterceptor.CACHE_INPUT_PROPERTY, PhaseInterceptorChain.PREVIOUS_MESSAGE,
                    AbstractHTTPDestination.RESPONSE_HEADERS_COPIED, SSE_EVENT_SINK, REQUESTOR_ROLE, PARTIAL_RESPONSE_MESSAGE,
                    EMPTY_PARTIAL_RESPONSE_MESSAGE, ENDPOINT_ADDRESS, INBOUND_MESSAGE };

    private static final Object NOT_FOUND = new Object();
    private static final Integer KEY_NOT_FOUND = Integer.valueOf(-1);

    private Collection<Object> values = null;
    private Set<String> keySet = null;
    private Set<Map.Entry<String, Object>> entrySet = null;
    private static final Object[] NOT_SET_ARRAY = new Object[TOTAL];
    static {
        Map<String, Integer> keymap = new HashMap<String, Integer>(TOTAL);
        for (int i = 0; i < TOTAL; i++) {
            keymap.put(propertyNames[i], i);
            NOT_SET_ARRAY[i] = NOT_FOUND;
        }
        KEYMAP = Collections.unmodifiableMap(keymap);
    }
    //Liberty code change end

    // Liberty change - used to avoid resize
    public MessageImpl(int isize, float factor) {
        super(isize, factor);
        //Liberty code change start
        System.arraycopy(NOT_SET_ARRAY, 0, propertyValues, 0, TOTAL);
        //Liberty code change end
    }

    public MessageImpl() {
        //Liberty code change start
        System.arraycopy(NOT_SET_ARRAY, 0, propertyValues, 0, TOTAL);
        //Liberty code change end
    }

    public MessageImpl(Message m) {
        super(m);
        if (m instanceof MessageImpl) {
            MessageImpl impl = (MessageImpl) m;
            exchange = impl.getExchange();
            id = impl.id;
            interceptorChain = impl.interceptorChain;
            contents = impl.contents;
            index = impl.index;
            //Liberty code change start
            System.arraycopy(NOT_SET_ARRAY, 0, propertyValues, 0, TOTAL);
            //Liberty code change end
        } else {
            throw new RuntimeException("Not a MessageImpl! " + m.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Attachment> getAttachments() {
        //Liberty code change start
        return (Collection<Attachment>) getFromPropertyArray(attachments);
        //Liberty code change end
    }

    @Override
    public void setAttachments(Collection<Attachment> a) {
        //Liberty code change start
        propertyValues[attachments] = a;
        //Liberty code change end
    }

    public String getAttachmentMimeType() {
        //for sub class overriding
        return null;
    }

    @Override
    public Destination getDestination() {
        //Liberty code change start
        return (Destination) getFromPropertyArray(destination);
        //Liberty code change start
    }

    @Override
    public Exchange getExchange() {
        return exchange;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public InterceptorChain getInterceptorChain() {
        return this.interceptorChain;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getContent(Class<T> format) {
        for (int x = 0; x < index; x += 2) {
            if (contents[x] == format) {
                return (T) contents[x + 1];
            }
        }
        return null;
    }

    @Override
    public <T> void setContent(Class<T> format, Object content) {
        for (int x = 0; x < index; x += 2) {
            if (contents[x] == format) {
                contents[x + 1] = content;
                return;
            }
        }
        if (index >= contents.length) {
            //very unlikely to happen.   Haven't seen more than about 6,
            //but just in case we'll add a few more
            Object[] tmp = new Object[contents.length + 10];
            System.arraycopy(contents, 0, tmp, 0, contents.length);
            contents = tmp;
        }
        contents[index] = format;
        contents[index + 1] = content;
        index += 2;
    }

    @Override
    public <T> void removeContent(Class<T> format) {
        for (int x = 0; x < index; x += 2) {
            if (contents[x] == format) {
                index -= 2;
                if (x != index) {
                    contents[x] = contents[index];
                    contents[x + 1] = contents[index + 1];
                }
                contents[index] = null;
                contents[index + 1] = null;
                return;
            }
        }
    }

    @Override
    public Set<Class<?>> getContentFormats() {

        Set<Class<?>> c = new HashSet<>();
        for (int x = 0; x < index; x += 2) {
            c.add((Class<?>) contents[x]);
        }
        return c;
    }

    public void setDestination(Destination d) {
        //Liberty code change start
    	propertyValues[destination] = d;
        //Liberty code change end
    }

    @Override
    public void setExchange(Exchange e) {
        this.exchange = e;
    }

    @Override
    public void setId(String i) {
        this.id = i;
    }

    @Override
    public void setInterceptorChain(InterceptorChain ic) {
        this.interceptorChain = ic;
    }

    //Liberty code change start
    // Since these maps can have null value, use the getOrDefault API
    // to prevent calling get twice under the covers
    @Override
    public Object getContextualProperty(String key) {
        //Liberty code change start
        Object o = getOrDefault(key, NOT_FOUND);
        if (o != NOT_FOUND) {
            return o;
        }
        //Liberty code change end

        return getFromExchange(key);
    }

    private Object getFromExchange(String key) {
        Exchange ex = getExchange();
        if (ex != null) {
            Object o = ex.getOrDefault(key, NOT_FOUND);
            if (o != NOT_FOUND) {
                return o;
            }
            
            Map<String, Object> p;
            Endpoint ep = ex.getEndpoint();
            if (ep != null) {
                o = ep.getOrDefault(key, NOT_FOUND);
                if (o != NOT_FOUND) {
                    return o;
                }

                EndpointInfo ei = ep.getEndpointInfo();
                if (ei != null) {
                    if ((p = ei.getProperties()) != null && (o = p.getOrDefault(key, NOT_FOUND)) != NOT_FOUND) {
                        return o;
                    }
                    if ((p = ei.getBinding().getProperties()) != null && (o = p.getOrDefault(key, NOT_FOUND)) != NOT_FOUND) {
                        return o;
                    }
                }
            }
            Service sv = ex.getService();
            if (sv != null && (o = sv.getOrDefault(key, NOT_FOUND)) != NOT_FOUND) {
                return o;
            }
            Bus b = ex.getBus();
            if (b != null && (p = b.getProperties()) != null) {
                if ((o = p.getOrDefault(key, NOT_FOUND)) != NOT_FOUND) {
                    return o;
                }
            }
        }
        return null;
    }

    private Set<String> getExchangeKeySet() {
        HashSet<String> keys = new HashSet<>();
        Exchange ex = getExchange();
        if (ex != null) {
            Bus b = ex.getBus();
            Map<String, Object> p;
            if (b != null && (p = b.getProperties()) != null) {
                if (!p.isEmpty()) {
                    keys.addAll(p.keySet());
                }
            }
            Service sv = ex.getService();
            if (sv != null && !sv.isEmpty()) {
                keys.addAll(sv.keySet());
            }
            Endpoint ep = ex.getEndpoint();
            if (ep != null) {
                EndpointInfo ei = ep.getEndpointInfo();
                if (ei != null) {
                    if ((p = ei.getBinding().getProperties()) != null) {
                        if (!p.isEmpty()) {
                            keys.addAll(p.keySet());
                        }
                    }
                    if ((p = ei.getProperties()) != null) {
                        if (!p.isEmpty()) {
                            keys.addAll(p.keySet());
                        }
                    }
                }
                
                if (!ep.isEmpty()) {
                    keys.addAll(ep.keySet());
                }
            }
            if (!ex.isEmpty()) {
                keys.addAll(ex.keySet());
            }
        }
        return keys;
    }

    @Override
    public Set<String> getContextualPropertyKeys() {
        Set<String> s = getExchangeKeySet();
        s.addAll(keySet());
        return s;
    }
    //Liberty code change end
    
    public static void copyContent(Message m1, Message m2) {
        for (Class<?> c : m1.getContentFormats()) {
            m2.setContent(c, m1.getContent(c));
        }
    }

    //Liberty code change start
    @Override
    public void resetContextCache() {
    }

    void setContextualProperty(String key, Object v) {
        putIfAbsent(key, v);
    }
    
    @SuppressWarnings("rawtypes")
    public Map getProtocolHeaders() {
        return (Map) getFromPropertyArray(protoHeaders);
    }
    
    @SuppressWarnings("rawtypes")
    public void setProtocolHeaders(Map p) {
        propertyValues[protoHeaders] = p;
    }
    
    @Override
    public Object remove(Object key) {
        return remove((String) key);
    }
    
    public Object remove(String key) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            Object ret = getFromPropertyArray(index);
            propertyValues[index] = NOT_FOUND;
            return ret;
        }
        return super.remove(key);
    }
    
    @Override
    public Object get(Object key) {
        return get((String) key);
    }

    public Object get(String key) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            return getFromPropertyArray(index);
        }
        
        return super.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            Object ret = getFromPropertyArray(index);
            propertyValues[index] = value;
            return ret;
        }

        return super.put(key, value);
    }

    @Trivial
    @Override
    public Set<String> keySet() {
        return keySet != null ? keySet : (keySet = new KeySet());
    }
    
    @Trivial
    abstract class MessageIterator<T> implements Iterator<T>{
        Iterator<T> backedIterator;
        int current = -1;
        int next = -1;
        boolean removeAllowed = false;
        MessageIterator(Iterator<T> it) {
            backedIterator = it;
        }
        @Override
        public final boolean hasNext() {
            if (backedIterator.hasNext()) {
                return true;
            }
            if (next == TOTAL) {
                return false;
            }
            if (next > current) {
                return true;
            }
            for (next = current+1; next < TOTAL; next++) {
                if (propertyValues[next] != NOT_FOUND) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public final T next() {
            if (hasNext()) {
                if (next == -1) {
                    removeAllowed = true;
                    return backedIterator.next();
                } else {
                    current = next;
                    removeAllowed = true;
                    return getNextFromProperties(next);
                }
            }
            removeAllowed = false;
            throw new NoSuchElementException();
        }
        @Override
        public final void remove() {
            if (!removeAllowed) {
                throw new IllegalStateException();
            }
            if (current == -1) {
                backedIterator.remove();
            } else {
                propertyValues[current] = NOT_FOUND;
            }
            removeAllowed = false;
        }
        abstract T getNextFromProperties(int index);
    }
    
    @Trivial
    final class KeyIterator extends MessageIterator<String> {
        KeyIterator() {
            super(MessageImpl.super.keySet().iterator());
        }

        @Override
        String getNextFromProperties(int index) {
            return propertyNames[index];
        }
        
    }

    @Trivial
    final class EntryIterator extends MessageIterator<Map.Entry<String, Object>> {
        EntryIterator() {
            super(MessageImpl.super.entrySet().iterator());
        }

        @Override
        Map.Entry<String, Object> getNextFromProperties(int index) {
            return new AbstractMap.SimpleEntry<String, Object>(propertyNames[index], propertyValues[index]);
        }
        
    }
    
    @Trivial
    final class ValuesIterator extends MessageIterator<Object> {
        ValuesIterator() {
            super(MessageImpl.super.values().iterator());
        }

        @Override
        Object getNextFromProperties(int index) {
            return propertyValues[index];
        }
        
    }
    
    @Trivial
    final class KeySet extends AbstractSet<String> {
        public final int size() {
            return MessageImpl.this.size();
        }
        public final void clear() {
            MessageImpl.this.clear();
        }
        public final Iterator<String> iterator() {
            return new KeyIterator(); 
        }
        public final boolean contains(Object o) {
            return containsKey(o);
        }
        public final boolean remove(Object key) {
            if (containsKey(key)) {
                MessageImpl.this.remove(key);
                return true;
            }
            return false;
        }
        public final Spliterator<String> spliterator() {
           throw new UnsupportedOperationException();
        }
        public final void forEach(Consumer<? super String> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            for (int i = 0; i < TOTAL; i++) {
                if (propertyValues[i] != NOT_FOUND) {
                    action.accept(propertyNames[i]);
                }
            }
            for (String k : MessageImpl.super.keySet()) {
                action.accept(k);
            }
        }
    }
    
    @Trivial
    final class Values extends AbstractCollection<Object> {
        public final int size() {
            return MessageImpl.this.size();
        }
        public final void clear() {
            MessageImpl.this.clear();
        }
        public final Iterator<Object> iterator() {
            return new ValuesIterator(); 
        }
        public final boolean contains(Object o) {
            return containsValue(o);
        }
        public final Spliterator<Object> spliterator() {
           throw new UnsupportedOperationException();
        }
        public final void forEach(Consumer<? super Object> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            for (int i = 0; i < TOTAL; i++) {
                if (propertyValues[i] != NOT_FOUND) {
                    action.accept(propertyValues[i]);
                }
            }
            for (Object v : MessageImpl.super.values()) {
                action.accept(v);
            }
        }
    }
    
    @Trivial
    final class EntrySet extends AbstractSet<Map.Entry<String, Object>> {
        public final int size() {
            return MessageImpl.this.size();
        }
        public final void clear() {
            MessageImpl.this.clear();
        }
        public final Iterator<Map.Entry<String, Object>> iterator() {
            return new EntryIterator(); 
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            String key = (String) e.getKey();
            
            Object val = getOrDefault(key, NOT_FOUND);
            if (val != NOT_FOUND) {
                AbstractMap.SimpleEntry<?,?> entry = new AbstractMap.SimpleEntry<>(key, val);
                return e.equals(entry);
            }
            return false;
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object val = e.getValue();
                return MessageImpl.this.remove(key, val);
            }
            return false;
        }
        public final Spliterator<Map.Entry<String, Object>> spliterator() {
           throw new UnsupportedOperationException();
        }
        public final void forEach(Consumer<? super Map.Entry<String, Object>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            for (int i = 0; i < TOTAL; i++) {
                if (propertyValues[i] != NOT_FOUND) {
                    action.accept(new AbstractMap.SimpleEntry<>(propertyNames[i], propertyValues[i]));
                }
            }
            for (Map.Entry<String, Object> e : MessageImpl.super.entrySet()) {
                action.accept(e);
            }
        }
    }

    @Override
    public boolean containsKey(Object key) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            return propertyValues[index] != NOT_FOUND;
        }
        return super.containsKey(key);
    }
    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
    @Trivial
    @Override
    public Collection<Object> values() {
        return values != null ? values : (values = new Values());
    }

    @Trivial
    @Override
    public Set<Map.Entry<String,Object>> entrySet() {
        return entrySet != null ? entrySet : (entrySet = new EntrySet());
    }

    public Object getAuthorizationPolicy() {
        return getFromPropertyArray(authorizationPolicy);
    }

    public void setAuthorizationPolicy(Object a) {
        propertyValues[authorizationPolicy] = a;
    }

    public Object getCertConstraints() {
        return getFromPropertyArray(certConstraints);
    }

    public void setCertConstraints(Object c) {
        propertyValues[certConstraints] = c;
    }

    public Object getServiceRedirection() {
        return getFromPropertyArray(serviceRedirection);
    }

    public void setServiceRedirection(Object s) {
        propertyValues[serviceRedirection] = s;
    }

    public Object getHttpServletResponse() {
        return getFromPropertyArray(httpServletResponse);
    }

    public void setHttpServletResponse(Object h) {
        propertyValues[httpServletResponse] = h;
    }

    public Object getResourceMethod() {
        return getFromPropertyArray(resourceMethod);
    }

    public void setResourceMethod(Object r) {
        propertyValues[resourceMethod] = r;
    }

    public Object getOneWayRequest() {
        return getFromPropertyArray(oneWayRequest);
    }

    public void setOneWayRequest(Object o) {
        propertyValues[oneWayRequest] = o;
    }

    public Object getAsyncResponse() {
        return getFromPropertyArray(asyncResponse);
    }

    public void setAsyncResponse(Object a) {
        propertyValues[asyncResponse] = a;
    }

    public Object getThreadContextSwitched() {
        return getFromPropertyArray(threadContextSwitched);
    }

    public void setThreadContextSwitched(Object t) {
        propertyValues[threadContextSwitched] = t;
    }

    public Object getPreviousMessage() {
        return getFromPropertyArray(previousMessage);
    }

    public boolean containsPreviousMessage() {
        return propertyValues[previousMessage] != NOT_FOUND;
    }

    public void setPreviousMessage(Object p) {
        propertyValues[previousMessage] = p;
    }

    public Object getCacheInputProperty() {
        return getFromPropertyArray(cacheInputProperty);
    }

    public void setCacheInputProperty(Object c) {
        propertyValues[cacheInputProperty] = c;
    }

    public Object getSseEventSink() {
        return getFromPropertyArray(sseEventSink);
    }

    public void setSseEventSink(Object s) {
        propertyValues[sseEventSink] = s;
    }

    public Object getResponseHeadersCopied() {
        return getFromPropertyArray(responseHeadersCopied);
    }

    public void setResponseHeadersCopied(Object r) {
        propertyValues[responseHeadersCopied] = r;
    }

    public Object getRequestorRole() {
        return getFromPropertyArray(requestorRole);
    }

    public void setRequestorRole(Object r) {
        propertyValues[requestorRole] = r;
    }

    public Object getEmptyPartialResponse() {
        return getFromPropertyArray(emptyPartialResponse);
    }

    public void setEmptyPartialResponse(Object e) {
        propertyValues[emptyPartialResponse] = e;
    }

    public Object getPartialResponse() {
        return getFromPropertyArray(partialResponse);
    }

    public void setPartialResponse(Object p) {
        propertyValues[partialResponse] = p;
    }

    public Object getEndpointAddress() {
        return getFromPropertyArray(endpointAddress);
    }

    public void setEndpointAddress(Object e) {
        propertyValues[endpointAddress] = e;
    }
    
    public Object getInboundMessage() {
        return getFromPropertyArray(inboundMessage);
    }

    public void setInboundMessage(Object i) {
        propertyValues[inboundMessage] = i;
    }
    public String getPathToMatchSlash() {
        return (String) getFromPropertyArray(pathToMatchSlash);
    }
    
    public void setPathToMatchSlash(String p) {
        propertyValues[pathToMatchSlash] = p;
    }
    
    public String getHttpRequestMethod() {
        return (String) getFromPropertyArray(httpRequestMethod);
    }
    
    public void setHttpRequestMethod(String h) {
        propertyValues[httpRequestMethod] = h;
    }

    public void removePathToMatchSlash() {
        propertyValues[pathToMatchSlash] = NOT_FOUND;
    }
    public String getQueryString() {
        return (String) getFromPropertyArray(queryString);
    }
    
    public void setQueryString(String q) {
        propertyValues[queryString] = q;
    }
    public Object getOperationResourceInfoStack() {
        return getFromPropertyArray(opStack);
    }
    
    public void setOperationResourceInfoStack(Object o) {
        propertyValues[opStack] = o;
    }

    public String getContentType() {
        return (String) getFromPropertyArray(contentType);
    }
    
    public boolean containsContentType() {
        return propertyValues[contentType] != NOT_FOUND;
    }
    
    public void setContentType(String c) {
        propertyValues[contentType] = c;
    }

    public Object getHttpRequest() {
        return getFromPropertyArray(httpRequest);
    }
    
    public boolean containsHttpRequest() {
        return propertyValues[httpRequest] != NOT_FOUND;
    }
    
    public void setHttpRequest(Object h) {
        propertyValues[httpRequest] = h;
    }
    
    public Object getHttpResponse() {
        return getFromPropertyArray(httpResponse);
    }
    
    public void setHttpResponse(Object h) {
        propertyValues[httpResponse] = h;
    }

    public Object getAccept() {
        return getFromPropertyArray(accept);
    }
    
    public void setAccept(Object a) {
        propertyValues[accept] = a;
    }

    public Object getContinuationProvider() {
        return getFromPropertyArray(continuationProvider);
    }
    
    public void setContinuationProvider(Object c) {
        propertyValues[continuationProvider] = c;
    }

    public Object getWsdlDescription() {
        return getFromPropertyArray(wsdlDescription);
    }
    
    public void setWsdlDescription(Object w) {
        propertyValues[wsdlDescription] = w;
    }

    public Object getWsdlInterface() {
        return getFromPropertyArray(wsdlInterface);
    }
    
    public void setWsdlInterface(Object w) {
        propertyValues[wsdlInterface] = w;
    }

    public Object getWsdlOperation() {
        return getFromPropertyArray(wsdlOperation);
    }
    
    public void setWsdlOperation(Object w) {
        propertyValues[wsdlOperation] = w;
    }

    public Object getWsdlPort() {
        return getFromPropertyArray(wsdlPort);
    }
    
    public void setWsdlPort(Object w) {
        propertyValues[wsdlPort] = w;
    }

    public Object getWsdlService() {
        return getFromPropertyArray(wsdlService);
    }
    
    public void setWsdlService(Object w) {
        propertyValues[wsdlService] = w;
    }

    public Object getRequestUrl() {
        return getFromPropertyArray(requestUrl);
    }
    
    public void setRequestUrl(Object r) {
        propertyValues[requestUrl] = r;
    }

    public Object getRequestUri() {
        return getFromPropertyArray(requestUri);
    }
    
    public void setRequestUri(Object r) {
        propertyValues[requestUri] = r;
    }
    
    public Object getPathInfo() {
        return getFromPropertyArray(pathInfo);
    }
    
    public void setPathInfo(Object p) {
       propertyValues[pathInfo] = p;
    }
    
    public Object getBasePath() {
        return getFromPropertyArray(basePath);
    }
    
    public boolean containsBasePath() {
        return propertyValues[basePath] != NOT_FOUND;
    }
    
    public void setBasePath(Object b) {
        propertyValues[basePath] = b;
    }

    public Object getFixedParamOrder() {
        return getFromPropertyArray(fixedParamOrder);
    }
    
    public void setFixedParamOrder(Object f) {
        propertyValues[fixedParamOrder] = f;
    }

    public Object getInInterceptors() {
        return getFromPropertyArray(inInterceptors);
    }
    
    public void setInInterceptors(Object i) {
        propertyValues[inInterceptors] = i;
    }

    public Object getOutInterceptors() {
        return getFromPropertyArray(outInterceptors);
    }
    
    public void setOutInterceptors(Object o) {
        propertyValues[outInterceptors] = o;
    }

    public Object getResponseCode() {
        return getFromPropertyArray(responseCode);
    }
    
    public void setResponseCode(Object r) {
        propertyValues[responseCode] = r;
    }

    public Object getEncoding() {
        return getFromPropertyArray(encoding);
    }
    
    public void setEncoding(Object e) {
        propertyValues[encoding] = e;
    }

    public Object getHttpContext() {
        return getFromPropertyArray(httpContext);
    }
    
    public void setHttpContext(Object h) {
        propertyValues[httpContext] = h;
    }

    public Object getHttpConfig() {
        return getFromPropertyArray(httpConfig);
    }
    
    public void setHttpConfig(Object h) {
        propertyValues[httpConfig] = h;
    }

    public Object getHttpContextMatchStrategy() {
        return getFromPropertyArray(httpContextMatchStrategy);
    }
    
    public void setHttpContextMatchStrategy(Object h) {
        propertyValues[httpContextMatchStrategy] = h;
    }

    public Object getHttpBasePath() {
        return getFromPropertyArray(httpBasePath);
    }
    
    public void setHttpBasePath(Object h) {
        propertyValues[httpBasePath] = h;
    }

    public Object getAsyncPostDispatch() {
        return getFromPropertyArray(asyncPostDispatch);
    }
    
    public void setAsyncPostDispatch(Object a) {
        propertyValues[asyncPostDispatch] = a;
    }
    
    public Object getSecurityContext() {
        return getFromPropertyArray(securityContext);
    }
    
    public void setSecurityContext(Object s) {
        propertyValues[securityContext] = s;
    }

    @SuppressWarnings("rawtypes")
    public Collection getInterceptorProviders() {
        return (Collection) getFromPropertyArray(interceptorProviders);
    }
    
    @SuppressWarnings("rawtypes")
    public void setInterceptorProviders(Collection i) {
        propertyValues[interceptorProviders] = i;
    }

    public Object getTemplateParameters() {
        return getFromPropertyArray(templateParameters);
    }
    
    public void setTemplateParameters(Object t) {
        propertyValues[templateParameters] = t;
    }

    public void removeContentType() {
        propertyValues[contentType] = NOT_FOUND;
    }
    public void removeHttpResponse() {
        propertyValues[httpResponse] = NOT_FOUND;
    }
    public void removeHttpRequest() {
        propertyValues[httpRequest] = NOT_FOUND;
    }
    
    private Object getFromPropertyArray(int index) {
        Object value = propertyValues[index];
        return value == NOT_FOUND ? null : value;
    }
    
    @Override
    public int size() {
        int size = super.size();
        for (Object o : propertyValues) {
            if (o != NOT_FOUND) {
                size++;
            }
        }
        return size;
    }
    
    @Override
    public void clear() {
        super.clear();
        for (int i = 0; i < TOTAL; i++) {
            propertyValues[i] = NOT_FOUND;
        }
    }
    
    @Override
    public boolean isEmpty() {
        if (!super.isEmpty()) {
            return false;
        }
        
        for (Object o : propertyValues) {
            if (o != NOT_FOUND) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean containsValue(Object value) {
        if (super.containsValue(value)) {
            return true;
        }
        if (value == null) {
            for (Object o : propertyValues) {
                if (o == null) {
                    return true;
                }
            }
        } else {
            for (Object o : propertyValues) {
                if (o != null && o != NOT_FOUND && value.equals(o)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public Object getOrDefault(Object key, Object d) {
        return getOrDefault((String) key, d);
    }
    
    public Object getOrDefault(String key, Object d) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            return propertyValues[index] == NOT_FOUND ? d : propertyValues[index];
        }

        return super.getOrDefault(key, d);
    }
    
    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        super.forEach(action);
        for (int i = 0; i < TOTAL; i++) {
            if (propertyValues[i] != NOT_FOUND) {
                action.accept(propertyNames[i], propertyValues[i]);
            }
        }
    }
    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ? extends Object> function) {
        super.replaceAll(function);
        for (int i = 0; i < TOTAL; i++) {
            if (propertyValues[i] != NOT_FOUND) {
                propertyValues[i] = function.apply(propertyNames[i], propertyValues[i]);
            }
        }
    }
    @Override
    public Object replace(String key, Object value) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            if (propertyValues[index] != NOT_FOUND) {
                Object ret = propertyValues[index];
                propertyValues[index] = value;
                return ret;
            } else {
                return null;
            }
        }

        return super.replace(key, value);
    }
    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            if (propertyValues[index] != NOT_FOUND) {
                if (oldValue == null) {
                    if (propertyValues[index] == null) {
                        propertyValues[index] = newValue;
                        return true;
                    } else {
                        return false;
                    }
                }
                if (propertyValues[index] == null) {
                    return false;
                }
                if (oldValue.equals(propertyValues[index])){
                    propertyValues[index] = newValue;
                    return true;
                }
                return false;
            }
            return false;
        }
        return super.replace(key, oldValue, newValue);
    }
    @Override
    public Object putIfAbsent(String key, Object value) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            if (propertyValues[index] == NOT_FOUND) {
                propertyValues[index] = value;
                return null;
            } else {
                return propertyValues[index];
            }
        }
        return super.putIfAbsent(key, value);
    }
    @Override
    public boolean remove(Object key, Object value) {
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            if (propertyValues[index] != NOT_FOUND) {
                if (value == null) {
                    if (propertyValues[index] == null) {
                        propertyValues[index] = NOT_FOUND;
                        return true;
                    } else {
                        return false;
                    }
                } else if (propertyValues[index] == null) {
                    return false;
                } else if (value.equals(propertyValues[index])) {
                    propertyValues[index] = NOT_FOUND;
                    return true;
                }
                return false;
            }
            return false;
        }
        return super.remove(key, value);
    }
    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        if (remappingFunction == null) {
            throw new NullPointerException();
        }
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            Object newValue = remappingFunction.apply(key, propertyValues[index] == NOT_FOUND ? null : propertyValues[index]);
            propertyValues[index] = newValue == null ? NOT_FOUND : newValue;
            return newValue;
        }
        return super.compute(key, remappingFunction);
    }
    @Override
    public Object computeIfAbsent(String key, Function<? super String, ? extends Object> mappingFunction) {
        if (mappingFunction == null) {
            throw new NullPointerException();
        }
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            if (propertyValues[index] == NOT_FOUND) {
                Object newValue = mappingFunction.apply(key);
                if (newValue != null) {
                    propertyValues[index] = newValue;
                }
                return newValue;
            }
            return getFromPropertyArray(index);
        }
        return super.computeIfAbsent(key, mappingFunction);
    }
    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
        if (remappingFunction == null) {
            throw new NullPointerException();
        }
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            if (propertyValues[index] != NOT_FOUND) {
                Object newValue = remappingFunction.apply(key, propertyValues[index]);
                propertyValues[index] = newValue == null ? NOT_FOUND : newValue;
                return newValue;
            }
            return null;
        }
        return super.computeIfPresent(key, remappingFunction);
    }
    @Override
    public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        if (value == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        Integer index = KEYMAP.getOrDefault(key, KEY_NOT_FOUND);
        if (index != KEY_NOT_FOUND) {
            if (propertyValues[index] != NOT_FOUND && propertyValues[index] != null) {
                Object newValue = remappingFunction.apply(propertyValues[index], value);
                propertyValues[index] = newValue == null ? NOT_FOUND : newValue;
            } else {
                propertyValues[index] = value;
            }
            return getFromPropertyArray(index);
        }
        return super.merge(key, value, remappingFunction);
    }
    public String[] getPropertyNames() {
        return propertyNames;
    }

    @FFDCIgnore(ConcurrentModificationException.class)
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i=0; i<TOTAL; i++) {
            if (propertyValues[i] != NOT_FOUND) {
                sb.append(propertyNames[i]).append("=").append(propertyValues[i]).append(", ");
            }
        }
        try {
            super.forEach((k, v) -> { sb.append(k).append("=").append(v).append(", "); });
        } catch (ConcurrentModificationException ex) {
            sb.append(" ConcurrentModificationException caught!");
        }
        int len = sb.length();
        if (len > 3) {
            sb.setLength(len-2); //remove trailing comma and space
        }
        sb.append("}");
        return sb.toString();
    }
    //Liberty code change end
}
