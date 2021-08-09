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
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.ProjectStage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.Location;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;

/**
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1194861 $ $Date: 2011-10-29 10:02:34 +0000 (Sat, 29 Oct 2011) $
 */
@JSFFaceletTag(name="composite:interface")
public class InterfaceHandler extends TagHandler implements InterfaceDescriptorCreator
{
    private static final Logger log = Logger.getLogger(InterfaceHandler.class.getName());
    
    public final static String NAME = "interface";
    
    /**
     * String array defining all standard attributes of this tag.
     * ATTENTION: this array MUST be sorted alphabetically in order to use binary search!!
     */
    private static final String[] STANDARD_ATTRIBUTES_SORTED = new String[]
    {
        "componentType",
        "displayName",
        "expert",
        "hidden",
        "name",
        "preferred",
        "shortDescription"
    };
    
    /**
     * 
     */
    @JSFFaceletAttribute(name="name",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _name;
    
    /**
     * 
     */
    @JSFFaceletAttribute(name="componentType",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _componentType;
    
    /**
     * Only available if ProjectStage is Development.
     */
    @JSFFaceletAttribute(name="displayName",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _displayName;
    
    /**
     * Only available if ProjectStage is Development.
     */
    @JSFFaceletAttribute(name="preferred",
            className="javax.el.ValueExpression",
            deferredValueType="boolean")
    private final TagAttribute _preferred;
    
    /**
     * Only available if ProjectStage is Development.
     */
    @JSFFaceletAttribute(name="expert",
            className="javax.el.ValueExpression",
            deferredValueType="boolean")
    private final TagAttribute _expert;
    
    /**
     * Only available if ProjectStage is Development.
     */
    @JSFFaceletAttribute(name="shortDescription",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _shortDescription;
    
    /**
     * The "hidden" flag is used to identify features that are intended only 
     * for tool use, and which should not be exposed to humans.
     * Only available if ProjectStage is Development.
     */
    @JSFFaceletAttribute(name="hidden",
            className="javax.el.ValueExpression",
            deferredValueType="boolean")
    protected final TagAttribute _hidden;
    
    /**
     * Check if the BeanInfo instance created by this handler
     * can be cacheable or not. 
     */
    private boolean _cacheable;
    

    private Collection<InterfaceDescriptorCreator> attrHandlerList;
    
    public InterfaceHandler(TagConfig config)
    {
        super(config);
        _name = getAttribute("name");
        _componentType = getAttribute("componentType");
        _displayName = getAttribute("displayName");
        _preferred = getAttribute("preferred");
        _expert = getAttribute("expert");
        _shortDescription = getAttribute("shortDescription");
        _hidden = getAttribute("hidden");
        
        // Note that only if ProjectStage is Development, The "displayName",
        // "shortDescription", "expert", "hidden", and "preferred" attributes are exposed
        final boolean development = FacesContext.getCurrentInstance()
                .isProjectStage(ProjectStage.Development);
        
        // note that we don't have to check the componentType and any unspecified
        // attributes here, because these ones are stored as a ValueExpression in the
        // BeanDescriptor and thus they have no effect on caching
        if ((_name == null || _name.isLiteral()) 
                && (!development || _areDevelopmentAttributesLiteral()))
        {
            _cacheable = true;
            // Check if all InterfaceDescriptorCreator children are cacheable.
            // If so, we can cache this instance, otherwise not.
            attrHandlerList = 
                TagHandlerUtils.findNextByType( nextHandler, InterfaceDescriptorCreator.class);
            for (InterfaceDescriptorCreator handler : attrHandlerList)
            {
                if (!handler.isCacheable())
                {
                    _cacheable = false;
                    break;
                }
            }
            if (!_cacheable)
            {
                // Disable cache on attributes because this tag is the responsible for reuse
                for (InterfaceDescriptorCreator handler : attrHandlerList)
                {
                    handler.setCacheable(false);
                }
            }
        }
        else
        {
            _cacheable = false;
        }
    }
    
    /**
     * True if the "displayName", "shortDescription", "expert", "hidden", and
     * "preferred" attributes are either null or literal.
     * @return
     */
    private boolean _areDevelopmentAttributesLiteral()
    {
        return CompositeTagAttributeUtils.areAttributesLiteral(
                _displayName, _shortDescription, _expert, _hidden, _preferred);
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        // Only apply if we are building composite component metadata,
        // in other words we are calling ViewDeclarationLanguage.getComponentMetadata
        if ( ((AbstractFaceletContext)ctx).isBuildingCompositeComponentMetadata() )
        {
            FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(ctx);
            UIComponent compositeBaseParent
                    = fcc.getCompositeComponentFromStack();
            
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
            
            BeanDescriptor descriptor = beanInfo.getBeanDescriptor();
            // Add values to descriptor according to pld javadoc
            if (_name != null)
            {
                descriptor.setName(_name.getValue(ctx));
            }
            if (_componentType != null)
            {
                // componentType is required by Application.createComponent(FacesContext, Resource)
                // to instantiate the base component for this composite component. It should be
                // as family javax.faces.NamingContainer .
                descriptor.setValue(UIComponent.COMPOSITE_COMPONENT_TYPE_KEY, 
                        _componentType.getValueExpression(ctx, String.class));
            }
            
            // If ProjectStage is Development, The "displayName", "shortDescription",
            // "expert", "hidden", and "preferred" attributes are exposed
            if (ctx.getFacesContext().isProjectStage(ProjectStage.Development))
            {
                CompositeTagAttributeUtils.addDevelopmentAttributes(descriptor, ctx, 
                        _displayName, _shortDescription, _expert, _hidden, _preferred);
            }
            
            // Any additional attributes are exposed as attributes accessible
            // from the getValue() and attributeNames() methods on BeanDescriptor
            CompositeTagAttributeUtils.addUnspecifiedAttributes(descriptor, tag, 
                    STANDARD_ATTRIBUTES_SORTED, ctx);
            
            try
            {
                fcc.startComponentUniqueIdSection("__ccmd_");
                
                nextHandler.apply(ctx, parent);
            }
            finally 
            {
                fcc.endComponentUniqueIdSection("__ccmd_");
            }
        }
    }
    
    public boolean isCacheable()
    {
        return _cacheable;
    }

    public void setCacheable(boolean cacheable)
    {
        _cacheable = cacheable;
        for (InterfaceDescriptorCreator handler : attrHandlerList)
        {
            handler.setCacheable(cacheable);
        }
    }
    
    public Location getLocation()
    {
        return this.tag.getLocation();
    }
}
