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
package org.apache.myfaces.component;

import java.io.IOException;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;

/**
 * Dummy class that is used as a container for resources 
 * (see JSF 2.0 rev A UIViewRoot.addComponentResource javadoc)
 * 
 * @author Leonardo Uribe (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 * @since 2.0.2
 * 
 */
@JSFComponent
public class ComponentResourceContainer extends UIPanel
{
    static public final String COMPONENT_FAMILY =
        "javax.faces.Panel";
    static public final String COMPONENT_TYPE =
        "javax.faces.ComponentResourceContainer";

    /**
     * Construct an instance of the UIPanel.
     */
    public ComponentResourceContainer()
    {
      setRendererType(null);
    }
    
    @Override
    public String getFamily()
    {
       return COMPONENT_FAMILY;
    }
      
    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException
    {
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException
    {
    }

    @Override
    public void encodeAll(FacesContext context) throws IOException
    {
    }
}
