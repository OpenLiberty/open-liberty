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
package org.apache.myfaces.flow.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.el.ValueExpression;
import javax.faces.flow.builder.NavigationCaseBuilder;
import org.apache.myfaces.flow.FlowImpl;
import org.apache.myfaces.flow.NavigationCaseImpl;

/**
 *
 * @author lu4242
 */
public class NavigationCaseBuilderImpl extends NavigationCaseBuilder
{
    private FlowImpl _facesFlow;
    private FlowBuilderImpl _flowBuilder;
    private NavigationCaseImpl _navigationCaseImpl;
    
    public NavigationCaseBuilderImpl(FlowBuilderImpl flowBuilder, FlowImpl facesFlow)
    {
        this._flowBuilder = flowBuilder;
        this._facesFlow = facesFlow;
        this._navigationCaseImpl = new NavigationCaseImpl();
    }
    
    @Override
    public NavigationCaseBuilder fromViewId(String fromViewId)
    {
        // This is the best place to add the navigation case into the flow, because
        // fromViewId is required (cannot be null, and null does not mean '*')
        if (this._navigationCaseImpl.getFromViewId() != null)
        {
            this._facesFlow.removeNavigationCase(_navigationCaseImpl);
        }
        if (fromViewId != null)
        {
            this._navigationCaseImpl.setFromViewId(fromViewId);
            this._facesFlow.addNavigationCase(_navigationCaseImpl);
        }
        return this;
    }

    @Override
    public NavigationCaseBuilder fromAction(String fromAction)
    {
        this._navigationCaseImpl.setFromAction(fromAction);
        return this;
    }

    @Override
    public NavigationCaseBuilder fromOutcome(String fromOutcome)
    {
        this._navigationCaseImpl.setFromOutcome(fromOutcome);
        return this;
    }

    @Override
    public NavigationCaseBuilder toViewId(String toViewId)
    {
        this._navigationCaseImpl.setToViewId(toViewId);
        return this;
    }

    @Override
    public NavigationCaseBuilder toFlowDocumentId(String toFlowDocumentId) 
    {
        this._navigationCaseImpl.setToFlowDocumentId(toFlowDocumentId);
        return this;
    }

    @Override
    public NavigationCaseBuilder condition(String condition)
    {
        this._navigationCaseImpl.setConditionExpression(null);
        this._navigationCaseImpl.setCondition(condition);
        return this;
    }

    @Override
    public NavigationCaseBuilder condition(ValueExpression condition)
    {
        this._navigationCaseImpl.setCondition(null);
        this._navigationCaseImpl.setConditionExpression(condition);
        return this;
    }

    @Override
    public RedirectBuilder redirect()
    {
        this._navigationCaseImpl.setRedirect(true);
        return new RedirectBuilderImpl();
    }
    
    public class RedirectBuilderImpl extends RedirectBuilder
    {

        @Override
        public RedirectBuilder parameter(String name, String value)
        {
            //_navigationCaseImpl.
            Map<String, List<String>> map = _navigationCaseImpl.getParameters();
            if (map == null)
            {
                map = new HashMap<String, List<String>>();
                _navigationCaseImpl.setParameters(map);
            }
            List<String> values = map.get(name);
            if (values == null)
            {
                values = new ArrayList<String>();
                map.put(name, values);
            }
            values.add(value);
            return this;
        }

        @Override
        public RedirectBuilder includeViewParams()
        {
            _navigationCaseImpl.setIncludeViewParams(true);
            return this;
        }
        
    }
}
