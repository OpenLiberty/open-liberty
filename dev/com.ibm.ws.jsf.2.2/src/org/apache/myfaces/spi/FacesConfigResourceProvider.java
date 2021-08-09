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
import java.net.URL;
import java.util.Collection;

import javax.faces.context.ExternalContext;

/**
 * Locate faces-config xml files through classpath. These
 * files has definitions that are used by initialize jsf environment.
 * <p>
 * By default it locate all files inside META-INF folder, named faces-config.xml
 * or ending with .faces-config.xml
 * </p> 
 * 
 * @since 2.0.2
 * @author Leonardo Uribe 
 */
public abstract class FacesConfigResourceProvider
{
    
    /**
     * Return a list of urls pointing to valid faces config-xml files. These
     * files will be parsed later and used initialize jsf environment.
     * 
     * @param context
     * @return
     */
    public abstract Collection<URL> getMetaInfConfigurationResources(ExternalContext context) throws IOException;
}