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
package jakarta.faces.component;

import org.apache.myfaces.core.api.shared.SelectItemsIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import jakarta.el.ValueExpression;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.model.SelectItem;
import jakarta.faces.component.UISelectItem;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.core.api.shared.MessageUtils;
import org.apache.myfaces.core.api.shared.SelectItemsUtil;

/**
 * Component for choosing one option out of a set of possibilities.
 * <p>
 * This component is expected to have children of type UISelectItem or UISelectItems; these define the set of possible
 * options that the user can choose from.
 * </p>
 * <p>
 * See the javadoc for this class in the <a
 * href="http://java.sun.com/j2ee/javaserverfaces/1.1_01/docs/api/index.html">Faces Specification</a> for further details.
 * </p>
 */
@JSFComponent(defaultRendererType = "jakarta.faces.Menu")
@JSFJspProperty(name="hideNoSelectionOption", returnType="boolean")
public class UISelectOne extends UIInput
{
    public static final String COMPONENT_TYPE = "jakarta.faces.SelectOne";
    public static final String COMPONENT_FAMILY = "jakarta.faces.SelectOne";

    public static final String INVALID_MESSAGE_ID = "jakarta.faces.component.UISelectOne.INVALID";

    private boolean previouslySubmittedOrValidated = false;

    public UISelectOne()
    {
        setRendererType("jakarta.faces.Menu");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    /**
     * Check whether a group exists and then
     * visit all the UISelectItem elements within the UISelectOne radio components to check if
     * the submitted value is empty (ie. not submitted) or if a previous group item has been
     * has failed to be validated (if no so further validation processing is needed)
     *
     * @see jakarta.faces.component.UIInput#processValidators(jakarta.faces.context.FacesContext)
     */
    @Override
    public void processValidators(FacesContext context) 
    {
        String group = getGroup();
        String submittedValue = (String) getSubmittedValue();

        if (group != null && !group.isEmpty()) 
        {
            final UIComponent form = getRadioNestingForm(context, this);

            form.visitTree(VisitContext.createVisitContext(context), new VisitCallback() 
            {
                @Override
                public VisitResult visit(VisitContext visitContext, UIComponent target) 
                {
                    // check they they are of the same group
                    if (target instanceof UISelectOne  && ((UISelectOne) target).getGroup().equals(group)) 
                    {
                        // check if the is empty (see ) or if it's not valid (means this path has been taken already)
                        // See conditions listed under spec: uiselectone#processValidators
                        if(isEmpty(submittedValue) && isSubmittedAlready((UIInput)target)){
                            previouslySubmittedOrValidated = true;
                            return VisitResult.COMPLETE;
                        }          
                    }
                    return VisitResult.ACCEPT;
                }
            });
        }

        if(previouslySubmittedOrValidated){
            // Skip further validation due to either 
            // 1) one of the submissions are not valid (for instance, required, but none submitted (see jakarta/faces#329))
            // 2) submitted value has been found and validated
            return;
        }
        
        super.processValidators(context);
    }

    private boolean isSubmittedAlready( UIComponent target){

        if(((EditableValueHolder)target).isLocalValueSet() 
            || !((EditableValueHolder)target).isValid() 
            || !isEmpty(((UIInput)target).getSubmittedValue()))
        {
            return true;
        }
        return false;

    }

    /**
     * Get the container component of a radio
     *
     * @param context
     * @param radio
     * @return
     */
    private static UIComponent getRadioNestingForm(FacesContext context, UISelectOne radio) 
    {
        UIComponent namingContainer = radio.getNamingContainer();

        while (namingContainer != null && !(namingContainer instanceof UIForm) && namingContainer.getParent() != null) 
        {
            namingContainer = namingContainer.getParent().getNamingContainer();
        }

        if (namingContainer != null) 
        {
            return namingContainer;
        } 
        else 
        {
            return context.getViewRoot();
        }
    }
    
    /**
     * Verify that the result of converting the newly submitted value is <i>equal</i> to the value property of one of
     * the child SelectItem objects. If this is not true, a validation error is reported.
     * 
     * @see jakarta.faces.component.UIInput#validateValue(jakarta.faces.context.FacesContext,java.lang.Object)
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
        Collection<SelectItem> items = new ArrayList<>();
        for (Iterator<SelectItem> iter = new SelectItemsIterator(this, context); iter.hasNext();)
        {
            items.add(iter.next());
        }
        
        if (SelectItemsUtil.matchValue(context, this, value, items.iterator(), converter))
        {
            if (!this.isRequired())
            {
                return; // Matched & Required false, so return ok.
            }
            if (!SelectItemsUtil.isNoSelectionOption(context, this, value, items.iterator(), converter))
            {
                return; // Matched & Required true & No-selection did NOT match, so return ok.
            }
        }

        // if previouslySubmittedOrValidated is true, then it means that we have found the select item value
        // in the other UISelectOne radio components, and no validation error is thrown.
        if (previouslySubmittedOrValidated) 
        {
            return;
        }

        MessageUtils.addErrorMessage(context, this, INVALID_MESSAGE_ID, 
                new Object[] {MessageUtils.getLabel(context, this) });
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
