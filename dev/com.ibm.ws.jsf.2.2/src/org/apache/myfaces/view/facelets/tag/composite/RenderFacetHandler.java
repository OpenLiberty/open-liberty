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
package org.apache.myfaces.view.facelets.tag.composite;

import java.beans.BeanDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;

/**
 * Render the facet defined on the composite component body to the current location
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1433528 $ $Date: 2013-01-15 17:10:46 +0000 (Tue, 15 Jan 2013) $
 */
@JSFFaceletTag(name="composite:renderFacet")
public class RenderFacetHandler extends ComponentHandler
{
    private static final Logger log = Logger.getLogger(RenderFacetHandler.class.getName());
    
    public static final String RENDER_FACET_USED = "org.apache.myfaces.RENDER_FACET_USED";
    
    /**
     * The name that identify the current facet.
     */
    @JSFFaceletAttribute(name="name",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String",
            required=true)
    protected final TagAttribute _name;
    
    /**
     * Define if the facet to be inserted is required or not for every instance of
     * this composite component.
     */
    @JSFFaceletAttribute(name="required",
            className="javax.el.ValueExpression",
            deferredValueType="boolean")
    protected final TagAttribute _required;
    
    public RenderFacetHandler(ComponentConfig config)
    {
        super(config);
        _name = getRequiredAttribute("name");
        _required = getAttribute("required");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        if (((AbstractFaceletContext)ctx).isBuildingCompositeComponentMetadata())
        {
            String facetName = _name.getValue(ctx);
            
            UIComponent compositeBaseParent
                    = FaceletCompositionContext.getCurrentInstance(ctx).getCompositeComponentFromStack();
            
            CompositeComponentBeanInfo beanInfo = 
                (CompositeComponentBeanInfo) compositeBaseParent.getAttributes()
                .get(UIComponent.BEANINFO_KEY);
            
            if (beanInfo == null)
            {
                if (log.isLoggable(Level.SEVERE))
                {
                    log.severe("Cannot find composite bean descriptor UIComponent.BEANINFO_KEY ");
                }
                return;
            }
            
            BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor(); 

            List<String> facetList = (List<String>) beanDescriptor.getValue(RENDER_FACET_USED);
            
            if (facetList == null)
            {
                //2. If not found create it and set
                facetList = new ArrayList<String>();
                beanDescriptor.setValue(
                        RENDER_FACET_USED,
                        facetList);
            }
            
            facetList.add(facetName);
            
            // Do not call super.apply(ctx, parent), because it forces component creation,
            // and in this step it is not necessary. Also, it changes the order of 
            // the generated ids.
        }
        else
        {
            super.apply(ctx, parent);
        }
    }

    @Override
    public void onComponentPopulated(FaceletContext ctx, UIComponent c,
            UIComponent parent)
    {
        if (!((AbstractFaceletContext)ctx).isBuildingCompositeComponentMetadata())
        {
            UIComponent parentCompositeComponent
                    = FaceletCompositionContext.getCurrentInstance(ctx).getCompositeComponentFromStack();
            
            String facetName = _name.getValue(ctx);
    
            if (_required != null && _required.getBoolean(ctx) && parentCompositeComponent.getFacet(facetName) == null)
            {
                throw new TagException(this.tag, "Cannot find facet with name '"+facetName+"' in composite component");
            }
            
            c.getAttributes().put(UIComponent.FACETS_KEY, facetName);
        }
    }
}
