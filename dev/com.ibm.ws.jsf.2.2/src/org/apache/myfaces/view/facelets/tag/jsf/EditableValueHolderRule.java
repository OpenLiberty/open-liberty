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

import javax.el.MethodExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.MethodExpressionValueChangeListener;
import javax.faces.event.ValueChangeEvent;
import javax.faces.validator.MethodExpressionValidator;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

import org.apache.myfaces.view.facelets.FaceletCompositionContext;

/**
 * 
 * @author Jacob Hookom
 * @version $Id: EditableValueHolderRule.java 1194861 2011-10-29 10:02:34Z struberg $
 */
public final class EditableValueHolderRule extends MetaRule
{

    final static class LiteralValidatorMetadata extends Metadata
    {

        private final String validatorId;

        public LiteralValidatorMetadata(String validatorId)
        {
            this.validatorId = validatorId;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((EditableValueHolder) instance).addValidator(ctx.getFacesContext().getApplication()
                    .createValidator(this.validatorId));
        }
    }

    final static class ValueChangedExpressionMetadata extends Metadata
    {
        private final TagAttribute attr;

        public ValueChangedExpressionMetadata(TagAttribute attr)
        {
            this.attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            // From JSF 2.0 it is possible to have valueChangeListener method without ValueChangeEvent parameter. 
            // It seems that MethodExpressionValueChangeListener from API contains support for it but there is one big
            // problem - one-arg constructor will not preserve the current VariableMapper.
            // This is a problem when using facelets and <ui:decorate/> with EL params (see MYFACES-2541 for details).
            // So we must create two MethodExpressions here - both are created from the current 
            // facelets context and thus variable mapping will work.
            final MethodExpression methodExpressionOneArg = attr.getMethodExpression(ctx, null, VALUECHANGE_SIG);
            final MethodExpression methodExpressionZeroArg = attr.getMethodExpression(ctx, null, EMPTY_CLASS_ARRAY);
            if (FaceletCompositionContext.getCurrentInstance(ctx).isUsingPSSOnThisView())
            {
                ((EditableValueHolder) instance).addValueChangeListener(
                      new PartialMethodExpressionValueChangeListener(methodExpressionOneArg, methodExpressionZeroArg));
            }
            else
            {
                ((EditableValueHolder) instance).addValueChangeListener(
                        new MethodExpressionValueChangeListener(methodExpressionOneArg, methodExpressionZeroArg));
            }
        }
    }

    final static class ValidatorExpressionMetadata extends Metadata
    {
        private final TagAttribute attr;

        public ValidatorExpressionMetadata(TagAttribute attr)
        {
            this.attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            if (FaceletCompositionContext.getCurrentInstance(ctx).isUsingPSSOnThisView())
            {
                ((EditableValueHolder) instance).addValidator(new PartialMethodExpressionValidator(this.attr
                        .getMethodExpression(ctx, null, VALIDATOR_SIG)));
            }
            else
            {
                ((EditableValueHolder) instance).addValidator(new MethodExpressionValidator(this.attr
                        .getMethodExpression(ctx, null, VALIDATOR_SIG)));
            }
        }
    }

    private final static Class<?>[] VALIDATOR_SIG = new Class[] { FacesContext.class, UIComponent.class, Object.class };

    private final static Class<?>[] VALUECHANGE_SIG = new Class[] { ValueChangeEvent.class };
    
    private final static Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    public final static EditableValueHolderRule INSTANCE = new EditableValueHolderRule();

    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta)
    {

        if (meta.isTargetInstanceOf(EditableValueHolder.class))
        {

            if ("validator".equals(name))
            {
                if (attribute.isLiteral())
                {
                    return new LiteralValidatorMetadata(attribute.getValue());
                }
                else
                {
                    return new ValidatorExpressionMetadata(attribute);
                }
            }

            if ("valueChangeListener".equals(name))
            {
                return new ValueChangedExpressionMetadata(attribute);
            }

        }
        return null;
    }

}
