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
package org.apache.myfaces.config.impl.digester.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public class RenderKitImpl extends org.apache.myfaces.config.element.RenderKit implements Serializable
{

    private String id;
    private List<String> renderKitClasses = new ArrayList<String>();
    private List<org.apache.myfaces.config.element.Renderer> renderer
            = new ArrayList<org.apache.myfaces.config.element.Renderer>();
    private List<org.apache.myfaces.config.element.ClientBehaviorRenderer> clientBehaviorRenderers
            = new ArrayList<org.apache.myfaces.config.element.ClientBehaviorRenderer>();
    
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public List<String> getRenderKitClasses()
    {
        return renderKitClasses;
    }

    public void addRenderKitClass(String renderKitClass)
    {
        renderKitClasses.add(renderKitClass);
    }
    
    public List<org.apache.myfaces.config.element.ClientBehaviorRenderer> getClientBehaviorRenderers ()
    {
        return clientBehaviorRenderers;
    }
    
    public List<org.apache.myfaces.config.element.Renderer> getRenderer()
    {
        return renderer;
    }

    public void addClientBehaviorRenderer (org.apache.myfaces.config.element.ClientBehaviorRenderer renderer)
    {
        clientBehaviorRenderers.add (renderer);   
    }
    
    public void addRenderer(org.apache.myfaces.config.element.Renderer value)
    {
        renderer.add(value);
    }

    public void merge(org.apache.myfaces.config.element.RenderKit renderKit)
    {
        renderKitClasses.addAll(renderKit.getRenderKitClasses());
        clientBehaviorRenderers.addAll (renderKit.getClientBehaviorRenderers());
        renderer.addAll(renderKit.getRenderer());
    }

}
