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

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.ActionSource2;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIGraphic;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.event.MethodExpressionValueChangeListener;
import javax.faces.validator.MethodExpressionValidator;

/**
 * @since 1.2
 */
public class UIComponentELTagUtils
{
    //private static final Log log = LogFactory.getLog(UIComponentELTagUtils.class);
    private static final Logger log = Logger
            .getLogger(UIComponentELTagUtils.class.getName());

    private UIComponentELTagUtils()
    {
    } //util class, no instantiation allowed

    /**
     * @since 1.2
     */
    public static void setIntegerProperty(UIComponent component,
            String propName, ValueExpression value)
    {
        setIntegerProperty(component, propName, value, null);
    }

    /**
     * @since 1.2
     */
    public static void setIntegerProperty(UIComponent component,
            String propName, ValueExpression value, Integer defaultValue)
    {
        if (value != null)
        {
            if (value.isLiteralText())
            {
                component.getAttributes().put(propName,
                        Integer.valueOf(value.getExpressionString()));
            }
            else
            {
                component.setValueExpression(propName, value);
            }
        }
        else
        {
            if (defaultValue != null)
            {
                component.getAttributes().put(propName, defaultValue);
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void setLongProperty(UIComponent component, String propName,
            ValueExpression value)
    {
        setLongProperty(component, propName, value, null);
    }

    /**
     * @since 1.2
     */
    public static void setLongProperty(UIComponent component, String propName,
            ValueExpression value, Long defaultValue)
    {
        if (value != null)
        {
            if (value.isLiteralText())
            {
                component.getAttributes().put(propName,
                        Long.valueOf(value.getExpressionString()));
            }
            else
            {
                component.setValueExpression(propName, value);
            }
        }
        else
        {
            if (defaultValue != null)
            {
                component.getAttributes().put(propName, defaultValue);
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void setStringProperty(UIComponent component,
            String propName, ValueExpression value)
    {
        setStringProperty(component, propName, value, null);
    }

    /**
     * @since 1.2
     */
    public static void setStringProperty(UIComponent component,
            String propName, ValueExpression value, String defaultValue)
    {
        if (value != null)
        {
            if (value.isLiteralText())
            {
                component.getAttributes().put(propName,
                        value.getExpressionString());
            }
            else
            {
                component.setValueExpression(propName, value);
            }
        }
        else
        {
            if (defaultValue != null)
            {
                component.getAttributes().put(propName, defaultValue);
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void setBooleanProperty(UIComponent component,
            String propName, ValueExpression value)
    {
        setBooleanProperty(component, propName, value, null);
    }

    /**
     * @since 1.2
     */
    public static void setBooleanProperty(UIComponent component,
            String propName, ValueExpression value, Boolean defaultValue)
    {
        if (value != null)
        {
            if (value.isLiteralText())
            {
                component.getAttributes().put(propName,
                        Boolean.valueOf(value.getExpressionString()));
            }
            else
            {
                component.setValueExpression(propName, value);
            }
        }
        else
        {
            if (defaultValue != null)
            {
                component.getAttributes().put(propName, defaultValue);
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void setValueProperty(FacesContext context,
            UIComponent component, ValueExpression value)
    {
        if (value != null)
        {
            if (!value.isLiteralText())
            {
                component.setValueExpression(
                        org.apache.myfaces.shared.renderkit.JSFAttr.VALUE_ATTR,
                        value);
            }
            else if (component instanceof UICommand)
            {
                ((UICommand) component).setValue(value.getExpressionString());
            }
            else if (component instanceof UIParameter)
            {
                ((UIParameter) component).setValue(value.getExpressionString());
            }
            else if (component instanceof UISelectBoolean)
            {
                ((UISelectBoolean) component).setValue(Boolean.valueOf(value
                        .getExpressionString()));
            }
            else if (component instanceof UIGraphic)
            {
                ((UIGraphic) component).setValue(value.getExpressionString());
            }
            //Since many input components are ValueHolders the special components
            //must come first, ValueHolder is the last resort.
            else if (component instanceof ValueHolder)
            {
                ((ValueHolder) component).setValue(value.getExpressionString());
            }
            else
            {
                log.severe("Component " + component.getClass().getName()
                        + " is no ValueHolder, cannot set value.");
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void setConverterProperty(FacesContext context,
            UIComponent component, ValueExpression value)
    {
        if (value != null)
        {
            if (component instanceof ValueHolder)
            {
                if (value.isLiteralText())
                {
                    FacesContext facesContext = FacesContext
                            .getCurrentInstance();
                    Converter converter = facesContext.getApplication()
                            .createConverter(value.getExpressionString());
                    ((ValueHolder) component).setConverter(converter);
                }
                else
                {
                    component
                            .setValueExpression(
                                    org.apache.myfaces.shared.renderkit.JSFAttr.CONVERTER_ATTR,
                                    value);
                }
            }
            else
            {
                log.severe("Component " + component.getClass().getName()
                        + " is no ValueHolder, cannot set value.");
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void addValidatorProperty(FacesContext context,
            UIComponent component, MethodExpression validator)
    {
        if (validator != null)
        {
            if (!(component instanceof EditableValueHolder))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no EditableValueHolder");
            }

            ((EditableValueHolder) component)
                    .addValidator(new MethodExpressionValidator(validator));
        }
    }

    /**
     * @since 1.2
     */
    public static void setValueBinding(FacesContext context,
            UIComponent component, String propName, ValueExpression value)
    {
        if (value != null)
        {
            if (!value.isLiteralText())
            {
                component.setValueExpression(propName, value);
            }
            else
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context) + " attribute "
                        + propName + " must be a value reference, was " + value);
            }
        }
    }

    /**
     * @since 1.2
     */
    public static void setActionProperty(FacesContext context,
            UIComponent component, MethodExpression action)
    {
        if (action != null)
        {
            if (!(component instanceof ActionSource2))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no ActionSource2");
            }

            ((ActionSource2) component).setActionExpression(action);
        }
    }

    /**
     * @since 1.2
     */
    public static void addActionListenerProperty(FacesContext context,
            UIComponent component, MethodExpression actionListener)
    {
        if (actionListener != null)
        {
            if (!(component instanceof ActionSource2))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no ActionSource");
            }

            ((ActionSource2) component)
                    .addActionListener(new MethodExpressionActionListener(
                            actionListener));
        }
    }

    /**
     * @since 1.2
     */
    public static void addValueChangedListenerProperty(FacesContext context,
            UIComponent component, MethodExpression valueChangedListener)
    {
        if (valueChangedListener != null)
        {
            if (!(component instanceof EditableValueHolder))
            {
                throw new IllegalArgumentException("Component "
                        + component.getClientId(context)
                        + " is no EditableValueHolder");
            }

            ((EditableValueHolder) component)
                    .addValueChangeListener(new MethodExpressionValueChangeListener(
                            valueChangedListener));
        }
    }

    /**
     * @since 1.2
     */
    public static Object evaluateValueExpression(ELContext elContext,
            ValueExpression valueExpression)
    {
        return valueExpression.isLiteralText() ? valueExpression
                .getExpressionString() : valueExpression.getValue(elContext);
    }

    /**
     * @since 1.2
     */
    public static Boolean getBooleanValue(ELContext elContext,
            ValueExpression valueExpression)
    {
        if (valueExpression.isLiteralText())
        {
            return Boolean.valueOf(valueExpression.getExpressionString());
        }

        return (Boolean) valueExpression.getValue(elContext);
    }

    public static Integer getIntegerValue(ELContext elContext,
            ValueExpression valueExpression)
    {
        if (valueExpression.isLiteralText())
        {
            return Integer.valueOf(valueExpression.getExpressionString());
        }

        return (Integer) valueExpression.getValue(elContext);
    }

}
