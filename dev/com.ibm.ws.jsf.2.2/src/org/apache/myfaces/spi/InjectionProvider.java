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

/**
 * Provide an interface to inject JSF artifacts using JavaEE annotations
 * as described in JSF 2.2 spec section 5.4 
 *
 * @author Leonardo Uribe
 */
public abstract class InjectionProvider
{
    public abstract Object inject(Object instance) throws InjectionProviderException;

    public abstract void postConstruct(Object instance, Object creationMetaData) throws InjectionProviderException;

    public abstract void preDestroy(Object instance, Object creationMetaData) throws InjectionProviderException;
    
    public boolean isAvailable()
    {
        return true;
    }
}
