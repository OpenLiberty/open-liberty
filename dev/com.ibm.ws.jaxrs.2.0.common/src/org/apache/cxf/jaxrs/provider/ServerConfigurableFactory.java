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

package org.apache.cxf.jaxrs.provider;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

/**
 * Manages the creation of server-side {@code Configurable<FeatureContext>} depending on
 * the presence of managed runtime (like CDI f.e.).
 *
 * <b>Note:</b> this interface may change in the future without a prior
 * notice, please be aware of that.
 */
public interface ServerConfigurableFactory {
    Class<?>[] SERVER_FILTER_INTERCEPTOR_CLASSES = new Class<?>[] {
                                                                    ContainerRequestFilter.class,
                                                                    ContainerResponseFilter.class,
                                                                    ReaderInterceptor.class,
                                                                    WriterInterceptor.class,
                                                                    //defect 211444, add all provider types of server side
                                                                    Feature.class,
                                                                    ExceptionMapper.class,
                                                                    ContextResolver.class,
                                                                    DynamicFeature.class,
                                                                    MessageBodyWriter.class,
                                                                    MessageBodyReader.class,
                                                                    ParamConverterProvider.class
    };

    Configurable<FeatureContext> create(FeatureContext context);
}
