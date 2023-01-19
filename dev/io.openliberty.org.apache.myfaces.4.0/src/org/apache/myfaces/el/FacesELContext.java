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

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
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
    private ELResolver elResolver;
    private FunctionMapper functionMapper;
    private VariableMapper variableMapper;

    public FacesELContext(ELResolver elResolver, FacesContext facesContext)
    {
        this.elResolver = elResolver;
        putContext(FacesContext.class, facesContext);
    }

    @Override
    public VariableMapper getVariableMapper()
    {
        return variableMapper;
    }

    public void setVariableMapper(VariableMapper variableMapper)
    {
        this.variableMapper = variableMapper;
    }

    @Override
    public FunctionMapper getFunctionMapper()
    {
        return functionMapper;
    }

    public void setFunctionMapper(FunctionMapper functionMapper)
    {
        this.functionMapper = functionMapper;
    }

    @Override
    public ELResolver getELResolver()
    {
        return elResolver;
    }
}
