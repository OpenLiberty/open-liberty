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
import java.util.Map;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

import org.apache.myfaces.spi.WebConfigProvider;
import org.apache.myfaces.util.WebXmlParser;
import org.apache.myfaces.util.WebXml;
import org.apache.myfaces.util.ServletMapping;

/**
 * The default WebXmlProvider implementation.
 *
 * @author Jakob Korherr
 */
public class DefaultWebConfigProvider extends WebConfigProvider
{
    public DefaultWebConfigProvider()
    {
    }
  
    @Override
    public List<ServletMapping> getFacesServletMappings(
            ExternalContext externalContext)
    {
       WebXml webXml = WebXml.getWebXml(externalContext);

        List mapping = webXml.getFacesServletMappings();

        // In MyFaces 2.0, getFacesServletMappings is used only at startup
        // time, so we don't need to cache this result.
        return new ArrayList<ServletMapping>(mapping);
    }

    @Override
    public boolean isErrorPagePresent(ExternalContext externalContext)
    {
        Map<String, String> errorPages
                = WebXmlParser.getErrorPages(FacesContext.getCurrentInstance().getExternalContext());

        return errorPages != null && !errorPages.isEmpty();
    }

}
