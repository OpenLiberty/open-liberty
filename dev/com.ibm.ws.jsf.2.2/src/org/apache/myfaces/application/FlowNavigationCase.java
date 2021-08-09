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
package org.apache.myfaces.application;

import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationCaseWrapper;

/**
 * Wrapper that helps overriding toFlowDocumentId and fromOutcome, to build
 * correctly a navigation case that cause a flow action (enter into a flow
 * or return from a flow).
 * 
 * The idea is if is necessary to enter into a flow set fromOutcome as the
 * flow id and toFlowDocumentId as the flow document id. If it is a return,
 * set fromOutcome as the return node and toFlowDocumentId as FlowHandler.NULL_FLOW
 * 
 * @author Leonardo Uribe
 */
public class FlowNavigationCase extends NavigationCaseWrapper
{
    
    private NavigationCase _delegate;
    private String _fromOutcome;
    private String _toFlowDocumentId;

    public FlowNavigationCase(NavigationCase delegate, String fromOutcome, String toFlowDocumentId)
    {
        this._delegate = delegate;
        this._fromOutcome = fromOutcome;
        this._toFlowDocumentId = toFlowDocumentId;
    }

    @Override
    public NavigationCase getWrapped()
    {
        return _delegate;
    }
    
    @Override
    public String getFromOutcome()
    {
        return _fromOutcome;
    }
    
    @Override
    public String getToFlowDocumentId()
    {
        return _toFlowDocumentId;
    }
}
