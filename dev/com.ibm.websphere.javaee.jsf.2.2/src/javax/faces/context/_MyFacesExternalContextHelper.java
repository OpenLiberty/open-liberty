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
package javax.faces.context;

/**
 * Stores the first instance of the ExternalContext in the
 * field _firstInstance. We cannot put this field directly
 * into ExternalContext, because this can cause classloading
 * problems when accessing it from myfaces-impl when Mojarra is
 * also on the classpath.
 */
final class _MyFacesExternalContextHelper
{

    /**
     * This variable holds the firstInstance where all ExternalContext
     * objects should call when new jsf 2.0 methods are called.
     * 
     * This variable is an implementation detail and should be 
     * initialized and released on FacesContextFactoryImpl (because
     * this is the place where ExternalContextFactory.getExternalContext()
     * is called).
     * 
     * The objective of this is keep compatibility of libraries that wrap 
     * ExternalContext objects before 2.0. It is similar as FacesContext._firstInstace,
     * but since we don't have any place to init and release this variable properly
     * we should do it using reflection.
     */
    static ThreadLocal<ExternalContext> firstInstance = new ThreadLocal<ExternalContext>();
    
    /**
     * this class should not be instantiated.
     */
    private _MyFacesExternalContextHelper()
    {
    }
    
}
