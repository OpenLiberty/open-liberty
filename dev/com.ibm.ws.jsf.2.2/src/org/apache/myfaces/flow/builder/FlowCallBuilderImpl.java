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
import javax.faces.flow.builder.FlowCallBuilder;
import org.apache.myfaces.flow.FlowCallNodeImpl;
import org.apache.myfaces.flow.FlowImpl;
import org.apache.myfaces.flow.ParameterImpl;
import org.apache.myfaces.view.facelets.el.ELText;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class FlowCallBuilderImpl extends FlowCallBuilder
{
    private FlowBuilderImpl _flowBuilder;
    private FlowImpl _facesFlow;
    private FlowCallNodeImpl _flowCallNode;

    public FlowCallBuilderImpl(FlowBuilderImpl flowBuilder, FlowImpl facesFlow, String flowCallNodeId)
    {
        this._flowBuilder = flowBuilder;
        this._facesFlow = facesFlow;
        this._flowCallNode = new FlowCallNodeImpl(flowCallNodeId);
        this._facesFlow.putFlowCall(flowCallNodeId, _flowCallNode);
    }

    @Override
    public FlowCallBuilder flowReference(String flowDocumentId, String flowId)
    {
        if (ELText.isLiteral(flowDocumentId))
        {
            this._flowCallNode.setCalledFlowDocumentId(flowDocumentId);
        }
        else
        {
            this._flowCallNode.setCalledFlowDocumentId(_flowBuilder.createValueExpression(flowDocumentId));
        }
        if (ELText.isLiteral(flowId))
        {
            this._flowCallNode.setCalledFlowId(flowId);
        }
        else
        {
            this._flowCallNode.setCalledFlowId(_flowBuilder.createValueExpression(flowId));
        }
        return this;
    }

    @Override
    public FlowCallBuilder outboundParameter(String name, ValueExpression value)
    {
        this._flowCallNode.putOutboundParameter(name, new ParameterImpl(name, value));
        return this;
    }

    @Override
    public FlowCallBuilder outboundParameter(String name, String value)
    {
        this._flowCallNode.putOutboundParameter(name, new ParameterImpl(name, 
            this._flowBuilder.createValueExpression(value)));
        return this;
    }

    @Override
    public FlowCallBuilder markAsStartNode()
    {
        _facesFlow.setStartNodeId(_flowCallNode.getId());
        return this;
    }
    
}
