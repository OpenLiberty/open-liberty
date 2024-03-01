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
import java.util.logging.Logger;
import java.util.List;
import java.util.logging.Level;
import javax.xml.ws.Holder;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils; // Liberty code change
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;

import com.ibm.websphere.ras.annotation.Trivial; // Liberty code change

@Trivial // Liberty code change
public class MessageImpl extends StringMapImpl implements Message {
    
    private static final Logger LOG = LogUtils.getL7dLogger(MessageImpl.class); // Liberty code change
    
    private static final long serialVersionUID = -3020763696429459865L;

    private Exchange exchange;
    private String id;
    private InterceptorChain interceptorChain;

    // array of Class<T>/T pairs for contents
    private Object[] contents = new Object[20];
    private int index;

    // private Map<String, Object> contextCache; Liberty code Change


    public MessageImpl() {
        //nothing
    }

    public MessageImpl(int initialSize, float factor) {
        super(initialSize, factor);
    }

    public MessageImpl(Message m) {
        super(m);
        if (m instanceof MessageImpl) {
            MessageImpl impl = (MessageImpl)m;
            exchange = impl.getExchange();
            id = impl.id;
            interceptorChain = impl.interceptorChain;
            contents = impl.contents;
            index = impl.index;
            // contextCache = impl.contextCache; Liberty code Change
        } else {
            throw new RuntimeException("Not a MessageImpl! " + m.getClass());
        }
    }

    public Collection<Attachment> getAttachments() {
        return CastUtils.cast((Collection<?>)get(ATTACHMENTS));
    }

    public void setAttachments(Collection<Attachment> attachments) {
        put(ATTACHMENTS, attachments);
    }

    public String getAttachmentMimeType() {
        //for sub class overriding
        return null;
    }

    public Destination getDestination() {
        return get(Destination.class);
    }

    public Exchange getExchange() {
        return exchange;
    }

    public String getId() {
        return id;
    }

    public InterceptorChain getInterceptorChain() {
        return this.interceptorChain;
    }

    @SuppressWarnings("unchecked")
    public <T> T getContent(Class<T> format) {
        // Liberty code change start - Add logging statments
        LOG.entering("MessageImpl", "getContent");
        for (int x = 0; x < index; x += 2) {
	    LOG.finest("getContent processing class: " + contents[x].getClass().getCanonicalName());
            if (contents[x] == format) {
                LOG.finest("getContent returning class: " + contents[x+1].getClass().getCanonicalName());
                return (T)contents[x + 1];
            }
        }
        LOG.exiting("MessageImpl", "getContent");
        // Liberty code change end
        return null;
    }

    public <T> void setContent(Class<T> format, Object content) {
        // Liberty code change start
        LOG.entering("MessageImpl", "setContent");
	if (LOG.isLoggable(Level.FINEST)) {
	   LOG.finest("setContent: Logging content for format: " + (format != null ? format.getCanonicalName() : "null"));
	   logContent(content);
	}
        for (int x = 0; x < index; x += 2) {
            if (contents[x] == format) {
		LOG.finest("setContent: Found format: Setting contents[" + x+1 + "] to " + (content != null ? content.getClass().getCanonicalName() : "NULL"));
                contents[x + 1] = content;
                LOG.exiting("MessageImpl", "setContent");
                return;
            }
        }
        if (index >= contents.length) {
            //very unlikely to happen.   Haven't seen more than about 6,
            //but just in case we'll add a few more
	    LOG.finest("Index: " + index + " is >= contents length: " + contents.length);
            Object[] tmp = new Object[contents.length + 10];
            System.arraycopy(contents, 0, tmp, 0, contents.length);
            contents = tmp;
        }
        contents[index] = format;
        contents[index + 1] = content;
        index += 2;
        LOG.exiting("MessageImpl", "setContent");
        // Liberty code change end
    }

    public <T> void removeContent(Class<T> format) {
        LOG.entering("MessageImpl", "removeContent");  // Liberty Change begin
        for (int x = 0; x < index; x += 2) {
            if (contents[x] == format) {
	        LOG.finest("removeContent: Found content for format: " + 
				(format != null ? format.getCanonicalName() : "null"));
                index -= 2;
                if (x != index) {
                    contents[x] = contents[index];
                    contents[x + 1] = contents[index + 1];
                }
	        LOG.finest("removeContent: " + (contents[index] != null ? contents[index].getClass().getCanonicalName() : "null"));
                contents[index] = null;
                contents[index + 1] = null;
                LOG.exiting("MessageImpl", "removeContent");
                return;
            }
        }
	LOG.exiting("MessageImpl", "removeContent"); // Liberty change end
    }

    public Set<Class<?>> getContentFormats() {

        Set<Class<?>> c = new HashSet<>();
        for (int x = 0; x < index; x += 2) {
            c.add((Class<?>)contents[x]);
        }
        return c;
    }

    public void setDestination(Destination d) {
        put(Destination.class, d);
    }

    public void setExchange(Exchange e) {
        this.exchange = e;
    }

    public void setId(String i) {
        LOG.finest("MessageImpl:setId to " + i);
        this.id = i;
    }

    public void setInterceptorChain(InterceptorChain ic) {
        this.interceptorChain = ic;
    }

    // Liberty code change start
    // Since these maps can have null value, use the getOrDefault API
    // to prevent calling get twice under the covers
    private static final Object NOT_FOUND = new Object();
    
    @Override
    public Object getContextualProperty(String key) {
        Object o = getOrDefault(key, NOT_FOUND);
        if (o != NOT_FOUND) {
            LOG.finest("MessageImpl:getContextualProperty from default for "  + key );
            return o;
        }
        LOG.finest("MessageImpl:getContextualProperty from Exchange for "  + key );
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

    private void logContent(Object content) {

	if (content instanceof List) {
           for (Object o1 : (List)content) {
                if (o1 != null && o1.getClass() != null) {
                   LOG.finest("Setcontent param: " + o1.getClass().getCanonicalName());
                }
                if (o1 instanceof Holder && o1 != null) {
                   if (((Holder)o1).value != null) {
                     LOG.finest("Setcontent Holder type: " + ((Holder)o1).value.getClass());
                   }
                }
             }
        }
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
    
    public static void copyContent(Message m1, Message m2) {
        for (Class<?> c : m1.getContentFormats()) {
            m2.setContent(c, m1.getContent(c));
        }
    }

    public void resetContextCache() {
    }

    public void setContextualProperty(String key, Object v) {
        if (!containsKey(key)) {
            put(key, v);
        }
    }
}
