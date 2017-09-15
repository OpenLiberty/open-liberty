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
package org.apache.myfaces.config.element;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public abstract class Factory implements Serializable
{
    public abstract List<String> getApplicationFactory();

    public abstract List<String> getExceptionHandlerFactory();

    public abstract List<String> getExternalContextFactory();

    public abstract List<String> getFacesContextFactory();

    public abstract List<String> getLifecycleFactory();

    public abstract List<String> getViewDeclarationLanguageFactory();

    public abstract List<String> getPartialViewContextFactory();

    public abstract List<String> getRenderkitFactory();

    public abstract List<String> getTagHandlerDelegateFactory();

    public abstract List<String> getVisitContextFactory();
    
    /**
     * 
     * @since 2.1.0
     * @return
     */
    public List<String> getFaceletCacheFactory()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public List<String> getFlashFactory()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public List<String> getFlowHandlerFactory()
    {
        return Collections.emptyList();
    }

    /**
     * @since 2.2
     * @return 
     */
    public List<String> getClientWindowFactory()
    {
        return Collections.emptyList();
    }
}
