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
package org.apache.myfaces.flow;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.flow.SwitchCase;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class SwitchCaseImpl extends SwitchCase implements Freezable
{
    private String _fromOutcome;
    
    private Boolean _condition;
    private ValueExpression _conditionEL;

    private boolean _initialized;

    @Override
    public String getFromOutcome()
    {
        return _fromOutcome;
    }

    @Override
    public Boolean getCondition(FacesContext context)
    {
        if (_conditionEL != null)
        {
            Object value = _conditionEL.getValue(context.getELContext());
            if (value instanceof String)
            {
                return Boolean.valueOf((String) value);
            }
            return (Boolean) value;
        }
        return _condition;
    }
    
    public void freeze()
    {
        _initialized = true;
    }
    
    private void checkInitialized() throws IllegalStateException
    {
        if (_initialized)
        {
            throw new IllegalStateException("Flow is inmutable once initialized");
        }
    }

    public void setFromOutcome(String fromOutcome)
    {
        checkInitialized();
        this._fromOutcome = fromOutcome;
    }

    public void setCondition(Boolean condition)
    {
        checkInitialized();
        this._condition = condition;
        this._conditionEL = null;
    }

    public void setCondition(ValueExpression conditionEL)
    {
        checkInitialized();
        this._conditionEL = conditionEL;
        this._condition = null;
    }
}
