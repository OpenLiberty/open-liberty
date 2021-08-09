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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.el.ValueExpression;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

public class FaceletState implements StateHolder, Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -7823771271935942737L;
    
    private Map<String, Object> stateMap;
    
    private Map<String, Map<String, ValueExpression>> bindingsMap;
    
    public FaceletState()
    {
    }
    
    public Object getState(String key)
    {
        if(stateMap == null)
        {
            return null;
        }
        return stateMap.get(key);
    }
    
    public Object putState(String key, Object value)
    {
        if (stateMap == null)
        {
            stateMap = new HashMap<String, Object>();
        }
        return stateMap.put(key, value);
    }
    

    public Object saveState(FacesContext context)
    {
        UIViewRoot root = context.getViewRoot();
        if (root != null && root.initialStateMarked())
        {
            if (stateMap != null)
            {
                Object values[] = new Object[1];
                values[0] = UIComponentBase.saveAttachedState(context, stateMap);
                return values;
            }
            return null;
        }
        else
        {
            Object values[] = new Object[2];
            values[0] = UIComponentBase.saveAttachedState(context, stateMap);
            // If the UIViewRoot instance was not marked with initial state, that means
            // we need to save the bindingsMap as well because it will not be restored
            // like with PSS, because in that case the view is built again using 
            // facelets algorithm.
            values[1] = UIComponentBase.saveAttachedState(context, bindingsMap);
            return values;
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreState(FacesContext context, Object state)
    {
        if (state == null)
        {
            stateMap = null;
            bindingsMap = null;
            return;
        }
        Object values[] = (Object[])state;
        if (values.length == 2)
        {
            // Full state
            stateMap = (Map<String,Object>) UIComponentBase.restoreAttachedState(context, values[0]);
            bindingsMap = (Map<String,Map<String, ValueExpression>>) 
                UIComponentBase.restoreAttachedState(context, values[1]);
        }
        else
        {
            if (values[0] == null)
            {
                stateMap = null;
            }
            else
            {
                stateMap = (Map<String,Object>) UIComponentBase.restoreAttachedState(context, values[0]);
            }            
        }
    }

    public boolean isTransient()
    {
        return false;
    }

    public void setTransient(boolean newTransientValue)
    {
    }
    
    public boolean isDynamic()
    {
        return stateMap == null ? false : !stateMap.isEmpty();
    }

    public void putBinding(String uniqueId, String key, ValueExpression expr)
    {
        if (bindingsMap == null)
        {
            bindingsMap = new HashMap<String, Map<String, ValueExpression>>();
        }
        Map<String, ValueExpression> bindings = bindingsMap.get(uniqueId);
        if (bindings == null)
        {
            bindings = new HashMap<String, ValueExpression>();
            bindingsMap.put(uniqueId, bindings);
        }
        bindings.put(key, expr);
    }
    
    public ValueExpression getBinding(String uniqueId, String key)
    {
        if (bindingsMap == null)
        {
            return null;
        }
        Map<String, ValueExpression> bindings = bindingsMap.get(uniqueId);
        if (bindings == null)
        {
            return null;
        }
        return bindings.get(key);
    }
    
    public Map<String, Map<String, ValueExpression>> getBindings()
    {
        return bindingsMap;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 79 * hash + (this.stateMap != null ? this.stateMap.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final FaceletState other = (FaceletState) obj;
        if (this.stateMap != other.stateMap && (this.stateMap == null || !this.stateMap.equals(other.stateMap)))
        {
            return false;
        }
        return true;
    }

}
