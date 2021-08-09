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
package javax.faces.view.facelets;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.view.Location;

/**
 * Representation of a Tag's attribute in a Facelet File
 */
public abstract class TagAttribute
{
    public TagAttribute()
    {
        
    }

    /**
     * If literal, return {@link Boolean#getBoolean(java.lang.String) Boolean.getBoolean(java.lang.String)} passing our
     * value, otherwise call {@link #getObject(FaceletContext, Class) getObject(FaceletContext, Class)}.
     * 
     * @see Boolean#getBoolean(java.lang.String)
     * @see #getObject(FaceletContext, Class)
     * @param ctx
     *            FaceletContext to use
     * @return boolean value
     */
    public abstract boolean getBoolean(FaceletContext ctx);

    /**
     * If literal, call {@link Integer#parseInt(java.lang.String) Integer.parseInt(String)}, otherwise call
     * {@link #getObject(FaceletContext, Class) getObject(FaceletContext, Class)}.
     * 
     * @see Integer#parseInt(java.lang.String)
     * @see #getObject(FaceletContext, Class)
     * @param ctx
     *            FaceletContext to use
     * @return int value
     */
    public abstract int getInt(FaceletContext ctx);

    /**
     * Local name of this attribute
     * 
     * @return local name of this attribute
     */
    public abstract String getLocalName();

    /**
     * The location of this attribute in the FaceletContext
     * 
     * @return the TagAttribute's location
     */
    public abstract Location getLocation();

    /**
     * Create a MethodExpression, using this attribute's value as the expression String.
     * 
     * @see javax.el.ExpressionFactory#createMethodExpression(javax.el.ELContext, java.lang.String, java.lang.Class,
     *      java.lang.Class[])
     * @see MethodExpression
     * @param ctx
     *            FaceletContext to use
     * @param type
     *            expected return type
     * @param paramTypes
     *            parameter type
     * @return a MethodExpression instance
     */
    public abstract MethodExpression getMethodExpression(FaceletContext ctx, Class type, Class[] paramTypes);

    /**
     * The resolved Namespace for this attribute
     * 
     * @return resolved Namespace
     */
    public abstract String getNamespace();

    /**
     * Delegates to getObject with Object.class as a param
     * 
     * @see #getObject(FaceletContext, Class)
     * @param ctx
     *            FaceletContext to use
     * @return Object representation of this attribute's value
     */
    public abstract Object getObject(FaceletContext ctx);

    /**
     * If literal, simply coerce our String literal value using an ExpressionFactory, otherwise create a ValueExpression
     * and evaluate it.
     * 
     * @see javax.el.ExpressionFactory#coerceToType(java.lang.Object, java.lang.Class)
     * @see javax.el.ExpressionFactory#createValueExpression(javax.el.ELContext, java.lang.String, java.lang.Class)
     * @see ValueExpression
     * @param ctx
     *            FaceletContext to use
     * @param type
     *            expected return type
     * @return Object value of this attribute
     */
    public abstract Object getObject(FaceletContext ctx, Class type);

    /**
     * The qualified name for this attribute
     * 
     * @return the qualified name for this attribute
     */
    public abstract String getQName();

    /**
     * Return the literal value of this attribute
     * 
     * @return literal value
     */
    public abstract String getValue();

    /**
     * If literal, then return our value, otherwise delegate to getObject, passing String.class.
     * 
     * @see #getObject(FaceletContext, Class)
     * @param ctx
     *            FaceletContext to use
     * @return String value of this attribute
     */
    public abstract String getValue(FaceletContext ctx);

    /**
     * Create a ValueExpression, using this attribute's literal value and the passed expected type.
     * 
     * @see javax.el.ExpressionFactory#createValueExpression(javax.el.ELContext, java.lang.String, java.lang.Class)
     * @see ValueExpression
     * @param ctx
     *            FaceletContext to use
     * @param type
     *            expected return type
     * @return ValueExpression instance
     */
    public abstract ValueExpression getValueExpression(FaceletContext ctx, Class type);

    /**
     * If this TagAttribute is literal (not #{..} or ${..})
     * 
     * @return true if this attribute is literal
     */
    public abstract boolean isLiteral();
    
    /**
     * @since 2.2
     * @return 
     */
    public Tag getTag()
    {
        return null;
    }

    /**
     * @since 2.2
     * @param tag 
     */
    public void setTag(Tag tag)
    {
    }
}
