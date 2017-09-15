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
package org.apache.myfaces.view.facelets;

import javax.faces.component.UIComponent;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;

/**
 *
 * @author Leonardo Uribe
 */
public class PublishDynamicComponentRefreshTransientBuildCallback implements VisitCallback
{
    public VisitResult visit(VisitContext context, UIComponent target)
    {
        if (target.getAttributes().containsKey(DynamicComponentRefreshTransientBuildEvent.DYN_COMP_REFRESH_FLAG))
        {
            context.getFacesContext().getApplication().publishEvent(
                    context.getFacesContext(), DynamicComponentRefreshTransientBuildEvent.class, 
                    target.getClass(), target);
        }
        return VisitResult.ACCEPT;
    }

}
