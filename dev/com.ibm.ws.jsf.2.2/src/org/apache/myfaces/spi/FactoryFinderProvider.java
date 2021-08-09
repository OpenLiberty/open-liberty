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

import javax.faces.FacesException;

/**
 * Provide an interface to override {@link javax.faces.FactoryFinder} class
 * methods. 
 * 
 * @since 2.0.5
 * @author Leonardo Uribe
 *
 */
public interface FactoryFinderProvider
{
    /**
     * See {@link javax.faces.FactoryFinder#getFactory(String)} javadoc for details
     * 
     * @param factoryName
     * @return
     * @throws FacesException
     */
    public Object getFactory(String factoryName) throws FacesException;
    
    /**
     * See {@link javax.faces.FactoryFinder#releaseFactories()} javadoc for details
     * 
     * @throws FacesException
     */
    public void releaseFactories() throws FacesException;
    
    /**
     * See {@link javax.faces.FactoryFinder#setFactory(String, String)} javadoc for details
     * 
     * @param factoryName
     * @param implName
     */
    public void setFactory(String factoryName, String implName);
}
