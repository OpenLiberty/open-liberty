/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

package com.ibm.ws.microprofile.appConfig.dynamicSources.test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class DiscoveredSource4 implements ConfigSource {

    public ConcurrentMap<String, String> props;
    public int ordinal;
    public String id = "mySource";

    public DiscoveredSource4() {
        props = new ConcurrentHashMap<String, String>();
        put("4", "4");
        put("5", "4");
        setOrdinal(750);
    }

    public Map<String, String> getProps() {
        return props;
    }

    public void setProps(ConcurrentMap<String, String> props) {
        this.props = props;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public void setid(String id) {
        this.id = id;
    }

    public DiscoveredSource4 put(String key, String value) {
        props.put(key, value);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    /*
     * Each time we give out a value we increment it!
     */
    public ConcurrentMap<String, String> getProperties() {
        return props;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        String result = props.get(propertyName);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return id;
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
