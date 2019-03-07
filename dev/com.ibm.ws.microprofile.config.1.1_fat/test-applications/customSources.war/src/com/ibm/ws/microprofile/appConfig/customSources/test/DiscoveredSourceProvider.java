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

package com.ibm.ws.microprofile.appConfig.customSources.test;

import java.util.ArrayList;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class DiscoveredSourceProvider implements ConfigSourceProvider {

    /** {@inheritDoc} */
    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        MySource s1 = new MySource();
        s1.put("configSourceProviderS1", "s1");

        MySource s2 = new MySource();
        s2.put("configSourceProviderS2", "s2");

        ArrayList<ConfigSource> result = new ArrayList<ConfigSource>();
        result.add(s1);
        result.add(s2);
        return result;

    }

}
