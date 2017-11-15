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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.el.ValueExpression;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;

/**
 * Component for choosing one option out of a set of possibilities.
 * <p>
 * This component is expected to have children of type UISelectItem or UISelectItems; these define the set of possible
 * options that the user can choose from.
 * </p>
 * <p>
 * See the javadoc for this class in the <a
 * href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/api/index.html">JSF Specification</a> for further details.
 * </p>
 */
@JSFComponent(defaultRendererType = "javax.faces.Menu")
@JSFJspProperty(name="hideNoSelectionOption", returnType="boolean")
public class UISelectOne extends UIInput
{
    public static final String COMPONENT_TYPE = "javax.faces.SelectOne";
    public static final String COMPONENT_FAMILY = "javax.faces.SelectOne";

    public static final String INVALID_MESSAGE_ID = "javax.faces.component.UISelectOne.INVALID";

    public UISelectOne()
    {
        setRendererType("javax.faces.Menu");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }
    
    /**
     * Verify that the result of converting the newly submitted value is <i>equal</i> to the value property of one of
     * the child SelectItem objects. If this is not true, a validation error is reported.
     * 
     * @see javax.faces.component.UIInput#validateValue(javax.faces.context.FacesContext,java.lang.Object)
     */
    @Override
    protected void validateValue(FacesContext context, Object value)
    {
        super.validateValue(context, value);

        if (!isValid() || value == null)
        {
            return;
        }

        // selected value must match to one of the available options
        // and if required is true it must not match an option with noSelectionOption set to true (since 2.0)
        Converter converter = getConverter();

        // Since the iterator is used twice, it has sense to traverse it only once.
        Collection<SelectItem> items = new ArrayList<SelectItem>();
        for (Iterator<SelectItem> iter = new _SelectItemsIterator(this, context); iter.hasNext();)
        {
            items.add(iter.next());
        }
        
        if (_SelectItemsUtil.matchValue(context, this, value, items.iterator(), converter))
        {
            if (! this.isRequired())
            {
                return; // Matched & Required false, so return ok.
            }
            if (! _SelectItemsUtil.isNoSelectionOption(context, this, value, 
                    items.iterator(), converter))
            {
                return; // Matched & Required true & No-selection did NOT match, so return ok.
            }
        }
        _MessageUtils.addErrorMessage(context, this, INVALID_MESSAGE_ID, 
                new Object[] {_MessageUtils.getLabel(context, this) });
        setValid(false);
    }

    public String getGroup()
    {
        return (String) getStateHelper().eval(PropertyKeys.group);
    }
    
    public void setGroup(String group)
    {
        getStateHelper().put(PropertyKeys.group, group ); 
    }
    
    enum PropertyKeys
    {
        group
    }
    
}
