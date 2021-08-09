/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.spi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesWrapper;
import javax.faces.context.ExternalContext;

/**
 * Wrapper class that all AnnotationProvider instances should extend. This is used
 * to wrap the default algorithm and add some additional custom processing.
 * 
 * @since 2.0.3
 * @author Leonardo Uribe 
 */
public abstract class AnnotationProviderWrapper extends AnnotationProvider implements FacesWrapper<AnnotationProvider>
{

    public AnnotationProviderWrapper()
    {
        
    }

    public Map<Class<? extends Annotation>,Set<Class<?>>> getAnnotatedClasses(ExternalContext ctx)
    {
        return getWrapped().getAnnotatedClasses(ctx);
    }

    public Set<URL> getBaseUrls() throws IOException
    {
        return getWrapped().getBaseUrls();
    }
    
    public Set<URL> getBaseUrls(ExternalContext ctx) throws IOException
    {
        return getWrapped().getBaseUrls(ctx);
    }
}
