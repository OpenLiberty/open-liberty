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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.provider.ProviderFactory;

public class FilterProviderInfo<T> extends ProviderInfo<T> {

    private Set<String> nameBinding;
    private Map<Class<?>, Integer> supportedContracts;
    private boolean dynamic;
    
    public FilterProviderInfo(T provider,
                              Bus bus,
                              Map<Class<?>, Integer> supportedContracts) {
        this(provider, bus, ProviderFactory.DEFAULT_FILTER_NAME_BINDING, false, supportedContracts);        
    }
    
    public FilterProviderInfo(T provider,
                              Bus bus,
                              String nameBinding,
                              boolean dynamic,
                              Map<Class<?>, Integer> supportedContracts) {
        super(provider, bus, true);
        this.nameBinding = Collections.singleton(nameBinding);
        this.supportedContracts = supportedContracts;
        this.dynamic = dynamic;
    }

    //Liberty change start -- this should be removed on next update of CXF 3.1.X stream
    public FilterProviderInfo(Class<?> resourceClass, 
                              Class<?> serviceClass,
                              T provider,
                              Bus bus,
                              Map<Class<?>, Integer> supportedContracts) {
        this(provider, bus, ProviderFactory.DEFAULT_FILTER_NAME_BINDING, false, supportedContracts);
    }
    //Liberty change end

    public Set<String> getNameBinding() {
        return nameBinding;
    }

    public int getPriority(Class<?> contract) {
        return supportedContracts.get(contract);
    }
    
    public boolean isDynamic() {
        return dynamic;
    }

    public Set<Class<?>> getSupportedContracts() {
        return supportedContracts.keySet();
    }

}
