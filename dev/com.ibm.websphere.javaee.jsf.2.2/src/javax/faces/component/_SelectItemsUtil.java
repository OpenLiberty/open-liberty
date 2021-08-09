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
package javax.faces.component;

import java.util.Arrays;
import java.util.Iterator;

import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

class _SelectItemsUtil
{

    /**
     * @param context the faces context
     * @param uiComponent the component instance
     * @param value the value to check
     * @param converter a converter instance
     * @param iterator contains instances of SelectItem
     * @return if the value of a selectitem is equal to the given value
     */
    public static boolean matchValue(FacesContext context,
            UIComponent uiComponent, Object value,
            Iterator<SelectItem> selectItemsIter, Converter converter)
    {
        while (selectItemsIter.hasNext())
        {
            SelectItem item = selectItemsIter.next();
            if (item instanceof SelectItemGroup)
            {
                SelectItemGroup itemgroup = (SelectItemGroup) item;
                SelectItem[] selectItems = itemgroup.getSelectItems();
                if (selectItems != null
                        && selectItems.length > 0
                        && matchValue(context, uiComponent, value, Arrays
                                .asList(selectItems).iterator(), converter))
                {
                    return true;
                }
            }
            else
            {
                Object itemValue = _convertOrCoerceValue(context,
                        uiComponent, value, item, converter);
                if (value == itemValue || value.equals(itemValue))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param context the faces context
     * @param uiComponent the component instance
     * @param value the value to check
     * @param converter 
     * @param iterator contains instances of SelectItem
     * @return if the value is a SelectItem of selectItemsIter, on which noSelectionOption is true
     */
    public static boolean isNoSelectionOption(FacesContext context,
            UIComponent uiComponent, Object value,
            Iterator<SelectItem> selectItemsIter, Converter converter)
    {
        while (selectItemsIter.hasNext())
        {
            SelectItem item = selectItemsIter.next();
            if (item instanceof SelectItemGroup)
            {
                SelectItemGroup itemgroup = (SelectItemGroup) item;
                SelectItem[] selectItems = itemgroup.getSelectItems();
                if (selectItems != null
                        && selectItems.length > 0
                        && isNoSelectionOption(context, uiComponent, value,
                                Arrays.asList(selectItems).iterator(),
                                converter))
                {
                    return true;
                }
            }
            else if (item.isNoSelectionOption())
            {
                Object itemValue = _convertOrCoerceValue(context, uiComponent,
                        value, item, converter);
                if (value == itemValue || value.equals(itemValue))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If converter is available and selectItem.value is String uses getAsObject,
     * otherwise uses EL type coertion and return result.
     */
    private static Object _convertOrCoerceValue(FacesContext facesContext,
            UIComponent uiComponent, Object value, SelectItem selectItem,
            Converter converter)
    {
        Object itemValue = selectItem.getValue();
        if (converter != null && itemValue instanceof String)
        {
            itemValue = converter.getAsObject(facesContext, uiComponent,
                    (String) itemValue);
        }
        else
        {
            // The javadoc of UISelectOne/UISelectMany says : 
            // "... Before comparing each option, coerce the option value type
            //  to the type of this component's value following the 
            // Expression Language coercion rules ..."
            // If the coercion fails, just return the value without coerce,
            // because it could be still valid the comparison for that value.
            // and swallow the exception, because its information is no relevant
            // on this context.
            try
            {
                if (value instanceof java.lang.Enum)
                {
                    // Values from an enum are a special case. There is one syntax were the
                    // particular enumeration is extended using something like
                    // SOMEVALUE { ... }, usually to override toString() method. In this case,
                    // value.getClass is not the target enum class, so we need to get the 
                    // right one from super class.
                    Class targetClass = value.getClass();
                    if (targetClass != null && !targetClass.isEnum())
                    {
                        targetClass = targetClass.getSuperclass();
                    }
                    itemValue = _ClassUtils.convertToTypeNoLogging(facesContext, itemValue, targetClass);
                }
                else
                {
                    itemValue = _ClassUtils.convertToTypeNoLogging(facesContext, itemValue, value.getClass());
                }
            }
            catch (IllegalArgumentException e)
            {
                //itemValue = selectItem.getValue();
            }
            catch (Exception e)
            {
                //itemValue = selectItem.getValue();
            }
        }
        return itemValue;
    }

}
