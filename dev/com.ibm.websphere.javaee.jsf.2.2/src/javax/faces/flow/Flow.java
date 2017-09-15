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
package javax.faces.flow;

import java.util.List;
import java.util.Map;
import javax.faces.application.NavigationCase;
import javax.faces.lifecycle.ClientWindow;

/**
 *
 * @since 2.2
 */
public abstract class Flow
{
    
    public abstract String getClientWindowFlowId(ClientWindow curWindow);

    public abstract String getDefiningDocumentId();

    public abstract String getId();

    public abstract Map<String,Parameter> getInboundParameters();
    
    public abstract javax.el.MethodExpression getInitializer();
    
    public abstract javax.el.MethodExpression getFinalizer();
    
    public abstract FlowCallNode getFlowCall(Flow targetFlow);
    
    public abstract Map<String,FlowCallNode> getFlowCalls();
    
    public abstract List<MethodCallNode> getMethodCalls();
    
    public abstract FlowNode getNode(String nodeId);
    
    public abstract Map<String,ReturnNode> getReturns();
    
    public abstract Map<String,SwitchNode> getSwitches();
    
    public abstract List<ViewNode> getViews();
    
    public abstract String getStartNodeId();
    
    public abstract java.util.Map<java.lang.String,java.util.Set<NavigationCase>> getNavigationCases();
    
}
