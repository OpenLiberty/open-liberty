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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.flow.FlowCallNode;
import javax.faces.flow.Parameter;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class FlowCallNodeImpl extends FlowCallNode implements Freezable
{
    private String _id;
    private String _calledFlowId;
    private ValueExpression _calledFlowIdEL;
    private String _calledFlowDocumentId;
    private ValueExpression _calledFlowDocumentIdEL;
    
    private Map<String, Parameter> _outboundParametersMap;
    private Map<String, Parameter> _unmodifiableOutboundParametersMap;
    
    private boolean _initialized;

    public FlowCallNodeImpl(String id)
    {
        this._id = id;
        _outboundParametersMap = new HashMap<String, Parameter>();
        _unmodifiableOutboundParametersMap = Collections.unmodifiableMap(_outboundParametersMap);
    }
    
    @Override
    public Map<String, Parameter> getOutboundParameters()
    {
        return _unmodifiableOutboundParametersMap;
    }
    
    public void putOutboundParameter(String key, Parameter value)
    {
        checkInitialized();
        _outboundParametersMap.put(key, value);
    }

    @Override
    public String getCalledFlowDocumentId(FacesContext context)
    {
        if (_calledFlowDocumentIdEL != null)
        {
            return (String) _calledFlowDocumentIdEL.getValue(context.getELContext());
        }
        return _calledFlowDocumentId;
    }

    @Override
    public String getCalledFlowId(FacesContext context)
    {
        if (_calledFlowIdEL != null)
        {
            return (String) _calledFlowIdEL.getValue(context.getELContext());
        }
        return _calledFlowId;
    }

    @Override
    public String getId()
    {
        return _id;
    }

    public void setId(String id)
    {
        checkInitialized();
        this._id = id;
    }

    public void setCalledFlowId(String calledFlowId)
    {
        checkInitialized();
        this._calledFlowId = calledFlowId;
        this._calledFlowIdEL = null;
    }

    public void setCalledFlowDocumentId(String calledFlowDocumentId)
    {
        checkInitialized();
        this._calledFlowDocumentId = calledFlowDocumentId;
        this._calledFlowDocumentIdEL = null;
    }
    
    public void freeze()
    {
        _initialized = true;
        
        for (Map.Entry<String, Parameter> entry : _outboundParametersMap.entrySet())
        {
            if (entry.getValue() instanceof Freezable)
            {
                ((Freezable)entry.getValue()).freeze();
            }
        }
    }
    
    private void checkInitialized() throws IllegalStateException
    {
        if (_initialized)
        {
            throw new IllegalStateException("Flow is inmutable once initialized");
        }
    }

    /**
     * @param calledFlowIdEL the _calledFlowIdEL to set
     */
    public void setCalledFlowId(ValueExpression calledFlowIdEL)
    {
        this._calledFlowIdEL = calledFlowIdEL;
        this._calledFlowId = null;
    }

    /**
     * @param calledFlowDocumentIdEL the _calledFlowDocumentIdEL to set
     */
    public void setCalledFlowDocumentId(ValueExpression calledFlowDocumentIdEL)
    {
        this._calledFlowDocumentIdEL = calledFlowDocumentIdEL;
        this._calledFlowDocumentId = null;
    }
}
