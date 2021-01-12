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

package org.apache.cxf.service.factory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.Service;
import org.apache.cxf.staxutils.StaxUtils;
import org.w3c.dom.Document;

public abstract class AbstractServiceFactoryBean {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractServiceFactoryBean.class);

    protected boolean dataBindingSet;
    protected List<String> schemaLocations;

    private Bus bus;
    private DataBinding dataBinding;
    private Service service;
    private final List<FactoryBeanListener> listeners = new ModCountCopyOnWriteArrayList<>();
    private final Map<String, Object> sessionState = new HashMap<>();

    public abstract Service create();

    /**
     * Returns a map that is useful for ServiceFactoryBeanListener to store state across
     * events during processing.
     */
    public Map<String, Object> getSessionState() {
        return sessionState;
    }

    public void sendEvent(FactoryBeanListener.Event ev, Object... args) {
        for (FactoryBeanListener l : listeners) {
            l.handleEvent(ev, this, args);
        }
    }

    protected void initializeDefaultInterceptors() {
        service.getInInterceptors().add(new ServiceInvokerInterceptor());
        service.getInInterceptors().add(new OutgoingChainInterceptor());
        // Liberty change - For jaxrs2.0 there is no such oneway call
        //service.getInInterceptors().add(new OneWayProcessorInterceptor());
    }

    protected void initializeDataBindings() {
        if (getDataBinding() instanceof AbstractDataBinding && schemaLocations != null) {
            fillDataBindingSchemas();
        }
        getDataBinding().initialize(getService());

        service.setDataBinding(getDataBinding());
        sendEvent(FactoryBeanListener.Event.DATABINDING_INITIALIZED, dataBinding);
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
        FactoryBeanListenerManager m = bus.getExtension(FactoryBeanListenerManager.class);
        if (m != null) {
            listeners.addAll(m.getListeners());
        }
    }

    public DataBinding getDataBinding() {
        return getDataBinding(true);
    }

    public DataBinding getDataBinding(boolean create) {
        if (dataBinding == null && create) {
            dataBinding = createDefaultDataBinding();
        }
        return dataBinding;
    }

    protected DataBinding createDefaultDataBinding() {
        return null;
    }

    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
        this.dataBindingSet = dataBinding != null;
    }

    public Service getService() {
        return service;
    }

    protected void setService(Service service) {
        this.service = service;
    }

    private void fillDataBindingSchemas() {
        ResourceManager rr = getBus().getExtension(ResourceManager.class);
        List<DOMSource> schemas = new ArrayList<>();
        for (String l : schemaLocations) {
            URL url = rr.resolveResource(l, URL.class);
            if (url == null) {
                URIResolver res;
                try {
                    res = new URIResolver(l);
                } catch (IOException e) {
                    throw new ServiceConstructionException(new Message("INVALID_SCHEMA_URL", LOG, l), e);
                }
                if (!res.isResolved()) {
                    throw new ServiceConstructionException(new Message("INVALID_SCHEMA_URL", LOG, l));
                }
                url = res.getURL();
            }
            Document d;
            try {
                d = StaxUtils.read(url.openStream());
            } catch (Exception e) {
                throw new ServiceConstructionException(new Message("ERROR_READING_SCHEMA", LOG, l), e);
            }
            schemas.add(new DOMSource(d, url.toString()));
        }
        ((AbstractDataBinding) getDataBinding()).setSchemas(schemas);
    }

}
