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
import javax.faces.flow.builder.SwitchCaseBuilder;
import org.apache.myfaces.flow.FlowImpl;
import org.apache.myfaces.flow.SwitchCaseImpl;
import org.apache.myfaces.flow.SwitchNodeImpl;
import org.apache.myfaces.view.facelets.el.ELText;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class SwitchCaseBuilderImpl extends SwitchCaseBuilder
{
    private FlowBuilderImpl _flowBuilder;
    private FlowImpl _facesFlow;
    private SwitchBuilderImpl _switchBuilderImpl;
    private SwitchNodeImpl _switchNodeImpl;
    private SwitchCaseImpl _switchCase;

    public SwitchCaseBuilderImpl(FlowBuilderImpl flowBuilder, FlowImpl facesFlow, 
        SwitchBuilderImpl switchBuilderImpl, SwitchNodeImpl switchNode)
    {
        this._flowBuilder = flowBuilder;
        this._facesFlow = facesFlow;
        this._switchBuilderImpl = switchBuilderImpl;
        this._switchNodeImpl = switchNode;
        this._switchCase = null;
    }
    
    @Override
    public SwitchCaseBuilder condition(String expression)
    {
        if (ELText.isLiteral(expression))
        {
            this._switchCase.setCondition(Boolean.valueOf(expression));
        }
        else
        {
            this._switchCase.setCondition(_flowBuilder.createValueExpression(expression));
        }
        return this;
    }

    @Override
    public SwitchCaseBuilder condition(ValueExpression expression)
    {
        this._switchCase.setCondition(expression);
        return this;
    }

    @Override
    public SwitchCaseBuilder fromOutcome(String outcome)
    {
        this._switchCase.setFromOutcome(outcome);
        return this;
    }

    @Override
    public SwitchCaseBuilder switchCase()
    {
        this._switchCase =  new SwitchCaseImpl();
        this._switchNodeImpl.addCase(this._switchCase);
        return this;
    }
}
