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
package javax.faces.component;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

class _ComponentChildrenList extends AbstractList<UIComponent> implements Serializable, RandomAccess
{
    private static final long serialVersionUID = -6775078929331154224L;
    private UIComponent _component;
    private List<UIComponent> _list = new ArrayList<UIComponent>(4);

    _ComponentChildrenList(UIComponent component)
    {
        _component = component;
    }

    @Override
    public UIComponent get(int index)
    {
        return _list.get(index);
    }

    @Override
    public int size()
    {
        return _list.size();
    }

    @Override
    public UIComponent set(int index, UIComponent value)
    {
        checkValue(value);
        removeChildrenFromParent(value);
        UIComponent child = _list.set(index, value);
        if (child != value)
        {
            updateParent(value);
            if (child != null)
            {
                childRemoved(child);
            }
        }
        
        return child;
    }

    @Override
    public boolean add(UIComponent value)
    {
        checkValue(value);

        removeChildrenFromParent(value);
        boolean res = _list.add(value);
        
        updateParent(value);
        
        return res;
    }

    @Override
    public void add(int index, UIComponent value)
    {
        checkValue(value);
        
        removeChildrenFromParent(value);
        
        _list.add(index, value);
        
        updateParent(value);
    }

    @Override
    public UIComponent remove(int index)
    {
        UIComponent child = _list.remove(index);
        if (child != null)
        {
            childRemoved(child);
        }
        
        return child;
    }

    private void checkValue(Object value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        
        // Not necessary anymore because  
        //if (!(value instanceof UIComponent))
        //{
        //    throw new ClassCastException("value is not a UIComponent");
        //}
    }

    private void childRemoved(UIComponent child)
    {
        child.setParent(null);
    }

    private void updateParent(UIComponent child)
    {
        child.setParent(_component);
    }
    
    private void removeChildrenFromParent(UIComponent child)
    {
        UIComponent oldParent = child.getParent();
        if (oldParent != null)
        {
            if (!oldParent.getChildren().remove(child))
            {
                // Check if the component is inside a facet and remove from there
                if (oldParent.getFacetCount() > 0)
                {
                    for (Iterator< Map.Entry<String, UIComponent > > it = 
                        oldParent.getFacets().entrySet().iterator() ; it.hasNext() ; )
                    {
                        Map.Entry<String, UIComponent > entry = it.next();
                        
                        if (entry.getValue().equals(child))
                        {
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean remove(Object value)
    {
        if (!(value instanceof UIComponent))
        {
            throw new ClassCastException("value is not a UIComponent");
        }
        
        checkValue(value);

        if (_list.remove(value))
        {
            childRemoved((UIComponent)value);
            return true;
        }
        return false;
    }
}
