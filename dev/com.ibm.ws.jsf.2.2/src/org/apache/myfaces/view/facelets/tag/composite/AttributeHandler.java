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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.List;
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
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;

/**
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */
@JSFFaceletTag(name="composite:attribute")
public class AttributeHandler extends TagHandler implements InterfaceDescriptorCreator
{
    
    //private static final Log log = LogFactory.getLog(AttributeHandler.class);
    private static final Logger log = Logger.getLogger(AttributeHandler.class.getName());
    
    /**
     * String array defining all standard attributes of this tag.
     * ATTENTION: this array MUST be sorted alphabetically in order to use binary search!!
     */
    private static final String[] STANDARD_ATTRIBUTES_SORTED = new String[]
    {
        "default",
        "displayName",
        "expert",
        "hidden",
        "method-signature",
        "name",
        "preferred",
        "required",
        "shortDescription",
        "targetAttributeName",
        "targets",
        "type"
    };

    @JSFFaceletAttribute(name="name",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String",
            required=true)
    private final TagAttribute _name;
    
    @JSFFaceletAttribute(name="targets",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _targets;
    
    /**
     * If this property is set and the attribute does not have any
     * value (null), the value set on this property is returned as default
     * instead null.
     */
    @JSFFaceletAttribute(name="default",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _default;
    
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
     * not ProjectStage.Production when a composite component is created.</p>
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

    @JSFFaceletAttribute(name="method-signature",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _methodSignature;

    @JSFFaceletAttribute(name="type",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _type;
    
    /**
     * The "hidden" flag is used to identify features that are intended only 
     * for tool use, and which should not be exposed to humans.
     * Only available if ProjectStage is Development.
     */
    @JSFFaceletAttribute(name="hidden",
            className="javax.el.ValueExpression",
            deferredValueType="boolean")
    protected final TagAttribute _hidden;
    
    @JSFFaceletAttribute(name="targetAttributeName",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    private final TagAttribute _targetAttributeName;
    
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
    
    public AttributeHandler(TagConfig config)
    {
        super(config);
        _name = getRequiredAttribute("name");
        _targets = getAttribute("targets");
        _default = getAttribute("default");
        _displayName = getAttribute("displayName");
        _required = getAttribute("required");
        _preferred = getAttribute("preferred");
        _expert = getAttribute("expert");
        _shortDescription = getAttribute("shortDescription");
        _methodSignature = getAttribute("method-signature");
        _type = getAttribute("type");
        _hidden = getAttribute("hidden");
        _targetAttributeName = getAttribute("targetAttributeName");
        
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
            // targets, default, required, methodSignature, type and possible
            // unspecified attributes. This prevents the racy single-check.
            _cacheable = true;
            if ( _targets == null && _default == null && _required == null &&
                 _methodSignature == null && _type == null && _targetAttributeName == null &&
                 !CompositeTagAttributeUtils.containsUnspecifiedAttributes(tag, 
                         STANDARD_ATTRIBUTES_SORTED))
            {
                _propertyDescriptor = _createPropertyDescriptor(development);
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
        
        List<PropertyDescriptor> attributeList = beanInfo.getPropertyDescriptorsList();
        
        if (isCacheable())
        {
            if (_propertyDescriptor == null)
            {
                _propertyDescriptor = _createPropertyDescriptor(ctx);
            }
            attributeList.add(_propertyDescriptor);
        }
        else
        {
            PropertyDescriptor attribute = _createPropertyDescriptor(ctx);
            attributeList.add(attribute);
        }
        
        // Any "next" handler is going to be used to process nested attributes, which we don't want
        // to do since they can only possibly refer to bean properties.
        
        //nextHandler.apply(ctx, parent);
    }
    
    /**
     * This method could be called only if it is not necessary to set the following properties:
     * targets, default, required, methodSignature and type
     * 
     * @param development true if the current ProjectStage is Development
     * @return
     */
    private PropertyDescriptor _createPropertyDescriptor(boolean development)
    {
        try
        {
            CompositeComponentPropertyDescriptor attributeDescriptor = 
                new CompositeComponentPropertyDescriptor(_name.getValue());
            
            // If ProjectStage is Development, The "displayName", "shortDescription",
            // "expert", "hidden", and "preferred" attributes are exposed
            if (development)
            {
                CompositeTagAttributeUtils.addDevelopmentAttributesLiteral(attributeDescriptor,
                        _displayName, _shortDescription, _expert, _hidden, _preferred);
            }
            
            // note that no unspecified attributes are handled here, because the current
            // tag does not contain any, otherwise this code would not have been called.
            
            return attributeDescriptor;
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
    
    private PropertyDescriptor _createPropertyDescriptor(FaceletContext ctx)
        throws TagException, IOException
    {
        try
        {
            CompositeComponentPropertyDescriptor attributeDescriptor = 
                new CompositeComponentPropertyDescriptor(_name.getValue(ctx));
            
            // The javadoc of ViewDeclarationLanguage.retargetMethodExpressions says that
            // 'type', 'method-signature', 'targets' should return ValueExpressions.
            if (_targets != null)
            {
                attributeDescriptor.setValue("targets", _targets.getValueExpression(ctx, String.class));
            }
            if (_default != null)
            {
                if (_type != null)
                {
                    String type = _type.getValue(ctx);
                    Class clazz = String.class;
                    if (type != null)
                    {
                        try
                        {
                            clazz = ClassUtils.javaDefaultTypeToClass(type);
                        }
                        catch (ClassNotFoundException e)
                        {
                            //Assume String
                        }
                    }
                    
                    if (_default.isLiteral())
                    {
                        //If it is literal, calculate it and store it on a ValueExpression
                        attributeDescriptor.setValue("default", _default.getObject(ctx, clazz));
                    }
                    else
                    {
                        attributeDescriptor.setValue("default", _default.getValueExpression(ctx, clazz));
                    }
                }
                else
                {
                    attributeDescriptor.setValue("default", _default.getValueExpression(ctx, String.class));
                }
            }
            if (_required != null)
            {
                attributeDescriptor.setValue("required", _required.getValueExpression(ctx, Boolean.class));
            }
            if (_methodSignature != null)
            {
                attributeDescriptor.setValue("method-signature",
                                             _methodSignature.getValueExpression(ctx, String.class));
            }
            if (_type != null)
            {
                attributeDescriptor.setValue("type", _type.getValueExpression(ctx, String.class));
            }
            if (_targetAttributeName != null)
            {
                attributeDescriptor.setValue("targetAttributeName",
                                             _targetAttributeName.getValueExpression(ctx, String.class));
            }
            
            // If ProjectStage is Development, The "displayName", "shortDescription",
            // "expert", "hidden", and "preferred" attributes are exposed
            if (ctx.getFacesContext().isProjectStage(ProjectStage.Development))
            {
                CompositeTagAttributeUtils.addDevelopmentAttributes(attributeDescriptor, ctx, 
                        _displayName, _shortDescription, _expert, _hidden, _preferred);
            }
            
            // Any additional attributes are exposed as attributes accessible
            // from the getValue() and attributeNames() methods on FeatureDescriptor
            CompositeTagAttributeUtils.addUnspecifiedAttributes(attributeDescriptor, tag, 
                    STANDARD_ATTRIBUTES_SORTED, ctx);
            
            return attributeDescriptor;
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
}
