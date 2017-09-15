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

import javax.el.ValueExpression;
import javax.faces.flow.builder.SwitchBuilder;
import javax.faces.flow.builder.SwitchCaseBuilder;
import org.apache.myfaces.flow.FlowImpl;
import org.apache.myfaces.flow.SwitchNodeImpl;
import org.apache.myfaces.view.facelets.el.ELText;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class SwitchBuilderImpl extends SwitchBuilder
{
    private FlowBuilderImpl _flowBuilder;
    private FlowImpl _facesFlow;
    private SwitchNodeImpl _switchNodeImpl;
    private SwitchCaseBuilderImpl _lastSwitchCaseBuilderImpl;

    public SwitchBuilderImpl(FlowBuilderImpl flowBuilder, FlowImpl facesFlow, String switchNodeId)
    {
        this._flowBuilder = flowBuilder;
        this._facesFlow = facesFlow;
        this._switchNodeImpl = new SwitchNodeImpl(switchNodeId);
        this._facesFlow.putSwitch(switchNodeId, _switchNodeImpl);
        this._lastSwitchCaseBuilderImpl = new SwitchCaseBuilderImpl(
            this._flowBuilder, this._facesFlow, this, this._switchNodeImpl);
    }

    @Override
    public SwitchCaseBuilder switchCase()
    {
        return this._lastSwitchCaseBuilderImpl.switchCase();
    }

    @Override
    public SwitchCaseBuilder defaultOutcome(String outcome)
    {
        if (ELText.isLiteral(outcome))
        {
            this._switchNodeImpl.setDefaultOutcome(outcome);
        }
        else
        {
            this._switchNodeImpl.setDefaultOutcome(_flowBuilder.createValueExpression(outcome));
        }
        return _lastSwitchCaseBuilderImpl;
    }

    @Override
    public SwitchCaseBuilder defaultOutcome(ValueExpression outcome)
    {
        this._switchNodeImpl.setDefaultOutcome(outcome);
        return _lastSwitchCaseBuilderImpl;
    }

    @Override
    public SwitchBuilder markAsStartNode()
    {
        _facesFlow.setStartNodeId(_switchNodeImpl.getId());
        return this;
    }    
}
