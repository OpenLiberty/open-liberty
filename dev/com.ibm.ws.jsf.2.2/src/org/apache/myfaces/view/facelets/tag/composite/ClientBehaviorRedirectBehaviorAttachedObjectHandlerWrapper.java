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
package org.apache.myfaces.view.facelets.tag.composite;

import javax.faces.FacesWrapper;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.BehaviorHolderAttachedObjectHandler;

/**
 * This wrapper is used in
 * FaceletViewDeclarationLanguage#retargetAttachedObjects(FacesContext, UIComponent, List&lt;AttachedObjectHandler&gt;)
 * to redirect the client behavior attached object when there is a chain of composite components.  
 * 
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1189343 $ $Date: 2011-10-26 17:53:36 +0000 (Wed, 26 Oct 2011) $
 */
public class ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper
        implements BehaviorHolderAttachedObjectHandler, FacesWrapper<BehaviorHolderAttachedObjectHandler>
{
    
    private final BehaviorHolderAttachedObjectHandler _delegate;
    private final String _eventName;

    public ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper(
            BehaviorHolderAttachedObjectHandler delegate, String eventName)
    {
        super();
        _delegate = delegate;
        _eventName = eventName;
    }

    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        _delegate.applyAttachedObject(context, parent);
    }

    public String getEventName()
    {
        return _eventName;
    }
    
    public String getWrappedEventName()
    {
        if (_delegate instanceof ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper)
        {
            return ((ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper) _delegate).getWrappedEventName();
        }
        else
        {
            return _delegate.getEventName();
        }
    }

    public String getFor()
    {
        return _delegate.getFor();
    }

    public BehaviorHolderAttachedObjectHandler getWrapped()
    {
        return _delegate;
    }
}
