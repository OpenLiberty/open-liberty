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
package org.apache.myfaces.renderkit.html.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UISelectItems;
import jakarta.faces.component.UISelectMany;
import jakarta.faces.component.UISelectOne;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.Converter;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import org.apache.myfaces.core.api.shared.AttributeUtils;
import org.apache.myfaces.core.api.shared.SelectItemsIterator;
import static org.apache.myfaces.renderkit.html.util.HtmlRendererUtils.isHideNoSelectionOption;

/**
 * Utility methods to manipulate SelectItem/SelectItems
 */
public class SelectItemsUtils
{
    private static final char TABULATOR = '\t';

    public static List<SelectItemInfo> getSelectItemInfoList(UISelectMany uiSelectMany, FacesContext facesContext)
    {
        List<SelectItemInfo> list = new ArrayList<>();

        for (SelectItemsIterator iter = new SelectItemsIterator(uiSelectMany, facesContext); iter.hasNext();)
        {
            list.add(new SelectItemInfo(iter.next(), iter.getCurrentComponent(), iter.getCurrentValue()));
        }
        return list;
    }

    public static List<SelectItemInfo> getSelectItemInfoList(UISelectOne uiSelectOne, FacesContext facesContext)
    {
        List<SelectItemInfo> list = new ArrayList<>();
        for (SelectItemsIterator iter = new SelectItemsIterator(uiSelectOne, facesContext); iter.hasNext();)
        {
            list.add(new SelectItemInfo(iter.next(), iter.getCurrentComponent(), iter.getCurrentValue()));
        }
        return list;
    }
    
    public static void renderSelectOptions(FacesContext context,
            UIComponent component, Converter converter, Set lookupSet,
            List<SelectItemInfo> selectItemList) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        // check for the hideNoSelectionOption attribute
        boolean hideNoSelectionOption = isHideNoSelectionOption(component);
        boolean componentDisabled = isTrue(component.getAttributes().get("disabled"));

        for (int i = 0; i < selectItemList.size(); i++)
        {
            SelectItemInfo selectItemInfo = selectItemList.get(i);
            SelectItem selectItem = selectItemInfo.getItem();
            if (selectItem instanceof SelectItemGroup)
            {
                writer.startElement(HTML.OPTGROUP_ELEM, selectItemInfo.getComponent()); // component);
                writer.writeAttribute(HTML.LABEL_ATTR, selectItem.getLabel(), null);

                SelectItem[] selectItems = ((SelectItemGroup) selectItem).getSelectItems();
                List<SelectItemInfo> selectItemsGroupList = new ArrayList<>(selectItems.length);
                for (SelectItem item : selectItems)
                {
                    selectItemsGroupList.add(new SelectItemInfo(item, null));
                }
                renderSelectOptions(context, component, converter, lookupSet,
                        selectItemsGroupList);
                writer.endElement(HTML.OPTGROUP_ELEM);
            }
            else
            {
                String itemStrValue = org.apache.myfaces.core.api.shared.SharedRendererUtils
                        .getConvertedStringValue(context, component, converter,
                                selectItem);
                boolean selected = lookupSet.contains(itemStrValue); 
                //TODO/FIX: we always compare the String values, better fill lookupSet with Strings 
                //only when useSubmittedValue==true, else use the real item value Objects

                // IF the hideNoSelectionOption attribute of the component is true
                // AND this selectItem is the "no selection option"
                // AND there are currently selected items 
                // AND this item (the "no selection option") is not selected
                // (if there is currently no value on UISelectOne, lookupSet contains "")
                if (hideNoSelectionOption && selectItem.isNoSelectionOption()
                        && !lookupSet.isEmpty()
                        && !(lookupSet.size() == 1 && lookupSet.contains(""))
                        && !selected)
                {
                    // do not render this selectItem
                    continue;
                }

                writer.write(TABULATOR);
                
                boolean wroteRequestMapVarValue = false;
                Object oldRequestMapVarValue = null;
                String var = null;
                if (selectItemInfo != null && selectItemInfo.getComponent() instanceof UISelectItems)
                {
                    var = (String) selectItemInfo.getComponent().getAttributes().get(ComponentAttrs.VAR_ATTR);
                    if(var != null && !var.isEmpty())
                    {
                        // save the current value of the key listed in var from the request map
                        oldRequestMapVarValue = context.getExternalContext().getRequestMap().put(var, 
                                selectItemInfo.getValue());
                        wroteRequestMapVarValue = true;
                    }
                }
                
                writer.startElement(HTML.OPTION_ELEM, selectItemInfo.getComponent());
                if (itemStrValue != null)
                {
                    writer.writeAttribute(HTML.VALUE_ATTR, itemStrValue, null);
                }
                else
                {
                    writer.writeAttribute(HTML.VALUE_ATTR, "", null);
                }

                if (selected)
                {
                    writer.writeAttribute(HTML.SELECTED_ATTR, HTML.SELECTED_ATTR, null);
                }

                boolean disabled = selectItem.isDisabled();
                if (disabled)
                {
                    writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, null);
                }

                String labelClass = null;

                if (componentDisabled || disabled)
                {
                    labelClass = (String) component.getAttributes().get(ComponentAttrs.DISABLED_CLASS_ATTR);
                }
                else
                {
                    labelClass = (String) component.getAttributes().get(ComponentAttrs.ENABLED_CLASS_ATTR);
                }
                if (labelClass != null)
                {
                    writer.writeAttribute("class", labelClass, "labelClass");
                }

                boolean escape = AttributeUtils.getBooleanAttribute(component, ComponentAttrs.ESCAPE_ATTR, false);
                //default is to escape
                //In Faces 1.2, when a SelectItem is created by default 
                //selectItem.isEscape() returns true (this property
                //is not available on Faces 1.1).
                //so, if we found a escape property on the component
                //set to true, escape every item, but if not
                //check if isEscape() = true first.
                if (escape || selectItem.isEscape())
                {
                    String label = selectItem.getLabel();

                    if(label == null)
                    {
                        label = "";
                    }

                    writer.writeText(label, null);
                }
                else
                {
                    writer.write(selectItem.getLabel());
                }

                writer.endElement(HTML.OPTION_ELEM);
                
                // remove the value with the key from var from the request map, if previously written
                if(wroteRequestMapVarValue)
                {
                    // If there was a previous value stored with the key from var in the request map, restore it
                    if (oldRequestMapVarValue != null)
                    {
                        context.getExternalContext().getRequestMap().put(var, oldRequestMapVarValue);
                    }
                    else
                    {
                        context.getExternalContext().getRequestMap().remove(var);
                    }
                }
            }
        }
    }

    private static boolean isTrue(Object obj)
    {
        if (obj instanceof String)
        {
            return Boolean.valueOf((String) obj);
        }
        if (!(obj instanceof Boolean))
        {
            return false;
        }
        return ((Boolean) obj);
    }
}
