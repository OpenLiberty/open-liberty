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
import java.util.List;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.flow.SwitchCase;
import javax.faces.flow.SwitchNode;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class SwitchNodeImpl extends SwitchNode implements Freezable
{
    private String _defaultOutcome;
    private ValueExpression _defaultOutcomeEL;
    private String _id;
    
    private List<SwitchCase> _cases;
    private List<SwitchCase> _unmodifiableCases;

    private boolean _initialized;

    public SwitchNodeImpl(String switchNodeId)
    {
        this._id = switchNodeId;
        _cases = new ArrayList<SwitchCase>();
        _unmodifiableCases = Collections.unmodifiableList(_cases);
    }
    
    @Override
    public List<SwitchCase> getCases()
    {
        return _unmodifiableCases;
    }
    
    public void addCase(SwitchCase switchCase)
    {
        checkInitialized();
        _cases.add(switchCase);
    }

    @Override
    public String getDefaultOutcome(FacesContext context)
    {
        if (_defaultOutcomeEL != null)
        {
            return (String) _defaultOutcomeEL.getValue(context.getELContext());
        }
        return _defaultOutcome;
    }

    @Override
    public String getId()
    {
        return _id;
    }

    public void freeze()
    {
        _initialized = true;
        
        for (SwitchCase switchCase : _cases)
        {
            if (switchCase instanceof Freezable)
            {
                ((Freezable)switchCase).freeze();
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
    
    public void setDefaultOutcome(String defaultOutcome)
    {
        checkInitialized();
        this._defaultOutcome = defaultOutcome;
        this._defaultOutcomeEL = null;
    }
    
    public void setDefaultOutcome(ValueExpression defaultOutcome)
    {
        checkInitialized();
        this._defaultOutcomeEL = defaultOutcome;
        this._defaultOutcome = null;
    }

    public void setId(String id)
    {
        checkInitialized();
        this._id = id;
    }
}
