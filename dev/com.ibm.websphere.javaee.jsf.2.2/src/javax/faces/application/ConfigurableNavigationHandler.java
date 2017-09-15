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
package javax.faces.application;

import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;

/**
 * @since 2.0
 */
public abstract class ConfigurableNavigationHandler extends NavigationHandler
{

    /**
     * 
     */
    public ConfigurableNavigationHandler()
    {
    }

    public abstract NavigationCase getNavigationCase(FacesContext context, String fromAction, String outcome);

    public abstract Map<String,Set<NavigationCase>> getNavigationCases();

    public void performNavigation(String outcome)
    {
        handleNavigation(FacesContext.getCurrentInstance(), null, outcome);
    }
    
    /**
     * @since 2.2
     * @param context
     * @param flow 
     */
    public void inspectFlow(FacesContext context, Flow flow)
    {
    }
    
    /**
     * @since 2.2
     * @param context
     * @param fromAction
     * @param outcome
     * @param toFlowDocumentId
     * @return 
     */
    public NavigationCase getNavigationCase(FacesContext context,
                                        java.lang.String fromAction,
                                        java.lang.String outcome,
                                        java.lang.String toFlowDocumentId)
    {
        return getNavigationCase(context, fromAction, outcome);
    }
}
