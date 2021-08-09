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
package org.apache.myfaces.config.impl.digester.elements;

import java.util.ArrayList;
import java.util.List;
import org.apache.myfaces.config.element.FacesFlowMethodParameter;

/**
 *
 * @author Leonardo Uribe
 */
public class FacesFlowMethodCallImpl extends org.apache.myfaces.config.element.FacesFlowMethodCall
{
    private String _id;
    private String _method;
    private String _defaultOutcome;
    
    private List<FacesFlowMethodParameter> _parameterList;
    
    public FacesFlowMethodCallImpl()
    {
        _parameterList = new ArrayList<FacesFlowMethodParameter>();
    }

    @Override
    public String getMethod()
    {
        return _method;
    }

    @Override
    public String getDefaultOutcome()
    {
        return _defaultOutcome;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(String method)
    {
        this._method = method;
    }

    /**
     * @param defaultOutcome the defaultOutcome to set
     */
    public void setDefaultOutcome(String defaultOutcome)
    {
        this._defaultOutcome = defaultOutcome;
    }
    
    @Override
    public String getId()
    {
        return _id;
    }

    public void setId(String id)
    {
        this._id = id;
    }

    @Override
    public List<FacesFlowMethodParameter> getParameterList()
    {
        return _parameterList;
    }
    
    public void addParameter(FacesFlowMethodParameter parameter)
    {
        _parameterList.add(parameter);
    }
}
