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
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;

/**
 * Insert or move the facet from the composite component body to the expected location.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */
@JSFFaceletTag(name="composite:insertFacet")
public class InsertFacetHandler extends TagHandler
{
    //public static String USES_INSERT_FACET = "org.apache.myfaces.USES_INSERT_FACET";
    //public static String INSERT_FACET_TARGET_ID = "org.apache.myfaces.INSERT_FACET_TARGET_ID.";
    //public static String INSERT_FACET_ORDERING = "org.apache.myfaces.INSERT_FACET_ORDERING.";
    
    public static final String INSERT_FACET_USED = "org.apache.myfaces.INSERT_FACET_USED";
    
    /**
     * Key used to save on bean descriptor a map containing the metadata
     * information related to this tag. It will be used later to check "required" property.
     */
    public static final String INSERT_FACET_KEYS = "org.apache.myfaces.INSERT_FACET_KEYS";
    
    private static final Logger log = Logger.getLogger(InsertFacetHandler.class.getName());
    
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
    
    public InsertFacetHandler(TagConfig config)
    {
        super(config);
        _name = getRequiredAttribute("name");
        _required = getAttribute("required");
    }
    
    public String getFacetName(FaceletContext ctx)
    {
        return _name.getValue(ctx);
    }

    @SuppressWarnings("unchecked")
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

            List<String> facetList = (List<String>) beanDescriptor.getValue(INSERT_FACET_USED);
            
            if (facetList == null)
            {
                //2. If not found create it and set
                facetList = new ArrayList<String>();
                beanDescriptor.setValue(
                        INSERT_FACET_USED,
                        facetList);
            }
            
            facetList.add(facetName);

            Map<String, PropertyDescriptor> insertFacetPropertyDescriptorMap = (Map<String, PropertyDescriptor>)
                beanDescriptor.getValue(INSERT_FACET_KEYS);
        
            if (insertFacetPropertyDescriptorMap == null)
            {
                insertFacetPropertyDescriptorMap = new HashMap<String, PropertyDescriptor>();
                beanDescriptor.setValue(INSERT_FACET_KEYS, insertFacetPropertyDescriptorMap);
            }
            
            PropertyDescriptor facetDescriptor = _createFacetPropertyDescriptor(facetName, ctx);
            insertFacetPropertyDescriptorMap.put(facetName, facetDescriptor);
        }
        else
        {
            String facetName = _name.getValue(ctx);
            
            AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
            
            actx.includeCompositeComponentDefinition(parent, facetName);
        }
        
    }
    
    private PropertyDescriptor _createFacetPropertyDescriptor(String facetName, FaceletContext ctx)
    throws TagException, IOException
    {
        try
        {
            CompositeComponentPropertyDescriptor facetPropertyDescriptor = 
                new CompositeComponentPropertyDescriptor(facetName);
            
            if (_required != null)
            {
                facetPropertyDescriptor.setValue("required", _required.getValueExpression(ctx, Boolean.class));
            }
            
            return facetPropertyDescriptor;
        }
        catch (IntrospectionException e)
        {
            if (log.isLoggable(Level.SEVERE))
            {
                log.log(Level.SEVERE, "Cannot create PropertyDescriptor for attribute ",e);
            }
            throw new TagException(tag,e);
        }
    }

}
