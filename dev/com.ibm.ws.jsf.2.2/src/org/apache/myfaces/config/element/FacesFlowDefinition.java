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
package org.apache.myfaces.config.element;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Leonardo Uribe
 */
public abstract class FacesFlowDefinition implements Serializable
{
    public abstract String getDefiningDocumentId();
    
    public abstract String getId();
    
    public abstract String getStartNode();
    
    public abstract List<FacesFlowView> getViewList();

    public abstract List<FacesFlowSwitch> getSwitchList();
    
    public abstract List<FacesFlowReturn> getReturnList();
    
    public abstract List<NavigationRule> getNavigationRuleList();

    public abstract List<FacesFlowCall> getFlowCallList();
    
    public abstract List<FacesFlowMethodCall> getMethodCallList();
    
    public abstract String getInitializer();
    
    public abstract String getFinalizer();
    
    public abstract List<FacesFlowParameter> getInboundParameterList();

}
