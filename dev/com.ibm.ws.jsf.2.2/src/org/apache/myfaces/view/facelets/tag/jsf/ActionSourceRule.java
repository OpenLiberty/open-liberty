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
import javax.faces.component.ActionSource;
import javax.faces.component.ActionSource2;
import javax.faces.event.ActionEvent;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.LegacyMethodBinding;

/**
 * 
 * @author Jacob Hookom
 * @version $Id: ActionSourceRule.java 1194861 2011-10-29 10:02:34Z struberg $
 */
public final class ActionSourceRule extends MetaRule
{
    public final static Class<?>[] ACTION_SIG = new Class[0];

    public final static Class<?>[] ACTION_LISTENER_SIG = new Class<?>[] { ActionEvent.class };

    final static class ActionMapper extends Metadata
    {

        private final TagAttribute attr;

        public ActionMapper(TagAttribute attr)
        {
            this.attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((ActionSource) instance).setAction(new LegacyMethodBinding(
                    this.attr.getMethodExpression(ctx, null, ActionSourceRule.ACTION_SIG)));
        }
    }
    
    final static class ActionMapper2 extends Metadata
    {
        private final TagAttribute _attr;

        public ActionMapper2(TagAttribute attr)
        {
            this._attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            MethodExpression expr = _attr.getMethodExpression(ctx, null, ActionSourceRule.ACTION_SIG);
            ((ActionSource2) instance).setActionExpression(expr);
        }
    }

    final static class ActionListenerMapper extends Metadata
    {

        private final TagAttribute attr;

        public ActionListenerMapper(TagAttribute attr)
        {
            this.attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            ((ActionSource) instance)
                    .setActionListener(new LegacyMethodBinding(this.attr
                            .getMethodExpression(ctx, null,
                                    ActionSourceRule.ACTION_LISTENER_SIG)));
        }

    }

    final static class ActionListenerMapper2 extends Metadata
    {
        private final TagAttribute _attr;

        public ActionListenerMapper2(TagAttribute attr)
        {
            _attr = attr;
        }

        public void applyMetadata(FaceletContext ctx, Object instance)
        {
            // From JSF 2.0 it is possible to have actionListener method without ActionEvent parameter. 
            // It seems that MethodExpressionActionListener from API contains support for it but there is one big
            // problem - one-arg constructor will not preserve the current VariableMapper.
            // This is a problem when using facelets and <ui:decorate/> with EL params (see MYFACES-2541 for details).
            // So we must create two MethodExpressions here - both are created from the current 
            // facelets context and thus varibale mapping will work.
            final MethodExpression methodExpressionOneArg
                    = _attr.getMethodExpression(ctx, null, ActionSourceRule.ACTION_LISTENER_SIG);
            final MethodExpression methodExpressionZeroArg
                    = _attr.getMethodExpression(ctx, null, ActionSourceRule.ACTION_SIG);
            if (FaceletCompositionContext.getCurrentInstance(ctx).isUsingPSSOnThisView())
            {
                ((ActionSource2) instance).addActionListener(
                        new PartialMethodExpressionActionListener(methodExpressionOneArg, methodExpressionZeroArg));
            }
            else
            {
                ((ActionSource2) instance).addActionListener(
                        new MethodExpressionActionListener(methodExpressionOneArg, methodExpressionZeroArg));
            }
        }
    }

    public final static ActionSourceRule INSTANCE = new ActionSourceRule();

    public ActionSourceRule()
    {
        super();
    }

    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta)
    {
        if (meta.isTargetInstanceOf(ActionSource.class))
        {
            if ("action".equals(name))
            {
                if (meta.isTargetInstanceOf(ActionSource2.class))
                {
                    return new ActionMapper2(attribute);
                }
                else
                {
                    return new ActionMapper(attribute);
                }
            }

            if ("actionListener".equals(name))
            {
                if (meta.isTargetInstanceOf(ActionSource2.class))
                {
                    return new ActionListenerMapper2(attribute);
                }
                else
                {
                    return new ActionListenerMapper(attribute);
                }
            }
        }
        
        return null;
    }
}
