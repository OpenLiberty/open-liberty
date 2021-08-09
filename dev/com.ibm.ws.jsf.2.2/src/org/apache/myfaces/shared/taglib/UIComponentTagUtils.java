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
package org.apache.myfaces.shared.taglib;

import java.util.logging.Logger;

import javax.faces.component.ActionSource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIGraphic;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.webapp.UIComponentTag;

import org.apache.myfaces.shared.el.SimpleActionMethodBinding;

/**
 * @deprecated replaced by @{link UIComponentELTagUtils}
 */
@Deprecated
public class UIComponentTagUtils
{
    //private static final Log log = LogFactory.getLog(UIComponentTagUtils.class);
    private static final Logger log = Logger
            .getLogger(UIComponentTagUtils.class.getName());

    private static final Class[] VALIDATOR_ARGS = { FacesContext.class,
            UIComponent.class, Object.class };
    private static final Class[] ACTION_LISTENER_ARGS = { ActionEvent.class };
    private static final Class[] VALUE_LISTENER_ARGS = { ValueChangeEvent.class };

    private UIComponentTagUtils()
    {
    } //util class, no instantiation allowed

    public static boolean isValueReference(String v)
    {
        return UIComponentTag.isValueReference(v);
    }

    public static void setIntegerProperty(FacesContext context,
            UIComponent component, String propName, String value)
    {
        if (value != null)
        {
            if (isValueReference(value))
            {
                ValueBinding vb = context.getApplication().createValueBinding(
                        value);
                component.setValueBinding(propName, vb);
            }
            else
            {
                //FIXME: should use converter maybe?
                component.getAttributes().put(propName, Integer.valueOf(value));
            }
        }
    }

    public static void setLongProperty(FacesContext context,
            UIComponent component, String propName, String value)
    {
        if (value != null)
        {
            if (isValueReference(value))
            {
                ValueBinding vb = context.getApplication().createValueBinding(
                        value);
                component.setValueBinding(propName, vb);
            }
            else
            {
                //FIXME: should use converter maybe?
                component.getAttributes().put(propName, Long.valueOf(value));
            }
        }
    }

    public static void setStringProperty(FacesContext context,
            UIComponent component, String propName, String value)
    {
        if (value != null)
        {
            if (isValueReference(value))
            {
                ValueBinding vb = context.getApplication().createValueBinding(
                        value);
                component.setValueBinding(propName, vb);
            }
            else
            {
                //TODO: Warning if component has no such property (with reflection)
                component.getAttributes().put(propName, value);
            }
        }
    }

    public static void setBooleanProperty(FacesContext context,
            UIComponent component, String propName, String value)
    {
        if (value != null)
        {
            if (isValueReference(value))
            {
                ValueBinding vb = context.getApplication().createValueBinding(
                        value);
                component.setValueBinding(propName, vb);
            }
            else
            {
                //TODO: More sophisticated way to convert boolean value (yes/no, 1/0, on/off, etc.)
                component.getAttributes().put(propName, Boolean.valueOf(value));
            }
        }
    }

    public static void setValueProperty(FacesContext context,
            UIComponent component, String value)
    {
        if (value != null)
        {
            if (isValueReference(value))
            {
                ValueBinding vb = context.getApplication().createValueBinding(
                        value);
                component.setValueBinding(
                        org.apache.myfaces.shared.renderkit.JSFAttr.VALUE_ATTR,
                        vb);
            }
            else if (component instanceof UICommand)
            {
                ((UICommand) component).setValue(value);
            }
            else if (component instanceof UIParameter)
            {
                ((UIParameter) component).setValue(value);
            }
            else if (component instanceof UISelectBoolean)
            {
                ((UISelectBoolean) component).setValue(Boolean.valueOf(value));
            }
            else if (component instanceof UIGraphic)
            {
                ((UIGraphic) component).setValue(value);
            }
            //Since many input components are ValueHolders the special components
            //must come first, ValueHolder is the last resort.
            else if (component instanceof ValueHolder)
            {
                ((ValueHolder) component).setValue(value);
            }
            else
            {
                log.severe("Component " + component.getClass().getName()
                        + " is no ValueHolder, cannot set value.");
            }
        }
    }

    public static void setConverterProperty(FacesContext context,
            UIComponent component, String value)
    {
        if (value != null)
        {
            if (component instanceof ValueHolder)
            {
                if (isValueReference(value))
                {
                    ValueBinding vb = context.getApplication()
                            .createValueBinding(value);
                    component
                            .setValueBinding(
                                    org.apache.myfaces.shared.renderkit.JSFAttr.CONVERTER_ATTR,
                                    vb);
                }
                else
                {
                    FacesContext facesContext = FacesContext
                            .getCurrentInstance();
                    Converter converter = facesContext.getApplication()
                            .createConverter(value);
                    ((ValueHolder) component).setConverter(converter);
                }
            }
            else
            {
                log.severe("Component " + component.getClass().getName()
                        + " is no ValueHolder, cannot set value.");
            }
        }
    }

    public static void setValidatorProperty(FacesContext context,
            UIComponent component, String validator)
    {
        if (validator != null)
        {
            if (!(component instanceof EditableValueHolder))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no EditableValueHolder");
            }
            if (isValueReference(validator))
            {
                MethodBinding mb = context.getApplication()
                        .createMethodBinding(validator, VALIDATOR_ARGS);
                ((EditableValueHolder) component).setValidator(mb);
            }
            else
            {
                log.severe("Component " + component.getClientId(context)
                        + " has invalid validation expression " + validator);
            }
        }
    }

    public static void setValueBinding(FacesContext context,
            UIComponent component, String propName, String value)
    {
        if (value != null)
        {
            if (isValueReference(value))
            {
                ValueBinding vb = context.getApplication().createValueBinding(
                        value);
                component.setValueBinding(propName, vb);
            }
            else
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context) + " attribute "
                        + propName + " must be a value reference, was " + value);
            }
        }
    }

    public static void setActionProperty(FacesContext context,
            UIComponent component, String action)
    {
        if (action != null)
        {
            if (!(component instanceof ActionSource))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no ActionSource");
            }
            MethodBinding mb;
            if (isValueReference(action))
            {
                mb = context.getApplication().createMethodBinding(action, null);
            }
            else
            {
                mb = new SimpleActionMethodBinding(action);
            }
            ((ActionSource) component).setAction(mb);
        }
    }

    public static void setActionListenerProperty(FacesContext context,
            UIComponent component, String actionListener)
    {
        if (actionListener != null)
        {
            if (!(component instanceof ActionSource))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no ActionSource");
            }
            if (isValueReference(actionListener))
            {
                MethodBinding mb = context.getApplication()
                        .createMethodBinding(actionListener,
                                ACTION_LISTENER_ARGS);

                /**
                if(! Void.class.equals(mb.getType(context)))
                {
                    throw new IllegalArgumentException(
                            actionListener +
                            " : Return types for action listeners must be void, see JSF spec. 3.2.1.1");
                }
                */

                ((ActionSource) component).setActionListener(mb);
            }
            else
            {
                log.severe("Component " + component.getClientId(context)
                        + " has invalid actionListener value: "
                        + actionListener);
            }
        }
    }

    public static void setValueChangedListenerProperty(FacesContext context,
            UIComponent component, String valueChangedListener)
    {
        if (valueChangedListener != null)
        {
            if (!(component instanceof EditableValueHolder))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no EditableValueHolder");
            }
            if (isValueReference(valueChangedListener))
            {
                MethodBinding mb = context.getApplication()
                        .createMethodBinding(valueChangedListener,
                                VALUE_LISTENER_ARGS);
                /**
                if(! Void.class.equals(mb.getType(context)))
                {
                    throw new IllegalArgumentException(
                            valueChangedListener +
                            " : Return types for value change listeners must be void, see JSF spec. 3.2.5.1");
                }
                */

                ((EditableValueHolder) component).setValueChangeListener(mb);
            }
            else
            {
                log.severe("Component " + component.getClientId(context)
                        + " has invalid valueChangedListener expression "
                        + valueChangedListener);
            }
        }
    }

}
