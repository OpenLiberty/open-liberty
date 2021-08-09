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
package org.apache.myfaces.view.facelets.tag.jsf.core;

import java.io.IOException;
import java.io.Serializable;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.view.ActionSource2AttachedObjectHandler;
import javax.faces.view.Location;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.el.ContextAware;
import org.apache.myfaces.view.facelets.el.ContextAwareELException;

@JSFFaceletTag(
        name = "f:setPropertyActionListener",
        bodyContent = "empty", 
        tagClass="org.apache.myfaces.taglib.core.SetPropertyActionListenerTag")
public class SetPropertyActionListenerHandler extends TagHandler
    implements ActionSource2AttachedObjectHandler 
{
    private final TagAttribute _target;
    private final TagAttribute _value;

    public SetPropertyActionListenerHandler(TagConfig config)
    {
        super(config);
        this._value = this.getRequiredAttribute("value");
        this._target = this.getRequiredAttribute("target");
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException
    {
        //Apply only if we are creating a new component
        if (!ComponentHandler.isNew(parent))
        {
            return;
        }
        if (parent instanceof ActionSource)
        {
            applyAttachedObject(ctx.getFacesContext(), parent);
        }
        else if (UIComponent.isCompositeComponent(parent))
        {
            FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
            mctx.addAttachedObjectHandler(parent, this);
        }
        else
        {
            throw new TagException(this.tag,
                    "Parent is not composite component or of type ActionSource, type is: " + parent);
        }
    }

    private static class SetPropertyListener implements ActionListener, Serializable
    {
        private ValueExpression _target;
        private ValueExpression _value;

        public SetPropertyListener()
        {
        };

        public SetPropertyListener(ValueExpression value, ValueExpression target)
        {
            _value = value;
            _target = target;
        }

        public void processAction(ActionEvent evt) throws AbortProcessingException
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            
            ELContext elContext = facesContext.getELContext();
            
            // Spec f:setPropertyActionListener: 
            
            // Call getValue() on the "value" ValueExpression.
            Object value = _value.getValue(elContext);
            
            // If value of the "value" expression is null, call setValue() on the "target" ValueExpression with the null
            
            // If the value of the "value" expression is not null, call getType()on the "value" and "target"
            // ValueExpressions to determine their property types.
            if (value != null)
            {
                Class<?> targetType = _target.getType(elContext);
                // Spec says: "all getType() on the "value" to determine  property type" but it is not necessary
                // beacuse type we have objValue already

                //   Coerce the value of the "value" expression to
                // the "target" expression value type following the Expression
                // Language coercion rules. 
                ExpressionFactory expressionFactory = facesContext.getApplication().getExpressionFactory();
                try
                {
                    value = expressionFactory.coerceToType(value, targetType);
                }
                catch (ELException e)
                {
                    // Happens when type of attribute "value" is not convertible to type of attribute "target"
                    // by EL coercion rules. 
                    // For example: value="#{10}" target="#{bean.booleanProperty}" 
                    // In this case is not sure if problematic attribute is "value" or "target". But EL
                    // impls say:
                    // JUEL: "Cannot coerce from class java.lang.Long to class java.lang.Boolean"
                    // Tomcat EL: Cannot convert 10 of type class java.long.Long to class java.lang.Boolean
                    // Thus we report "value" attribute as exception source - that should be enough for user
                    // to solve the problem.
                    Location location = null;
                    // Wrapping of ValueExpressions to org.apache.myfaces.view.facelets.el.ContextAware
                    // can be disabled:
                    if (_value instanceof ContextAware)
                    {
                        ContextAware contextAware = (ContextAware) _value;
                        location = contextAware.getLocation();
                    }
                    throw new ContextAwareELException(location,
                            _value.getExpressionString(), "value", e);
                }
            }

            // Call setValue()on the "target" ValueExpression with the resulting value.
            _target.setValue(elContext, value);
        }
    }

    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(
                FaceletContext.FACELET_CONTEXT_KEY);

        ActionSource src = (ActionSource) parent;
        ValueExpression valueExpr = _value.getValueExpression(faceletContext, Object.class);
        ValueExpression targetExpr = _target.getValueExpression(faceletContext, Object.class);

        src.addActionListener(new SetPropertyListener(valueExpr, targetExpr));
    }

    /**
     * TODO: Document me!
     */
    @JSFFaceletAttribute
    public String getFor()
    {
        TagAttribute forAttribute = getAttribute("for");
        
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
