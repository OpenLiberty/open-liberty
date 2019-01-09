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

import javax.faces.context.FacesContext;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFExclude;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * This tag associates a single SelectItem with the nearest parent UIComponent. The item represents a single option for
 * a component such as an h:selectBooleanCheckbox or h:selectOneMenu. See also component selectItems.
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * <p>
 * UISelectItem should be nestetd inside a UISelectMany or UISelectOne component, and results in the addition of a
 * SelectItem instance to the list of available options for the parent component
 * </p>
 */
@JSFComponent(clazz = "javax.faces.component.UISelectItem", template = true, name = "f:selectItem", 
              tagClass = "org.apache.myfaces.taglib.core.SelectItemTag", bodyContent = "empty")
abstract class _UISelectItem extends UIComponentBase
{

    static public final String COMPONENT_FAMILY = "javax.faces.SelectItem";
    static public final String COMPONENT_TYPE = "javax.faces.SelectItem";

    /**
     * Disable this property; although this class extends a base-class that defines a read/write rendered property, this
     * particular subclass does not support setting it. Yes, this is broken OO design: direct all complaints to the JSF
     * spec group.
     */
    @Override
    @JSFExclude
    @JSFProperty(tagExcluded = true)
    public void setRendered(boolean state)
    {
        super.setRendered(state);
        // call parent method due TCK problems
        // throw new UnsupportedOperationException();
    }

    @Override
    protected FacesContext getFacesContext()
    {
        //In theory the parent most of the times has 
        //the cached FacesContext instance, because this
        //element is purely logical, and the parent is the one
        //where encodeXXX was invoked. But only limit the
        //search to the closest parent.
        UIComponent parent = getParent();
        if (parent != null && parent.isCachedFacesContext())
        {
            return parent.getFacesContext();
        }
        else
        {
            return super.getFacesContext();
        }
    }
   
    /**
     * The initial value of this component.
     * 
     * @return the new value value
     */
    @JSFProperty(deferredValueType="javax.faces.model.SelectItem")
    public abstract Object getValue();

    /**
     * Determine whether this item can be chosen by the user. When true, this item cannot be chosen by the user. If this
     * method is ever called, then any EL-binding for the disabled property will be ignored.
     * 
     * @return the new itemDisabled value
     */
    @JSFProperty(defaultValue = "false", deferredValueType="java.lang.Boolean")
    public abstract boolean isItemDisabled();

    /**
     * The escape setting for the label of this selection item.
     * 
     * @return the new itemEscaped value
     */
    @JSFProperty(defaultValue = "true", jspName = "escape", deferredValueType="java.lang.String")
    public abstract boolean isItemEscaped();

    /**
     * For use in development tools.
     * 
     * @return the new itemDescription value
     */
    @JSFProperty
    public abstract String getItemDescription();

    /**
     * The string which will be presented to the user for this option.
     * 
     * @return the new itemLabel value
     */
    @JSFProperty
    public abstract String getItemLabel();

    /**
     * The value for this Item.
     * 
     * @return the new itemValue value
     */
    @JSFProperty
    public abstract Object getItemValue();

    /**
     * Indicate this component represent no selection option. 
     * Default value is false.
     * 
     * @since 2.0
     * @return is the component represent no select option
     */
    @JSFProperty
    (defaultValue="false", deferredValueType="java.lang.Boolean")
    public abstract boolean isNoSelectionOption();
}
