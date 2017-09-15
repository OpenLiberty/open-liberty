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

import java.io.Serializable;

import javax.el.ELException;
import javax.el.MethodExpression;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.el.MethodNotFoundException;

/**
 * For legacy ActionSources
 * 
 * @author Jacob Hookom
 * @version $Id: LegacyMethodBinding.java 1187701 2011-10-22 12:21:54Z bommel $
 * @deprecated
 */
public final class LegacyMethodBinding extends MethodBinding implements
        Serializable
{

    private static final long serialVersionUID = 1L;

    private final MethodExpression m;

    public LegacyMethodBinding(MethodExpression m)
    {
        this.m = m;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.el.MethodBinding#getType(javax.faces.context.FacesContext)
     */
    public Class getType(FacesContext context) throws MethodNotFoundException
    {
        try
        {
            return m.getMethodInfo(context.getELContext()).getReturnType();
        }
        catch (javax.el.MethodNotFoundException e)
        {
            throw new MethodNotFoundException(e.getMessage(), e.getCause());
        }
        catch (ELException e)
        {
            throw new EvaluationException(e.getMessage(), e.getCause());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.el.MethodBinding#invoke(javax.faces.context.FacesContext,
     *      java.lang.Object[])
     */
    public Object invoke(FacesContext context, Object[] params)
            throws EvaluationException, MethodNotFoundException
    {
        try
        {
            return m.invoke(context.getELContext(), params);
        }
        catch (javax.el.MethodNotFoundException e)
        {
            throw new MethodNotFoundException(e.getMessage(), e.getCause());
        }
        catch (ELException e)
        {
            throw new EvaluationException(e.getMessage(), e.getCause());
        }
    }

    public String getExpressionString()
    {
        return m.getExpressionString();
    }
}