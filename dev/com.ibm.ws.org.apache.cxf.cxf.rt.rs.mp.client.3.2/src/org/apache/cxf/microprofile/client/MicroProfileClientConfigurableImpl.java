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
package org.apache.cxf.microprofile.client;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.impl.ConfigurationImpl;
import org.apache.cxf.microprofile.client.config.ConfigFacade;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class MicroProfileClientConfigurableImpl<C extends Configurable<C>>
        extends ConfigurableImpl<C>
    implements Configurable<C> {
    // Liberty Change begin - need to maintain CONTRACTS unless CXF-7638 is backported to the 3.1.X stream
    static final Class<?>[] CONTRACTS = new Class<?>[] {ClientRequestFilter.class,
        ClientResponseFilter.class, ReaderInterceptor.class, WriterInterceptor.class,
        MessageBodyWriter.class, MessageBodyReader.class, ResponseExceptionMapper.class};
    // Liberty Change end
    private static final String CONFIG_KEY_DISABLE_MAPPER = "microprofile.rest.client.disable.default.mapper";

    public MicroProfileClientConfigurableImpl(C configurable) {
        this(configurable, null);
    }

    public MicroProfileClientConfigurableImpl(C configurable, Configuration config) {
        super(configurable, CONTRACTS, // Liberty change - CONTRACTS - see above
                config == null ? new ConfigurationImpl(RuntimeType.CLIENT)
                        : new ConfigurationImpl(config, CONTRACTS));
    }

    boolean isDefaultExceptionMapperDisabled() {
        Object prop = getConfiguration().getProperty(CONFIG_KEY_DISABLE_MAPPER);
        if (prop instanceof Boolean) {
            return (Boolean)prop;
        }
        return ConfigFacade.getOptionalValue(CONFIG_KEY_DISABLE_MAPPER,
                                             Boolean.class).orElse(false);
    }
}
