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
package org.apache.myfaces.shared.renderkit;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.ValueBinding;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.ResponseStateManager;

import org.apache.myfaces.shared.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared.util.HashMapUtils;
import org.apache.myfaces.shared.util.SelectItemsIterator;

public final class RendererUtils
{
    private RendererUtils()
    {
        //nope
    }

    //private static final Log log = LogFactory.getLog(RendererUtils.class);
    private static final Logger log = Logger.getLogger(RendererUtils.class
            .getName());

    public static final String SELECT_ITEM_LIST_ATTR = RendererUtils.class
            .getName() + ".LIST";
    public static final String EMPTY_STRING = "";
    //This constant is no longer used by UISelectOne/UISelectMany instances
    public static final Object NOTHING = new Serializable()
    {
        public boolean equals(final Object o)
        {
            if (o != null)
            {
                if (o.getClass().equals(this.getClass()))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }
    };

    public static final String ACTION_FOR_LIST = "org.apache.myfaces.ActionForList";
    public static final String ACTION_FOR_PHASE_LIST = "org.apache.myfaces.ActionForPhaseList";

    public static final String SEQUENCE_PARAM = "jsf_sequence";

    private static final String RENDER_KIT_IMPL = RendererUtils.class.getName()
            + ".RenderKitImpl";

    // This nice constant is "specified" 13.1.1.2 The Resource API Approach in Spec as an example
    public static final String RES_NOT_FOUND = "RES_NOT_FOUND";

    public static String getPathToComponent(UIComponent component)
    {
        StringBuilder buf = new StringBuilder();

        if (component == null)
        {
            buf.append("{Component-Path : ");
            buf.append("[null]}");
            return buf.toString();
        }

        getPathToComponent(component, buf);

        buf.insert(0, "{Component-Path : ");
        Object location = component.getAttributes().get(
                UIComponent.VIEW_LOCATION_KEY);
        if (location != null)
        {
            buf.append(" Location: ").append(location);
        }
        buf.append("}");

        return buf.toString();
    }

    private static void getPathToComponent(UIComponent component,
            StringBuilder buf)
    {
        if (component == null)
        {
            return;
        }

        StringBuilder intBuf = new StringBuilder();

        intBuf.append("[Class: ");
        intBuf.append(component.getClass().getName());
        if (component instanceof UIViewRoot)
        {
            intBuf.append(",ViewId: ");
            intBuf.append(((UIViewRoot) component).getViewId());
        }
        else
        {
            intBuf.append(",Id: ");
            intBuf.append(component.getId());
        }
        intBuf.append("]");

        buf.insert(0, intBuf.toString());

        getPathToComponent(component.getParent(), buf);
    }

    public static String getConcatenatedId(FacesContext context,
            UIComponent container, String clientId)
    {
        UIComponent child = container.findComponent(clientId);

        if (child == null)
        {
            return clientId;
        }

        return getConcatenatedId(context, child);
    }

    public static String getConcatenatedId(FacesContext context,
            UIComponent component)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        StringBuilder idBuf = new StringBuilder();

        idBuf.append(component.getId());

        UIComponent parent;

        while ((parent = component.getParent()) != null)
        {
            if (parent instanceof NamingContainer)
            {
                idBuf.insert(0, context.getNamingContainerSeparatorChar());
                idBuf.insert(0, parent.getId());
            }
        }

        return idBuf.toString();
    }

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
                        + getPathToComponent(component));

    }

    public static Date getDateValue(UIComponent component)
    {
        Object value = getObjectValue(component);
        if (value == null || value instanceof Date)
        {
            return (Date) value;
        }

        throw new IllegalArgumentException(
                "Expected submitted value of type Date for component : "
                        + getPathToComponent(component));
    }

    public static Object getObjectValue(UIComponent component)
    {
        if (!(component instanceof ValueHolder))
        {
            throw new IllegalArgumentException("Component : "
                    + getPathToComponent(component) + "is not a ValueHolder");
        }

        if (component instanceof EditableValueHolder)
        {
            Object value = ((EditableValueHolder) component)
                    .getSubmittedValue();
            if (value != null)
            {
                return value;
            }
        }

        return ((ValueHolder) component).getValue();
    }

    @Deprecated
    public static String getStringValue(FacesContext context, ValueBinding vb)
    {
        Object value = vb.getValue(context);
        if (value != null)
        {
            return value.toString();
        }
        return null;
    }

    public static String getStringValue(FacesContext context, ValueExpression ve)
    {
        Object value = ve.getValue(context.getELContext());
        if (value != null)
        {
            return value.toString();
        }
        return null;
    }

    public static String getStringValue(FacesContext facesContext,
            UIComponent component)
    {
        if (!(component instanceof ValueHolder))
        {
            throw new IllegalArgumentException("Component : "
                    + getPathToComponent(component)
                    + "is not a ValueHolder");
        }

        if (component instanceof EditableValueHolder)
        {
            Object submittedValue = ((EditableValueHolder) component)
                    .getSubmittedValue();
            if (submittedValue != null)
            {
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("returning 1 '" + submittedValue + "'");
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
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("returning an empty string");
                }
                return "";
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

    public static String getStringFromSubmittedValueOrLocalValueReturnNull(
            FacesContext facesContext, UIComponent component)
    {
        try
        {
            if (!(component instanceof ValueHolder))
            {
                throw new IllegalArgumentException("Component : "
                        + getPathToComponent(component)
                        + "is not a ValueHolder");
            }

            if (component instanceof EditableValueHolder)
            {
                Object submittedValue = ((EditableValueHolder) component)
                        .getSubmittedValue();
                if (submittedValue != null)
                {
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine("returning 1 '" + submittedValue + "'");
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
                    //if (log.isLoggable(Level.FINE))
                    //    log.fine("returning an empty string");
                    return null;
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
                    + getPathToComponent(component), ex);

            throw ex;
        }
    }

    private static Object getValue(UIComponent component)
    {
        Object value = ((ValueHolder) component).getValue();
        return value;
    }

    /**
     * See JSF Spec. 8.5 Table 8-1
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
            return !((Boolean) value).booleanValue();
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
        return _SharedRendererUtils.findUIOutputConverter(facesContext,
                component);
    }

    /**
     * Calls findUISelectManyConverter with considerValueType = false.
     * @param facesContext
     * @param component
     * @return
     */
    public static Converter findUISelectManyConverter(
            FacesContext facesContext, UISelectMany component)
    {
        return findUISelectManyConverter(facesContext, component, false);
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
            converter = _SharedRendererUtils.getValueTypeConverter(
                    facesContext, component);
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
        valueType = (value != null) ? value.getClass() : ve
                .getType(facesContext.getELContext());

        if (valueType == null)
        {
            return null;
        }

        // a valueType of Object is also permitted, in order to support
        // managed bean properties of type Object that resolve to null at this point
        if (Collection.class.isAssignableFrom(valueType)
                || Object.class.equals(valueType))
        {
            // try to get the by-type-converter from the type of the SelectItems
            return _SharedRendererUtils.getSelectItemsValueConverter(
                    new SelectItemsIterator(component, facesContext),
                    facesContext);
        }

        if (!valueType.isArray())
        {
            throw new IllegalArgumentException(
                    "ValueExpression for UISelectMany : "
                            + getPathToComponent(component)
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
            return _SharedRendererUtils.getSelectItemsValueConverter(
                    new SelectItemsIterator(component, facesContext),
                    facesContext);
        }

        try
        {
            return facesContext.getApplication().createConverter(
                    arrayComponentType);
        }
        catch (FacesException e)
        {
            log.log(Level.SEVERE,
                    "No Converter for type " + arrayComponentType.getName()
                            + " found", e);
            return null;
        }
    }

    public static void checkParamValidity(FacesContext facesContext,
            UIComponent uiComponent, Class compClass)
    {
        if (facesContext == null)
        {
            throw new NullPointerException("facesContext may not be null");
        }
        if (uiComponent == null)
        {
            throw new NullPointerException("uiComponent may not be null");
        }

        //if (compClass != null && !(compClass.isAssignableFrom(uiComponent.getClass())))
        // why isAssignableFrom with additional getClass method call if isInstance does the same?
        if (compClass != null && !(compClass.isInstance(uiComponent)))
        {
            throw new IllegalArgumentException("uiComponent : "
                    + getPathToComponent(uiComponent) + " is not instance of "
                    + compClass.getName() + " as it should be");
        }
    }

    public static void renderChildren(FacesContext facesContext,
            UIComponent component) throws IOException
    {
        if (component.getChildCount() > 0)
        {
            for (int i = 0; i < component.getChildCount(); i++)
            {
                UIComponent child = component.getChildren().get(i);
                //renderChild(facesContext, child);
                child.encodeAll(facesContext);
            }
        }
    }

    /**
     * 
     * @param facesContext
     * @param child
     * @throws IOException
     * @deprecated use UIComponent.encodeAll() instead
     */
    @Deprecated
    public static void renderChild(FacesContext facesContext, UIComponent child)
            throws IOException
    {
        // The next isRendered() call is only shortcut:
        // methods encodeBegin, encodeChildren and encodeEnd should proceed only if 
        // "If our rendered property is true, render the (beginning, child, ending) of this component"
        if (!isRendered(facesContext, child))
        {
            return;
        }

        child.encodeBegin(facesContext);
        if (child.getRendersChildren())
        {
            child.encodeChildren(facesContext);
        }
        else
        {
            renderChildren(facesContext, child);
        }
        child.encodeEnd(facesContext);
    }

    /**
     * Call {@link #pushComponentToEL(javax.faces.context.FacesContext,javax.faces.component.UIComponent)}, 
     * reads the isRendered property, call {@link
     * UIComponent#popComponentFromEL} and returns the value of isRendered.
     */
    public static boolean isRendered(FacesContext facesContext,
            UIComponent uiComponent)
    {
        // We must call pushComponentToEL here because ValueExpression may have 
        // implicit object "component" used. 
        try
        {
            uiComponent.pushComponentToEL(facesContext, uiComponent);
            return uiComponent.isRendered();
        }
        finally
        {
            uiComponent.popComponentFromEL(facesContext);
        }
    }

    public static List getSelectItemList(UISelectOne uiSelectOne)
    {
        return internalGetSelectItemList(uiSelectOne,
                FacesContext.getCurrentInstance());
    }

    /**
     * @param uiSelectOne
     * @param facesContext
     * @return List of SelectItem Objects
     */
    public static List getSelectItemList(UISelectOne uiSelectOne,
            FacesContext facesContext)
    {
        return internalGetSelectItemList(uiSelectOne, facesContext);
    }

    public static List getSelectItemList(UISelectMany uiSelectMany)
    {
        return internalGetSelectItemList(uiSelectMany,
                FacesContext.getCurrentInstance());
    }

    /**
     * @param uiSelectMany
     * @param facesContext
     * @return List of SelectItem Objects
     */
    public static List getSelectItemList(UISelectMany uiSelectMany,
            FacesContext facesContext)
    {
        return internalGetSelectItemList(uiSelectMany, facesContext);
    }

    private static List internalGetSelectItemList(UIComponent uiComponent,
            FacesContext facesContext)
    {
        /* TODO: Shall we cache the list in a component attribute?
        ArrayList list = (ArrayList)uiComponent.getAttributes().get(SELECT_ITEM_LIST_ATTR);
        if (list != null)
        {
            return list;
        }
         */

        List list = new ArrayList();

        for (Iterator iter = new SelectItemsIterator(uiComponent, facesContext); iter
                .hasNext();)
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

        return internalSubmittedOrSelectedValuesAsSet(context, component,
                converter, uiSelectMany, selectedValues, true);
    }

    /**
     * Convenient utility method that returns the currently given value as String,
     * using the given converter.
     * Especially usefull for dealing with primitive types.
     */
    public static String getConvertedStringValue(FacesContext context,
            UIComponent component, Converter converter, Object value)
    {
        if (converter == null)
        {
            if (value == null)
            {
                return "";
            }
            else if (value instanceof String)
            {
                return (String) value;
            }
            else
            {
                return value.toString();
            }
        }

        return converter.getAsString(context, component, value);
    }

    /**
     * Convenient utility method that returns the currently given SelectItem value
     * as String, using the given converter.
     * Especially usefull for dealing with primitive types.
     */
    public static String getConvertedStringValue(FacesContext context,
            UIComponent component, Converter converter, SelectItem selectItem)
    {
        return getConvertedStringValue(context, component, converter,
                selectItem.getValue());
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
                set.add(getConvertedStringValue(context, component, converter,
                        ar[i]));
            }
            return set;
        }
        else if (values.getClass().isArray())
        {
            //primitive array
            int len = Array.getLength(values);
            HashSet set = new HashSet(
                    org.apache.myfaces.shared.util.HashMapUtils
                            .calcCapacity(len));
            for (int i = 0; i < len; i++)
            {
                set.add(getConvertedStringValue(context, component, converter,
                        Array.get(values, i)));
            }
            return set;
        }
        else if (values instanceof Collection)
        {
            Collection col = (Collection) values;
            if (col.size() == 0)
            {
                return Collections.EMPTY_SET;
            }

            HashSet set = new HashSet(HashMapUtils.calcCapacity(col.size()));
            for (Iterator i = col.iterator(); i.hasNext();)
            {
                set.add(getConvertedStringValue(context, component, converter,
                        i.next()));
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
                            + getPathToComponent(uiSelectMany)
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
                            + getPathToComponent(output) + "expected");
        }

        //To be compatible with jsf ri, and according to issue 69
        //[  Permit the passing of a null value to SelectItem.setValue()  ]
        //If submittedValue == "" then convert to null.
        if ((submittedValue != null)
                && ("".equals(submittedValue)))
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
    public static Object getConvertedUISelectManyValue(
            FacesContext facesContext, UISelectMany selectMany,
            Object submittedValue) throws ConverterException
    {
        // do not consider the valueType attribute
        return getConvertedUISelectManyValue(facesContext, selectMany,
                submittedValue, false);
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
                            + getPathToComponent(selectMany) + "expected");
        }

        return _SharedRendererUtils.getConvertedUISelectManyValue(facesContext,
                selectMany, (String[]) submittedValue, considerValueType);
    }

    public static boolean getBooleanAttribute(UIComponent component,
            String attrName, boolean defaultValue)
    {
        Boolean b = (Boolean) component.getAttributes().get(attrName);
        return b != null ? b.booleanValue() : defaultValue;
    }

    public static int getIntegerAttribute(UIComponent component,
            String attrName, int defaultValue)
    {
        Integer i = (Integer) component.getAttributes().get(attrName);
        return i != null ? i.intValue() : defaultValue;
    }

    private static final String TRINIDAD_FORM_COMPONENT_FAMILY = "org.apache.myfaces.trinidad.Form";
    private static final String ADF_FORM_COMPONENT_FAMILY = "oracle.adf.Form";

    /**
     * Find the enclosing form of a component
     * in the view-tree.
     * All Subclasses of <code>UIForm</code> and all known
     * form-families are searched for.
     * Currently those are the Trinidad form family,
     * and the (old) ADF Faces form family.
     * <p/>
     * There might be additional form families
     * which have to be explicitly entered here.
     *
     * @param uiComponent
     * @param facesContext
     * @return FormInfo Information about the form - the form itself and its name.
     */
    public static FormInfo findNestingForm(UIComponent uiComponent,
            FacesContext facesContext)
    {
        UIComponent parent = uiComponent.getParent();
        while (parent != null
                && (!ADF_FORM_COMPONENT_FAMILY.equals(parent.getFamily())
                        && !TRINIDAD_FORM_COMPONENT_FAMILY.equals(parent
                                .getFamily()) && !(parent instanceof UIForm)))
        {
            parent = parent.getParent();
        }

        if (parent != null)
        {
            //link is nested inside a form
            String formName = parent.getClientId(facesContext);
            return new FormInfo(parent, formName);
        }

        return null;
    }

    public static boolean getBooleanValue(String attribute, Object value,
            boolean defaultValue)
    {
        if (value instanceof Boolean)
        {
            return ((Boolean) value).booleanValue();
        }
        else if (value instanceof String)
        {
            return Boolean.valueOf((String) value).booleanValue();
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

    public static void copyHtmlInputTextAttributes(HtmlInputText src,
            HtmlInputText dest)
    {
        dest.setId(src.getId());
        boolean forceId = getBooleanValue(JSFAttr.FORCE_ID_ATTR, src
                .getAttributes().get(JSFAttr.FORCE_ID_ATTR), false);
        if (forceId)
        {
            dest.getAttributes().put(JSFAttr.FORCE_ID_ATTR, Boolean.TRUE);
        }
        dest.setImmediate(src.isImmediate());
        dest.setTransient(src.isTransient());
        dest.setAccesskey(src.getAccesskey());
        dest.setAlt(src.getAlt());
        dest.setConverter(src.getConverter());
        dest.setDir(src.getDir());
        dest.setDisabled(src.isDisabled());
        dest.setLang(src.getLang());
        dest.setLocalValueSet(src.isLocalValueSet());
        dest.setMaxlength(src.getMaxlength());
        dest.setOnblur(src.getOnblur());
        dest.setOnchange(src.getOnchange());
        dest.setOnclick(src.getOnclick());
        dest.setOndblclick(src.getOndblclick());
        dest.setOnfocus(src.getOnfocus());
        dest.setOnkeydown(src.getOnkeydown());
        dest.setOnkeypress(src.getOnkeypress());
        dest.setOnkeyup(src.getOnkeyup());
        dest.setOnmousedown(src.getOnmousedown());
        dest.setOnmousemove(src.getOnmousemove());
        dest.setOnmouseout(src.getOnmouseout());
        dest.setOnmouseover(src.getOnmouseover());
        dest.setOnmouseup(src.getOnmouseup());
        dest.setOnselect(src.getOnselect());
        dest.setReadonly(src.isReadonly());
        dest.setRendered(src.isRendered());
        dest.setRequired(src.isRequired());
        dest.setSize(src.getSize());
        dest.setStyle(src.getStyle());
        dest.setStyleClass(src.getStyleClass());
        dest.setTabindex(src.getTabindex());
        dest.setTitle(src.getTitle());
        dest.setValidator(src.getValidator());
    }

    public static UIComponent findComponent(UIComponent headerComp, Class clazz)
    {
        if (clazz.isAssignableFrom(headerComp.getClass()))
        {
            return headerComp;
        }

        List li = headerComp.getChildren();

        for (int i = 0; i < li.size(); i++)
        {
            UIComponent comp = (UIComponent) li.get(i);

            //recursively iterate through children to find the component
            UIComponent lookupComp = findComponent(comp, clazz);

            if (lookupComp != null)
            {
                return lookupComp;
            }
        }

        return null;
    }

    public static void addOrReplaceChild(UIInput component, UIComponent child)
    {
        List li = component.getChildren();

        for (int i = 0; i < li.size(); i++)
        {
            UIComponent oldChild = (UIComponent) li.get(i);

            if (oldChild.getId() != null
                    && oldChild.getId().equals(child.getId()))
            {
                li.set(i, child);
                return;
            }
        }

        component.getChildren().add(child);
    }

    public static String getClientId(FacesContext facesContext,
            UIComponent uiComponent, String forAttr)
    {
        UIComponent forComponent = uiComponent.findComponent(forAttr);
        if (forComponent == null)
        {
            final char separatorChar = facesContext.getNamingContainerSeparatorChar();
            
            Level level = Level.WARNING;
            boolean productionStage = facesContext.isProjectStage(ProjectStage.Production);
            if (productionStage)
            {
                level = Level.FINE;
            }
            if (log.isLoggable(level))
            {
                StringBuilder sb = new StringBuilder();
                sb.append("Unable to find component '");
                sb.append(forAttr);
                sb.append("' (calling findComponent on component '");
                sb.append(uiComponent.getClientId(facesContext));
                sb.append("'");
                if (!productionStage)
                {
                    sb.append(", viewLocation: ");
                    sb.append(uiComponent.getAttributes().get(UIComponent.VIEW_LOCATION_KEY));
                }
                sb.append(").");
                sb.append(" We'll try to return a guessed client-id anyways -");
                sb.append(" this will be a problem if you put the referenced component");
                sb.append(" into a different naming-container. If this is the case, ");
                sb.append("you can always use the full client-id.");
                log.info(sb.toString());
            }
            if (forAttr.length() > 0 && forAttr.charAt(0) == separatorChar)
            {
                //absolute id path
                return forAttr.substring(1);
            }

            //relative id path, we assume a component on the same level as the label component
            String labelClientId = uiComponent.getClientId(facesContext);
            int colon = labelClientId.lastIndexOf(separatorChar);

            return colon == -1 ? forAttr : labelClientId
                    .substring(0, colon + 1) + forAttr;

        }

        return forComponent.getClientId(facesContext);

    }

    public static List convertIdsToClientIds(String actionFor,
            FacesContext facesContext, UIComponent component)
    {
        List li = new ArrayList();

        String[] ids = actionFor.split(",");

        for (int i = 0; i < ids.length; i++)
        {
            String trimedId = ids[i].trim();
            if (trimedId.equals("none"))
            {
                li.add(trimedId);
            }
            else
            {
                li.add(RendererUtils.getClientId(facesContext, component,
                        trimedId));
            }
        }
        return li;
    }

    public static List convertPhasesToPhasesIds(String actionForPhase)
    {
        List li = new ArrayList();

        if (actionForPhase == null)
        {
            return li;
        }

        String[] ids = actionForPhase.split(",");

        for (int i = 0; i < ids.length; i++)
        {
            if (ids[i].equals("PROCESS_VALIDATIONS"))
            {
                li.add(PhaseId.PROCESS_VALIDATIONS);
            }
            else if (ids[i].equals("UPDATE_MODEL_VALUES"))
            {
                li.add(PhaseId.UPDATE_MODEL_VALUES);
            }
        }
        return li;
    }

    /**
     * Helper method which loads a resource file (such as css) by a given context path and a file name.
     * Useful to provide css files (or js files) inline.
     * 
     * @param ctx <code>FacesContext</code> object to calculate the context path of the web application.
     * @param file name of the resource file (e.g. <code>foo.css</code>).
     * @return the content of the resource file, or <code>null</code> if no such file is available.
     */
    public static String loadResourceFile(FacesContext ctx, String file)
    {

        ByteArrayOutputStream content = new ByteArrayOutputStream(10240);

        InputStream in = null;
        try
        {
            in = ctx.getExternalContext().getResourceAsStream(file);
            if (in == null)
            {
                return null;
            }

            byte[] fileBuffer = new byte[10240];
            int read;
            while ((read = in.read(fileBuffer)) > -1)
            {
                content.write(fileBuffer, 0, read);
            }
        }
        catch (FileNotFoundException e)
        {
            if (log.isLoggable(Level.WARNING))
            {
                log.log(Level.WARNING, "no such file " + file, e);
            }
            content = null;
        }
        catch (IOException e)
        {
            if (log.isLoggable(Level.WARNING))
            {
                log.log(Level.WARNING, "problems during processing resource "
                        + file, e);
            }
            content = null;
        }
        finally
        {
            try
            {
                if (content != null)
                {
                    content.close();
                }
            }
            catch (IOException e)
            {
                log.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    log.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }
        }

        return content != null ? content.toString() : null;
    }

    /**
     * check for partial validation or model update attributes being set
     * and initialize the request-map accordingly.
     * SubForms will work with this information.
     */
    public static void initPartialValidationAndModelUpdate(
            UIComponent component, FacesContext facesContext)
    {
        String actionFor = (String) component.getAttributes().get("actionFor");

        if (actionFor != null)
        {
            List li = convertIdsToClientIds(actionFor, facesContext, component);

            facesContext.getExternalContext().getRequestMap()
                    .put(ACTION_FOR_LIST, li);

            String actionForPhase = (String) component.getAttributes().get(
                    "actionForPhase");

            if (actionForPhase != null)
            {
                List phaseList = convertPhasesToPhasesIds(actionForPhase);

                facesContext.getExternalContext().getRequestMap()
                        .put(ACTION_FOR_PHASE_LIST, phaseList);
            }
        }
    }

    public static boolean isAdfOrTrinidadForm(UIComponent component)
    {
        if (component == null)
        {
            return false;
        }
        return ADF_FORM_COMPONENT_FAMILY.equals(component.getFamily())
                || TRINIDAD_FORM_COMPONENT_FAMILY.equals(component.getFamily());
    }

    /**
     * Gets the ResponseStateManager for the renderKit Id provided
     * 
     * @deprecated use FacesContext.getRenderKit() or getRenderKitFactory().getRenderKit(
     *               context, renderKitId).getResponseStateManager()
     */
    @Deprecated
    public static ResponseStateManager getResponseStateManager(
            FacesContext facesContext, String renderKitId)
            throws FacesException
    {
        RenderKit renderKit = facesContext.getRenderKit();

        if (renderKit == null)
        {
            // look for the renderkit in the request
            Map attributesMap = facesContext.getAttributes();
            RenderKitFactory factory = (RenderKitFactory) attributesMap
                    .get(RENDER_KIT_IMPL);

            if (factory != null)
            {
                renderKit = factory.getRenderKit(facesContext, renderKitId);
            }
            else
            {
                factory = (RenderKitFactory) FactoryFinder
                        .getFactory(FactoryFinder.RENDER_KIT_FACTORY);

                if (factory == null)
                {
                    throw new IllegalStateException("Factory is null");
                }

                attributesMap.put(RENDER_KIT_IMPL, factory);

                renderKit = factory.getRenderKit(facesContext, renderKitId);
            }
        }

        if (renderKit == null)
        {
            throw new IllegalArgumentException(
                    "Could not find a RenderKit for \"" + renderKitId + "\"");
        }

        return renderKit.getResponseStateManager();
    }

    /**
      * Checks for name/library attributes on component and if they are avaliable,
      * creates {@link Resource} and returns it's path suitable for rendering.
      * If component doesn't have name/library gets value for attribute named <code>attributeName</code> 
      * returns it processed with {@link CoreRenderer#toResourceUri(FacesContext, Object)}
      *       
      * @param facesContext a {@link FacesContext}
      * @param component a {@link UIComponent}
      * @param attributeName name of attribute that represents "image", "icon", "source", ... 
      * 
      * @since 4.0.1
      */
    public static String getIconSrc(final FacesContext facesContext,
            final UIComponent component, final String attributeName)
    {

        // JSF 2.0: if "name" attribute is available, treat as a resource reference.
        final Map<String, Object> attributes = component.getAttributes();
        final String resourceName = (String) attributes.get(JSFAttr.NAME_ATTR);
        if (resourceName != null && (resourceName.length() > 0))
        {

            final ResourceHandler resourceHandler = facesContext
                    .getApplication().getResourceHandler();
            final Resource resource;

            final String libraryName = (String) component.getAttributes().get(
                    JSFAttr.LIBRARY_ATTR);
            if ((libraryName != null) && (libraryName.length() > 0))
            {
                resource = resourceHandler.createResource(resourceName,
                        libraryName);
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
            String value = (String) component.getAttributes()
                    .get(attributeName);
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

        // With JSF 2.0 url for resources can be done with EL like #{resource['resourcename']}
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
            String resourceURL = facesContext.getApplication().getViewHandler()
                    .getResourceURL(facesContext, uri);
            return facesContext.getExternalContext().encodeResourceURL(
                    resourceURL);
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

        public Object getAsObject(FacesContext context, UIComponent component,
                String value) throws ConverterException
        {
            return converter.getAsObject(context, component, value);
        }

        public String getAsString(FacesContext context, UIComponent component,
                Object value) throws ConverterException
        {
            return (String) value;
        }

    }
}
