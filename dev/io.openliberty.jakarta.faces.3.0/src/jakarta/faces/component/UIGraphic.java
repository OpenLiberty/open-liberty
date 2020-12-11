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

import jakarta.el.ValueExpression;
import jakarta.faces.el.ValueBinding;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Displays a graphical image.
 * <p>
 * See the javadoc for this class in the
 * <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 * for further details.
 */
@JSFComponent(defaultRendererType = "jakarta.faces.Image")
public class UIGraphic extends UIComponentBase
{
    public static final String COMPONENT_TYPE = "jakarta.faces.Graphic";
    public static final String COMPONENT_FAMILY = "jakarta.faces.Graphic";

    private static final String URL_PROPERTY = "url";
    private static final String VALUE_PROPERTY = "value";

    /**
     * Construct an instance of the UIGraphic.
     */
    public UIGraphic()
    {
        setRendererType("jakarta.faces.Image");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    /**
     * An alias for the "value" attribute.
     */
    @JSFProperty
    public String getUrl()
    {
        return (String) getValue();
    }

    public void setUrl(String url)
    {
        setValue(url);
    }

    /**
     * @deprecated Use getValueExpression instead
     */
    @Override
    public ValueBinding getValueBinding(String name)
    {
        if (URL_PROPERTY.equals(name)) 
        {
            return super.getValueBinding(VALUE_PROPERTY);
        } 
        else 
        {
            return super.getValueBinding(name);
        }
    }

    /**
     * @deprecated Use setValueExpression instead
     */
    @Override
    public void setValueBinding(String name, ValueBinding binding) 
    {
        if (URL_PROPERTY.equals(name)) 
        {
            super.setValueBinding(VALUE_PROPERTY, binding);
        } 
        else 
        {
            super.setValueBinding(name, binding);
        }
    }

    @Override
    public ValueExpression getValueExpression(String name)
    {
        if (URL_PROPERTY.equals(name))
        {
            return super.getValueExpression(VALUE_PROPERTY);
        }
        else
        {
            return super.getValueExpression(name);
        }
    }

    @Override
    public void setValueExpression(String name, ValueExpression binding)
    {
        if (URL_PROPERTY.equals(name))
        {
            super.setValueExpression(VALUE_PROPERTY, binding);
        }
        else
        {
            super.setValueExpression(name, binding);
        }
    }

    /**
     * The URL of the image.
     * <p>
     * If the URL starts with a '/', it is relative to the context path of the web application.
     * </p>
     */
    @JSFProperty
    public Object getValue()
    {
        return  getStateHelper().eval(PropertyKeys.value);
    }

    public void setValue(Object value)
    {
        getStateHelper().put(PropertyKeys.value, value );
    }
    
    enum PropertyKeys
    {
         value
    }
}
