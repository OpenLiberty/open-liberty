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
import javax.faces.component.behavior.Behavior;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.view.BehaviorHolderAttachedObjectHandler;
import javax.faces.view.facelets.BehaviorHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandlerDelegate;

import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;
import org.apache.myfaces.view.facelets.tag.jsf.core.AjaxHandler;

/**
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1194861 $ $Date: 2011-10-29 10:02:34 +0000 (Sat, 29 Oct 2011) $
 *
 * @since 2.0
 */
public class BehaviorTagHandlerDelegate extends TagHandlerDelegate implements BehaviorHolderAttachedObjectHandler
{

    private BehaviorHandler _delegate;
    
    public BehaviorTagHandlerDelegate(BehaviorHandler delegate)
    {
        _delegate = delegate;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException
    {
        if (!ComponentHandler.isNew(parent))
        {
            return;
        }
        // Note that the only contract defined at this moment based on behavior api
        // is client behavior, so it is enough to cast it here. In the future, new
        // implementations should be added here.
        if (parent instanceof ClientBehaviorHolder)
        {
            applyAttachedObject(ctx.getFacesContext(), parent);
        }
        else if (UIComponent.isCompositeComponent(parent))
        {
            FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
            // It is supposed that for composite components, this tag should
            // add itself as a target, but note that on whole api does not exists
            // some tag that expose client behaviors as targets for composite
            // components. In RI, there exists a tag called composite:clientBehavior,
            // but does not appear on spec or javadoc, maybe because this could be
            // understand as an implementation detail, after all there exists a key
            // called AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY that could be
            // used to create a tag outside jsf implementation to attach targets.
            mctx.addAttachedObjectHandler(parent, _delegate);
        }
        else
        {
            throw new TagException(_delegate.getTag(),
                    "Parent not composite component or an instance of ClientBehaviorHolder: " + parent);
        }
        
    }
    
    protected Behavior createBehavior(FaceletContext ctx)
    {
        if (_delegate.getBehaviorId() == null)
        {
            throw new TagException(
                                   _delegate.getTag(),
                                   "No behavior id defined");
        }
        return ctx.getFacesContext().getApplication().createBehavior(_delegate.getBehaviorId());
    }

    /**
     * This tag call _delegate.setAttributes, so the returned MetaRuleset
     * should ignore attributes that are not supposed to be there like
     * "binding" and "event"
     */
    @Override
    public MetaRuleset createMetaRuleset(Class type)
    {
        MetaRuleset ruleset = new MetaRulesetImpl(_delegate.getTag(), type);
        ruleset.ignore("binding");
        ruleset.ignore("event");
        return ruleset;
    }

    /**
     * Create a ClientBehavior and attach it to the component
     */
    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(
                FaceletContext.FACELET_CONTEXT_KEY);
        
        ValueExpression ve = null;
        Behavior behavior = null;
        if (_delegate.getBinding() != null)
        {
            ve = _delegate.getBinding().getValueExpression(faceletContext, Behavior.class);
            behavior = (Behavior) ve.getValue(faceletContext);
        }
        if (behavior == null)
        {
            behavior = this.createBehavior(faceletContext);
            if (ve != null)
            {
                ve.setValue(faceletContext, behavior);
            }
        }
        if (behavior == null)
        {
            throw new TagException(_delegate.getTag(), "No Validator was created");
        }
        _delegate.setAttributes(faceletContext, behavior);
        
        if (behavior instanceof ClientBehavior)
        {
            // cast to a ClientBehaviorHolder
            ClientBehaviorHolder cvh = (ClientBehaviorHolder) parent;
            
            // TODO: check if the behavior could be applied to the current parent
            // For run tests it is not necessary, so we let this one pending.

            // It is necessary to obtain a event name for add it, so we have to
            // look first to the defined event name, otherwise take the default from
            // the holder
            String eventName = getEventName();
            if (eventName == null)
            {
                eventName = cvh.getDefaultEventName();
            }
            if (eventName == null)
            {
                throw new TagAttributeException(_delegate.getEvent(),
                        "eventName could not be defined for client behavior "+ behavior.toString());
            }
            else if (!cvh.getEventNames().contains(eventName))
            {
                throw new TagAttributeException(_delegate.getEvent(),
                        "eventName "+eventName+" not found on component instance");
            }
            else
            {
                cvh.addClientBehavior(eventName, (ClientBehavior) behavior);
            }
            
            AjaxHandler.registerJsfAjaxDefaultResource(faceletContext, parent);
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

    public String getEventName()
    {
        return _delegate.getEventName();
    }

}
