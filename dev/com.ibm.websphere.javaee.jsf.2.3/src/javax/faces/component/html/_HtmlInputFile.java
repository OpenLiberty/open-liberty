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
package javax.faces.component.html;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Renders a HTML input element.
 *
 */
@JSFComponent
(name = "h:inputFile",
clazz = "javax.faces.component.html.HtmlInputFile",template=true,
tagClass = "org.apache.myfaces.taglib.html.HtmlInputFileTag",
defaultRendererType = "javax.faces.File",
implementz = "javax.faces.component.behavior.ClientBehaviorHolder",
defaultEventName = "valueChange"
)
abstract class _HtmlInputFile extends UIInput
    implements _AccesskeyProperty,
    _AltProperty, _UniversalProperties, _DisabledReadonlyProperties,
    _FocusBlurProperties, _ChangeSelectProperties, _EventProperties,
    _StyleProperties, _TabindexProperty, _LabelProperty, _RoleProperty
{

  static public final String COMPONENT_FAMILY =
    "javax.faces.Input";
  static public final String COMPONENT_TYPE =
    "javax.faces.HtmlInputFile";

  /**
   * HTML: The maximum number of characters allowed to be entered.
   * 
   * @JSFProperty
   *   defaultValue = "Integer.MIN_VALUE"
   */
  public abstract int getMaxlength();

  /**
   * HTML: The initial width of this control, in characters.
   * 
   * @JSFProperty
   *   defaultValue = "Integer.MIN_VALUE"
   */
  public abstract int getSize();

  /**
   * If the value of this attribute is "off", render "off" as the value of the attribute.
   * This indicates that the browser should disable its autocomplete feature for this component.
   * This is useful for components that perform autocompletion and do not want the browser interfering.
   * If this attribute is not set or the value is "on", render nothing.
   *
   * @return  the new autocomplete value
   */
  @JSFProperty
  public abstract String getAutocomplete();

    protected void validateValue(FacesContext context, Object convertedValue)
    {
        if (!isValid())
        {
            return;
        }

        // If our value is empty, check the required property
        boolean isEmpty = isEmptyValue(convertedValue); 

        if (isRequired() && isEmpty)
        {
            if (getRequiredMessage() != null)
            {
                String requiredMessage = getRequiredMessage();
                context.addMessage(this.getClientId(context), new javax.faces.application.FacesMessage(
                        javax.faces.application.FacesMessage.SEVERITY_ERROR,
                    requiredMessage, requiredMessage));
            }
            else
            {
                _MessageUtils.addErrorMessage(context, this, REQUIRED_MESSAGE_ID,
                    new Object[] { _MessageUtils.getLabel(context, this) });
            }
            setValid(false);
            return;
        }

        if (!isEmpty)
        {
            super.validateValue(context, convertedValue);
        }
    }
    
    private static boolean isEmptyValue(Object value)
    {
        if (value == null)
        {
            return true;
        }
        else if (value instanceof String)
        {
            if ( ((String)value).trim().length() <= 0 )
            {
                return true;
            }
        }
        else if (value instanceof java.util.Collection)
        {
            if ( ((java.util.Collection)value).isEmpty())
            {
                return true;
            }
        }
        else if (value.getClass().isArray())
        {
            if (java.lang.reflect.Array.getLength(value) <= 0)
            {
                return true;
            }
        }
        else if (value instanceof java.util.Map)
        {
            if ( ((java.util.Map)value).isEmpty())
            {
                return true;
            }
        }
        else if (value instanceof javax.servlet.http.Part) 
        {
            if (((javax.servlet.http.Part)value).getSize() <= 0) 
            {
                return true;
            }
        }
        return false;
    }

}
