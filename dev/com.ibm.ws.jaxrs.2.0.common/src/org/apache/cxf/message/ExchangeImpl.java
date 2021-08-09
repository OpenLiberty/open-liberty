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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.PreexistingConduitSelector;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.Session;

public class ExchangeImpl extends ConcurrentHashMap<String, Object>  implements Exchange {
    
    private static final long serialVersionUID = -3112077559217623594L;
    private Destination destination;
    private boolean oneWay;
    private boolean synchronous = true;
    
    private Message inMessage;
    private Message outMessage;
    private Message inFaultMessage;
    private Message outFaultMessage;
    
    private Session session;
    
    private Bus bus;
    private Endpoint endpoint;
    private Service service;
    private Binding binding;
    private BindingOperationInfo bindingOp;

    public ExchangeImpl() {
    }
    public ExchangeImpl(ExchangeImpl ex) {
        super(ex);
        this.destination = ex.destination;
        this.oneWay = ex.oneWay;
        this.synchronous = ex.synchronous;
        this.inMessage = ex.inMessage;
        this.outMessage = ex.outMessage;
        this.inFaultMessage = ex.inFaultMessage;
        this.outFaultMessage = ex.outFaultMessage;
        this.session = ex.session;
        this.bus = ex.bus;
        this.endpoint = ex.endpoint;
        this.service = ex.service;
        this.binding = ex.binding;
        this.bindingOp = ex.bindingOp;
    }

    private void resetContextCaches() {
        if (inMessage != null) {
            inMessage.resetContextCache();
        }
        if (outMessage != null) {
            outMessage.resetContextCache();
        }
        if (inFaultMessage != null) {
            inFaultMessage.resetContextCache();
        }
        if (outFaultMessage != null) {
            outFaultMessage.resetContextCache();
        }
    }
    
    public <T> T get(Class<T> key) {
        T t = key.cast(get(key.getName()));
        
        if (t == null) {
            if (key == Bus.class) {
                t = key.cast(bus);
            } else if (key == OperationInfo.class && bindingOp != null) {
                t = key.cast(bindingOp.getOperationInfo());
            } else if (key == BindingOperationInfo.class) {
                t = key.cast(bindingOp);
            } else if (key == Endpoint.class) {
                t = key.cast(endpoint);
            } else if (key == Service.class) {
                t = key.cast(service);
            } else if (key == Binding.class) {
                t = key.cast(binding);
            } else if (key == BindingInfo.class && binding != null) {
                t = key.cast(binding.getBindingInfo());
            } else if (key == InterfaceInfo.class && endpoint != null) {
                t = key.cast(endpoint.getEndpointInfo().getService().getInterface());
            } else if (key == ServiceInfo.class && endpoint != null) {
                t = key.cast(endpoint.getEndpointInfo().getService());
            }
        }
        return t;
    }

    public void putAll(Map<? extends String, ?> m) {
        for (Map.Entry<? extends String, ?> e : m.entrySet()) {
            // just skip the null value to void the NPE in JDK1.8
            if (e.getValue() != null) {
                super.put(e.getKey(), e.getValue());
            }
        }
    }

    public <T> void put(Class<T> key, T value) {
        if (value == null) {
            super.remove(key);
        } else if (key == Bus.class) {
            resetContextCaches();
            bus = (Bus)value;
        } else if (key == Endpoint.class) {
            resetContextCaches();
            endpoint = (Endpoint)value;
        } else if (key == Service.class) {
            resetContextCaches();
            service = (Service)value;
        } else if (key == BindingOperationInfo.class) {
            bindingOp = (BindingOperationInfo)value;
        } else if (key == Binding.class) {
            binding = (Binding)value;
        } else {
            super.put(key.getName(), value);
        }
    }
    
    public Object put(String key, Object value) {
        setMessageContextProperty(inMessage, key, value);
        setMessageContextProperty(outMessage, key, value);
        setMessageContextProperty(inFaultMessage, key, value);
        setMessageContextProperty(outFaultMessage, key, value);
        if (value == null) {
            return super.remove(key);
        }
        return super.put(key, value);
    }

    public <T> T remove(Class<T> key) {
        return key.cast(super.remove(key.getName()));
    }

    private void setMessageContextProperty(Message m, String key, Object value) {
        if (m == null) {
            return;
        }
        if (m instanceof MessageImpl) {
            ((MessageImpl)m).setContextualProperty(key, value);
        }  else if (m instanceof AbstractWrappedMessage) {
            ((AbstractWrappedMessage)m).setContextualProperty(key, value);
        } else {
            //cannot set directly.  Just invalidate the cache.
            m.resetContextCache();
        }
    }
    
    public Destination getDestination() {
        return destination;
    }

    public Message getInMessage() {
        return inMessage;
    }

    public Conduit getConduit(Message message) {
        return get(ConduitSelector.class) != null
               ? get(ConduitSelector.class).selectConduit(message)
               : null;
    }

    public Message getOutMessage() {
        return outMessage;
    }

    public Message getInFaultMessage() {
        return inFaultMessage;
    }

    public void setInFaultMessage(Message m) {
        inFaultMessage = m;
        if (null != m) {
            m.setExchange(this);
        }
    }

    public Message getOutFaultMessage() {
        return outFaultMessage;
    }

    public void setOutFaultMessage(Message m) {
        outFaultMessage = m;
        if (null != m) {
            m.setExchange(this);
        }
    }

    public void setDestination(Destination d) {
        destination = d;
    }

    public void setInMessage(Message m) {
        inMessage = m;
        if (null != m) {
            m.setExchange(this);
        }
    }

    public void setConduit(Conduit c) {
        put(ConduitSelector.class,
            new PreexistingConduitSelector(c, getEndpoint()));
    }

    public void setOutMessage(Message m) {
        outMessage = m;
        if (null != m) {
            m.setExchange(this);
        }
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public void setOneWay(boolean b) {
        oneWay = b;
    }
    
    public boolean isSynchronous() {
        return synchronous;
    }

    public void setSynchronous(boolean b) {
        synchronous = b;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
    
    public void clear() {
        super.clear();
        resetContextCaches();
        destination = null;
        oneWay = false;
        inMessage = null;
        outMessage = null;
        inFaultMessage = null;
        outFaultMessage = null;
        session = null;
        bus = null;
    }

    public Bus getBus() {
        return bus;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Service getService() {
        return service;
    }

    public Binding getBinding() {
        return binding;
    }

    public BindingOperationInfo getBindingOperationInfo() {
        return bindingOp;
    }
}
