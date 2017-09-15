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
import org.apache.myfaces.config.element.FacesFlowMethodCall;
import org.apache.myfaces.config.element.FacesFlowParameter;
import org.apache.myfaces.config.element.FacesFlowReturn;
import org.apache.myfaces.config.element.FacesFlowSwitch;
import org.apache.myfaces.config.element.FacesFlowView;
import org.apache.myfaces.config.element.NavigationRule;

/**
 *
 * @author Leonardo Uribe
 */
public class FacesFlowDefinitionImpl extends org.apache.myfaces.config.element.FacesFlowDefinition
{
    private String _definingDocumentId;
    private String _id;
    private String _startNode;
    private String _initializer;
    private String _finalizer;
    
    private List<FacesFlowView> _viewList;
    private List<FacesFlowSwitch> _switchList;
    private List<FacesFlowReturn> _returnList;
    private List<NavigationRule> _navigationRuleList;
    private List<FacesFlowCall> _flowCallList;
    private List<FacesFlowMethodCall> _methodCallList;
    private List<FacesFlowParameter> _inboundParameterList;

    public FacesFlowDefinitionImpl()
    {
        _viewList = new ArrayList<FacesFlowView>();
        _switchList = new ArrayList<FacesFlowSwitch>();
        _returnList = new ArrayList<FacesFlowReturn>();
        _navigationRuleList = new ArrayList<NavigationRule>();
        _flowCallList = new ArrayList<FacesFlowCall>();
        _methodCallList = new ArrayList<FacesFlowMethodCall>();
        _inboundParameterList = new ArrayList<FacesFlowParameter>();
    }
    
    @Override
    public String getStartNode()
    {
        return _startNode;
    }

    @Override
    public List<FacesFlowView> getViewList()
    {
        return _viewList;
    }
    
    public void addView(FacesFlowView view)
    {
        _viewList.add(view);
    }

    @Override
    public List<FacesFlowSwitch> getSwitchList()
    {
        return _switchList;
    }
    
    public void addSwitch(FacesFlowSwitch switchItem)
    {
        _switchList.add(switchItem);
    }

    @Override
    public List<FacesFlowReturn> getReturnList()
    {
        return _returnList;
    }

    public void addReturn(FacesFlowReturn value)
    {
        _returnList.add(value);
    }
    
    @Override
    public List<NavigationRule> getNavigationRuleList()
    {
        return _navigationRuleList;
    }

    public void addNavigationRule(NavigationRule value)
    {
        _navigationRuleList.add(value);
    }
    
    @Override
    public List<FacesFlowCall> getFlowCallList()
    {
        return _flowCallList;
    }

    public void addFlowCall(FacesFlowCall value)
    {
        _flowCallList.add(value);
    }

    @Override
    public List<FacesFlowMethodCall> getMethodCallList()
    {
        return _methodCallList;
    }

    public void addMethodCall(FacesFlowMethodCall value)
    {
        _methodCallList.add(value);
    }
    
    @Override
    public String getInitializer()
    {
        return _initializer;
    }

    @Override
    public String getFinalizer()
    {
        return _finalizer;
    }

    @Override
    public List<FacesFlowParameter> getInboundParameterList()
    {
        return _inboundParameterList;
    }

    public void addInboundParameter(FacesFlowParameter value)
    {
        _inboundParameterList.add(value);
    }
    
    /**
     * @param startNode the startNode to set
     */
    public void setStartNode(String startNode)
    {
        this._startNode = startNode;
    }

    /**
     * @param initializer the initializer to set
     */
    public void setInitializer(String initializer)
    {
        this._initializer = initializer;
    }

    /**
     * @param finalizer the finalizer to set
     */
    public void setFinalizer(String finalizer)
    {
        this._finalizer = finalizer;
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

    /**
     * @return the _definingDocumentId
     */
    public String getDefiningDocumentId()
    {
        return _definingDocumentId;
    }

    /**
     * @param definingDocumentId the _definingDocumentId to set
     */
    public void setDefiningDocumentId(String definingDocumentId)
    {
        this._definingDocumentId = definingDocumentId;
    }
}
