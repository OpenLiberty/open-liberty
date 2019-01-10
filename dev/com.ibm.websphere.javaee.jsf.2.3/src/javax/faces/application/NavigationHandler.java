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

import javax.faces.context.FacesContext;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class NavigationHandler
{
    public abstract void handleNavigation(FacesContext context,
                                          String fromAction,
                                          String outcome);
    
    /**
     * @since 2.2
     * @param context
     * @param fromAction
     * @param outcome
     * @param toFlowDocumentId 
     */
    public void handleNavigation(FacesContext context,
                                 String fromAction,
                                 String outcome,
                                 String toFlowDocumentId)
    {
        this.handleNavigation(context, fromAction, outcome);
    }
}
