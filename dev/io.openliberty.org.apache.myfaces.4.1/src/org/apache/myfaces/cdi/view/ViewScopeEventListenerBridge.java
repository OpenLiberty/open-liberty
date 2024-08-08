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
package org.apache.myfaces.cdi.view;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Event;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.PostConstructViewMapEvent;
import jakarta.faces.event.PreDestroyViewMapEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ViewScopeEventListenerBridge
{
    @Inject
    @Initialized(ViewScoped.class)
    private Event<UIViewRoot> viewScopeInitializedEvent;

    @Inject
    @Destroyed(ViewScoped.class)
    private Event<UIViewRoot> viewScopeDestroyedEvent;

    public void processEvent(SystemEvent event) throws AbortProcessingException
    {
        if (event instanceof PostConstructViewMapEvent)
        {
            viewScopeInitializedEvent.fire((UIViewRoot) event.getSource());
        }

        if (event instanceof PreDestroyViewMapEvent)
        {
            viewScopeDestroyedEvent.fire((UIViewRoot) event.getSource());
        }
    }
}
