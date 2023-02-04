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
package org.apache.myfaces.renderkit;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.el.PropertyNotFoundException;

import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.component.EditableValueHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.UISelectMany;
import jakarta.faces.component.UISelectOne;
import jakarta.faces.component.ValueHolder;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.model.SelectItem;
import org.apache.myfaces.core.api.shared.ComponentUtils;
import org.apache.myfaces.core.api.shared.SelectItemsIterator;
import org.apache.myfaces.core.api.shared.SharedRendererUtils;

import org.apache.myfaces.util.lang.HashMapUtils;
import org.apache.myfaces.core.api.shared.lang.Assert;
import org.apache.myfaces.renderkit.html.util.ComponentAttrs;

public final class RendererUtils
{

    private RendererUtils()
    {
        //nope
    }

    private static final Logger log = Logger.getLogger(RendererUtils.class.getName());

    public static final String EMPTY_STRING = "";

    public static final String SEQUENCE_PARAM = "jsf_sequence";

    // This nice constant is "specified" 13.1.1.2 The Resource API Approach in Spec as an example
    public static final String RES_NOT_FOUND = "RES_NOT_FOUND";

    public static Boolean getBooleanValue(UIComponent component)
    {
        Object value = getObjectValue(component);
        // Try to convert to Boolean if it is a String
        if (value instanceof String)
        {
            value = Boolean.valueOf((String) value);
        }

        if (value == null || value instanceof Boolean)
        {
            return (Boolean) value;
        }

        throw new IllegalArgumentException(
                "Expected submitted value of type Boolean for Component : "
                        + ComponentUtils.getPathToComponent(component));
    }

    public static Object getObjectValue(UIComponent component)
    {
        if (!(component instanceof ValueHolder))
        {
            throw new IllegalArgumentException("Component : "
                    + ComponentUtils.getPathToComponent(component) + " is not a ValueHolder");
        }

        if (component instanceof EditableValueHolder)
        {
            Object value = ((EditableValueHolder) component).getSubmittedValue();
            if (value != null)
            {
                return value;
            }
        }

        return ((ValueHolder) component).getValue();
    }

    public static String getStringValue(FacesContext context, ValueExpression ve)
    {
        Object value = ve.getValue(context.getELContext());
        if (value != null)
        {
            if (value instanceof String)
            {
                return (String) value;
            }
            
            return value.toString();
        }
        return null;
    }

    public static String getStringValue(FacesContext facesContext, UIComponent component)
    {
        if (!(component instanceof ValueHolder))
        {
            throw new IllegalArgumentException("Component : "
                    + ComponentUtils.getPathToComponent(component)
                    + " is not a ValueHolder");
        }

        if (component instanceof EditableValueHolder)
        {
            Object submittedValue = ((EditableValueHolder) component).getSubmittedValue();
            if (submittedValue != null)
            {
                if (submittedValue instanceof String)
                {
                    return (String) submittedValue;
                }
                
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("returning '" + submittedValue + '\'');
                }
                return submittedValue.toString();
            }
        }

        Object value;

        if (component instanceof EditableValueHolder)
        {
            EditableValueHolder holder = (EditableValueHolder) component;
            if (holder.isLocalValueSet())
            {
                value = holder.getLocalValue();
            }
            else
            {
                value = getValue(component);
            }
        }
        else
        {
            value = getValue(component);
        }

        Converter converter = ((ValueHolder) component).getConverter();
        if (converter == null && value != null)
        {
            try
            {
                converter = facesContext.getApplication().createConverter(value.getClass());
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("the created converter is " + converter);
                }
            }
            catch (FacesException e)
            {
                log.log(Level.SEVERE, "No converter for class "
                        + value.getClass().getName()
                        + " found (component id=" + component.getId()
                        + ").", e);
            }
        }

        if (converter == null)
        {
            if (value == null)
            {
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("returning an empty string");
                }
                return "";
            }

            if (value instanceof String)
            {
                return (String) value;
            }

            if (log.isLoggable(Level.FINE))
            {
                log.fine("returning an .toString");
            }
            return value.toString();

        }

        if (log.isLoggable(Level.FINE))
        {
            log.fine("returning converter get as string " + converter);
        }
        return converter.getAsString(facesContext, component, value);
    }

    public static String getStringFromSubmittedValueOrLocalValueReturnNull(FacesContext facesContext,
            UIComponent component)
    {
        try
        {
            if (!(component instanceof ValueHolder))
            {
                throw new IllegalArgumentException("Component : "
                        + ComponentUtils.getPathToComponent(component)
                        + "is not a ValueHolder");
            }

            if (component instanceof EditableValueHolder)
            {
                Object submittedValue = ((EditableValueHolder) component).getSubmittedValue();
                if (submittedValue != null)
                {
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine("returning 1 '" + submittedValue + '\'');
                    }
                    return submittedValue.toString();
                }
            }

            Object value;

            if (component instanceof EditableValueHolder)
            {
                EditableValueHolder holder = (EditableValueHolder) component;
                if (holder.isLocalValueSet())
                {
                    value = holder.getLocalValue();
                }
                else
                {
                    value = getValue(component);
                }
            }
            else
            {
                value = getValue(component);
            }

            Converter converter = ((ValueHolder) component).getConverter();
            if (converter == null && value != null)
            {
                try
                {
                    converter = facesContext.getApplication().createConverter(
                            value.getClass());
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine("the created converter is " + converter);
                    }
                }
                catch (FacesException e)
                {
                    log.log(Level.SEVERE, "No converter for class "
                            + value.getClass().getName()
                            + " found (component id=" + component.getId()
                            + ").", e);
                    // converter stays null
                }
            }

            if (converter == null)
            {
                if (value == null)
                {
                    return null;
                }
                
                if (value instanceof String)
                {
                    return (String) value;
                }

                if (log.isLoggable(Level.FINE))
                {
                    log.fine("returning an .toString");
                }
                return value.toString();

            }

            if (log.isLoggable(Level.FINE))
            {
                log.fine("returning converter get as string " + converter);
            }
            return converter.getAsString(facesContext, component, value);

        }
        catch (PropertyNotFoundException ex)
        {
            log.log(Level.SEVERE, "Property not found - called by component : "
                    + ComponentUtils.getPathToComponent(component), ex);

            throw ex;
        }
    }

    private static Object getValue(UIComponent component)
    {
        Object value = ((ValueHolder) component).getValue();
        return value;
    }

    /**
     * See Faces Spec. 8.5 Table 8-1
     * @param value
     * @return boolean
     */
    public static boolean isDefaultAttributeValue(Object value)
    {
        if (value == null)
        {
            return true;
        }
        else if (value instanceof Boolean)
        {
            return !((Boolean) value);
        }
        else if (value instanceof Number)
        {
            if (value instanceof Integer)
            {
                return ((Number) value).intValue() == Integer.MIN_VALUE;
            }
            else if (value instanceof Double)
            {
                return ((Number) value).doubleValue() == Double.MIN_VALUE;
            }
            else if (value instanceof Long)
            {
                return ((Number) value).longValue() == Long.MIN_VALUE;
            }
            else if (value instanceof Byte)
            {
                return ((Number) value).byteValue() == Byte.MIN_VALUE;
            }
            else if (value instanceof Float)
            {
                return ((Number) value).floatValue() == Float.MIN_VALUE;
            }
            else if (value instanceof Short)
            {
                return ((Number) value).shortValue() == Short.MIN_VALUE;
            }
        }
        return false;
    }

    /**
     * Find the proper Converter for the given UIOutput component.
     * @return the Converter or null if no Converter specified or needed
     * @throws FacesException if the Converter could not be created
     */
    public static Converter findUIOutputConverter(FacesContext facesContext,
            UIOutput component) throws FacesException
    {
        Converter converter = component.getConverter();
        if (converter != null)
        {
            return converter;
        }

        //Try to find out by value expression
        ValueExpression expression = component.getValueExpression("value");
        if (expression == null)
        {
            return null;
        }

        Class<?> valueType = expression.getType(facesContext.getELContext());
        if (valueType == null)
        {
            return null;
        }

        if (Object.class.equals(valueType))
        {
            return null; //There is no converter for Object class
        }

        try
        {
            return facesContext.getApplication().createConverter(valueType);
        }
        catch (FacesException e)
        {
            log.log(Level.SEVERE, "No Converter for type " + valueType.getName() + " found", e);
            return null;
        }
    }

    /**
     * Find proper Converter for the entries in the associated Collection or array of
     * the given UISelectMany as specified in API Doc of UISelectMany.
     * If considerValueType is true, the valueType attribute will be used
     * in addition to the standard algorithm to get a valid converter.
     * 
     * @return the Converter or null if no Converter specified or needed
     * @throws FacesException if the Converter could not be created
     */
    public static Converter findUISelectManyConverter(
            FacesContext facesContext, UISelectMany component,
            boolean considerValueType)
    {
        // If the component has an attached Converter, use it.
        Converter converter = component.getConverter();
        if (converter != null)
        {
            return converter;
        }

        if (considerValueType)
        {
            // try to get a converter from the valueType attribute
            converter = SharedRendererUtils.getValueTypeConverter(facesContext, component);
            if (converter != null)
            {
                return converter;
            }
        }

        //Try to find out by value expression
        ValueExpression ve = component.getValueExpression("value");
        if (ve == null)
        {
            return null;
        }

        // Try to get the type from the actual value or,
        // if value == null, obtain the type from the ValueExpression
        Class<?> valueType = null;
        Object value = ve.getValue(facesContext.getELContext());
        valueType = (value != null) ? value.getClass() : ve.getType(facesContext.getELContext());

        if (valueType == null)
        {
            return null;
        }

        // a valueType of Object is also permitted, in order to support
        // managed bean properties of type Object that resolve to null at this point
        if (Collection.class.isAssignableFrom(valueType) || Object.class.equals(valueType))
        {
            // try to get the by-type-converter from the type of the SelectItems
            return SharedRendererUtils.getSelectItemsValueConverter(new SelectItemsIterator(component, facesContext),
                    facesContext);
        }

        if (!valueType.isArray())
        {
            throw new IllegalArgumentException(
                    "ValueExpression for UISelectMany : "
                            + ComponentUtils.getPathToComponent(component)
                            + " must be of type Collection or Array");
        }

        Class<?> arrayComponentType = valueType.getComponentType();
        if (String.class.equals(arrayComponentType))
        {
            return null; //No converter needed for String type
        }

        if (Object.class.equals(arrayComponentType))
        {
            // There is no converter for Object class
            // try to get the by-type-converter from the type of the SelectItems
            return SharedRendererUtils.getSelectItemsValueConverter(new SelectItemsIterator(component, facesContext),
                    facesContext);
        }

        try
        {
            return facesContext.getApplication().createConverter(arrayComponentType);
        }
        catch (FacesException e)
        {
            log.log(Level.SEVERE,
                    "No Converter for type " + arrayComponentType.getName() + " found",
                    e);
            return null;
        }
    }

    public static void checkParamValidity(FacesContext facesContext, UIComponent uiComponent, Class compClass)
    {
        Assert.notNull(facesContext, "facesContext");
        Assert.notNull(uiComponent, "uiComponent");

        if (compClass != null && !(compClass.isInstance(uiComponent)))
        {
            throw new IllegalArgumentException("uiComponent : "
                    + ComponentUtils.getPathToComponent(uiComponent) + " is not instance of "
                    + compClass.getName() + " as it should be");
        }
    }

    public static void renderChildren(FacesContext facesContext, UIComponent component) throws IOException
    {
        int childCount = component.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                child.encodeAll(facesContext);
            }
        }
    }

    public static List getSelectItemList(UISelectOne uiSelectOne)
    {
        return internalGetSelectItemList(uiSelectOne, FacesContext.getCurrentInstance());
    }

    /**
     * @param uiSelectOne
     * @param facesContext
     * @return List of SelectItem Objects
     */
    public static List<SelectItem> getSelectItemList(UISelectOne uiSelectOne, FacesContext facesContext)
    {
        return internalGetSelectItemList(uiSelectOne, facesContext);
    }

    /**
     * @param uiSelectMany
     * @param facesContext
     * @return List of SelectItem Objects
     */
    public static List<SelectItem> getSelectItemList(UISelectMany uiSelectMany, FacesContext facesContext)
    {
        return internalGetSelectItemList(uiSelectMany, facesContext);
    }

    private static List<SelectItem> internalGetSelectItemList(UIComponent uiComponent, FacesContext facesContext)
    {
        List<SelectItem> list = new ArrayList<>();

        for (SelectItemsIterator iter = new SelectItemsIterator(uiComponent, facesContext); iter.hasNext();)
        {
            list.add(iter.next());
        }
        return list;
    }

    /**
     * Convenient utility method that returns the currently submitted values of
     * a UISelectMany component as a Set, of which the contains method can then be
     * easily used to determine if a select item is currently selected.
     * Calling the contains method of this Set with the renderable (String converted) item value
     * as argument returns true if this item is selected.
     * @param uiSelectMany
     * @return Set containing all currently selected values
     */
    public static Set getSubmittedValuesAsSet(FacesContext context,
            UIComponent component, Converter converter,
            UISelectMany uiSelectMany)
    {
        Object submittedValues = uiSelectMany.getSubmittedValue();
        if (submittedValues == null)
        {
            return null;
        }

        if (converter != null)
        {
            converter = new PassThroughAsStringConverter(converter);
        }

        return internalSubmittedOrSelectedValuesAsSet(context, component,
                converter, uiSelectMany, submittedValues, false);
    }

    /**
     * Convenient utility method that returns the currently selected values of
     * a UISelectMany component as a Set, of which the contains method can then be
     * easily used to determine if a value is currently selected.
     * Calling the contains method of this Set with the item value
     * as argument returns true if this item is selected.
     * @param uiSelectMany
     * @return Set containing all currently selected values
     */
    public static Set getSelectedValuesAsSet(FacesContext context,
            UIComponent component, Converter converter,
            UISelectMany uiSelectMany)
    {
        Object selectedValues = uiSelectMany.getValue();
        return internalSubmittedOrSelectedValuesAsSet(
                context, component, converter, uiSelectMany, selectedValues, true);
    }

    private static Set internalSubmittedOrSelectedValuesAsSet(
            FacesContext context, UIComponent component, Converter converter,
            UISelectMany uiSelectMany, Object values,
            boolean allowNonArrayOrCollectionValue)
    {
        if (values == null || EMPTY_STRING.equals(values))
        {
            return Collections.EMPTY_SET;
        }
        else if (values instanceof Object[])
        {
            //Object array
            Object[] ar = (Object[]) values;
            if (ar.length == 0)
            {
                return Collections.EMPTY_SET;
            }

            HashSet set = new HashSet(HashMapUtils.calcCapacity(ar.length));
            for (int i = 0; i < ar.length; i++)
            {
                set.add(SharedRendererUtils.getConvertedStringValue(context, component, converter,
                        ar[i]));
            }
            return set;
        }
        else if (values.getClass().isArray())
        {
            //primitive array
            int len = Array.getLength(values);
            HashSet set = new HashSet(HashMapUtils.calcCapacity(len));
            for (int i = 0; i < len; i++)
            {
                set.add(
                    SharedRendererUtils.getConvertedStringValue(context, component, converter, Array.get(values, i)));
            }
            return set;
        }
        else if (values instanceof Collection)
        {
            Collection col = (Collection) values;
            if (col.isEmpty())
            {
                return Collections.EMPTY_SET;
            }

            HashSet set = new HashSet(HashMapUtils.calcCapacity(col.size()));
            for (Iterator i = col.iterator(); i.hasNext();)
            {
                set.add(SharedRendererUtils.getConvertedStringValue(context, component, converter, i.next()));
            }

            return set;

        }
        else if (allowNonArrayOrCollectionValue)
        {
            HashSet set = new HashSet(HashMapUtils.calcCapacity(1));
            set.add(values);
            return set;
        }
        else
        {
            throw new IllegalArgumentException(
                    "Value of UISelectMany component with path : "
                            + ComponentUtils.getPathToComponent(uiSelectMany)
                            + " is not of type Array or List");
        }
    }

    public static Object getConvertedUISelectOneValue(
            FacesContext facesContext, UISelectOne output, Object submittedValue)
    {
        if (submittedValue != null && !(submittedValue instanceof String))
        {
            throw new IllegalArgumentException(
                    "Submitted value of type String for component : "
                            + ComponentUtils.getPathToComponent(output) + "expected");
        }

        //To be compatible with jsf ri, and according to issue 69
        //[  Permit the passing of a null value to SelectItem.setValue()  ]
        //If submittedValue == "" then convert to null.
        if (submittedValue != null && "".equals(submittedValue))
        {
            //Replace "" by null value
            submittedValue = null;
        }

        Converter converter;
        try
        {
            converter = findUIOutputConverter(facesContext, output);
        }
        catch (FacesException e)
        {
            throw new ConverterException(e);
        }

        return converter == null ? submittedValue : converter.getAsObject(
                facesContext, output, (String) submittedValue);
    }

    public static Object getConvertedUIOutputValue(FacesContext facesContext,
            UIOutput output, Object submittedValue) throws ConverterException
    {
        if (submittedValue != null && !(submittedValue instanceof String))
        {
            submittedValue = submittedValue.toString();
        }

        Converter converter;
        try
        {
            converter = findUIOutputConverter(facesContext, output);
        }
        catch (FacesException e)
        {
            throw new ConverterException(e);
        }

        return converter == null ? submittedValue : converter.getAsObject(
                facesContext, output, (String) submittedValue);
    }

    /**
     * Invokes getConvertedUISelectManyValue() with considerValueType = false, thus
     * implementing the standard behavior of the spec (valueType comes from Tomahawk).
     * 
     * @param facesContext
     * @param selectMany
     * @param submittedValue
     * @return
     * @throws ConverterException
     */
    public static Object getConvertedUISelectManyValue(FacesContext facesContext, UISelectMany selectMany,
            Object submittedValue) throws ConverterException
    {
        // do not consider the valueType attribute
        return getConvertedUISelectManyValue(facesContext, selectMany, submittedValue, false);
    }

    /**
     * Gets the converted value of a UISelectMany component.
     * 
     * @param facesContext
     * @param selectMany
     * @param submittedValue
     * @param considerValueType if true, the valueType attribute of the component will
     *                          also be used (applies for Tomahawk UISelectMany components)
     * @return
     * @throws ConverterException
     */
    public static Object getConvertedUISelectManyValue(
            FacesContext facesContext, UISelectMany selectMany,
            Object submittedValue, boolean considerValueType)
            throws ConverterException
    {
        if (submittedValue == null)
        {
            return null;
        }

        if (!(submittedValue instanceof String[]))
        {
            throw new ConverterException(
                    "Submitted value of type String[] for component : "
                            + ComponentUtils.getPathToComponent(selectMany) + "expected");
        }

        return SharedRendererUtils.getConvertedUISelectManyValue(facesContext,
                selectMany, (String[]) submittedValue, considerValueType);
    }

    public static boolean getBooleanValue(String attribute, Object value, boolean defaultValue)
    {
        if (value instanceof Boolean)
        {
            return ((Boolean) value);
        }
        else if (value instanceof String)
        {
            return Boolean.parseBoolean((String) value);
        }
        else if (value != null)
        {
            log.severe("value for attribute "
                    + attribute
                    + " must be instanceof 'Boolean' or 'String', is of type : "
                    + value.getClass());

            return defaultValue;
        }

        return defaultValue;
    }

    /**
      * Checks for name/library attributes on component and if they are avaliable,
      * creates {@link Resource} and returns it's path suitable for rendering.
      * If component doesn't have name/library gets value for attribute named <code>attributeName</code> 
      * returns it processed with {@link #toResourceUri(jakarta.faces.context.FacesContext, java.lang.Object)}
      *       
      * @param facesContext a {@link FacesContext}
      * @param component a {@link UIComponent}
      * @param attributeName name of attribute that represents "image", "icon", "source", ... 
      */
    public static String getIconSrc(final FacesContext facesContext,
            final UIComponent component, final String attributeName)
    {

        // Faces 2.0: if "name" attribute is available, treat as a resource reference.
        final Map<String, Object> attributes = component.getAttributes();
        final String resourceName = (String) attributes.get(ComponentAttrs.NAME_ATTR);
        if (resourceName != null && (resourceName.length() > 0))
        {

            final ResourceHandler resourceHandler = facesContext.getApplication().getResourceHandler();
            final Resource resource;

            final String libraryName = (String) component.getAttributes().get(ComponentAttrs.LIBRARY_ATTR);
            if ((libraryName != null) && (libraryName.length() > 0))
            {
                resource = resourceHandler.createResource(resourceName, libraryName);
            }
            else
            {
                resource = resourceHandler.createResource(resourceName);
            }

            if (resource == null)
            {
                // If resourceName/libraryName are set but no resource created -> probably a typo,
                // show a message
                if (facesContext.isProjectStage(ProjectStage.Development))
                {
                    String summary = "Unable to find resource: " + resourceName;
                    if (libraryName != null)
                    {
                        summary = summary + " from library: " + libraryName;
                    }
                    facesContext.addMessage(
                            component.getClientId(facesContext),
                            new FacesMessage(FacesMessage.SEVERITY_WARN,
                                    summary, summary));
                }

                return RES_NOT_FOUND;
            }
            else
            {
                return resource.getRequestPath();
            }
        }
        else
        {
            String value = (String) component.getAttributes().get(attributeName);
            return toResourceUri(facesContext, value);
        }
    }

    /**
     * Coerces an object into a resource URI, calling the view-handler.
     */
    static public String toResourceUri(FacesContext facesContext, Object o)
    {
        if (o == null)
        {
            return null;
        }

        String uri = o.toString();

        // *** EL Coercion problem ***
        // If icon or image attribute was declared with #{resource[]} and that expression
        // evaluates to null (it means ResourceHandler.createResource returns null because 
        // requested resource does not exist)
        // EL implementation turns null into ""
        // see http://www.irian.at/blog/blogid/unifiedElCoercion/#unifiedElCoercion
        if (uri.length() == 0)
        {
            return null;
        }

        // With Faces 2.0 url for resources can be done with EL like #{resource['resourcename']}
        // and such EL after evalution contains context path for the current web application already,
        // -> we dont want call viewHandler.getResourceURL()
        if (uri.contains(ResourceHandler.RESOURCE_IDENTIFIER))
        {
            return uri;
        }

        // Treat two slashes as server-relative
        if (uri.startsWith("//"))
        {
            return uri.substring(1);
        }
        else
        {
            // If the specified path starts with a "/",
            // following method will prefix it with the context path for the current web application,
            // and return the result
            String resourceURL = facesContext.getApplication().getViewHandler().getResourceURL(facesContext, uri);
            return facesContext.getExternalContext().encodeResourceURL(resourceURL);
        }
    }

    /**
     * Special converter for handling submitted values which don't need to be converted.
     */
    private static class PassThroughAsStringConverter implements Converter
    {
        private final Converter converter;

        public PassThroughAsStringConverter(Converter converter)
        {
            this.converter = converter;
        }

        @Override
        public Object getAsObject(FacesContext context, UIComponent component,
                String value) throws ConverterException
        {
            return converter.getAsObject(context, component, value);
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component,
                Object value) throws ConverterException
        {
            return (String) value;
        }

    }
}
