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
package javax.faces.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFValidator;

/**
 * Check if a value is empty, in the same way as set UIInput required 
 * property to true (including all rules related to this property).
 * 
 * @since 2.0
 */
@JSFValidator(name = "f:validateRequired",
        bodyContent = "empty")
@JSFJspProperty(name = "binding", 
        returnType = "javax.faces.validator.RequiredValidator",
        longDesc = "A ValueExpression that evaluates to a RequiredValidator.")
public class RequiredValidator implements Validator
{

    // FIELDS
    public static final String VALIDATOR_ID = "javax.faces.Required";

    // CONSTRUCTORS    
    public RequiredValidator()
    {
    }

    // VALIDATE
    public void validate(FacesContext facesContext, UIComponent uiComponent,
            Object value) throws ValidatorException
    {
        if (facesContext == null)
        {
            throw new NullPointerException("facesContext");
        }
        if (uiComponent == null)
        {
            throw new NullPointerException("uiComponent");
        }

        //Check if the value is empty like UIInput.validateValue
        boolean empty = value == null
                || (value instanceof String && ((String) value).length() == 0);

        if (empty)
        {
            if (uiComponent instanceof UIInput)
            {
                UIInput uiInput = (UIInput) uiComponent;
                if (uiInput.getRequiredMessage() != null)
                {
                    String requiredMessage = uiInput.getRequiredMessage();
                    throw new ValidatorException(new FacesMessage(
                            FacesMessage.SEVERITY_ERROR, requiredMessage,
                            requiredMessage));
                }
            }
            throw new ValidatorException(_MessageUtils.getMessage(facesContext,
                    facesContext.getViewRoot().getLocale(),
                    FacesMessage.SEVERITY_ERROR, UIInput.REQUIRED_MESSAGE_ID,
                    new Object[] { _MessageUtils.getLabel(facesContext,
                            uiComponent) }));
        }
    }
    
    @JSFProperty(faceletsOnly=true)
    @SuppressWarnings("unused")
    private Boolean isDisabled()
    {
        return null;
    }
    
    @JSFProperty(faceletsOnly=true)
    @SuppressWarnings("unused")
    private String getFor()
    {
        return null;
    }
}
