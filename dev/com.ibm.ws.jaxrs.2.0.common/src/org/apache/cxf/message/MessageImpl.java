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
import java.util.HashMap;
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

    private Map<String, Object> contextCache;

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
            contextCache = impl.contextCache;
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
            Object tmp[] = new Object[contents.length + 10];
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

        Set<Class<?>> c = new HashSet<Class<?>>();
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

    @Override
    public Object put(String key, Object value) {
        if (contextCache != null) {
            contextCache.put(key, value);
        }
        return super.put(key, value);
    }

    @Override
    public Object getContextualProperty(String key) {
        if (contextCache == null) {
            calcContextCache();
        }
        return contextCache.get(key);
    }

    @Override
    public Set<String> getContextualPropertyKeys() {
        return contextCache.keySet();
    }

    private void calcContextCache() {
        Map<String, Object> o = new HashMap<String, Object>() {
            private static final long serialVersionUID = 7067290677790419348L;

            @Override
            public void putAll(Map<? extends String, ? extends Object> m) {
                if (m != null && m.size() > 0) {
                    super.putAll(m);
                }
            }
        };
        Exchange ex = getExchange();
        if (ex != null) {
            Bus b = ex.getBus();
            if (b != null) {
                o.putAll(b.getProperties());
            }
            Service sv = ex.getService();
            if (sv != null) {
                o.putAll(sv);
            }
            Endpoint ep = ex.getEndpoint();
            if (ep != null) {
                EndpointInfo ei = ep.getEndpointInfo();
                if (ei != null) {
                    o.putAll(ep.getEndpointInfo().getBinding().getProperties());
                    o.putAll(ep.getEndpointInfo().getProperties());
                }
                o.putAll(ep);
            }
        }
        o.putAll(ex);
        o.putAll(this);
        contextCache = o;
    }

    public static void copyContent(Message m1, Message m2) {
        for (Class<?> c : m1.getContentFormats()) {
            m2.setContent(c, m1.getContent(c));
        }
    }

    @Override
    public void resetContextCache() {
        if (contextCache != null) {
            contextCache = null;
        }
    }

    void setContextualProperty(String key, Object v) {
        if (contextCache != null && !containsKey(key)) {
            contextCache.put(key, v);
        }
    }
}
