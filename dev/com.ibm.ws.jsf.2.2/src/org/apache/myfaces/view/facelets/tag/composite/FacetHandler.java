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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.ProjectStage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;

/**
 * Define the facets used by this composite component.
 * <p>
 * This tag is used inside composite:interface tag. All facets
 * should be saved under the key UIComponent.FACETS_KEY on the
 * bean descriptor map as a Map&lt;String, PropertyDescriptor&gt;
 * </p>
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */
@JSFFaceletTag(name="composite:facet")
public class FacetHandler extends TagHandler implements InterfaceDescriptorCreator
{

    //private static final Log log = LogFactory.getLog(FacetHandler.class);
    private static final Logger log = Logger.getLogger(FacetHandler.class.getName());
    
    /**
     * String array defining all standard attributes of this tag.
     * ATTENTION: this array MUST be sorted alphabetically in order to use binary search!!
     */
    private static final String[] STANDARD_ATTRIBUTES_SORTED = new String[]
    {
        "displayName",
        "expert",
        "hidden",
        "name",
        "preferred",
        "required",
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
     * Only available if ProjectStage is Development.
     */
    @JSFFaceletAttribute(name="displayName",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _displayName;

    /**
     * Indicate if the attribute is required or not
     * <p>
     * Myfaces specific feature: this attribute is checked only if project stage is
     * not ProjectStage.Production when a composite component is created.
     * </p>
     */
    @JSFFaceletAttribute(name="required",
            className="javax.el.ValueExpression",
            deferredValueType="boolean")
    private final TagAttribute _required;

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
     * Check if the PropertyDescriptor instance created by this handler
     * can be cacheable or not. 
     */
    private boolean _cacheable;
    
    /**
     * Cached instance used by this component. Note here we have a 
     * "racy single-check". If this field is used, it is supposed 
     * the object cached by this handler is immutable, and this is
     * granted if all properties not saved as ValueExpression are
     * "literal". 
     */
    private PropertyDescriptor _propertyDescriptor; 
    
    public FacetHandler(TagConfig config)
    {
        super(config);
        _name = getRequiredAttribute("name");
        _displayName = getAttribute("displayName");
        _required = getAttribute("required");
        _preferred = getAttribute("preferred");
        _expert = getAttribute("expert");
        _shortDescription = getAttribute("shortDescription");
        _hidden = getAttribute("hidden");
        
        // We can reuse the same PropertyDescriptor only if the properties
        // that requires to be evaluated when apply (build view time)
        // occur are literal or null. Otherwise we need to create it.
        // Note that only if ProjectStage is Development, The "displayName",
        // "shortDescription", "expert", "hidden", and "preferred" attributes are exposed
        final boolean development = FacesContext.getCurrentInstance()
                .isProjectStage(ProjectStage.Development);
        
        if (_name.isLiteral() 
                && (!development || _areDevelopmentAttributesLiteral()))
        {
            // Unfortunately its not possible to create the required 
            // PropertyDescriptor instance here, because there is no way 
            // to get a FaceletContext to create ValueExpressions. It is
            // possible to create it if we not have set all this properties:
            // required and possible unspecified attributes. This prevents 
            // the racy single-check.
            _cacheable = true;
            if (_required == null &&
                    !CompositeTagAttributeUtils.containsUnspecifiedAttributes(tag, 
                            STANDARD_ATTRIBUTES_SORTED))
            {
                _propertyDescriptor = _createFacetPropertyDescriptor(development);
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

    @SuppressWarnings("unchecked")
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        CompositeComponentBeanInfo beanInfo = 
            (CompositeComponentBeanInfo) parent.getAttributes()
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

        Map<String, PropertyDescriptor> facetPropertyDescriptorMap = (Map<String, PropertyDescriptor>)
            beanDescriptor.getValue(UIComponent.FACETS_KEY);
        
        if (facetPropertyDescriptorMap == null)
        {
            facetPropertyDescriptorMap = new HashMap<String, PropertyDescriptor>();
            beanDescriptor.setValue(UIComponent.FACETS_KEY, facetPropertyDescriptorMap);
        }
        
        String facetName = _name.getValue(ctx);
        
        if (isCacheable())
        {
            if (_propertyDescriptor == null)
            {
                _propertyDescriptor = _createFacetPropertyDescriptor(facetName, ctx);
            }
            facetPropertyDescriptorMap.put(facetName, _propertyDescriptor);
        }
        else
        {
            PropertyDescriptor facetDescriptor = _createFacetPropertyDescriptor(facetName, ctx);
            facetPropertyDescriptorMap.put(facetName, facetDescriptor);
        }
                
        nextHandler.apply(ctx, parent);
    }
    
    /**
     * This method could be called only if it is not necessary to set the following properties:
     * targets, default, required, methodSignature and type
     * 
     * @return
     */
    private PropertyDescriptor _createFacetPropertyDescriptor(boolean development)
    {
        try
        {
            CompositeComponentPropertyDescriptor facetPropertyDescriptor = 
                new CompositeComponentPropertyDescriptor(_name.getValue());
            
            // If ProjectStage is Development, The "displayName", "shortDescription",
            // "expert", "hidden", and "preferred" attributes are exposed
            if (development)
            {
                CompositeTagAttributeUtils.addDevelopmentAttributesLiteral(facetPropertyDescriptor,
                        _displayName, _shortDescription, _expert, _hidden, _preferred);
            }
            
            // note that no unspecified attributes are handled here, because the current
            // tag does not contain any, otherwise this code would not have been called.
            
            return facetPropertyDescriptor;
        }
        catch (IntrospectionException e)
        {
            if (log.isLoggable(Level.SEVERE))
            {
                log.log(Level.SEVERE, "Cannot create PropertyDescriptor for facet ",e);
            }
            throw new TagException(tag,e);
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
            
            // If ProjectStage is Development, The "displayName", "shortDescription",
            // "expert", "hidden", and "preferred" attributes are exposed
            if (ctx.getFacesContext().isProjectStage(ProjectStage.Development))
            {
                CompositeTagAttributeUtils.addDevelopmentAttributes(facetPropertyDescriptor, ctx, 
                        _displayName, _shortDescription, _expert, _hidden, _preferred);
            }
            
            // Any additional attributes are exposed as attributes accessible
            // from the getValue() and attributeNames() methods on FeatureDescriptor
            CompositeTagAttributeUtils.addUnspecifiedAttributes(facetPropertyDescriptor, tag, 
                    STANDARD_ATTRIBUTES_SORTED, ctx);
            
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

    public boolean isCacheable()
    {
        return _cacheable;
    }
    
    public void setCacheable(boolean cacheable)
    {
        _cacheable = cacheable;
    }

    //@Override
    //public FaceletHandler getNextHandler()
    //{
    //    return nextHandler;
    //}
}
