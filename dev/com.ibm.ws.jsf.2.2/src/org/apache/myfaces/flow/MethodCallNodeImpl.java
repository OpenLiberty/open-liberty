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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.flow.MethodCallNode;
import javax.faces.flow.Parameter;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class MethodCallNodeImpl extends MethodCallNode implements Freezable
{
    private String _id;
    private MethodExpression _methodExpression;
    private ValueExpression _outcome;
    
    private List<Parameter> _parameters;
    private List<Parameter> _unmodifiableParameters;
    
    private boolean _initialized;
    
    public MethodCallNodeImpl(String methodCallNodeId)
    {
        this._id = methodCallNodeId;
        _parameters = new ArrayList<Parameter>();
        _unmodifiableParameters = Collections.unmodifiableList(_parameters);
    }

    @Override
    public MethodExpression getMethodExpression()
    {
        return _methodExpression;
    }

    @Override
    public ValueExpression getOutcome()
    {
        return _outcome;
    }

    @Override
    public List<Parameter> getParameters()
    {
        return _unmodifiableParameters;
    }
    
    public void addParameter(Parameter parameter)
    {
        checkInitialized();
        _parameters.add(parameter);
    }

    @Override
    public String getId()
    {
        return _id;
    }
    
    public void freeze()
    {
        _initialized = true;
        
        for (Parameter value : _parameters)
        {
            if (value instanceof Freezable)
            {
                ((Freezable)value).freeze();
            }
        }
    }
    
    private void checkInitialized() throws IllegalStateException
    {
        if (_initialized)
        {
            throw new IllegalStateException("Flow is inmutable once initialized");
        }
    }

    public void setMethodExpression(MethodExpression methodExpression)
    {
        checkInitialized();
        this._methodExpression = methodExpression;
    }

    public void setOutcome(ValueExpression outcome)
    {
        checkInitialized();
        this._outcome = outcome;
    }

    public void setId(String id)
    {
        checkInitialized();
        this._id = id;
    }
}
