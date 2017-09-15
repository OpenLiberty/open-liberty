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
package org.apache.myfaces.view.facelets.tag.jsf.core;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.component.UIViewRoot;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;

/**
 * Defines the view metadata. It is expected that this tag contains only
 * one or many f:viewParam tags.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1406265 $ $Date: 2012-11-06 18:33:41 +0000 (Tue, 06 Nov 2012) $
 */
@JSFFaceletTag(name="f:metadata")
public final class ViewMetadataHandler extends TagHandler
{

    public ViewMetadataHandler(TagConfig config)
    {
        super(config);
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        if (parent == null)
        {
            throw new TagException(this.tag, "Parent UIComponent was null");
        }
        if (! (parent instanceof UIViewRoot) )
        {
            throw new TagException(this.tag, "Parent UIComponent "+parent.getId()+" should be instance of UIViewRoot");
        }
        FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
        if (mctx.isBuildingViewMetadata())
        {
            UIComponent metadataFacet = parent.getFacet(UIViewRoot.METADATA_FACET_NAME);
            if (metadataFacet == null)
            {
                metadataFacet = ctx.getFacesContext().getApplication().createComponent(
                        ctx.getFacesContext(), UIPanel.COMPONENT_TYPE, null);
                metadataFacet.setId(UIViewRoot.METADATA_FACET_NAME);
                metadataFacet.getAttributes().put(ComponentSupport.FACET_CREATED_UIPANEL_MARKER, true);
                metadataFacet.getAttributes().put(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER, Boolean.TRUE);
                parent.getFacets().put(UIViewRoot.METADATA_FACET_NAME, metadataFacet);
            }
        }

        // We have to do nextHandler.apply() in any case, because even if we're not building ViewMetadata
        // we still need to do it so that the mark/delete components can be applied correctly.
        // (The only tag that needs to do something special is f:event, because in this case
        // ComponentHandler.isNew(parent) does not work for UIViewRoot.)
        parent.getAttributes().put(FacetHandler.KEY, UIViewRoot.METADATA_FACET_NAME);
        mctx.startMetadataSection();
        try
        {
            this.nextHandler.apply(ctx, parent);
        }
        finally
        {
            mctx.endMetadataSection();
            parent.getAttributes().remove(FacetHandler.KEY);
        }
    }
}
