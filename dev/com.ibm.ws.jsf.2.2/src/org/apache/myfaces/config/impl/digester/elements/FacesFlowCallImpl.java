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
import org.apache.myfaces.config.element.FacesFlowCall;
import org.apache.myfaces.config.element.FacesFlowParameter;
import org.apache.myfaces.config.element.FacesFlowReference;

/**
 *
 * @author Leonardo Uribe
 */
public class FacesFlowCallImpl extends FacesFlowCall
{
    private FacesFlowReference _flowReference;
    private List<FacesFlowParameter> _outboundParameterList;
    private String _id;

    public FacesFlowCallImpl()
    {
        _outboundParameterList = new ArrayList<FacesFlowParameter>();
    }

    @Override
    public List<FacesFlowParameter> getOutboundParameterList()
    {
        return _outboundParameterList;
    }
    
    public void addOutboundParameter(FacesFlowParameter parameter)
    {
        _outboundParameterList.add(parameter);
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
    public FacesFlowReference getFlowReference()
    {
        return _flowReference;
    }

    public void setFlowReference(FacesFlowReference flowReference)
    {
        this._flowReference = flowReference;
    }

}
