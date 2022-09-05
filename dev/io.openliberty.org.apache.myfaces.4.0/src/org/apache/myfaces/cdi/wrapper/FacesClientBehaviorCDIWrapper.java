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

package org.apache.myfaces.cdi.wrapper;

import java.util.Set;
import jakarta.faces.FacesWrapper;
import jakarta.faces.component.PartialStateHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorContext;
import jakarta.faces.component.behavior.ClientBehaviorHint;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.BehaviorEvent;
import org.apache.myfaces.cdi.util.CDIUtils;

public class FacesClientBehaviorCDIWrapper implements PartialStateHolder, ClientBehavior, FacesWrapper<ClientBehavior>
{
    private transient ClientBehavior delegate;
    
    private String behaviorId;
    private boolean _transient;
    private boolean _initialStateMarked = false;

    public FacesClientBehaviorCDIWrapper()
    {
    }

    public FacesClientBehaviorCDIWrapper(Class<? extends ClientBehavior> behaviorClass, String behaviorId)
    {
        this.behaviorId = behaviorId;
    }
    
    @Override
    public void broadcast(BehaviorEvent event)
    {
        getWrapped().broadcast(event);
    }
    
    @Override
    public void decode(FacesContext context, UIComponent component)
    {
        getWrapped().decode(context, component);
    }

    @Override
    public Set<ClientBehaviorHint> getHints()
    {
        return getWrapped().getHints();
    }

    @Override
    public String getScript(ClientBehaviorContext behaviorContext)
    {
        return getWrapped().getScript(behaviorContext);
    }

    @Override
    public ClientBehavior getWrapped()
    {
        if (delegate == null)
        {
            delegate = (ClientBehavior) CDIUtils.get(CDIUtils.getBeanManager(FacesContext.getCurrentInstance()), 
                    ClientBehavior.class, true, new FacesBehaviorAnnotationLiteral(behaviorId));
        }
        return delegate;
    }
    
    @Override
    public Object saveState(FacesContext context)
    {
        if (!initialStateMarked())
        {
            Object values[] = new Object[1];
            values[0] = behaviorId;
            return values;
        }
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state)
    {
        if (state != null)
        {
            Object values[] = (Object[])state;
            behaviorId = (String)values[0];
        }
    }

    @Override
    public boolean isTransient()
    {
        return _transient;
    }

    @Override
    public void setTransient(boolean newTransientValue)
    {
        _transient = newTransientValue;
    }

    @Override
    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    @Override
    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    @Override
    public void markInitialState()
    {
        _initialStateMarked = true;
    }

}
