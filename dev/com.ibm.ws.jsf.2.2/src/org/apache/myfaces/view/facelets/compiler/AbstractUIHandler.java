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
package org.apache.myfaces.view.facelets.compiler;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TextHandler;

import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.jsf.core.FacetHandler;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public abstract class AbstractUIHandler implements FaceletHandler, TextHandler
{

    /**
     * @param ctx
     * @param parent
     * @param c
     * @deprecated
     */
    @Deprecated
    public void addComponent(FaceletContext ctx, UIComponent parent, UIComponent c)
    {
        // possible facet scoped
        String facetName = this.getFacetName(ctx, parent);
        if (facetName == null)
        {
            parent.getChildren().add(c);
        }
        else
        {
            ComponentSupport.addFacet(ctx, parent, c, facetName);
        }
    }

    protected final String getFacetName(FaceletContext ctx, UIComponent parent)
    {
        return (String) parent.getAttributes().get(FacetHandler.KEY);
    }

}
