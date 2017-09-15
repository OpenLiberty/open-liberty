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
package org.apache.myfaces.view.facelets.el;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.myfaces.view.facelets.PageContext;
import org.apache.myfaces.view.facelets.TemplateContext;

/**
 * Default instance of a VariableMapper backed by a Map
 * 
 * @see javax.el.VariableMapper
 * @see javax.el.ValueExpression
 * @see java.util.Map
 * 
 * @author Jacob Hookom
 * @version $Id: DefaultVariableMapper.java 1544852 2013-11-23 18:09:50Z lu4242 $
 */
public final class DefaultVariableMapper extends VariableMapperBase
{
    private Map<String, ValueExpression> _vars;
    
    private PageContext _pageContext;
    
    private TemplateContext _templateContext;
    
    private VariableMapper _delegate;
    
    public boolean _trackResolveVariables;
    
    public boolean _variableResolved;

    public DefaultVariableMapper()
    {
        super();
        _trackResolveVariables = false;
        _variableResolved = false;
    }

    public DefaultVariableMapper(VariableMapper delegate)
    {
        super();
        this._delegate = delegate;
    }
    
    /**
     * @see javax.el.VariableMapper#resolveVariable(java.lang.String)
     */
    public ValueExpression resolveVariable(String name)
    {
        ValueExpression returnValue = null;
        
        if (_vars != null)
        {
            returnValue = _vars.get(name);
        }

        //If the variable is not on the VariableMapper
        if (returnValue == null)
        {
            //Check on page and template context
            if (_pageContext != null && _pageContext.getAttributeCount() > 0)
            {
                if (_pageContext.getAttributes().containsKey(name))
                {
                    returnValue = _pageContext.getAttributes().get(name);
                    if (_trackResolveVariables)
                    {
                        if (returnValue instanceof CacheableValueExpression)
                        {
                            if (returnValue instanceof CacheableValueExpressionWrapper)
                            {
                                returnValue = ((CacheableValueExpressionWrapper)returnValue).
                                    getWrapped();
                            }
                        }
                        else
                        {
                            _variableResolved = true;
                        }
                    }
                    return returnValue;
                }
            }
            
            if (_templateContext != null)
            {
                if (!_templateContext.isParameterEmpty() &&
                    _templateContext.containsParameter(name))
                {
                    returnValue = _templateContext.getParameter(name);
                    if (_trackResolveVariables &&
                        !(returnValue instanceof CacheableValueExpression))
                    {
                        _variableResolved = true;
                    }
                    return returnValue;
                }
                else if (!_templateContext.isKnownParametersEmpty() &&
                    _templateContext.containsKnownParameter(name))
                {
                    // This part is the most important in alwaysRecompile EL cache hack.
                    // The idea is maintain a list of the parameters used in a template,
                    // and if the name to be resolved match one of the list, even if the
                    // param was not set it is necessary to track it as variable resolved.
                    // This will force create a new EL expression each time the view is
                    // built, preserving ui:param and VariableMapper behavior, but allow
                    // cache all expressions that are not related.
                    if (_trackResolveVariables)
                    {
                        _variableResolved = true;
                    }
                }
            }
            
            if (_delegate != null)
            {
                returnValue = _delegate.resolveVariable(name);
            }
        }
        else if (_trackResolveVariables)
        {
            // Is this code in a block that wants to cache 
            // the resulting expression(s) and variable has been resolved?
            _variableResolved = true;
        }
        
        return returnValue;
    }

    /**
     * @see javax.el.VariableMapper#setVariable(java.lang.String, javax.el.ValueExpression)
     */
    public ValueExpression setVariable(String name, ValueExpression expression)
    {
        if (_vars == null)
        {
            _vars = new HashMap<String, ValueExpression>();
        }
        
        return _vars.put(name, expression);
    }
    
    /**
     * Set the current page context this variable mapper should resolve against.
     * 
     * @param pageContext
     */
    public void setPageContext(PageContext pageContext)
    {
        this._pageContext = pageContext;
    }
    
    /**
     * Set the current template context this variable mapper should resolve against.
     * 
     * @param templateContext
     */
    public void setTemplateContext(TemplateContext templateContext)
    {
        this._templateContext = templateContext;
    }

    @Override
    public boolean isAnyFaceletsVariableResolved()
    {
        if (_trackResolveVariables)
        {
            return _variableResolved;
        }
        else
        {
            //Force expression creation
            return true;
        }
    }

    @Override
    public void beforeConstructELExpression()
    {
        _trackResolveVariables = true;
        _variableResolved = false;
    }

    @Override
    public void afterConstructELExpression()
    {
        _trackResolveVariables = false;
        _variableResolved = false;
    }
}
