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
package org.apache.myfaces.view.facelets.tag;

import java.util.Arrays;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.view.Location;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;

import org.apache.myfaces.util.ExternalSpecifications;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.el.CompositeComponentELUtils;
import org.apache.myfaces.view.facelets.el.ContextAwareTagMethodExpression;
import org.apache.myfaces.view.facelets.el.ContextAwareTagValueExpression;
import org.apache.myfaces.view.facelets.el.ContextAwareTagValueExpressionUEL;
import org.apache.myfaces.view.facelets.el.ELText;
import org.apache.myfaces.view.facelets.el.LocationMethodExpression;
import org.apache.myfaces.view.facelets.el.LocationValueExpression;
import org.apache.myfaces.view.facelets.el.LocationValueExpressionUEL;
import org.apache.myfaces.view.facelets.el.ResourceELUtils;
import org.apache.myfaces.view.facelets.el.ResourceLocationValueExpression;
import org.apache.myfaces.view.facelets.el.ResourceLocationValueExpressionUEL;
import org.apache.myfaces.view.facelets.el.TagMethodExpression;
import org.apache.myfaces.view.facelets.el.TagValueExpression;
import org.apache.myfaces.view.facelets.el.TagValueExpressionUEL;
import org.apache.myfaces.view.facelets.el.ValueExpressionMethodExpression;

/**
 * Representation of a Tag's attribute in a Facelet File
 * 
 * @author Jacob Hookom
 * @version $Id: TagAttributeImpl.java 1406265 2012-11-06 18:33:41Z lu4242 $
 */
public final class TagAttributeImpl extends TagAttribute
{

    private final static int EL_LITERAL = 1;
    
    private final static int EL_CC = 2;
    
    private final static int EL_CC_ATTR_ME = 4;
    
    private final static int EL_RESOURCE = 8;
    
    private final int capabilities;

    private final String localName;

    private final Location location;

    private final String namespace;

    private final String qName;

    private final String value;

    private String string;

    /**
     * This variable is used to cache created expressions using
     * getValueExpression or getMethodExpression methods. It uses
     * a racy single check strategy, because if the expression can be
     * cached the same instance will be built.
     */
    private volatile Object[] cachedExpression;

    public TagAttributeImpl(Location location, String ns, String localName, String qName, String value)
    {
        boolean literal;
        boolean compositeComponentExpression;
        boolean compositeComponentAttrMethodExpression;
        boolean resourceExpression;
        this.location = location;
        this.namespace = ns;
        // "xmlns" attribute name can be swallowed by SAX compiler, so we should check if
        // localName is null or empty and if that so, assign it from the qName 
        // (if localName is empty it is not prefixed, so it is save to set it directly). 
        this.localName = (localName == null) ? qName : ((localName.length() > 0) ? localName : qName);
        this.qName = qName;
        this.value = value;

        try
        {
            literal = ELText.isLiteral(this.value);
        }
        catch (ELException e)
        {
            throw new TagAttributeException(this, e);
        }
        
        compositeComponentExpression = !literal ? 
                CompositeComponentELUtils.isCompositeComponentExpression(this.value) : 
                    false;
        compositeComponentAttrMethodExpression = compositeComponentExpression ? 
                CompositeComponentELUtils.isCompositeComponentAttrsMethodExpression(this.value) : 
                    false;
        resourceExpression = !literal ? ResourceELUtils.isResourceExpression(this.value) : false;

        this.capabilities = (literal ? EL_LITERAL : 0) | (compositeComponentExpression ? EL_CC : 0) | 
            (compositeComponentAttrMethodExpression ? EL_CC_ATTR_ME : 0) | ( resourceExpression ? EL_RESOURCE : 0); 
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
    public boolean getBoolean(FaceletContext ctx)
    {
        if ((this.capabilities & EL_LITERAL) != 0)
        {
            return Boolean.valueOf(this.value).booleanValue();
        }
        else
        {
            return ((Boolean) this.getObject(ctx, Boolean.class)).booleanValue();
        }
    }

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
    public int getInt(FaceletContext ctx)
    {
        if ((this.capabilities & EL_LITERAL) != 0)
        {
            return Integer.parseInt(this.value);
        }
        else
        {
            return ((Number) this.getObject(ctx, Integer.class)).intValue();
        }
    }

    /**
     * Local name of this attribute
     * 
     * @return local name of this attribute
     */
    public String getLocalName()
    {
        return this.localName;
    }

    /**
     * The location of this attribute in the FaceletContext
     * 
     * @return the TagAttribute's location
     */
    public Location getLocation()
    {
        return this.location;
    }

    /**
     * Create a MethodExpression, using this attribute's value as the expression String.
     * 
     * @see ExpressionFactory#createMethodExpression(javax.el.ELContext, java.lang.String, java.lang.Class,
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
    public MethodExpression getMethodExpression(FaceletContext ctx, Class type, Class[] paramTypes)
    {
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        
        //volatile reads are atomic, so take the tuple to later comparison.
        Object[] localCachedExpression = cachedExpression; 
        
        if (actx.isAllowCacheELExpressions() && localCachedExpression != null &&
            (localCachedExpression.length % 3 == 0))
        {
            //If the expected type and paramTypes are the same return the cached one
            for (int i = 0; i < (localCachedExpression.length/3); i++)
            {
                if ( ((type == null && localCachedExpression[(i*3)] == null ) ||
                     (type != null && type.equals(localCachedExpression[(i*3)])) ) &&
                     (Arrays.equals(paramTypes, (Class[]) localCachedExpression[(i*3)+1])) )
                {
                    if ((this.capabilities & EL_CC) != 0 &&
                        localCachedExpression[(i*3)+2] instanceof LocationMethodExpression)
                    {
                        return ((LocationMethodExpression)localCachedExpression[(i*3)+2]).apply(
                                actx.getFaceletCompositionContext().getCompositeComponentLevel());
                    }
                    return (MethodExpression) localCachedExpression[(i*3)+2];
                }
            }
        }
        
        actx.beforeConstructELExpression();
        try
        {
            MethodExpression methodExpression = null;
            
            // From this point we can suppose this attribute contains a ELExpression
            // Now we have to check if the expression points to a composite component attribute map
            // and if so deal with it as an indirection.
            // NOTE that we have to check if the expression refers to cc.attrs for a MethodExpression
            // (#{cc.attrs.myMethod}) or only for MethodExpression parameters (#{bean.method(cc.attrs.value)}).
            if ((this.capabilities & EL_CC_ATTR_ME) != 0)
            {
                // The MethodExpression is on parent composite component attribute map.
                // create a pointer that are referred to the real one that is created in other side
                // (see VDL.retargetMethodExpressions for details)
                
                // check for params in the the MethodExpression
                if (ExternalSpecifications.isUnifiedELAvailable() && this.value.contains("("))
                {
                    // if we don't throw this exception here, another ELException will be
                    // thrown later, because #{cc.attrs.method(param)} will not work as a
                    // ValueExpression pointing to a MethodExpression
                    throw new ELException("Cannot add parameters to a MethodExpression "
                            + "pointing to cc.attrs");
                }
                
                ValueExpression valueExpr = this.getValueExpression(ctx, Object.class);
                methodExpression = new ValueExpressionMethodExpression(valueExpr);
                
                if (actx.getFaceletCompositionContext().isWrapTagExceptionsAsContextAware())
                {
                    methodExpression = new ContextAwareTagMethodExpression(this, methodExpression);
                }
                else
                {
                    methodExpression = new TagMethodExpression(this, methodExpression);
                }
            }
            else
            {
                ExpressionFactory f = ctx.getExpressionFactory();
                methodExpression = f.createMethodExpression(ctx, this.value, type, paramTypes);

                if (actx.getFaceletCompositionContext().isWrapTagExceptionsAsContextAware())
                {
                    methodExpression = new ContextAwareTagMethodExpression(this, methodExpression);
                }
                else
                {
                    methodExpression = new TagMethodExpression(this, methodExpression);
                }

                // if the MethodExpression contains a reference to the current composite
                // component, the Location also has to be stored in the MethodExpression 
                // to be able to resolve the right composite component (the one that was
                // created from the file the Location is pointing to) later.
                // (see MYFACES-2561 for details)
                if ((this.capabilities & EL_CC) != 0)
                {
                    methodExpression = new LocationMethodExpression(getLocation(), methodExpression, 
                            actx.getFaceletCompositionContext().getCompositeComponentLevel());
                }
            }
            
                
            if (actx.isAllowCacheELExpressions() && !actx.isAnyFaceletsVariableResolved())
            {
                if (localCachedExpression != null && (localCachedExpression.length % 3 == 0))
                {
                    // If you use a racy single check, assign
                    // the volatile variable at the end.
                    Object[] array = new Object[localCachedExpression.length+3];
                    array[0] = type;
                    array[1] = paramTypes;
                    array[2] = methodExpression;
                    for (int i = 0; i < localCachedExpression.length; i++)
                    {
                        array[i+3] = localCachedExpression[i];
                    }
                    cachedExpression = array;
                }
                else
                {
                    cachedExpression = new Object[]{type, paramTypes, methodExpression};
                }
            }

            return methodExpression; 
        }
        catch (Exception e)
        {
            throw new TagAttributeException(this, e);
        }
        finally
        {
            actx.afterConstructELExpression();
        }
    }
    
    /**
     * The resolved Namespace for this attribute
     * 
     * @return resolved Namespace
     */
    public String getNamespace()
    {
        return this.namespace;
    }

    /**
     * Delegates to getObject with Object.class as a param
     * 
     * @see #getObject(FaceletContext, Class)
     * @param ctx
     *            FaceletContext to use
     * @return Object representation of this attribute's value
     */
    public Object getObject(FaceletContext ctx)
    {
        return this.getObject(ctx, Object.class);
    }

    /**
     * The qualified name for this attribute
     * 
     * @return the qualified name for this attribute
     */
    public String getQName()
    {
        return this.qName;
    }

    /**
     * Return the literal value of this attribute
     * 
     * @return literal value
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * If literal, then return our value, otherwise delegate to getObject, passing String.class.
     * 
     * @see #getObject(FaceletContext, Class)
     * @param ctx
     *            FaceletContext to use
     * @return String value of this attribute
     */
    public String getValue(FaceletContext ctx)
    {
        if ((this.capabilities & EL_LITERAL) != 0)
        {
            return this.value;
        }
        else
        {
            return (String) this.getObject(ctx, String.class);
        }
    }

    /**
     * If literal, simply coerce our String literal value using an ExpressionFactory, otherwise create a ValueExpression
     * and evaluate it.
     * 
     * @see ExpressionFactory#coerceToType(java.lang.Object, java.lang.Class)
     * @see ExpressionFactory#createValueExpression(javax.el.ELContext, java.lang.String, java.lang.Class)
     * @see ValueExpression
     * @param ctx
     *            FaceletContext to use
     * @param type
     *            expected return type
     * @return Object value of this attribute
     */
    public Object getObject(FaceletContext ctx, Class type)
    {
        if ((this.capabilities & EL_LITERAL) != 0)
        {
            if (String.class.equals(type))
            {
                return this.value;
            }
            else
            {
                try
                {
                    return ctx.getExpressionFactory().coerceToType(this.value, type);
                }
                catch (Exception e)
                {
                    throw new TagAttributeException(this, e);
                }
            }
        }
        else
        {
            ValueExpression ve = this.getValueExpression(ctx, type);
            try
            {
                return ve.getValue(ctx);
            }
            catch (Exception e)
            {
                throw new TagAttributeException(this, e);
            }
        }
    }

    /**
     * Create a ValueExpression, using this attribute's literal value and the passed expected type.
     * 
     * @see ExpressionFactory#createValueExpression(javax.el.ELContext, java.lang.String, java.lang.Class)
     * @see ValueExpression
     * @param ctx
     *            FaceletContext to use
     * @param type
     *            expected return type
     * @return ValueExpression instance
     */
    public ValueExpression getValueExpression(FaceletContext ctx, Class type)
    {
        AbstractFaceletContext actx = (AbstractFaceletContext) ctx;
        
        //volatile reads are atomic, so take the tuple to later comparison.
        Object[] localCachedExpression = cachedExpression;
        if (actx.isAllowCacheELExpressions() && localCachedExpression != null && localCachedExpression.length == 2)
        {
            //If the expected type is the same return the cached one
            if (localCachedExpression[0] == null && type == null)
            {
                // If #{cc} recalculate the composite component level
                if ((this.capabilities & EL_CC) != 0)
                {
                    return ((LocationValueExpression)localCachedExpression[1]).apply(
                            actx.getFaceletCompositionContext().getCompositeComponentLevel());
                }
                return (ValueExpression) localCachedExpression[1];
            }
            else if (localCachedExpression[0] != null && localCachedExpression[0].equals(type))
            {
                // If #{cc} recalculate the composite component level
                if ((this.capabilities & EL_CC) != 0)
                {
                    return ((LocationValueExpression)localCachedExpression[1]).apply(
                            actx.getFaceletCompositionContext().getCompositeComponentLevel());
                }
                return (ValueExpression) localCachedExpression[1];
            }
        }

        actx.beforeConstructELExpression();
        try
        {
            ExpressionFactory f = ctx.getExpressionFactory();
            ValueExpression valueExpression = f.createValueExpression(ctx, this.value, type);
            
            if (ExternalSpecifications.isUnifiedELAvailable())
            {
                if (actx.getFaceletCompositionContext().isWrapTagExceptionsAsContextAware())
                {
                    valueExpression = new ContextAwareTagValueExpressionUEL(this, valueExpression);
                }
                else
                {
                    valueExpression = new TagValueExpressionUEL(this, valueExpression);
                }
            }
            else
            {
                if (actx.getFaceletCompositionContext().isWrapTagExceptionsAsContextAware())
                {
                    valueExpression = new ContextAwareTagValueExpression(this, valueExpression);
                }
                else
                {
                    valueExpression = new TagValueExpression(this, valueExpression);
                }
            }

            // if the ValueExpression contains a reference to the current composite
            // component, the Location also has to be stored in the ValueExpression 
            // to be able to resolve the right composite component (the one that was
            // created from the file the Location is pointing to) later.
            // (see MYFACES-2561 for details)
            if ((this.capabilities & EL_CC) != 0)
            {
                if (ExternalSpecifications.isUnifiedELAvailable())
                {
                    valueExpression = new LocationValueExpressionUEL(getLocation(), valueExpression, 
                            actx.getFaceletCompositionContext().getCompositeComponentLevel());
                }
                else
                {
                    valueExpression = new LocationValueExpression(getLocation(), valueExpression, 
                            actx.getFaceletCompositionContext().getCompositeComponentLevel());
                }
            }
            else if ((this.capabilities & EL_RESOURCE) != 0)
            {
                if (ExternalSpecifications.isUnifiedELAvailable())
                {
                    valueExpression = new ResourceLocationValueExpressionUEL(getLocation(), valueExpression);
                }
                else
                {
                    valueExpression = new ResourceLocationValueExpression(getLocation(), valueExpression);
                }
            }
            
            
            if (actx.isAllowCacheELExpressions() && !actx.isAnyFaceletsVariableResolved())
            {
                cachedExpression = new Object[]{type, valueExpression};
            }
            return valueExpression;
        }
        catch (Exception e)
        {
            throw new TagAttributeException(this, e);
        }
        finally
        {
            actx.afterConstructELExpression();
        }
    }

    /**
     * If this TagAttribute is literal (not #{..} or ${..})
     * 
     * @return true if this attribute is literal
     */
    public boolean isLiteral()
    {
        return (this.capabilities & EL_LITERAL) != 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        if (this.string == null)
        {
            this.string = this.location + " " + this.qName + "=\"" + this.value + "\"";
        }
        return this.string;
    }

}
