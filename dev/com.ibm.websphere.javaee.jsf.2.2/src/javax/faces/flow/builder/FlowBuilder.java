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
package javax.faces.flow.builder;

import javax.faces.flow.Flow;

/**
 *
 * @since 2.2
 */
public abstract class FlowBuilder
{

    public abstract FlowBuilder id(String definingDocumentId,
        String id);

    public abstract ViewBuilder viewNode(String viewNodeId,
        String vdlDocumentId);

    public abstract SwitchBuilder switchNode(String switchNodeId);

    public abstract ReturnBuilder returnNode(String returnNodeId);

    public abstract MethodCallBuilder methodCallNode(String methodCallNodeId);

    public abstract FlowCallBuilder flowCallNode(String flowCallNodeId);

    public abstract FlowBuilder initializer(javax.el.MethodExpression methodExpression);

    public abstract FlowBuilder initializer(String methodExpression);

    public abstract FlowBuilder finalizer(javax.el.MethodExpression methodExpression);

    public abstract FlowBuilder finalizer(String methodExpression);

    public abstract FlowBuilder inboundParameter(String name,
        javax.el.ValueExpression value);

    public abstract FlowBuilder inboundParameter(String name,
        String value);

    public abstract Flow getFlow();
    
    public abstract NavigationCaseBuilder navigationCase();
}
