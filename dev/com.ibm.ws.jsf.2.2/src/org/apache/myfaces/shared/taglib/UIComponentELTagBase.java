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

import org.apache.myfaces.shared.renderkit.JSFAttr;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.webapp.UIComponentELTag;

public abstract class UIComponentELTagBase extends UIComponentELTag
{
    //private static final Log log = LogFactory.getLog(UIComponentTagBase.class);

    //UIComponent attributes
    private ValueExpression _forceId;

    private ValueExpression _forceIdIndex;
    private static final Boolean DEFAULT_FORCE_ID_INDEX_VALUE = Boolean.TRUE;

    private ValueExpression _javascriptLocation;
    private ValueExpression _imageLocation;
    private ValueExpression _styleLocation;

    //Special UIComponent attributes (ValueHolder, ConvertibleValueHolder)
    private ValueExpression _value;
    private ValueExpression _converter;

    //attributes id, rendered and binding are handled by UIComponentTag

    public void release()
    {
        super.release();

        _forceId = null;
        _forceIdIndex = null;

        _value = null;
        _converter = null;

        _javascriptLocation = null;
        _imageLocation = null;
        _styleLocation = null;
    }

    protected void setProperties(UIComponent component)
    {
        super.setProperties(component);

        setBooleanProperty(component,
                org.apache.myfaces.shared.renderkit.JSFAttr.FORCE_ID_ATTR,
                _forceId);
        setBooleanProperty(
                component,
                org.apache.myfaces.shared.renderkit.JSFAttr.FORCE_ID_INDEX_ATTR,
                _forceIdIndex, DEFAULT_FORCE_ID_INDEX_VALUE);
        if (_javascriptLocation != null)
        {
            setStringProperty(component, JSFAttr.JAVASCRIPT_LOCATION,
                    _javascriptLocation);
        }
        if (_imageLocation != null)
        {
            setStringProperty(component, JSFAttr.IMAGE_LOCATION, _imageLocation);
        }
        if (_styleLocation != null)
        {
            setStringProperty(component, JSFAttr.STYLE_LOCATION, _styleLocation);
        }

        //rendererType already handled by UIComponentTag

        setValueProperty(component, _value);
        setConverterProperty(component, _converter);
    }

    /**
     * Sets the forceId attribute of the tag.  NOTE: Not every tag that extends this class will
     * actually make use of this attribute.  Check the TLD to see which components actually
     * implement it.
     *
     * @param aForceId The value of the forceId attribute.
     */
    public void setForceId(ValueExpression aForceId)
    {
        _forceId = aForceId;
    }

    /**
     * Sets the forceIdIndex attribute of the tag.  NOTE: Not every tag that extends this class will
     * actually make use of this attribute.  Check the TLD to see which components actually implement it.
     *
     * @param aForceIdIndex The value of the forceIdIndex attribute.
     */
    public void setForceIdIndex(ValueExpression aForceIdIndex)
    {
        _forceIdIndex = aForceIdIndex;
    }

    public void setValue(ValueExpression value)
    {
        _value = value;
    }

    public void setConverter(ValueExpression converter)
    {
        _converter = converter;
    }

    /**
     * Sets the javascript location attribute of the tag.  NOTE: Not every tag that extends this class will
     * actually make use of this attribute.  Check the TLD to see which components actually implement it.
     *
     * @param aJavascriptLocation The alternate javascript location to use.
     */
    public void setJavascriptLocation(ValueExpression aJavascriptLocation)
    {
        _javascriptLocation = aJavascriptLocation;
    }

    /**
     * Sets the image location attribute of the tag.  NOTE: Not every tag that extends this class will
     * actually make use of this attribute.  Check the TLD to see which components actually implement it.
     *
     * @param aImageLocation The alternate image location to use.
     */
    public void setImageLocation(ValueExpression aImageLocation)
    {
        _imageLocation = aImageLocation;
    }

    /**
     * Sets the style location attribute of the tag.  NOTE: Not every tag that extends this class will
     * actually make use of this attribute.  Check the TLD to see which components actually implement it.
     *
     * @param aStyleLocation The alternate style location to use.
     */
    public void setStyleLocation(ValueExpression aStyleLocation)
    {
        _styleLocation = aStyleLocation;
    }

    // sub class helpers

    protected void setIntegerProperty(UIComponent component, String propName,
            ValueExpression value)
    {
        UIComponentELTagUtils.setIntegerProperty(component, propName, value);
    }

    protected void setIntegerProperty(UIComponent component, String propName,
            ValueExpression value, Integer defaultValue)
    {
        UIComponentELTagUtils.setIntegerProperty(component, propName, value,
                defaultValue);
    }

    protected void setLongProperty(UIComponent component, String propName,
            ValueExpression value)
    {
        UIComponentELTagUtils.setLongProperty(component, propName, value);
    }

    protected void setLongProperty(UIComponent component, String propName,
            ValueExpression value, Long defaultValue)
    {
        UIComponentELTagUtils.setLongProperty(component, propName, value,
                defaultValue);
    }

    @Deprecated
    protected void setStringProperty(UIComponent component, String propName,
            String value)
    {
        UIComponentTagUtils.setStringProperty(getFacesContext(), component,
                propName, value);
    }

    protected void setStringProperty(UIComponent component, String propName,
            ValueExpression value)
    {
        UIComponentELTagUtils.setStringProperty(component, propName, value);
    }

    protected void setStringProperty(UIComponent component, String propName,
            ValueExpression value, String defaultValue)
    {
        UIComponentELTagUtils.setStringProperty(component, propName, value,
                defaultValue);
    }

    @Deprecated
    protected void setBooleanProperty(UIComponent component, String propName,
            String value)
    {
        UIComponentTagUtils.setBooleanProperty(getFacesContext(), component,
                propName, value);
    }

    protected void setBooleanProperty(UIComponent component, String propName,
            ValueExpression value)
    {
        UIComponentELTagUtils.setBooleanProperty(component, propName, value);
    }

    protected void setBooleanProperty(UIComponent component, String propName,
            ValueExpression value, Boolean defaultValue)
    {
        UIComponentELTagUtils.setBooleanProperty(component, propName, value,
                defaultValue);
    }

    private void setValueProperty(UIComponent component, ValueExpression value)
    {
        UIComponentELTagUtils.setValueProperty(getFacesContext(), component,
                value);
    }

    private void setConverterProperty(UIComponent component,
            ValueExpression value)
    {
        UIComponentELTagUtils.setConverterProperty(getFacesContext(),
                component, value);
    }

    protected void addValidatorProperty(UIComponent component,
            MethodExpression value)
    {
        UIComponentELTagUtils.addValidatorProperty(getFacesContext(),
                component, value);
    }

    @Deprecated
    protected void setActionProperty(UIComponent component, String action)
    {
        UIComponentTagUtils.setActionProperty(getFacesContext(), component,
                action);
    }

    protected void setActionProperty(UIComponent component,
            MethodExpression action)
    {
        UIComponentELTagUtils.setActionProperty(getFacesContext(), component,
                action);
    }

    @Deprecated
    protected void setActionListenerProperty(UIComponent component,
            String actionListener)
    {
        UIComponentTagUtils.setActionListenerProperty(getFacesContext(),
                component, actionListener);
    }

    protected void setActionListenerProperty(UIComponent component,
            MethodExpression actionListener)
    {
        UIComponentELTagUtils.addActionListenerProperty(getFacesContext(),
                component, actionListener);
    }

    protected void addValueChangedListenerProperty(UIComponent component,
            MethodExpression valueChangedListener)
    {
        UIComponentELTagUtils.addValueChangedListenerProperty(
                getFacesContext(), component, valueChangedListener);
    }

    protected void setValueBinding(UIComponent component, String propName,
            ValueExpression value)
    {
        UIComponentELTagUtils.setValueBinding(getFacesContext(), component,
                propName, value);
    }

    protected Object evaluateValueExpression(ValueExpression expression)
    {
        return UIComponentELTagUtils.evaluateValueExpression(getFacesContext()
                .getELContext(), expression);
    }

}
