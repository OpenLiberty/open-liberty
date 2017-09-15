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
package org.apache.myfaces.spi.impl;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.shared_impl.webapp.webxml.WebXml;
import org.apache.myfaces.spi.ServletMapping;
import org.apache.myfaces.spi.WebConfigProvider;

/**
 * The default WebXmlProvider implementation.
 *
 * This impl uses the WebXmlParser to create a new instance of WebXmlImpl.
 *
 * @author Jakob Korherr
 */
public class DefaultWebConfigProvider extends WebConfigProvider
{

    @Override
    public List<ServletMapping> getFacesServletMappings(
            ExternalContext externalContext)
    {
        WebXml webXml = WebXml.getWebXml(externalContext);
       
        List mapping = webXml.getFacesServletMappings();
     
        // In MyFaces 2.0, getFacesServletMappins is used only at startup
        // time, so we don't need to cache this result.
        List<ServletMapping> mappingList = new ArrayList<ServletMapping>(mapping.size());
        
        for (int i = 0; i < mapping.size(); i++)
        {
            org.apache.myfaces.shared_impl.webapp.webxml.ServletMapping delegateMapping = 
                (org.apache.myfaces.shared_impl.webapp.webxml.ServletMapping) mapping.get(i);
            
            mappingList.add(new ServletMappingImpl(delegateMapping));
        }
        return mappingList;
    }

    @Override
    public boolean isErrorPagePresent(ExternalContext externalContext)
    {
        return WebXml.getWebXml(externalContext).isErrorPagePresent();
    }

}
