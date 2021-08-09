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

package javax.faces.component;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.ValueBinding;

/**
 * Converter for legacy ValueBinding objects. See JSF 1.2 section 5.8.3
 * 
 * ATTENTION: If you make changes to this class, treat javax.faces.component.ValueExpressionToValueBinding accordingly.
 * 
 * @see javax.faces.component.ValueExpressionToValueBinding
 */
class _ValueExpressionToValueBinding extends ValueBinding implements StateHolder
{

    private ValueExpression _valueExpression;

    private boolean isTransient = false;

    // required no-arg constructor for StateHolder
    protected _ValueExpressionToValueBinding()
    {
        _valueExpression = null;
    }

    @Override
    public int hashCode()
    {
        int prime = 31;
        int result = 1;
        result = prime * result + ((_valueExpression == null) ? 0 : _valueExpression.hashCode());
        result = prime * result + (isTransient ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        _ValueExpressionToValueBinding other = (_ValueExpressionToValueBinding) obj;
        if (_valueExpression == null)
        {
            if (other._valueExpression != null)
            {
                return false;
            }
        }
        else if (!_valueExpression.equals(other._valueExpression))
        {
            return false;
        }
        if (isTransient != other.isTransient)
        {
            return false;
        }
        return true;
    }

    /**
     * @return the valueExpression
     */
    public ValueExpression getValueExpression()
    {
        return getNotNullValueExpression();
    }

    /**
     * @return the valueExpression
     */
    private ValueExpression getNotNullValueExpression()
    {
        if (_valueExpression == null)
        {
            throw new IllegalStateException("value expression is null");
        }
        return _valueExpression;
    }

    @Override
    public String getExpressionString()
    {
        return getNotNullValueExpression().getExpressionString();
    }

    /** Creates a new instance of ValueExpressionToValueBinding */
    public _ValueExpressionToValueBinding(ValueExpression valueExpression)
    {
        if (valueExpression == null)
        {
            throw new IllegalArgumentException("value expression must not be null.");
        }
        _valueExpression = valueExpression;
    }

    @Override
    public void setValue(FacesContext facesContext, Object value) throws EvaluationException, PropertyNotFoundException
    {
        try
        {
            getNotNullValueExpression().setValue(facesContext.getELContext(), value);
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

    @Override
    public boolean isReadOnly(FacesContext facesContext) throws EvaluationException, PropertyNotFoundException
    {

        try
        {
            return getNotNullValueExpression().isReadOnly(facesContext.getELContext());
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }

    }

    @Override
    public Object getValue(FacesContext facesContext) throws EvaluationException, PropertyNotFoundException
    {
        try
        {
            return getNotNullValueExpression().getValue(facesContext.getELContext());
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

    @Override
    public Class getType(FacesContext facesContext) throws EvaluationException, PropertyNotFoundException
    {
        try
        {
            return getNotNullValueExpression().getType(facesContext.getELContext());
        }
        catch (javax.el.PropertyNotFoundException e)
        {
            throw new javax.faces.el.PropertyNotFoundException(e);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

    // -------- StateHolder methods -------------------------------------------

    public void restoreState(FacesContext facesContext, Object state)
    {
        if (state != null)
        {
            if (state instanceof ValueExpression)
            {
                _valueExpression = (ValueExpression) state;
            }
            else
            {
                Object[] stateArray = (Object[]) state;
                _valueExpression = (ValueExpression) _ClassUtils.newInstance((String) stateArray[0],
                        ValueExpression.class);
                ((StateHolder) _valueExpression).restoreState(facesContext, stateArray[1]);
            }
        }
    }

    public Object saveState(FacesContext context)
    {
        if (!isTransient)
        {
            if (_valueExpression instanceof StateHolder)
            {
                Object[] state = new Object[2];
                state[0] = _valueExpression.getClass().getName();
                state[1] = ((StateHolder) _valueExpression).saveState(context);
                return state;
            }
            else
            {
                return _valueExpression;
            }
        }
        return null;
    }

    public void setTransient(boolean newTransientValue)
    {
        isTransient = newTransientValue;
    }

    public boolean isTransient()
    {
        return isTransient;
    }

}
