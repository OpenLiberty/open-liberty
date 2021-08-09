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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

// ATTENTION
// This class is associated with org.apache.myfaces.shared.util.SelectItemsIterator.
// Changes here should also be applied to this class.

class _SelectItemsIterator implements Iterator<SelectItem>
{
    
    private static final Logger log = Logger.getLogger(_SelectItemsIterator.class.getName());
    
    private static final Iterator<UIComponent> _EMPTY_UICOMPONENT_ITERATOR = new _EmptyIterator<UIComponent>();
    
    // org.apache.myfaces.shared.util.SelectItemsIterator uses JSFAttr
    private static final String VAR_ATTR = "var";
    private static final String ITEM_VALUE_ATTR = "itemValue";
    private static final String ITEM_LABEL_ATTR = "itemLabel";
    private static final String ITEM_DESCRIPTION_ATTR = "itemDescription";
    private static final String ITEM_DISABLED_ATTR = "itemDisabled";
    private static final String ITEM_LABEL_ESCAPED_ATTR = "itemLabelEscaped";
    private static final String NO_SELECTION_VALUE_ATTR = "noSelectionValue";
    
    private final Iterator<UIComponent> _children;
    private Iterator<?> _nestedItems;
    private SelectItem _nextItem;
    private UIComponent _currentComponent;
    private UISelectItems _currentUISelectItems;
    private FacesContext _facesContext;

    public _SelectItemsIterator(UIComponent selectItemsParent, FacesContext facesContext)
    {
        _children = selectItemsParent.getChildCount() > 0
                        ? selectItemsParent.getChildren().iterator()
                        : _EMPTY_UICOMPONENT_ITERATOR;
        _facesContext = facesContext;
    }

    @SuppressWarnings("unchecked")
    public boolean hasNext()
    {
        if (_nextItem != null)
        {
            return true;
        }
        if (_nestedItems != null)
        {
            if (_nestedItems.hasNext())
            {
                return true;
            }
            _nestedItems = null;
            _currentComponent = null;
        }
        if (_children.hasNext())
        {
            UIComponent child = _children.next();
            // When there is other components nested that does
            // not extends from UISelectItem or UISelectItems
            // the behavior for this iterator is just skip this
            // element(s) until an element that extends from these
            // classes are found. If there is no more elements
            // that conform this condition, just return false.
            while (!(child instanceof UISelectItem) && !(child instanceof UISelectItems))
            {
                // Try to skip it
                if (_children.hasNext())
                {
                    // Skip and do the same check
                    child = _children.next();
                }
                else
                {
                    // End loop, so the final result is return false,
                    // since there are no more components to iterate.
                    return false;
                }
            }
            if (child instanceof UISelectItem)
            {
                UISelectItem uiSelectItem = (UISelectItem) child;
                Object item = uiSelectItem.getValue();
                if (item == null)
                {
                    // no value attribute --> create the SelectItem out of the other attributes
                    Object itemValue = uiSelectItem.getItemValue();
                    String label = uiSelectItem.getItemLabel();
                    String description = uiSelectItem.getItemDescription();
                    boolean disabled = uiSelectItem.isItemDisabled();
                    boolean escape = uiSelectItem.isItemEscaped();
                    boolean noSelectionOption = uiSelectItem.isNoSelectionOption();
                    if (label == null)
                    {
                        label = itemValue.toString();
                    }
                    item = new SelectItem(itemValue, label, description, disabled, escape, noSelectionOption);
                }
                else if (!(item instanceof SelectItem))
                {
                    ValueExpression expression = uiSelectItem.getValueExpression("value");
                    throw new IllegalArgumentException("ValueExpression '"
                            + (expression == null ? null : expression.getExpressionString()) + "' of UISelectItem : "
                            + getPathToComponent(child) + " does not reference an Object of type SelectItem");
                }
                _nextItem = (SelectItem) item;
                _currentComponent = child;
                return true;
            }
            else if (child instanceof UISelectItems)
            {
                _currentUISelectItems = ((UISelectItems) child);
                Object value = _currentUISelectItems.getValue();
                _currentComponent = child;

                if (value instanceof SelectItem)
                {
                    _nextItem = (SelectItem) value;
                    return true;
                }
                else if (value != null && value.getClass().isArray())
                {
                    // value is any kind of array (primitive or non-primitive)
                    // --> we have to use class Array to get the values
                    int length = Array.getLength(value);
                    Collection<Object> items = new ArrayList<Object>(length);
                    for (int i = 0; i < length; i++)
                    {
                        items.add(Array.get(value, i));
                    }
                    _nestedItems = items.iterator();
                    return hasNext();
                }
                else if (value instanceof Iterable)
                {
                    // value is Iterable --> Collection, DataModel,...
                    _nestedItems = ((Iterable<?>) value).iterator();
                    return hasNext();
                }
                else if (value instanceof Map)
                {
                    Map<Object, Object> map = ((Map<Object, Object>) value);
                    Collection<SelectItem> items = new ArrayList<SelectItem>(map.size());
                    for (Map.Entry<Object, Object> entry : map.entrySet())
                    {
                        items.add(new SelectItem(entry.getValue(), entry.getKey().toString()));
                    }
                    
                    _nestedItems = items.iterator();
                    return hasNext();
                }
                else
                {
                    Level level = Level.FINE;
                    if (!_facesContext.isProjectStage(ProjectStage.Production))
                    {
                        level = Level.WARNING;
                    }

                    if (log.isLoggable(level))
                    {
                        ValueExpression expression = _currentUISelectItems.getValueExpression("value");
                        log.log(level, "ValueExpression {0} of UISelectItems with component-path {1}"
                                + " does not reference an Object of type SelectItem,"
                                + " array, Iterable or Map, but of type: {2}",
                                new Object[] {
                                    (expression == null ? null : expression.getExpressionString()),
                                    getPathToComponent(child),
                                    (value == null ? null : value.getClass().getName()) 
                                });
                    }
                }
            }
            else
            {
                _currentComponent = null;
            }
        }
        return false;
    }

    public SelectItem next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        if (_nextItem != null)
        {
            SelectItem value = _nextItem;
            _nextItem = null;
            return value;
        }
        if (_nestedItems != null)
        {
            Object item = _nestedItems.next();
            
            if (!(item instanceof SelectItem))
            {
                // check new params of SelectItems (since 2.0): itemValue, itemLabel, itemDescription,...
                // Note that according to the spec UISelectItems does not provide Getter and Setter 
                // methods for this values, so we have to use the attribute map
                Map<String, Object> attributeMap = _currentUISelectItems.getAttributes();
                
                // write the current item into the request map under the key listed in var, if available
                boolean wroteRequestMapVarValue = false;
                Object oldRequestMapVarValue = null;
                String var = (String) attributeMap.get(VAR_ATTR);
                if(var != null && !"".equals(var))
                {
                    // save the current value of the key listed in var from the request map
                    oldRequestMapVarValue = _facesContext.getExternalContext().getRequestMap().put(var, item);
                    wroteRequestMapVarValue = true;
                }
                
                // check the itemValue attribute
                Object itemValue = attributeMap.get(ITEM_VALUE_ATTR);
                if (itemValue == null)
                {
                    // the itemValue attribute was not provided
                    // --> use the current item as the itemValue
                    itemValue = item;
                }
                
                // Spec: When iterating over the select items, toString() 
                // must be called on the string rendered attribute values
                Object itemLabel = attributeMap.get(ITEM_LABEL_ATTR);
                if (itemLabel == null)
                {
                    itemLabel = itemValue.toString();
                }
                else
                {
                    itemLabel = itemLabel.toString();
                }
                Object itemDescription = attributeMap.get(ITEM_DESCRIPTION_ATTR);
                if (itemDescription != null)
                {
                    itemDescription = itemDescription.toString();
                }
                Boolean itemDisabled = getBooleanAttribute(_currentUISelectItems, ITEM_DISABLED_ATTR, false);
                Boolean itemLabelEscaped = getBooleanAttribute(_currentUISelectItems, ITEM_LABEL_ESCAPED_ATTR, true);
                Object noSelectionValue = attributeMap.get(NO_SELECTION_VALUE_ATTR);
                item = new SelectItem(itemValue,
                        (String) itemLabel,
                        (String) itemDescription,
                        itemDisabled,
                        itemLabelEscaped,
                        itemValue.equals(noSelectionValue)); 
                    
                // remove the value with the key from var from the request map, if previously written
                if(wroteRequestMapVarValue)
                {
                    // If there was a previous value stored with the key from var in the request map, restore it
                    if (oldRequestMapVarValue != null)
                    {
                        _facesContext.getExternalContext()
                                .getRequestMap().put(var, oldRequestMapVarValue);
                    }
                    else
                    {
                        _facesContext.getExternalContext()
                                .getRequestMap().remove(var);
                    }
                } 
            }
            return (SelectItem) item;
        }
        throw new NoSuchElementException();
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
    
    public UIComponent getCurrentComponent()
    {
        return _currentComponent;
    }

    private boolean getBooleanAttribute(UIComponent component, String attrName, boolean defaultValue)
    {
        Object value = component.getAttributes().get(attrName);
        if (value == null)
        {
            return defaultValue;
        }
        else if (value instanceof Boolean)
        {
            return (Boolean) value;
        }
        else
        {
            // If the value is a String, parse the boolean.
            // This makes the following code work: <tag attribute="true" />,
            // otherwise you would have to write <tag attribute="#{true}" />.
            return Boolean.valueOf(value.toString());
        }
    }

    private String getPathToComponent(UIComponent component)
    {
        StringBuffer buf = new StringBuffer();

        if (component == null)
        {
            buf.append("{Component-Path : ");
            buf.append("[null]}");
            return buf.toString();
        }

        getPathToComponent(component, buf);

        buf.insert(0, "{Component-Path : ");
        buf.append("}");

        return buf.toString();
    }

    private void getPathToComponent(UIComponent component, StringBuffer buf)
    {
        if (component == null)
        {
            return;
        }

        StringBuffer intBuf = new StringBuffer();

        intBuf.append("[Class: ");
        intBuf.append(component.getClass().getName());
        if (component instanceof UIViewRoot)
        {
            intBuf.append(",ViewId: ");
            intBuf.append(((UIViewRoot) component).getViewId());
        }
        else
        {
            intBuf.append(",Id: ");
            intBuf.append(component.getId());
        }
        intBuf.append("]");

        buf.insert(0, intBuf);

        getPathToComponent(component.getParent(), buf);
    }
}
