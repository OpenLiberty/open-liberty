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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;

import org.apache.myfaces.shared.util.StringUtils;

/**
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1298418 $ $Date: 2012-03-08 14:54:27 +0000 (Thu, 08 Mar 2012) $
 */
public class ClientBehaviorAttachedObjectTargetImpl 
    implements ClientBehaviorAttachedObjectTarget, Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = -4645087262404844925L;

    protected ValueExpression _name;
    
    protected ValueExpression _targets;
    
    protected ValueExpression _event;
    
    protected boolean _default;

    public ClientBehaviorAttachedObjectTargetImpl()
    {
    }

    public String getName()
    {
        if (_name != null)
        {
            return (String) _name.getValue(FacesContext.getCurrentInstance().getELContext());
        }        
        return null;
    }

    public List<UIComponent> getTargets(UIComponent topLevelComponent)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String [] targetsArray = getTargets(facesContext);
        
        if (targetsArray.length > 0)
        {
            List<UIComponent> targetsList = new ArrayList<UIComponent>(targetsArray.length);
            for (String target : targetsArray)
            {
                UIComponent innerComponent = topLevelComponent.findComponent(target);
                
                if (innerComponent != null)
                {
                    if (innerComponent instanceof ClientBehaviorHolder ||
                        UIComponent.isCompositeComponent(innerComponent))
                    {
                        targetsList.add(
                                new ClientBehaviorRedirectEventComponentWrapper(innerComponent, getName(), getEvent()));
                    }
                    else
                    {
                        throw new FacesException("Component with clientId " + innerComponent.getClientId(facesContext)
                                                 + "should be instance of ClientBehaviorHolder");
                    }
                }
            }
            return targetsList;
        }
        else
        {
            // composite:actionSource/valueHolder/editableValueHolder
            // "name" description says if targets is not set, name is 
            // the component id of the target component where
            // it should be mapped to
            String name = getName();
            if (name != null)
            {
                UIComponent innerComponent = topLevelComponent.findComponent(getName());
                if (innerComponent != null)
                {
                    if (innerComponent instanceof ClientBehaviorHolder ||
                        UIComponent.isCompositeComponent(innerComponent))
                    {
                        List<UIComponent> targetsList = new ArrayList<UIComponent>(1);
                        targetsList.add(
                                new ClientBehaviorRedirectEventComponentWrapper(innerComponent, getName(), getEvent()));
                        return targetsList;
                    }
                    else
                    {
                        throw new FacesException("Component with clientId "+ innerComponent.getClientId(facesContext)
                                                 + "should be instance of ClientBehaviorHolder");
                    }
                }
            }
            return Collections.emptyList();
        }
    }
    
    public String [] getTargets(FacesContext context)
    {
        if (_targets != null)
        {
            return StringUtils.splitShortString((String) _targets.getValue(context.getELContext()), ' ');
        }
        return org.apache.myfaces.shared.util.ArrayUtils.EMPTY_STRING_ARRAY;
    }
    
    public void setName(ValueExpression name)
    {
        _name = name;
    }
    
    public void setTargets(ValueExpression ve)
    {
        _targets = ve;
    }
    
    public String getEvent()
    {
        if (_event != null)
        {
            return (String) _event.getValue(FacesContext.getCurrentInstance().getELContext());
        }        
        return null;
    }
    
    public void setEvent(ValueExpression event)
    {
        this._event = event;
    }

    public boolean isDefaultEvent()
    {
        return _default;
    }

    public void setDefault(boolean default1)
    {
        _default = default1;
    }
}
