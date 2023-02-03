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
package org.apache.myfaces.component.validate;

import jakarta.faces.application.ProjectStage;
import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.validator.BeanValidator;
import jakarta.faces.validator.Validator;

import java.io.IOException;
import java.util.List;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.core.api.shared.ComponentUtils;
import org.apache.myfaces.util.WebConfigParamUtils;

/**
 *
 */
@JSFComponent
public class ValidateWholeBeanComponent extends UIInput
{
    static public final String COMPONENT_FAMILY = "jakarta.faces.Input";
    static public final String COMPONENT_TYPE = "org.apache.myfaces.component.validate.ValidateWholeBean";

    public ValidateWholeBeanComponent()
    {
        setRendererType(null);
    }

    @Override
    public Object getSubmittedValue()
    {
        return "WholeBeanValidator";
    }

    @Override
    public void addValidator(Validator validator)
    {
        // No-op. It does not make sense to allow additional validators to be installed.
    }


    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {    
        // https://github.com/jakartaee/faces/issues/1780
        if (context.isProjectStage(ProjectStage.Development)) 
        {
            // find closest form
            UIForm closestForm = ComponentUtils.findClosest(UIForm.class, this);
        
            if (closestForm == null)
            {
                // Throw an exception just as Mojarra
                throw new IllegalStateException("f:validateWholeBean must be placed within a form");
            }
        
            validateTagPlacement(closestForm, this.getClientId(context));
        }
    }
  
    /*
     * As required by https://github.com/jakartaee/faces/issues/1
     * Also ensures all inputs are available for f:wholeBeanValidate processing
     * (otherwise they'd be empty during the validation)
     * Inspired by Mojarra's UIValidateWholeBean#misplacedComponentCheck
     * 1) Get all children of the form component
     * 2) Loop in reverse thorough each child in the form
     * 3) If we find an editable component (EditableValueHolder)
     * and it's group validator matches f:wholeBeanValidate (this part is unique to
     * myfaces) then throw an exception.
     * 4) If we find the f:wholeBeanValidate's client id before any
     * EditableValueHolder tags, return.
     */
    public void validateTagPlacement(UIComponent component, String clientId) throws IllegalStateException
    {
        List<UIComponent> children = component.getChildren();
    
        for (int i = children.size() -1; i >=0; i--)
        {
          UIComponent c = children.get(i);
          if (c instanceof EditableValueHolder && !(c instanceof ValidateWholeBeanComponent))
          {
              Validator[] validators = ((EditableValueHolder) c).getValidators();
              for (Validator v : validators)
              {
                  if (v instanceof BeanValidator
                      && ((BeanValidator) v).getValidationGroups().equals(this.getValidationGroups()))
                  {
                    throw new IllegalStateException("f:validateWholeBean must be placed after all validated inputs");
                  }
              }
          }
          else
          {
              if (c.getClientId().equals(clientId))
              {
                  return; // found f:validateWholeBean before any inputs
              }
              else
              {
                  validateTagPlacement(c, clientId);
              }
          }
        }
    }

    @Override
    public void validate(FacesContext context)
    {
        
        Boolean enabled = WebConfigParamUtils.getBooleanInitParameter(context.getExternalContext(), 
                BeanValidator.ENABLE_VALIDATE_WHOLE_BEAN_PARAM_NAME, Boolean.FALSE);
        
        if (Boolean.TRUE.equals(enabled) && !isDisabled())
        {
            //Install WholeBeanValidator
            Validator[] validators = this.getValidators();
            if (validators != null && validators.length > 0)
            {
                //No op
            }
            else
            {
                super.addValidator(new WholeBeanValidator());
            }
            super.validate(context);
        }
    }

    @Override
    public void setConverter(Converter converter)
    {
        // No-op. It does not make sense to allow a converter to be installed.
    }

    @Override
    public void updateModel(FacesContext context)
    {
        // Do nothing! See the following specification issue: https://github.com/eclipse-ee4j/mojarra/issues/4313
    }

    @JSFProperty
    public String getValidationGroups()
    {
        return (String) getStateHelper().eval(PropertyKeys.validationGroups);
    }
    
    public void setValidationGroups(String validationGroups)
    {
        getStateHelper().put(PropertyKeys.validationGroups, validationGroups);
    }
    
    @JSFProperty(defaultValue="false")
    public boolean isDisabled()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.disabled, false);
    }
    
    public void setDisabled(boolean disabled)
    {
        getStateHelper().put(PropertyKeys.disabled, disabled);
    }
    
    enum PropertyKeys
    {
        validationGroups,
        disabled
    }
}
