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
package org.apache.myfaces.el;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;
import jakarta.faces.context.FacesContext;

/**
 * ELContext used for Faces.
 * 
 * @author Stan Silvert
 */
public class FacesELContext extends ELContext
{
    private ELResolver _elResolver;
    private FunctionMapper _functionMapper;
    private VariableMapper _variableMapper;

    // overwrite to optimize access and reduce created objects
    private List<EvaluationListener> listeners;

    public FacesELContext(ELResolver elResolver, FacesContext facesContext)
    {
        this._elResolver = elResolver;
        putContext(FacesContext.class, facesContext);
    }

    @Override
    public VariableMapper getVariableMapper()
    {
        return _variableMapper;
    }

    public void setVariableMapper(VariableMapper varMapper)
    {
        _variableMapper = varMapper;
    }

    @Override
    public FunctionMapper getFunctionMapper()
    {
        return _functionMapper;
    }

    public void setFunctionMapper(FunctionMapper functionMapper)
    {
        _functionMapper = functionMapper;
    }

    @Override
    public ELResolver getELResolver()
    {
        return _elResolver;
    }

    @Override
    public void addEvaluationListener(EvaluationListener listener)
    {
        if (listeners == null)
        {
            listeners = new ArrayList<>();
        }

        listeners.add(listener);
    }

    @Override
    public List<EvaluationListener> getEvaluationListeners()
    {
        return listeners == null ? Collections.emptyList() : listeners;
    }

    @Override
    public void notifyBeforeEvaluation(String expression)
    {
        if (listeners == null)
        {
            return;
        }

        for (int i = 0; i < listeners.size(); i++)
        {
            EvaluationListener listener = listeners.get(i);
            listener.beforeEvaluation(this, expression);
        }
    }

    @Override
    public void notifyAfterEvaluation(String expression)
    {
        if (listeners == null)
        {
            return;
        }

        for (int i = 0; i < listeners.size(); i++)
        {
            EvaluationListener listener = listeners.get(i);
            listener.afterEvaluation(this, expression);
        }
    }

    @Override
    public void notifyPropertyResolved(Object base, Object property)
    {
        if (listeners == null)
        {
            return;
        }

        for (int i = 0; i < listeners.size(); i++)
        {
            EvaluationListener listener = listeners.get(i);
            listener.propertyResolved(this, base, property);
        }
    }
}
