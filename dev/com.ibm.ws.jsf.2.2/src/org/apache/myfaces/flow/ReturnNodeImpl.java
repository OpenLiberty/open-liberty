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

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.flow.ReturnNode;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class ReturnNodeImpl extends ReturnNode implements Freezable
{
    private String _fromOutcome;
    private ValueExpression _fromOutcomeEL;
    private String _id;

    private boolean _initialized;

    public ReturnNodeImpl(String returnNodeId)
    {
        this._id = returnNodeId;
    }
    
    @Override
    public String getFromOutcome(FacesContext context)
    {
        if (_fromOutcomeEL != null)
        {
            return (String) _fromOutcomeEL.getValue(context.getELContext());
        }
        return _fromOutcome;
    }

    @Override
    public String getId()
    {
        return _id;
    }
    
    public void freeze()
    {
        _initialized = true;
    }
    
    private void checkInitialized() throws IllegalStateException
    {
        if (_initialized)
        {
            throw new IllegalStateException("Flow is inmutable once initialized");
        }
    }

    public void setFromOutcome(String fromOutcome)
    {
        checkInitialized();
        this._fromOutcome = fromOutcome;
        this._fromOutcomeEL = null;
    }
    
    public void setFromOutcome(ValueExpression fromOutcome)
    {
        checkInitialized();
        this._fromOutcomeEL = fromOutcome;
        this._fromOutcome = null;
    }

    public void setId(String id)
    {
        checkInitialized();
        this._id = id;
    }
}
