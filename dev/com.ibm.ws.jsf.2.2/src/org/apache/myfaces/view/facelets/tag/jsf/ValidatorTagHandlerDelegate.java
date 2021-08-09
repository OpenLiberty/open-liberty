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
import java.util.ArrayList;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.FacesWrapper;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.view.EditableValueHolderAttachedObjectHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandlerDelegate;
import javax.faces.view.facelets.ValidatorHandler;

import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.compiler.FaceletsCompilerUtils;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;

/**
 * Handles setting a Validator instance on a EditableValueHolder. Will wire all attributes set to the Validator instance
 * created/fetched. Uses the "binding" attribute for grabbing instances to apply attributes to. <p/> Will only
 * set/create Validator is the passed UIComponent's parent is null, signifying that it wasn't restored from an existing
 * tree.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1294324 $ $Date: 2012-02-27 21:01:32 +0000 (Mon, 27 Feb 2012) $
 *
 * @since 2.0
 */
public class ValidatorTagHandlerDelegate extends TagHandlerDelegate 
    implements EditableValueHolderAttachedObjectHandler, FacesWrapper<ValidatorHandler>
{
    
    /**
     * if <f:validateBean> has no children and its disabled attribute is true,
     * its validatorId will be added to the exclusion list stored under
     * this key on the parent UIComponent.
     */
    public final static String VALIDATOR_ID_EXCLUSION_LIST_KEY
            = "org.apache.myfaces.validator.VALIDATOR_ID_EXCLUSION_LIST";
    
    private ValidatorHandler _delegate;
    
    /**
     * true - this tag has children
     * false - this tag is a leave
     */
    private final boolean _wrapMode;
    
    public ValidatorTagHandlerDelegate(ValidatorHandler delegate)
    {
        _delegate = delegate;

        // According to jsf 2.0 spec section 10.4.1.4
        // this tag can be used as a leave within an EditableValueHolder
        // or as a container to provide validator information for all 
        // EditableValueHolder-children (and grandchildren and ...)
        // (this behavior is analog to <f:ajax>)
        // --> Determine if we have children:
        _wrapMode = FaceletsCompilerUtils.hasChildren(_delegate.getValidatorConfig());
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException
    {
        // we need methods from AbstractFaceletContext
        FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);

        if (_wrapMode)
        {
            // the tag has children --> provide validator information for all children
            
            // FIXME the spec says we should save the validation groups in an attribute
            // on the parent UIComponent, but this will be a problem in the following scenario:
            // <h:form>
            //     <f:validateBean>
            //         <h:inputText />
            //     </f:validateBean>
            //     <h:inputText />
            // </h:form>
            // because the validator would also be applied to the second h:inputText,
            // which it should not, on my opinion. In addition, mojarra also does not
            // attach the validator to the second h:inputText in this scenario (blackbox test).
            // So I use the same way as f:ajax for this problem. -=Jakob Korherr=-
            
            String validatorId = _delegate.getValidatorConfig().getValidatorId();
            /*
            boolean disabled = _delegate.isDisabled(ctx);
            if (disabled)
            {
                // the validator is disabled --> add its id to the exclusion stack
                boolean validatorIdAvailable = validatorId != null && !"".equals(validatorId);
                try
                {
                    if (validatorIdAvailable)
                    {
                        mctx.pushExcludedValidatorIdToStack(validatorId);
                    }
                    _delegate.getValidatorConfig().getNextHandler().apply(ctx, parent);
                }
                finally
                {
                    if (validatorIdAvailable)
                    {
                        mctx.popExcludedValidatorIdToStack();
                    }
                }
            }
            else
            {*/
                // the validator is enabled 
                // --> add the validation groups and the validatorId to the stack
                //String groups = getValidationGroups(ctx);
                // spec: don't save the validation groups string if it is null or empty string
                //boolean groupsAvailable = groups != null 
                        //&& !groups.matches(BeanValidator.EMPTY_VALIDATION_GROUPS_PATTERN);
                //try
                //{
                    //if (groupsAvailable)
                    //{
                    //    mctx.pushValidationGroupsToStack(groups);
                    //}
                    try
                    {
                        mctx.pushEnclosingValidatorIdToStack(validatorId, this);
                        _delegate.applyNextHandler(ctx, parent);
                    }
                    finally
                    {
                        mctx.popEnclosingValidatorIdToStack();
                    }
                //}
                //finally
                //{
                    //if (groupsAvailable)
                    //{
                        //mctx.popValidationGroupsToStack();
                    //}
                //}
            /*}*/
        }
        else
        {
            // Apply only if we are creating a new component
            if (!ComponentHandler.isNew(parent))
            {
                return;
            }

            // the tag is a leave --> attach validator to parent
            if (parent instanceof EditableValueHolder)
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
                mctx.addAttachedObjectHandler(parent, _delegate);
            }
            else
            {
                throw new TagException(_delegate.getTag(),
                        "Parent not composite component or an instance of EditableValueHolder: " + parent);
            }
        }
    }

    /**
     * Template method for creating a Validator instance
     * 
     * @param ctx FaceletContext to use
     * @return a new Validator instance
     */
    protected Validator createValidator(FaceletContext ctx)
    {
        if (_delegate.getValidatorId(ctx) == null)
        {
            throw new TagException(_delegate.getTag(), "Default behavior invoked of requiring " +
                    "a validator-id passed in the constructor, must override ValidateHandler(ValidatorConfig)");
        }
        return ctx.getFacesContext().getApplication().createValidator(_delegate.getValidatorId(ctx));
    }

    @Override
    public MetaRuleset createMetaRuleset(Class type)
    {
        MetaRuleset metaRuleset = new MetaRulesetImpl(_delegate.getTag(), type);
        
        // ignore binding and disabled, because they are handled by DelegatingMetaTagHandler
        metaRuleset.ignore(JSFAttr.BINDING_ATTR).ignore(JSFAttr.DISABLED_ATTR);
        // ignore for, because it is handled by FaceletsAttachedObjectHandler
        metaRuleset.ignore(JSFAttr.FOR_ATTR);
        
        return metaRuleset;
    }

    @SuppressWarnings("unchecked")
    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(
                FaceletContext.FACELET_CONTEXT_KEY);
        
        // spec: if the disabled attribute is true, the validator should not be added.
        // in addition, the validatorId, if present, should be added to an exclusion
        // list on the parent component to prevent a default validator with the same
        // id from beeing registered on the component.
        if (_delegate.isDisabled(faceletContext))
        {
            // tag is disabled --> add its validatorId to the parent's exclusion list
            String validatorId = _delegate.getValidatorConfig().getValidatorId();
            if (validatorId != null && !"".equals(validatorId))
            {
                List<String> exclusionList = (List<String>) parent.getAttributes()
                        .get(VALIDATOR_ID_EXCLUSION_LIST_KEY);
                if (exclusionList == null)
                {
                    exclusionList = new ArrayList<String>();
                    parent.getAttributes().put(VALIDATOR_ID_EXCLUSION_LIST_KEY, exclusionList);
                }
                exclusionList.add(validatorId);
            }
        }
        else
        {
            // tag is enabled --> create the validator and attach it
            
            // cast to a ValueHolder
            EditableValueHolder evh = (EditableValueHolder) parent;
            ValueExpression ve = null;
            Validator v = null;
            if (_delegate.getBinding() != null)
            {
                ve = _delegate.getBinding().getValueExpression(faceletContext, Validator.class);
                v = (Validator) ve.getValue(faceletContext);
            }
            if (v == null)
            {
                v = this.createValidator(faceletContext);
                if (ve != null)
                {
                    ve.setValue(faceletContext, v);
                }
            }
            if (v == null)
            {
                throw new TagException(_delegate.getTag(), "No Validator was created");
            }
            _delegate.setAttributes(faceletContext, v);
            evh.addValidator(v); 
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
    
    public String getValidationGroups(FaceletContext ctx)
    {
        TagAttribute attribute = _delegate.getTagAttribute("validationGroups");
        
        if (attribute == null)
        {
            return null;
        }
        else
        {
            return attribute.getValue(ctx);
        }
    }

    public ValidatorHandler getWrapped()
    {
        return _delegate;
    }

}
