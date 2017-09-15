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
package org.apache.myfaces.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.myfaces.config.element.NavigationRule;

/**
 *
 * @author lu4242
 */
public final class NavigationUtils
{
    public static Set<javax.faces.application.NavigationCase> convertNavigationCasesToAPI(NavigationRule rule)
    {
        Collection<? extends org.apache.myfaces.config.element.NavigationCase> configCases = 
                rule.getNavigationCases();
        Set<javax.faces.application.NavigationCase> apiCases = 
                new HashSet<javax.faces.application.NavigationCase>(configCases.size());
        
        for(org.apache.myfaces.config.element.NavigationCase configCase : configCases)
        {   
            if(configCase.getRedirect() != null)
            {
                String includeViewParamsAttribute = configCase.getRedirect().getIncludeViewParams();
                boolean includeViewParams = false; // default value is false
                if (includeViewParamsAttribute != null)
                {
                    includeViewParams = Boolean.valueOf(includeViewParamsAttribute);
                }
                apiCases.add(new javax.faces.application.NavigationCase(rule.getFromViewId(),
                        configCase.getFromAction(),
                                                configCase.getFromOutcome(),configCase.getIf(),
                        configCase.getToViewId(),
                                                configCase.getRedirect().getViewParams(),true,includeViewParams));
            }
            else
            {
                apiCases.add(new javax.faces.application.NavigationCase(rule.getFromViewId(),
                        configCase.getFromAction(),
                                                configCase.getFromOutcome(),configCase.getIf(),
                                                configCase.getToViewId(),null,false,false));
            }
        }
        
        return apiCases;
    }
}
