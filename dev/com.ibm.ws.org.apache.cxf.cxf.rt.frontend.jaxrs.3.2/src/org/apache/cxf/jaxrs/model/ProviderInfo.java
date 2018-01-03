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

package org.apache.cxf.jaxrs.model;

import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;

public class ProviderInfo<T> extends AbstractResourceInfo {

    private T provider;
// Liberty Change for CXF Begin
    private T oldProvider = null;

    private boolean isInit = false;

// Liberty Change for CXF End

    private boolean custom;
    private boolean busGlobal;

    public ProviderInfo(T provider, Bus bus, boolean custom) {
        this(provider, bus, true, custom);
    }

    public ProviderInfo(T provider, Bus bus, boolean checkContexts, boolean custom) {
        this(provider, null, bus, checkContexts, custom);
    }

    public ProviderInfo(T provider,
                        Map<Class<?>, ThreadLocalProxy<?>> constructorProxies,
                        Bus bus,
                        boolean custom) {
        this(provider, constructorProxies, bus, true, custom);
    }

    public ProviderInfo(T provider,
                        Map<Class<?>, ThreadLocalProxy<?>> constructorProxies,
                        Bus bus,
                        boolean checkContexts,
                        boolean custom) {
        super(provider.getClass(), provider.getClass(), true, checkContexts, constructorProxies, bus, provider);
        this.provider = provider;
        this.custom = custom;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public T getProvider() {
        return provider;
    }

// Liberty Change for CXF Begin
    public T getOldProvider() {
        return (oldProvider == null) ? provider : oldProvider;
    }

    /**
     * we need use this interface to replace the provider object
     *
     * @param pObj
     */
    public void setProvider(Object pObj) {

        this.oldProvider = this.provider;
        this.provider = (T) pObj;
    }

    /**
     * we need check if the object is already replaced.
     *
     * @return
     */
    public boolean isInit() {
        return this.isInit;
    }

    public void setIsInit(boolean check) {
        this.isInit = check;
    }

// Liberty Change for CXF End
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProviderInfo)) {
            return false;
        }
        return provider.equals(((ProviderInfo<?>) obj).getProvider());
    }

    @Override
    public int hashCode() {
        if (provider != null)
            return provider.hashCode();
        else
            return super.hashCode();
    }

    public boolean isCustom() {
        return custom;
    }

    public boolean isBusGlobal() {
        return busGlobal;
    }

    public void setBusGlobal(boolean busGlobal) {
        this.busGlobal = busGlobal;
    }

    //Liberty 226760 begin
    @Override
    public String toString() {
        if (provider != null) {
            return super.toString() + "{" + provider.getClass().getName() + "}";
        } else {
            return "{NULL}";
        }

    }
    //Liberty 226760 end

}
