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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.application.NavigationCase;

/**
 * This class contains the navigation structure of a flow, including
 * navigation rules.
 * 
 * @author Leonardo Uribe
 */
class _FlowNavigationStructure
{
    private String _definingDocumentId;
    private String _id;
    
    private Map<String, Set<NavigationCase>> _navigationCases = null;
    private List<_WildcardPattern> _wildcardKeys = new ArrayList<_WildcardPattern>();

    public _FlowNavigationStructure(String definingDocumentId, String id, 
        Map<String, Set<NavigationCase>> navigationCases, List<_WildcardPattern> wildcardKeys)
    {
        this._definingDocumentId = definingDocumentId;
        this._id = id;
        this._navigationCases = navigationCases;
        this._wildcardKeys = wildcardKeys;
    }

    public String getDefiningDocumentId()
    {
        return _definingDocumentId;
    }

    public String getId()
    {
        return _id;
    }

    public Map<String, Set<NavigationCase>> getNavigationCases()
    {
        return _navigationCases;
    }

    public List<_WildcardPattern> getWildcardKeys()
    {
        return _wildcardKeys;
    }
}
