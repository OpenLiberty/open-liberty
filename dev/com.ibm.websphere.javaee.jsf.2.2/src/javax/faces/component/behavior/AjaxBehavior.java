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
package javax.faces.component.behavior;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.component.StateHelper;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorListener;

/**
 * @since 2.0
 */
public class AjaxBehavior extends ClientBehaviorBase 
{

    /**
     * not needed anymore but enforced by the spec
     * theoretically a
     * @FacesBehavior(value = "javax.faces.behavior.Ajax")
     * could do it
     */
    public static final String BEHAVIOR_ID = "javax.faces.behavior.Ajax";

    private static final String ATTR_EXECUTE = "execute";
    private static final String ATTR_ON_ERROR = "onerror";
    private static final String ATTR_ON_EVENT = "onevent";
    private static final String ATTR_RENDER = "render";
    private static final String ATTR_DISABLED = "disabled";
    private static final String ATTR_IMMEDIATE = "immediate";
    private static final String ATTR_DELAY = "delay";
    private static final String ATTR_RESET_VALUES = "resetValues";

    /**
     * special render and execute targets
     */
    private static final String VAL_FORM = "@form";
    private static final String VAL_ALL = "@all";
    private static final String VAL_THIS = "@this";
    private static final String VAL_NONE = "@none";

    private static final Collection<String> VAL_FORM_LIST = Collections.singletonList(VAL_FORM);
    private static final Collection<String> VAL_ALL_LIST = Collections.singletonList(VAL_ALL);
    private static final Collection<String> VAL_THIS_LIST = Collections.singletonList(VAL_THIS);
    private static final Collection<String> VAL_NONE_LIST = Collections.singletonList(VAL_NONE);

    //To enable delta state saving we need this one
    private _DeltaStateHelper<AjaxBehavior> _stateHelper = null;
    
    //private Map<String, ValueExpression> _valueExpressions 
    //        = new HashMap<String, ValueExpression>();

    public AjaxBehavior() 
    {
        super();
    }

    public void addAjaxBehaviorListener(AjaxBehaviorListener listener) 
    {
        super.addBehaviorListener(listener);
    }
    
    public void removeAjaxBehaviorListener(AjaxBehaviorListener listener) 
    {
        removeBehaviorListener(listener);
    }

    public Collection<String> getExecute() 
    {
        // we have to evaluate the real value in this method,
        // because the value of the ValueExpression might
        // change (almost sure it does!)
        return evalForCollection(ATTR_EXECUTE);
    }

    public void setExecute(Collection<String> execute) 
    {
        getStateHelper().put(ATTR_EXECUTE, execute);
    }

    public String getOnerror() 
    {
        return (String) getStateHelper().eval(ATTR_ON_ERROR);
    }

    public void setOnerror(String onError) 
    {
        getStateHelper().put(ATTR_ON_ERROR, onError);
    }

    public String getOnevent() 
    {
        return (String) getStateHelper().eval(ATTR_ON_EVENT);
    }

    public void setOnevent(String onEvent) 
    {
        getStateHelper().put(ATTR_ON_EVENT, onEvent);
    }

    public Collection<String> getRender() 
    {
        // we have to evaluate the real value in this method,
        // because the value of the ValueExpression might
        // change (almost sure it does!)
        return evalForCollection(ATTR_RENDER);
    }

    public void setRender(Collection<String> render) 
    {
        getStateHelper().put(ATTR_RENDER, render);
    }

    @SuppressWarnings("unchecked")
    public ValueExpression getValueExpression(String name) 
    {
        //return getValueExpressionMap().get(name);
        if (name == null)
        {
            throw new NullPointerException("name can not be null");
        }
        
        Map<String,Object> bindings = (Map<String,Object>) getStateHelper().
            get(PropertyKeys.bindings);
        if (bindings != null)
        {
            return (ValueExpression) bindings.get(name);
        }
        else
        {
            return null;
        }
    }

    public void setValueExpression(String name, ValueExpression expression) 
    {
        /*
        if (item == null) 
        {
            getValueExpressionMap().remove(name);
            getStateHelper().remove(name);
        } 
        else 
        {
            getValueExpressionMap().put(name, item);
        }
        */
        if (name == null)
        {
            throw new NullPointerException("name");
        }

        if (expression == null)
        {
            getStateHelper().remove(PropertyKeys.bindings, name);
        }
        else
        {
            getStateHelper().put(PropertyKeys.bindings, name, expression);
        }
    }

    public boolean isDisabled() 
    {
        Boolean retVal = (Boolean) getStateHelper().eval(ATTR_DISABLED);
        retVal = (retVal == null) ? false : retVal;
        return retVal;
    }

    public void setDisabled(boolean disabled) 
    {
        getStateHelper().put(ATTR_DISABLED, disabled);
    }

    public boolean isImmediate() 
    {
        Boolean retVal = (Boolean) getStateHelper().eval(ATTR_IMMEDIATE);
        retVal = (retVal == null) ? false : retVal;
        return retVal;
    }

    public void setImmediate(boolean immediate) 
    {
        getStateHelper().put(ATTR_IMMEDIATE, immediate);
    }

    public boolean isImmediateSet() 
    {
        return (getStateHelper().get(ATTR_IMMEDIATE) != null) ||
            (getValueExpression(ATTR_IMMEDIATE) != null);
    }

    /**
     * @since 2.2
     */
    public boolean isResetValues()
    {
        Boolean retVal = (Boolean) getStateHelper().eval(ATTR_RESET_VALUES);
        retVal = (retVal == null) ? false : retVal;
        return retVal;
    }
    
    /**
     * @since 2.2
     */
    public void setResetValues(boolean resetValues)
    {
        getStateHelper().put(ATTR_RESET_VALUES, resetValues);
    }
    
    /**
     * @since 2.2
     */
    public boolean isResetValuesSet()
    {
        return (getStateHelper().get(ATTR_RESET_VALUES) != null) ||
            (getValueExpression(ATTR_RESET_VALUES) != null);
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public String getDelay()
    {
        return (String) getStateHelper().eval(ATTR_DELAY);
    }

    /**
     * @since 2.2
     * @param delay 
     */
    public void setDelay(String delay)
    {
        getStateHelper().put(ATTR_DELAY, delay);
    }

    @Override
    public Set<ClientBehaviorHint> getHints() 
    {
        return EnumSet.of(ClientBehaviorHint.SUBMITTING);
    }

    @Override
    public String getRendererType() 
    {
        return BEHAVIOR_ID;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object o)
    {
        if (o == null)
        {
            return;
        }
        Object[] values = (Object[]) o;
        if (values[0] != null) 
        {
            super.restoreState(facesContext, values[0]);
        }
        getStateHelper().restoreState(facesContext, values[1]);
    }

    private StateHelper getStateHelper()
    {
        return getStateHelper(true);
    }

    /**
     * returns a delta state saving enabled state helper
     * for the current component
     * @param create if true a state helper is created if not already existing
     * @return an implementation of the StateHelper interface or null if none exists and create is set to false
     */
    private StateHelper getStateHelper(boolean create)
    {
        if(_stateHelper != null)
        {
            return _stateHelper;
        }
        if(create)
        {
            _stateHelper = new _DeltaStateHelper<AjaxBehavior>(this);
        }
        return _stateHelper;
    }
    
    @Override
    public Object saveState(FacesContext facesContext)
    {
        if (initialStateMarked())
        {
            Object parentSaved = super.saveState(facesContext);
            Object stateHelperSaved = null;
            StateHelper stateHelper = getStateHelper(false);
            if (stateHelper != null)
            {
                stateHelperSaved = stateHelper.saveState(facesContext);
            }

            if (parentSaved == null && stateHelperSaved == null)
            {
                //No values
                return null;
            }   
            return new Object[]{parentSaved, stateHelperSaved};
        }
        else
        {
            Object[] values = new Object[2];
            values[0] = super.saveState(facesContext);
            StateHelper stateHelper = getStateHelper(false);
            if (stateHelper != null)
            {
                values[1] = stateHelper.saveState(facesContext);
            }
            return values;
        }
    }

    //private Map<String, ValueExpression> getValueExpressionMap() 
    //{
    //    return _valueExpressions;
    //}
    
    /**
     * Invokes eval on the getStateHelper() and tries to get a
     * Collection out of the result.
     * @param attributeName
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<String> evalForCollection(String attributeName)
    {
        Object value = getStateHelper().eval(attributeName);
        if (value == null)
        {
            return Collections.<String>emptyList();
        }
        else if (value instanceof Collection)
        {
            return (Collection<String>) value;
        }
        else if (value instanceof String)
        {
            return getCollectionFromSpaceSplitString((String) value);
        }
        else
        {
            throw new IllegalArgumentException("Type " + value.getClass()
                    + " not supported for attribute " + attributeName);
        }
    }
    
    /**
     * Splits the String based on spaces and returns the 
     * resulting Strings as Collection.
     * @param stringValue
     * @return
     */
    private Collection<String> getCollectionFromSpaceSplitString(String stringValue)
    {
        //@special handling for @all, @none, @form and @this
        if (stringValue.equals(VAL_FORM)) 
        {
            return VAL_FORM_LIST;
        } 
        else if (stringValue.equals(VAL_ALL)) 
        {
            return VAL_ALL_LIST;
        } 
        else if (stringValue.equals(VAL_NONE)) 
        {
            return VAL_NONE_LIST;
        } 
        else if (stringValue.equals(VAL_THIS)) 
        {
            return VAL_THIS_LIST; 
        }

        // not one of the "normal" values - split it and return the Collection
        String[] arrValue = stringValue.split(" ");
        return Arrays.asList(arrValue);
    }
 
    private enum PropertyKeys
    {
        bindings,
    }
}
