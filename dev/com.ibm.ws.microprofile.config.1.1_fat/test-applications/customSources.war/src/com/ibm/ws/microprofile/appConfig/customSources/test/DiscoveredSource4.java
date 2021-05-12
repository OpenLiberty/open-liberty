/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ibm.ws.microprofile.appConfig.customSources.test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class DiscoveredSource4 implements ConfigSource {

    public ConcurrentMap<String, String> props;
    public int ordinal = 750;
    public String name = "mySource";

    public DiscoveredSource4() {
        props = new ConcurrentHashMap<String, String>();
        put("4", "4");
        put("5", "4");
        setOrdinal(750);
    }

    public ConcurrentMap<String, String> getProps() {
        return props;
    }

    public void setProps(ConcurrentMap<String, String> props) {
        this.props = props;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DiscoveredSource4 put(String key, String value) {
        props.put(key, value);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return props;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return props.get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
}
