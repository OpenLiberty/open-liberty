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

package javax.faces.convert;

import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFConverter;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

/**
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
@JSFConverter
public class EnumConverter implements Converter, PartialStateHolder
{

    public static final String CONVERTER_ID = "javax.faces.Enum";
    public static final String ENUM_ID = "javax.faces.converter.EnumConverter.ENUM";
    public static final String ENUM_NO_CLASS_ID = "javax.faces.converter.EnumConverter.ENUM_NO_CLASS";

    /**
     * If value is a String instance and this param is true, pass it directly without try any change.
     * 
     * See MYFACES-2739 for details.
     */
    @JSFWebConfigParam(name="org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH", since="2.0.1",
                       expectedValues="true,false",defaultValue="false", group="validation")
    private static final String ALLOW_STRING_PASSTROUGH = "org.apache.myfaces.ENUM_CONVERTER_ALLOW_STRING_PASSTROUGH";
    
    // TODO: Find a valid generic usage -= Simon Lessard =-
    private Class targetClass;

    private boolean isTransient = false;

    /** Creates a new instance of EnumConverter */
    public EnumConverter()
    {
    }

    public EnumConverter(Class targetClass)
    {
        if (!targetClass.isEnum())
        {
            throw new IllegalArgumentException("targetClass for EnumConverter must be an Enum");
        }
        this.targetClass = targetClass;
    }

    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object value)
        throws ConverterException
    {
        if (facesContext == null)
        {
            throw new NullPointerException("facesContext can not be null");
        }
        if (uiComponent == null)
        {
            throw new NullPointerException("uiComponent can not be null");
        }

        checkTargetClass(facesContext, uiComponent, value);

        if (value == null)
        {
            return "";
        }
        
        if (value instanceof String
                && _isPassThroughStringValues(facesContext))
        {
            // pass through the String value
            return (String) value;
        }

        // check if the value is an instance of the enum class
        if (targetClass.isInstance(value))
        {
            return ((Enum<?>) value).name();
        }
        
        Object[] params =
            new Object[] { value, firstConstantOfEnum(), _MessageUtils.getLabel(facesContext, uiComponent) };

        throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, ENUM_ID, params));
    }

    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value)
        throws ConverterException
    {
        if (facesContext == null)
        {
            throw new NullPointerException("facesContext");
        }
        if (uiComponent == null)
        {
            throw new NullPointerException("uiComponent");
        }
        if (value == null)
        {
            return null;
        }
        value = value.trim();
        if (value.length() == 0)
        {
            return null;
        }
        checkTargetClass(facesContext, uiComponent, value);

        // we know targetClass and value can't be null, so we can use Enum.valueOf
        // instead of the hokey looping called for in the javadoc
        try
        {
            return Enum.valueOf(targetClass, value);
        }
        catch (IllegalArgumentException e)
        {
            Object[] params =
                    new Object[] { value, firstConstantOfEnum(), _MessageUtils.getLabel(facesContext, uiComponent) };

            throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, ENUM_ID, params));
        }
    }

    private void checkTargetClass(FacesContext facesContext, UIComponent uiComponent, Object value)
    {
        if (targetClass == null)
        {
            Object[] params = new Object[] { value, _MessageUtils.getLabel(facesContext, uiComponent) };
            throw new ConverterException(_MessageUtils.getErrorMessage(facesContext, ENUM_NO_CLASS_ID, params));
        }
    }

    // find the first constant value of the targetClass and return as a String
    private String firstConstantOfEnum()
    {
        Object[] enumConstants = targetClass.getEnumConstants();

        if (enumConstants.length != 0)
        {
            return enumConstants[0].toString();
        }

        return ""; // if empty Enum
    }

    public void restoreState(FacesContext context, Object state)
    {
        if (state != null)
        {
            targetClass = (Class<?>)state;
        }
    }

    public Object saveState(FacesContext context)
    {
        if (!initialStateMarked())
        {
            return targetClass;
        }
        return null;
    }

    public void setTransient(boolean newTransientValue)
    {
        isTransient = newTransientValue;
    }

    public boolean isTransient()
    {
        return isTransient;
    }
    
    private boolean _initialStateMarked = false;

    public void clearInitialState()
    {
        _initialStateMarked = false;
    }

    public boolean initialStateMarked()
    {
        return _initialStateMarked;
    }

    public void markInitialState()
    {
        _initialStateMarked = true;
    }
    
    private boolean _isPassThroughStringValues(FacesContext facesContext)
    {
        String param = facesContext.getExternalContext().getInitParameter(ALLOW_STRING_PASSTROUGH);
        if (param != null)
        {
            return param.trim().equalsIgnoreCase("true");
        }
        // default: false
        return false;
    }

}
