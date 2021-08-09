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

import java.util.List;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.config.element.FacesConfig;

/**
 * This interface provide a way to merge and store all JSF config information retrieved
 * from faces-config files, META-INF/service files and annotations that works as base
 * point to initialize MyFaces. The objective is allow server containers to "store" or
 * this information, preventing calculate it over and over each time the web application
 * is started.
 * 
 * <p>To wrap the default FacesConfigurationProvider, use a constructor like 
 * CustomFacesConfigurationProvider(FacesConfigurationProvider fcp)
 * and extend it from FacesConfigurationProviderWrapper</p>
 * 
 * @author Leonardo Uribe
 * @since 2.0.3
 *
 */
public abstract class FacesConfigurationProvider
{
    /**
     * Return the FacesConfig object model retrieved from MyFaces META-INF/standard-faces-config.xml file. 
     * 
     * @param ectx
     * @return
     */
    public abstract FacesConfig getStandardFacesConfig(ExternalContext ectx);
    
    /**
     * Return the FacesConfig object model retrieved from locate all JSF factories from META-INF/services/[factory_key].
     * 
     * The default implementation uses ServiceProviderFinder facilities to locate this SPI interfaces.
     * 
     * @param ectx
     * @return
     */
    public abstract FacesConfig getMetaInfServicesFacesConfig(ExternalContext ectx);
    
    /**
     * Return the FacesConfig object model retrieved from scanning annotations on the classpath.
     * 
     * @param ectx
     * @param metadataComplete
     * @return
     */
    public abstract FacesConfig getAnnotationsFacesConfig(ExternalContext ectx, boolean metadataComplete);
    
    /**
     * Return the FacesConfig object model retrieved from resources under the path
     * META-INF/faces-config.xml and META-INF/[prefix].faces-config.xml
     * 
     * @param ectx
     * @return
     */
    public abstract List<FacesConfig> getClassloaderFacesConfig(ExternalContext ectx);
    
    /**
     * Return the FacesConfig object model retrieved from javax.faces.CONFIG_FILES web config attribute
     * 
     * @param ectx
     * @return
     */
    public abstract List<FacesConfig> getContextSpecifiedFacesConfig(ExternalContext ectx);
    
    /**
     * Return the FacesConfig object model retrieved from WEB-INF/faces-config.xml
     * 
     * @param ectx
     * @return
     */
    public abstract FacesConfig getWebAppFacesConfig(ExternalContext ectx); 
    
    /**
     * Return the FacesConfig object model retrieved from a folder with a faces flow definition
     * See JSF 2.2 section 11.4.3.3 and section 7.5.1
     * 
     * @param ectx
     * @return 
     */
    public abstract List<FacesConfig> getFacesFlowFacesConfig(ExternalContext ectx);
    
    /**
     * Return the FacesConfig object model retrieved from SPI ApplicationConfigurationPopulator
     */
    public abstract List<FacesConfig> 
        getApplicationConfigurationResourceDocumentPopulatorFacesConfig(ExternalContext ectx);
    
    /**
     * Return the FacesConfig object model retrieved from parsing .taglib.xml files according
     * to spec rules.
     */    
    public abstract List<FacesConfig> getFaceletTaglibFacesConfig(ExternalContext ectx);
}
