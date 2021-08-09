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

package org.apache.myfaces.view.facelets.tag.jsf;

import javax.faces.component.UIComponent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.SystemEventListener;

/**
 * MyFaces specific event used to clear all component binding if
 * normal navigation occur during a POST request.
 * 
 * NOTE: this event is not intended for use outside MyFaces Impl project.
 * 
 * @author Leonardo Uribe
 * @since 2.0.6
 *
 */
public class PreDisposeViewEvent extends ComponentSystemEvent
{

    /**
     * 
     */
    private static final long serialVersionUID = 8124621389770967678L;

    public PreDisposeViewEvent(UIComponent component)
    {
        super(component);
    }

    @Override
    public boolean isAppropriateListener(FacesListener listener)
    {
        return listener instanceof SystemEventListener;
    }
    
    
}
