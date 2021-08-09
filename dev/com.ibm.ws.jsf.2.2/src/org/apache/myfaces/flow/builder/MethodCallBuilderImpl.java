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

import java.util.List;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.flow.Parameter;
import javax.faces.flow.builder.MethodCallBuilder;
import org.apache.myfaces.flow.FlowImpl;
import org.apache.myfaces.flow.MethodCallNodeImpl;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class MethodCallBuilderImpl extends MethodCallBuilder
{
    private FlowImpl _facesFlow;
    private MethodCallNodeImpl _methodCallNode;
    private FlowBuilderImpl _flowBuilder;

    public MethodCallBuilderImpl(FlowBuilderImpl flowBuilder, FlowImpl facesFlow, String methodCallNodeId)
    {
        this._flowBuilder = flowBuilder;
        this._facesFlow = facesFlow;
        this._methodCallNode = new MethodCallNodeImpl(methodCallNodeId);
        this._facesFlow.addMethodCall(_methodCallNode);
    }

    @Override
    public MethodCallBuilder expression(MethodExpression me)
    {
        this._methodCallNode.setMethodExpression(me);
        return this;
    }

    @Override
    public MethodCallBuilder expression(String methodExpression)
    {
        this._methodCallNode.setMethodExpression(
            _flowBuilder.createMethodExpression(methodExpression));
        return this;
    }

    @Override
    public MethodCallBuilder expression(String methodExpression, Class[] paramTypes)
    {
        this._methodCallNode.setMethodExpression(
            _flowBuilder.createMethodExpression(methodExpression, paramTypes));
        return this;
    }

    @Override
    public MethodCallBuilder parameters(List<Parameter> parameters)
    {
        for (Parameter p : parameters)
        {
            this._methodCallNode.addParameter(p);
        }
        return this;
    }

    @Override
    public MethodCallBuilder defaultOutcome(String outcome)
    {
        this._methodCallNode.setOutcome(
            this._flowBuilder.createValueExpression(outcome));
        return this;
    }

    @Override
    public MethodCallBuilder defaultOutcome(ValueExpression outcome)
    {
        this._methodCallNode.setOutcome(outcome);
        return this;
    }

    @Override
    public MethodCallBuilder markAsStartNode()
    {
        _facesFlow.setStartNodeId(_methodCallNode.getId());
        return this;
    }
    
}
