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
package org.apache.myfaces.el.convert;

import javax.el.ELException;
import javax.el.MethodExpression;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.el.MethodNotFoundException;

/**
 * Converts a MethodExpression to a MethodBinding. See JSF 1.2 spec section 5.8.4
 * 
 * ATTENTION: If you make changes to this class, treat javax.faces.component._MethodExpressionToMethodBinding
 * accordingly.
 * 
 * @author Stan Silvert
 * @see javax.faces.component._MethodExpressionToMethodBinding
 */
public final class MethodExpressionToMethodBinding extends MethodBinding implements StateHolder
{

    private MethodExpression methodExpression;

    private boolean isTransient = false;

    public MethodExpressionToMethodBinding()
    {
        methodExpression = null;
    }

    /** Creates a new instance of MethodExpressionToMethodBinding */
    public MethodExpressionToMethodBinding(final MethodExpression methodExpression)
    {
        this.methodExpression = methodExpression;
    }

    @Override
    public String getExpressionString()
    {
        return methodExpression.getExpressionString();
    }

    @Override
    public Class getType(FacesContext facesContext) throws MethodNotFoundException
    {

        try
        {
            return methodExpression.getMethodInfo(facesContext.getELContext()).getReturnType();
        }
        catch (javax.el.MethodNotFoundException e)
        {
            throw new javax.faces.el.MethodNotFoundException(e);
        }
    }

    @Override
    public Object invoke(final FacesContext facesContext, final Object[] params) throws EvaluationException,
                                                                                MethodNotFoundException
    {

        try
        {
            return methodExpression.invoke(facesContext.getELContext(), params);
        }
        catch (javax.el.MethodNotFoundException e)
        {
            throw new javax.faces.el.MethodNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e.getCause());
        }
    }

    // -------- StateHolder methods -------------------------------------------

    public void restoreState(final FacesContext context, final Object state)
    {
        methodExpression = (MethodExpression) state;
    }

    public Object saveState(final FacesContext context)
    {
        return methodExpression;
    }

    public void setTransient(final boolean newTransientValue)
    {
        isTransient = newTransientValue;
    }

    public boolean isTransient()
    {
        return isTransient;
    }

}
