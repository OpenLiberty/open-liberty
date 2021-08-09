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

import java.util.Map;
import java.util.Set;

import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

/**
 * This class is used as a double wrapper for NavigationHandler
 * and ConfigurableNavigationHandler to be backwards compatible
 * to the pre JSF 2.0 NavigationHandlers which are not 
 * ConfigurableNavigationHandlers (since JSF 2.0 the standard
 * NavigationHandler implementation has to inherit from
 * ConfigurableNavigationHandler).
 * 
 * It just passes through handleNavigation() to the wrapped
 * NavigationHandler and getNavigationCase() and getNavigationCases()
 * to the wrapped ConfigurableNavigationHandler.
 * 
 * @author Jakob Korherr (latest modification by $Author: jakobk $)
 * @version $Revision: 916247 $ $Date: 2010-02-25 11:00:37 +0000 (Thu, 25 Feb 2010) $
 */
public class BackwardsCompatibleNavigationHandlerWrapper 
        extends ConfigurableNavigationHandler
{

    private NavigationHandler _wrapped;
    private ConfigurableNavigationHandler _doubleWrapped;
    
    public BackwardsCompatibleNavigationHandlerWrapper(NavigationHandler wrapped, 
            ConfigurableNavigationHandler doubleWrapped)
    {
        _wrapped = wrapped;
        _doubleWrapped = doubleWrapped;
    }
    
    @Override
    public NavigationCase getNavigationCase(FacesContext context,
            String fromAction, String outcome)
    {
        return _doubleWrapped.getNavigationCase(context, fromAction, outcome);
    }

    @Override
    public Map<String, Set<NavigationCase>> getNavigationCases()
    {
        return _doubleWrapped.getNavigationCases();
    }

    @Override
    public void handleNavigation(FacesContext context, String fromAction, String outcome)
    {
        _wrapped.handleNavigation(context, fromAction, outcome);
    }

}
