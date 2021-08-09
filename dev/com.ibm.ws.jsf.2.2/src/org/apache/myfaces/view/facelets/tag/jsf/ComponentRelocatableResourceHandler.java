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

package org.apache.myfaces.view.facelets.tag.jsf;

import java.util.Iterator;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.view.facelets.FaceletContext;

/**
 * This class is used in cases where alternate implementations of h:outputScript and h:outputStylesheet
 * components are used overriding a tag handler and providing a new tag name, but preserving the componentType
 * and rendererType. In those components, the relocation hack is done, but it is necessary to provide the 
 * additional code from the implementation.
 */
public class ComponentRelocatableResourceHandler implements RelocatableResourceHandler
{
    public static final ComponentRelocatableResourceHandler INSTANCE = new ComponentRelocatableResourceHandler();
    
    public UIComponent findChildByTagId(FaceletContext ctx, UIComponent parent,
            String id)
    {
        UIComponent c = null;
        UIViewRoot root = ComponentSupport.getViewRoot(ctx, parent);
        if (root.getFacetCount() > 0)
        {
            Iterator<UIComponent> itr = root.getFacets().values().iterator();
            while (itr.hasNext() && c == null)
            {
                UIComponent facet = itr.next();
                c = ComponentSupport.findChildByTagId(facet, id);
            }
        }
        return c;
    }
    
}