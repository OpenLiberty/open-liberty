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
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.SystemEventListener;

/**
 * TODO: Remove it since after MYFACES-2389 this is not necessary anymore.
 * Now, PostAddToViewEvent can be triggered inclusive on postback and restore view
 * phase, but note RestoreViewExecutor call facesContext.setProcessingEvents(false)
 * and cause the same effect than the check on UIComponent.setParent()
 * (postback and restore view phase).
 * 
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 899026 $ $Date: 2010-01-14 01:47:14 +0000 (Thu, 14 Jan 2010) $
 */
public class PostBuildComponentTreeOnRestoreViewEvent extends ComponentSystemEvent
{
    /**
     * @param component
     */
    public PostBuildComponentTreeOnRestoreViewEvent(UIComponent component)
    {
        super(component);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAppropriateListener(FacesListener listener)
    {
        return listener instanceof SystemEventListener;
    }
}
