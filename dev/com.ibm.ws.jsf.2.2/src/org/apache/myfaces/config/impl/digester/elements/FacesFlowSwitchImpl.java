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
import org.apache.myfaces.config.element.NavigationCase;

/**
 *
 * @author Leonardo Uribe
 */
public class FacesFlowSwitchImpl extends org.apache.myfaces.config.element.FacesFlowSwitch
{
    private String _id;
    private NavigationCase _defaultOutcome;
    private List<NavigationCase> _navigationCaseList;
    
    public FacesFlowSwitchImpl()
    {
        this._navigationCaseList = new ArrayList<NavigationCase>();
    }

    @Override
    public List<NavigationCase> getNavigationCaseList()
    {
        return _navigationCaseList;
    }
    
    public void addNavigationCase(NavigationCase navcase)
    {
        _navigationCaseList.add(navcase);
    }

    @Override
    public NavigationCase getDefaultOutcome()
    {
        return _defaultOutcome;
    }

    /**
     * @param defaultOutcome the defaultOutcome to set
     */
    public void setDefaultOutcome(NavigationCase defaultOutcome)
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

}
