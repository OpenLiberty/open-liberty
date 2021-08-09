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

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.view.ValueHolderAttachedObjectHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandlerDelegate;

import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;

/**
 * Handles setting a Converter instance on a ValueHolder. Will wire all attributes set to the Converter instance
 * created/fetched. Uses the "binding" attribute for grabbing instances to apply attributes to. <p/> Will only
 * set/create Converter is the passed UIComponent's parent is null, signifying that it wasn't restored from an existing
 * tree.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lofwyr $)
 * @version $Revision: 1581887 $ $Date: 2014-03-26 15:30:26 +0000 (Wed, 26 Mar 2014) $
 *
 * @since 2.0
 */
public class ConverterTagHandlerDelegate extends TagHandlerDelegate implements ValueHolderAttachedObjectHandler
{
    private ConverterHandler _delegate;
    
    public ConverterTagHandlerDelegate(ConverterHandler delegate)
    {
        _delegate = delegate;
    }

    /**
     * Set Converter instance on parent ValueHolder if it's not being restored.
     * <ol>
     * <li>Cast to ValueHolder</li>
     * <li>If "binding" attribute was specified, fetch/create and re-bind to expression.</li>
     * <li>Otherwise, call {@link #createConverter(FaceletContext) createConverter}.</li>
     * <li>Call {@link ObjectHandler#setAttributes(FaceletContext, Object) setAttributes} on Converter instance.</li>
     * <li>Set the Converter on the ValueHolder</li>
     * <li>If the ValueHolder has a localValue, convert it and set the value</li>
     * </ol>
     * 
     * @see ValueHolder
     * @see Converter
     * @see #createConverter(FaceletContext)
     * @see javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext, javax.faces.component.UIComponent)
     */
    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException
    {
        // only process if it's been created
        if (!ComponentHandler.isNew(parent))
        {
            return;
        }
        if (parent instanceof ValueHolder)
        {
            applyAttachedObject(ctx.getFacesContext(), parent);
        }
        else if (UIComponent.isCompositeComponent(parent))
        {
            if (getFor() == null)
            {
                throw new TagException(_delegate.getTag(), "is nested inside a composite component"
                        + " but does not have a for attribute.");
            }
            FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
            mctx.addAttachedObjectHandler(parent, _delegate);
        }
        else
        {
            throw new TagException(_delegate.getTag(),
                    "Parent not composite component or an instance of ValueHolder: " + parent);
        }      
    }

    /**
     * Create a Converter instance
     * 
     * @param ctx
     *            FaceletContext to use
     * @return Converter instance, cannot be null
     */
    protected Converter createConverter(FaceletContext ctx)
    {
        if (_delegate.getConverterId(ctx) == null)
        {
            throw new TagException(_delegate.getTag(),
                                    "Default behavior invoked of requiring a converter-id passed in the "
                                    + "constructor, must override ConvertHandler(ConverterConfig)");
        }
        return ctx.getFacesContext().getApplication().createConverter(_delegate.getConverterId(ctx));
    }

    @Override
    public MetaRuleset createMetaRuleset(Class type)
    {
        return new MetaRulesetImpl(_delegate.getTag(), type).ignore("binding").ignore("for");
    }

    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(
                FaceletContext.FACELET_CONTEXT_KEY);
        
        // cast to a ValueHolder
        ValueHolder vh = (ValueHolder) parent;
        ValueExpression ve = null;
        Converter c = null;
        if (_delegate.getBinding() != null)
        {
            ve = _delegate.getBinding().getValueExpression(faceletContext, Converter.class);
            c = (Converter) ve.getValue(faceletContext);
        }
        if (c == null)
        {
            c = this.createConverter(faceletContext);
            if (ve != null)
            {
                ve.setValue(faceletContext, c);
            }
        }
        if (c == null)
        {
            throw new TagException(_delegate.getTag(), "No Converter was created");
        }
        _delegate.setAttributes(faceletContext, c);
        vh.setConverter(c);
        Object lv = vh.getLocalValue();
        FacesContext faces = faceletContext.getFacesContext();
        if (lv instanceof String)
        {
            vh.setValue(c.getAsObject(faces, parent, (String) lv));
        }
    }

    public String getFor()
    {
        TagAttribute forAttribute = _delegate.getTagAttribute("for");
        
        if (forAttribute == null)
        {
            return null;
        }
        else
        {
            return forAttribute.getValue();
        }
    }
}
