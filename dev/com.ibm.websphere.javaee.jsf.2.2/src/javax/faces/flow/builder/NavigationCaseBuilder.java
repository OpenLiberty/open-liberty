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

/**
 *
 * @since 2.2
 */
public abstract class NavigationCaseBuilder
{
    
    public abstract NavigationCaseBuilder fromViewId(String fromViewId);
    
    public abstract NavigationCaseBuilder fromAction(String fromAction);
    
    public abstract NavigationCaseBuilder fromOutcome(String fromOutcome);
    
    public abstract NavigationCaseBuilder toViewId(String toViewId);
    
    public abstract NavigationCaseBuilder toFlowDocumentId(String toFlowDocumentId);
    
    public abstract NavigationCaseBuilder condition(String condition);
    
    public abstract NavigationCaseBuilder condition(javax.el.ValueExpression condition);
    
    public abstract NavigationCaseBuilder.RedirectBuilder redirect();
    
    public abstract class RedirectBuilder
    {
        public abstract NavigationCaseBuilder.RedirectBuilder parameter(String name,
                                                                String value);
        
        public abstract NavigationCaseBuilder.RedirectBuilder includeViewParams();
    }
    
}
