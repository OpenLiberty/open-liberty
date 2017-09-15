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
package org.apache.myfaces.view.facelets.tag.jstl.core;

import java.io.IOException;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.ELExpressionCacheMode;
import org.apache.myfaces.view.facelets.el.CacheableValueExpressionUELWrapper;
import org.apache.myfaces.view.facelets.el.CacheableValueExpressionWrapper;

/**
 * Simplified implementation of c:set
 * 
 * Sets the result of an expression evaluation in a 'scope'
 * 
 * @author Jacob Hookom
 * @version $Id: SetHandler.java 1544852 2013-11-23 18:09:50Z lu4242 $
 */
@JSFFaceletTag(name="c:set")
public class SetHandler extends TagHandler
{

    /**
     * Name of the exported scoped variable to hold the value
     * specified in the action. The type of the scoped variable is
     * whatever type the value expression evaluates to.
     */
    @JSFFaceletAttribute(className="java.lang.String")
    private final TagAttribute var;

    /**
     * Expression to be evaluated.
     */
    @JSFFaceletAttribute(
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.Object")
    private final TagAttribute value;

    @JSFFaceletAttribute(
            name="scope",
            className="java.lang.String",
            longDescription="Scope for var.")
    private final TagAttribute scope;

    @JSFFaceletAttribute(
        name="target",
        className="java.lang.String",
        longDescription="Target object whose property will be set."+
        " Must evaluate to a JavaBeans object with setter property"+
        "property, or to a java.util.Map object.")
    private final TagAttribute target;

    @JSFFaceletAttribute(
        name="property",
        className="java.lang.String",
        longDescription="Name of the property to be set in the target object.")
    private final TagAttribute property;

    public SetHandler(TagConfig config)
    {
        super(config);
        this.value = this.getAttribute("value");
        this.var = this.getAttribute("var");
        this.scope = this.getAttribute("scope");
        this.target = this.getAttribute("target");
        this.property = this.getAttribute("property");
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        ValueExpression veObj = this.value.getValueExpression(ctx, Object.class);

        if (this.var != null)
        {
            // Get variable name
            String varStr = this.var.getValue(ctx);

            if (this.scope != null)
            {
                String scopeStr = this.scope.getValue(ctx);

                // Check scope string
                if (scopeStr == null || scopeStr.length() == 0)
                {
                    throw new TagException(tag, "scope must not be empty");
                }
                if ("page".equals(scopeStr))
                {
                    throw new TagException(tag, "page scope is not allowed");
                }

                // Build value expression string to set variable
                StringBuilder expStr = new StringBuilder().append("#{").append(scopeStr);
                if ("request".equals(scopeStr) || "view".equals(scopeStr) || "session".equals(scopeStr)
                        || "application".equals(scopeStr))
                {
                    expStr.append("Scope");
                }
                expStr.append(".").append(varStr).append("}");
                ELContext elCtx = ctx.getFacesContext().getELContext();
                ValueExpression expr = ctx.getExpressionFactory().createValueExpression(
                        elCtx, expStr.toString(), Object.class);
                expr.setValue(elCtx, veObj.getValue(elCtx));
            }
            else
            {
                //ctx.getVariableMapper().setVariable(varStr, veObj);
                AbstractFaceletContext actx = ((AbstractFaceletContext) ctx);
                if (ELExpressionCacheMode.alwaysRecompile.equals(actx.getELExpressionCacheMode()))
                {
                    if (ExternalSpecifications.isUnifiedELAvailable())
                    {
                        actx.getPageContext().getAttributes().put(varStr, 
                            new CacheableValueExpressionUELWrapper(veObj));
                    }
                    else
                    {
                        actx.getPageContext().getAttributes().put(varStr, 
                            new CacheableValueExpressionWrapper(veObj));
                    }
                }
                else
                {
                    actx.getPageContext().getAttributes().put(varStr, veObj);
                }
                if (actx.getPageContext().isAllowCacheELExpressions())
                {
                    if (ELExpressionCacheMode.strict.equals(actx.getELExpressionCacheMode()))
                    {
                        actx.getPageContext().setAllowCacheELExpressions(false);
                    }
                }
            }
        }
        else
        {
            // Check attributes
            if (this.target == null || this.property == null || this.value == null)
            {
                throw new TagException(
                        tag, "either attributes var and value or target, property and value must be set");
            }
            if (this.target.isLiteral())
            {
                throw new TagException(tag, "attribute target must contain a value expression");
            }

            // Get target object and name of property to set
            ELContext elCtx = ctx.getFacesContext().getELContext();
            ValueExpression targetExpr = this.target.getValueExpression(ctx, Object.class);
            Object targetObj = targetExpr.getValue(elCtx);
            String propertyName = this.property.getValue(ctx);
            // Set property on target object
            ctx.getELResolver().setValue(elCtx, targetObj, propertyName, veObj.getValue(elCtx));
        }
    }
}
