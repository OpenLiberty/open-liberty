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
import javax.faces.flow.Parameter;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class ParameterImpl extends Parameter implements Freezable
{
    private String _name;
    private ValueExpression _value;

    private boolean _initialized;
    
    public ParameterImpl(String name, ValueExpression value)
    {
        this._name = name;
        this._value = value;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public ValueExpression getValue()
    {
        return _value;
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

    public void setName(String name)
    {
        checkInitialized();
        this._name = name;
    }

    public void setValue(ValueExpression value)
    {
        checkInitialized();
        this._value = value;
    }
}