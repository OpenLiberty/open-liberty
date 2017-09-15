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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.el.MethodExpression;
import javax.faces.application.NavigationCase;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import javax.faces.flow.FlowCallNode;
import javax.faces.flow.FlowNode;
import javax.faces.flow.MethodCallNode;
import javax.faces.flow.Parameter;
import javax.faces.flow.ReturnNode;
import javax.faces.flow.SwitchNode;
import javax.faces.flow.ViewNode;
import javax.faces.lifecycle.ClientWindow;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class FlowImpl extends Flow implements Freezable
{
    private MethodExpression _initializer;
    private MethodExpression _finalizer;
    private String _startNodeId;
    private String _id;
    private String _definingDocumentId;
    
    private Map<String, FlowNode> _flowNodeMap;
    
    // The idea is use a normal HashMap, since there will not be modifications
    // after initialization ( all setters must call checkInitialized() )
    private Map<String, Parameter> _inboundParametersMap;
    private Map<String, FlowCallNode> _flowCallsMap;
    private List<MethodCallNode> _methodCallsList;
    private Map<String, ReturnNode> _returnsMap;
    private Map<String, SwitchNode> _switchesMap;
    private List<ViewNode> _viewsList;
    
    // Note this class should be thread safe and inmutable once
    // the flow is initialized or placed into service by the runtime.
    private Map<String, Parameter> _unmodifiableInboundParametersMap;
    private Map<String, FlowCallNode> _unmodifiableFlowCallsMap;
    private List<MethodCallNode> _unmodifiableMethodCallsList;
    private Map<String, ReturnNode> _unmodifiableReturnsMap;
    private Map<String, SwitchNode> _unmodifiableSwitchesMap;
    private List<ViewNode> _unmodifiableViewsList;
    
    private Map<String, Set<NavigationCase>> _navigationCases;
    private Map<String, Set<NavigationCase>> _unmodifiableNavigationCases;
    
    // No need to make it volatile, because FlowImpl instances are
    // created and initialized only at application startup, by a single
    // thread.
    private boolean _initialized;
    
    public FlowImpl()
    {
        _flowNodeMap = new HashMap<String, FlowNode>();
        _inboundParametersMap = new HashMap<String, Parameter>();
        _flowCallsMap = new HashMap<String, FlowCallNode>();
        _methodCallsList = new ArrayList<MethodCallNode>();
        _returnsMap = new HashMap<String, ReturnNode>();
        _switchesMap = new HashMap<String, SwitchNode>();
        _viewsList = new ArrayList<ViewNode>();
        _navigationCases = new HashMap<String, Set<NavigationCase>>();
        
        // Collections.unmodifiableMap(...) uses delegation pattern, so as long
        // as we don't modify _inboundParametersMap in the wrong time, it
        // will be thread safe and inmutable.
        _unmodifiableInboundParametersMap = Collections.unmodifiableMap(_inboundParametersMap);
        _unmodifiableFlowCallsMap = Collections.unmodifiableMap(_flowCallsMap);
        _unmodifiableMethodCallsList = Collections.unmodifiableList(_methodCallsList);
        _unmodifiableReturnsMap = Collections.unmodifiableMap(_returnsMap);
        _unmodifiableSwitchesMap = Collections.unmodifiableMap(_switchesMap);
        _unmodifiableViewsList = Collections.unmodifiableList(_viewsList);
        
        _unmodifiableNavigationCases = Collections.unmodifiableMap(_navigationCases);
    }
    
    public void freeze()
    {
        
        
        _initialized = true;
        
        for (Map.Entry<String, Parameter> entry : _inboundParametersMap.entrySet())
        {
            if (entry.getValue() instanceof Freezable)
            {
                ((Freezable)entry.getValue()).freeze();
            }
        }
            
        for (Map.Entry<String, FlowCallNode> entry : _flowCallsMap.entrySet())
        {
            if (entry.getValue() instanceof Freezable)
            {
                ((Freezable)entry.getValue()).freeze();
            }
        }

        for (MethodCallNode value : _methodCallsList)
        {
            if (value instanceof Freezable)
            {
                ((Freezable)value).freeze();
            }
        }

        for (Map.Entry<String, ReturnNode> entry : _returnsMap.entrySet())
        {
            if (entry.getValue() instanceof Freezable)
            {
                ((Freezable)entry.getValue()).freeze();
            }
        }

        for (Map.Entry<String, SwitchNode> entry : _switchesMap.entrySet())
        {
            if (entry.getValue() instanceof Freezable)
            {
                ((Freezable)entry.getValue()).freeze();
            }
        }
        
        for (ViewNode value : _viewsList)
        {
            if (value instanceof Freezable)
            {
                ((Freezable)value).freeze();
            }
        }
    }

    @Override
    public String getClientWindowFlowId(ClientWindow curWindow)
    {
        String id = getId();
        String documentId = getDefiningDocumentId();
        // Faces Flow relies on ClientWindow feature, so it should be enabled,
        // and the expected id cannot be null.
        String windowId = curWindow.getId();
        StringBuilder sb = new StringBuilder( id.length() + 1 + windowId.length() );
        sb.append(windowId).append('_').append(documentId).append('_').append(id);
        return sb.toString();
    }

    @Override
    public String getDefiningDocumentId()
    {
        return _definingDocumentId;
    }
    
    public void setDefiningDocumentId(String definingDocumentId)
    {
        checkInitialized();
        _definingDocumentId = definingDocumentId;
    }

    @Override
    public String getId()
    {
        return _id;
    }
    
    public void setId(String id)
    {
        checkInitialized();
        _id = id;
    }

    @Override
    public MethodExpression getInitializer()
    {
        return _initializer;
    }
    
    public void setInitializer(MethodExpression initializer)
    {
        checkInitialized();
        _initializer = initializer;
    }

    @Override
    public MethodExpression getFinalizer()
    {
        return _finalizer;
    }
    
    public void setFinalizer(MethodExpression finalizer)
    {
        checkInitialized();
        _finalizer = finalizer;
    }

    @Override
    public String getStartNodeId()
    {
        return _startNodeId;
    }
    
    public void setStartNodeId(String startNodeId)
    {
        checkInitialized();
        _startNodeId = startNodeId;
    }
    
    @Override
    public Map<String, Parameter> getInboundParameters()
    {
        return _unmodifiableInboundParametersMap;
    }
    
    public void putInboundParameter(String key, Parameter value)
    {
        checkInitialized();
        _inboundParametersMap.put(key, value);
    }
    
    @Override
    public Map<String, FlowCallNode> getFlowCalls()
    {
        return _unmodifiableFlowCallsMap;
    }
    
    public void putFlowCall(String key, FlowCallNode value)
    {
        checkInitialized();
        _flowCallsMap.put(key, value);
        _flowNodeMap.put(value.getId(), value);
    }

    @Override
    public List<MethodCallNode> getMethodCalls()
    {
        return _unmodifiableMethodCallsList;
    }

    public void addMethodCall(MethodCallNode value)
    {
        checkInitialized();
        _methodCallsList.add(value);
        _flowNodeMap.put(value.getId(), value);
    }

    @Override
    public Map<String, ReturnNode> getReturns()
    {
        return _unmodifiableReturnsMap;
    }
    
    public void putReturn(String key, ReturnNode value)
    {
        checkInitialized();
        _returnsMap.put(key, value);
        _flowNodeMap.put(value.getId(), value);
    }

    @Override
    public Map<String, SwitchNode> getSwitches()
    {
        return _unmodifiableSwitchesMap;
    }
    
    public void putSwitch(String key, SwitchNode value)
    {
        checkInitialized();
        _switchesMap.put(key, value);
        _flowNodeMap.put(value.getId(), value);
    }

    @Override
    public List<ViewNode> getViews()
    {
        return _unmodifiableViewsList;
    }
    
    public void addView(ViewNode value)
    {
        checkInitialized();
        _viewsList.add(value);
        _flowNodeMap.put(value.getId(), value);
    }

    @Override
    public FlowCallNode getFlowCall(Flow targetFlow)
    {
        FacesContext facesContext = null;
        for (Map.Entry<String, FlowCallNode> entry : _flowCallsMap.entrySet())
        {
            if (facesContext == null)
            {
                facesContext = FacesContext.getCurrentInstance();
            }
            String calledDocumentId = entry.getValue().getCalledFlowDocumentId(facesContext);
            String calledFlowId = entry.getValue().getCalledFlowId(facesContext);
            if (targetFlow.getDefiningDocumentId().equals(calledDocumentId) &&
                targetFlow.getId().equals(calledFlowId) )
            {
                return entry.getValue();
            }
        }
        return null;
    }
    
    @Override
    public FlowNode getNode(String nodeId)
    {
        return _flowNodeMap.get(nodeId);
    }
    
    public void addNavigationCases(String fromViewId, Set<NavigationCase> navigationCases)
    {
        checkInitialized();
        Set<NavigationCase> navigationCaseSet = _navigationCases.get(fromViewId);
        if (navigationCaseSet == null)
        {
            navigationCaseSet = new HashSet<NavigationCase>();
            _navigationCases.put(fromViewId, navigationCaseSet);
        }
        navigationCaseSet.addAll(navigationCases);
    }
    
    public void addNavigationCase(NavigationCase navigationCase)
    {
        checkInitialized();
        Set<NavigationCase> navigationCaseSet = _navigationCases.get(navigationCase.getFromViewId());
        if (navigationCaseSet == null)
        {
            navigationCaseSet = new HashSet<NavigationCase>();
            _navigationCases.put(navigationCase.getFromViewId(), navigationCaseSet);
        }
        navigationCaseSet.add(navigationCase);
    }
    
    public void removeNavigationCase(NavigationCase navigationCase)
    {
        checkInitialized();
        Set<NavigationCase> navigationCaseSet = _navigationCases.get(navigationCase.getFromViewId());
        if (navigationCaseSet == null)
        {
            return;
        }
        navigationCaseSet.remove(navigationCase);
    }

    private void checkInitialized() throws IllegalStateException
    {
        if (_initialized)
        {
            throw new IllegalStateException("Flow is inmutable once initialized");
        }
    }

    @Override
    public Map<String, Set<NavigationCase>> getNavigationCases()
    {
        return _unmodifiableNavigationCases;
    }
    
}
