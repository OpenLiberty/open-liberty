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
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;

import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.CompositeComponentELUtils;
import org.apache.myfaces.view.facelets.tag.TagHandlerUtils;

/**
 * This handler wraps a composite component definition. 
 * <p>
 * This handler is set by facelets compiler through 
 * CompositeComponentUnit class by the presence of cc:interface 
 * or cc:implementation tag.
 * </p> 
 * <p>
 * The presence of this class has the following objectives:
 * </p>
 * <ul>
 * <li>Cache the BeanInfo instance for a composite component</li>
 * <li>Set a Location object to resolve #{cc} correctly</li>
 * <li>Push the current composite component on FaceletCompositionContext stack</li>
 * <li>Set the attributes with declared default values</li>
 * </ul>
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */
public final class CompositeComponentDefinitionTagHandler implements FaceletHandler
{
    private final FaceletHandler _nextHandler;
    
    private boolean _cacheable;
    
    /**
     * Cached instance used by this component. Note here we have a 
     * "racy single-check".If this field is used, it is supposed 
     * the object cached by this handler is immutable, and this is
     * granted if all properties not saved as ValueExpression are
     * "literal". 
     **/
    private BeanInfo _cachedBeanInfo;
    
    private InterfaceHandler _interfaceHandler;
    
    private ImplementationHandler _implementationHandler;
    
    public CompositeComponentDefinitionTagHandler(FaceletHandler next)
    {
        this._nextHandler = next;
        
        _cacheable = true;
        
        _interfaceHandler = TagHandlerUtils.findFirstNextByType(_nextHandler, InterfaceHandler.class);
        
        _implementationHandler = TagHandlerUtils.findFirstNextByType(_nextHandler, ImplementationHandler.class);
        
        Collection<InterfaceDescriptorCreator> metadataInterfaceHandlerList = 
            TagHandlerUtils.findNextByType( _nextHandler, InterfaceDescriptorCreator.class);
        
        for (InterfaceDescriptorCreator handler : metadataInterfaceHandlerList)
        {
            if (!handler.isCacheable())
            {
                _cacheable = false;
                break;
            }
        }
        if (!_cacheable)
        {
            for (InterfaceDescriptorCreator handler : metadataInterfaceHandlerList)
            {
                handler.setCacheable(false);
            }
        }
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
        AbstractFaceletContext actx = (AbstractFaceletContext)ctx;
        UIComponent compositeBaseParent = actx.isBuildingCompositeComponentMetadata() ? parent : parent.getParent();
        
        // Store the current Location on the parent (the location is needed
        // to resolve the related composite component via #{cc} properly).
        if (_interfaceHandler != null)
        {
            if (!compositeBaseParent.getAttributes().containsKey(CompositeComponentELUtils.LOCATION_KEY))
            {
                compositeBaseParent.getAttributes()
                    .put(CompositeComponentELUtils.LOCATION_KEY, this._interfaceHandler.getLocation());
            }
        }
        else if (_implementationHandler != null)
        {
            if (!compositeBaseParent.getAttributes().containsKey(CompositeComponentELUtils.LOCATION_KEY))
            {
                compositeBaseParent.getAttributes()
                    .put(CompositeComponentELUtils.LOCATION_KEY, this._implementationHandler.getLocation());
            }
        }
        
        // Only apply if we are building composite component metadata,
        // in other words we are calling ViewDeclarationLanguage.getComponentMetadata
        if ( actx.isBuildingCompositeComponentMetadata() )
        {
            CompositeComponentBeanInfo tempBeanInfo = 
                (CompositeComponentBeanInfo) compositeBaseParent.getAttributes()
                .get(UIComponent.BEANINFO_KEY);
            
            if (tempBeanInfo == null)
            {
                if (_cacheable)
                {
                    if (_cachedBeanInfo == null)
                    {
                        tempBeanInfo  = _createCompositeComponentMetadata(compositeBaseParent);
                        compositeBaseParent.getAttributes().put(
                                UIComponent.BEANINFO_KEY, tempBeanInfo);
                        
                        try
                        {
                            mctx.pushCompositeComponentToStack(compositeBaseParent);
                            
                            // Store the ccLevel key here
                            if (!compositeBaseParent.getAttributes().containsKey(CompositeComponentELUtils.LEVEL_KEY))
                            {
                                compositeBaseParent.getAttributes()
                                    .put(CompositeComponentELUtils.LEVEL_KEY, mctx.getCompositeComponentLevel());
                            }

                            _nextHandler.apply(ctx, parent);
                            
                            Collection<String> declaredDefaultValues = null;
                            
                            for (PropertyDescriptor pd : tempBeanInfo.getPropertyDescriptors())
                            {
                                if (pd.getValue("default") != null)
                                {
                                    if (declaredDefaultValues  == null)
                                    {
                                        declaredDefaultValues = new ArrayList<String>();
                                    }
                                    declaredDefaultValues.add(pd.getName());
                                }
                            }
                            if (declaredDefaultValues == null)
                            {
                                declaredDefaultValues = Collections.emptyList();
                            }
                            tempBeanInfo.getBeanDescriptor().
                                    setValue(UIComponent.ATTRS_WITH_DECLARED_DEFAULT_VALUES, declaredDefaultValues);
                        }
                        finally
                        {
                            mctx.popCompositeComponentToStack();
                            
                            _cachedBeanInfo = tempBeanInfo;
                        }
                    }
                    else
                    {
                        // Put the cached instance, but in that case it is not necessary to call
                        // nextHandler
                        compositeBaseParent.getAttributes().put(
                                UIComponent.BEANINFO_KEY, _cachedBeanInfo);
                    }
                }
                else
                {
                    tempBeanInfo = _createCompositeComponentMetadata(compositeBaseParent);
                    compositeBaseParent.getAttributes().put(
                            UIComponent.BEANINFO_KEY, tempBeanInfo);
                    
                    try
                    {
                        mctx.pushCompositeComponentToStack(compositeBaseParent);
                        
                        // Store the ccLevel key here
                        if (!compositeBaseParent.getAttributes().containsKey(CompositeComponentELUtils.LEVEL_KEY))
                        {
                            compositeBaseParent.getAttributes()
                                .put(CompositeComponentELUtils.LEVEL_KEY, mctx.getCompositeComponentLevel());
                        }
                    
                        _nextHandler.apply(ctx, parent);
                        
                        Collection<String> declaredDefaultValues = null;
                        
                        for (PropertyDescriptor pd : tempBeanInfo.getPropertyDescriptors())
                        {
                            if (pd.getValue("default") != null)
                            {
                                if (declaredDefaultValues  == null)
                                {
                                    declaredDefaultValues = new ArrayList<String>();
                                }
                                declaredDefaultValues.add(pd.getName());
                            }
                        }
                        if (declaredDefaultValues == null)
                        {
                            declaredDefaultValues = Collections.emptyList();
                        }
                        tempBeanInfo.getBeanDescriptor().
                                setValue(UIComponent.ATTRS_WITH_DECLARED_DEFAULT_VALUES, declaredDefaultValues);
                    }
                    finally
                    {
                        mctx.popCompositeComponentToStack();
                    }
                }
            }
        }
        else
        {
            try
            {
                mctx.pushCompositeComponentToStack(compositeBaseParent);

                // Store the ccLevel key here
                if (!compositeBaseParent.getAttributes().containsKey(CompositeComponentELUtils.LEVEL_KEY))
                {
                    compositeBaseParent.getAttributes()
                        .put(CompositeComponentELUtils.LEVEL_KEY, mctx.getCompositeComponentLevel());
                }
            
                _nextHandler.apply(ctx, parent);
            }
            finally
            {
                mctx.popCompositeComponentToStack();
            }
        }
    }
    
    private CompositeComponentBeanInfo _createCompositeComponentMetadata(UIComponent parent)
    {
        BeanDescriptor descriptor = new BeanDescriptor(parent.getClass());
        CompositeComponentBeanInfo beanInfo = new CompositeComponentBeanInfo(descriptor);
        return beanInfo;
    }
}
