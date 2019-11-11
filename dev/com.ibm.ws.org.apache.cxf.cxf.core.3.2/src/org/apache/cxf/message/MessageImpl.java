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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;

public class MessageImpl extends StringMapImpl implements Message {
    private static final long serialVersionUID = -3020763696429459865L;

    private Exchange exchange;
    private String id;
    private InterceptorChain interceptorChain;

    // array of Class<T>/T pairs for contents
    private Object[] contents = new Object[20];
    private int index;

    // Liberty change - used to avoid resize
    public MessageImpl(int isize, float factor) {
        super(isize, factor);
    }

    public MessageImpl() {
        //nothing
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
        } else {
            throw new RuntimeException("Not a MessageImpl! " + m.getClass());
        }
    }

    @Override
    public Collection<Attachment> getAttachments() {
        return CastUtils.cast((Collection<?>) get(ATTACHMENTS));
    }

    @Override
    public void setAttachments(Collection<Attachment> attachments) {
        put(ATTACHMENTS, attachments);
    }

    public String getAttachmentMimeType() {
        //for sub class overriding
        return null;
    }

    @Override
    public Destination getDestination() {
        return get(Destination.class);
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
        put(Destination.class, d);
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
    private static final Object NOT_FOUND = new Object();
    
    @Override
    public Object getContextualProperty(String key) {
        Object o = getOrDefault(key, NOT_FOUND);
        if (o != NOT_FOUND) {
            return o;
        }
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
        if (!containsKey(key)) {
            put(key, v);
        }
    }
    //Liberty code change end
}
