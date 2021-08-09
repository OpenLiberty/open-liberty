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
package org.apache.myfaces.config;

import org.apache.myfaces.config.element.FacesConfigData;


/**
 * Subsumes several unmarshalled faces config objects and presents a simple interface
 * to the combined configuration data.
 *
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 1461361 $ $Date: 2013-03-26 22:54:30 +0000 (Tue, 26 Mar 2013) $
 */
public abstract class FacesConfigDispenser extends FacesConfigData
{
    /**
     * 
     */
    private static final long serialVersionUID = 9123062381457766144L;

    /**
     * Add another unmarshalled faces config object.
     * @param facesConfig unmarshalled faces config object
     */
    public abstract void feed(org.apache.myfaces.config.element.FacesConfig facesConfig);

    /**
     * Add another ApplicationFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedApplicationFactory(String factoryClassName);

    /**
     * Add another ExceptionHandlerFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedExceptionHandlerFactory(String factoryClassName);

    /**
     * Add another ExternalContextFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedExternalContextFactory(String factoryClassName);

    /**
     * Add another FacesContextFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedFacesContextFactory(String factoryClassName);

    /**
     * Add another LifecycleFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedLifecycleFactory(String factoryClassName);
    
    /**
     * Add another ViewDeclarationLanguageFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedViewDeclarationLanguageFactory(String factoryClassName);

    /**
     * Add another PartialViewContextFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedPartialViewContextFactory(String factoryClassName);

    /**
     * Add another RenderKitFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedRenderKitFactory(String factoryClassName);
    
    /**
     * Add another TagHandlerDelegateFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedTagHandlerDelegateFactory(String factoryClassName);

    /**
     * Add another VisitContextFactory class name
     * @param factoryClassName a class name
     */
    public abstract void feedVisitContextFactory(String factoryClassName);

    /**
     * Add another FaceletCacheFactory class name
     * @since 2.1.0
     * @param factoryClassName a class name
     */
    public void feedFaceletCacheFactory(String factoryClassName)
    {
    }

    /**
     * @since 2.2
     * @param factoryClassName 
     */
    public void feedFlashFactory(String factoryClassName)
    {
    }
    
    /**
     * @since 2.2
     * @param factoryClassName 
     */
    public void feedClientWindowFactory(String factoryClassName)
    {
    }

}
