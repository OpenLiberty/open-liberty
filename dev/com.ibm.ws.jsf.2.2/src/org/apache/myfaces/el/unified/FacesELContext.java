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
package org.apache.myfaces.el.unified;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;
import javax.faces.context.FacesContext;

/**
 * ELContext used for JSF.
 * 
 * @author Stan Silvert
 */
public class FacesELContext extends ELContext
{

    private ELResolver _elResolver;
    private FunctionMapper _functionMapper;
    private VariableMapper _variableMapper;

    public FacesELContext(ELResolver elResolver, FacesContext facesContext)
    {
        this._elResolver = elResolver;
        putContext(FacesContext.class, facesContext);

        // TODO: decide if we need to implement our own FunctionMapperImpl and
        // VariableMapperImpl instead of relying on Tomcat's version.
        // this.functionMapper = new FunctionMapperImpl();
        // this.variableMapper = new VariableMapperImpl();
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

}
