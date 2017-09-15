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
package org.apache.myfaces.shared.renderkit.html;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectBoolean;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.component.html.HtmlSelectManyCheckbox;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

public class HtmlCheckboxRendererBase extends HtmlRenderer
{
    //private static final Log log = LogFactory
    //        .getLog(HtmlCheckboxRendererBase.class);
    private static final Logger log = Logger.getLogger(HtmlCheckboxRendererBase.class.getName());

    private static final String PAGE_DIRECTION = "pageDirection";

    private static final String LINE_DIRECTION = "lineDirection";

    private static final String EXTERNAL_TRUE_VALUE = "true";

    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, uiComponent, null);
        
        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, facesContext.getResponseWriter());
            }
        }
        
        if (uiComponent instanceof UISelectBoolean)
        {
            Boolean value = org.apache.myfaces.shared.renderkit.RendererUtils.getBooleanValue( uiComponent );
            boolean isChecked = value != null ? value.booleanValue() : false;
            renderCheckbox(facesContext, uiComponent, EXTERNAL_TRUE_VALUE, false,isChecked, true, null); 
                //TODO: the selectBoolean is never disabled
        }
        else if (uiComponent instanceof UISelectMany)
        {
            renderCheckboxList(facesContext, (UISelectMany) uiComponent);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class "
                    + uiComponent.getClass().getName());
        }
    }

    public void renderCheckboxList(FacesContext facesContext,
            UISelectMany selectMany) throws IOException
    {

        String layout = getLayout(selectMany);
        boolean pageDirectionLayout = false; //Default to lineDirection
        if (layout != null)
        {
            if (layout.equals(PAGE_DIRECTION))
            {
                pageDirectionLayout = true;
            } 
            else if (layout.equals(LINE_DIRECTION))
            {
                pageDirectionLayout = false;
            }
            else
            {
                log.severe("Wrong layout attribute for component "
                        + selectMany.getClientId(facesContext) + ": " + layout);
            }
        }

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement(HTML.TABLE_ELEM, selectMany);
        HtmlRendererUtils.renderHTMLAttributes(writer, selectMany,
                HTML.SELECT_TABLE_PASSTHROUGH_ATTRIBUTES);
        
        Map<String, List<ClientBehavior>> behaviors = null;
        if (selectMany instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) selectMany).getClientBehaviors();
        }
        
        if (behaviors != null && !behaviors.isEmpty())
        {
            writer.writeAttribute(HTML.ID_ATTR, selectMany.getClientId(facesContext), null);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, selectMany, facesContext);
        }        

        if (!pageDirectionLayout)
        {
            writer.startElement(HTML.TR_ELEM, null); //selectMany);
        }
        
        Converter converter = getConverter(facesContext, selectMany);

        Set lookupSet = org.apache.myfaces.shared.renderkit.RendererUtils.getSubmittedValuesAsSet(
                facesContext, selectMany, converter, selectMany);
        boolean useSubmittedValues = lookupSet != null;

        if (!useSubmittedValues)
        {
            lookupSet = org.apache.myfaces.shared.renderkit.RendererUtils.getSelectedValuesAsSet(
                    facesContext, selectMany, converter, selectMany);
        }

        int itemNum = 0;

        for (Iterator it = org.apache.myfaces.shared.renderkit.RendererUtils.getSelectItemList(
                selectMany, facesContext)
                .iterator(); it.hasNext();)
        {
            SelectItem selectItem = (SelectItem) it.next();
            
            itemNum = renderGroupOrItemCheckbox(facesContext, selectMany, 
                                                selectItem, useSubmittedValues, lookupSet, 
                                                converter, pageDirectionLayout, itemNum);
        }

        if (!pageDirectionLayout)
        {
            writer.endElement(HTML.TR_ELEM);
        }
        writer.endElement(HTML.TABLE_ELEM);
    }

    protected String getLayout(UISelectMany selectMany)
    {
        if (selectMany instanceof HtmlSelectManyCheckbox)
        {
            return ((HtmlSelectManyCheckbox) selectMany).getLayout();
        } 
        
        return (String) selectMany.getAttributes().get(JSFAttr.LAYOUT_ATTR);
    }
    
    /**
     * 
     * @param facesContext
     * @param uiComponent
     * @param selectItem
     * @param useSubmittedValues
     * @param lookupSet
     * @param converter
     * @param pageDirectionLayout
     * @param itemNum
     * @return the itemNum for the next option
     * @throws IOException
     */
    protected int renderGroupOrItemCheckbox(FacesContext facesContext,
                                             UIComponent uiComponent, SelectItem selectItem,
                                             boolean useSubmittedValues, Set lookupSet,
                                             Converter converter, boolean pageDirectionLayout, 
                                             Integer itemNum) throws IOException
    {

        ResponseWriter writer = facesContext.getResponseWriter();

        boolean isSelectItemGroup = (selectItem instanceof SelectItemGroup);

        UISelectMany selectMany = (UISelectMany) uiComponent;

        if (isSelectItemGroup)
        {
            if (pageDirectionLayout)
            {
                writer.startElement(HTML.TR_ELEM, null); // selectMany);
            }

            writer.startElement(HTML.TD_ELEM, null); // selectMany);
            if (selectItem.isEscape())
            {
                writer.writeText(selectItem.getLabel(),HTML.LABEL_ATTR);
            }
            else
            {
                writer.write(selectItem.getLabel());
            }
            writer.endElement(HTML.TD_ELEM);

            if (pageDirectionLayout)
            {
                writer.endElement(HTML.TR_ELEM);
                writer.startElement(HTML.TR_ELEM, null); // selectMany);
            }
            writer.startElement(HTML.TD_ELEM, null); // selectMany);

            writer.startElement(HTML.TABLE_ELEM, null); // selectMany);
            writer.writeAttribute(HTML.BORDER_ATTR, "0", null);
            
            if(!pageDirectionLayout)
            {
                writer.startElement(HTML.TR_ELEM, null); // selectMany);
            }

            SelectItemGroup group = (SelectItemGroup) selectItem;
            SelectItem[] selectItems = group.getSelectItems();
            
            for (SelectItem groupSelectItem : selectItems)
            {
                itemNum = renderGroupOrItemCheckbox(facesContext, selectMany, groupSelectItem, useSubmittedValues,
                                                    lookupSet, converter, pageDirectionLayout, itemNum);
            }

            if(!pageDirectionLayout)
            {
                writer.endElement(HTML.TR_ELEM);
            }
            writer.endElement(HTML.TABLE_ELEM);
            writer.endElement(HTML.TD_ELEM);

            if (pageDirectionLayout)
            {
                writer.endElement(HTML.TR_ELEM);
            }

        }
        else
        {
            Object itemValue = selectItem.getValue(); // TODO : Check here for getSubmittedValue. 
                                                      // Look at RendererUtils.getValue
            String itemStrValue = org.apache.myfaces.shared.renderkit.RendererUtils.getConvertedStringValue(
                    facesContext, selectMany, converter, itemValue);
            
            boolean checked = lookupSet.contains(itemStrValue);
            
            // IF the hideNoSelectionOption attribute of the component is true
            // AND this selectItem is the "no selection option"
            // AND there are currently selected items
            // AND this item (the "no selection option") is not selected
            if (HtmlRendererUtils.isHideNoSelectionOption(uiComponent) && selectItem.isNoSelectionOption() 
                    && lookupSet.size() != 0 && !checked)
            {
                // do not render this selectItem
                return itemNum;
            }

            writer.write("\t\t");
            if (pageDirectionLayout)
            {
                writer.startElement(HTML.TR_ELEM, null); // selectMany);
            }
            writer.startElement(HTML.TD_ELEM, null); // selectMany);

            boolean disabled = selectItem.isDisabled();

            String itemId = renderCheckbox(facesContext, selectMany, itemStrValue, disabled, checked, false, itemNum);

            // label element after the input
            boolean componentDisabled = isDisabled(facesContext, selectMany);
            boolean itemDisabled = (componentDisabled || disabled);

            HtmlRendererUtils.renderLabel(writer, selectMany, itemId, selectItem, itemDisabled, checked);

            writer.endElement(HTML.TD_ELEM);
            if (pageDirectionLayout)
            {
                writer.endElement(HTML.TR_ELEM);
            }
            
            // we rendered one checkbox --> increment itemNum
            itemNum++;
        }
        
        return itemNum;
    }

    @Deprecated
    protected void renderCheckbox(FacesContext facesContext,
            UIComponent uiComponent, String value, String label,
            boolean disabled, boolean checked, boolean renderId) throws IOException
    {
        renderCheckbox(facesContext, uiComponent, value, disabled, checked, renderId, 0);
    }

    /**
     * Renders the input item
     * @return the 'id' value of the rendered element
     */
    protected String renderCheckbox(FacesContext facesContext,
            UIComponent uiComponent, String value, boolean disabled, boolean checked, 
            boolean renderId, Integer itemNum) throws IOException
    {
        String clientId = uiComponent.getClientId(facesContext);

        String itemId = (itemNum == null)? null : clientId + 
                facesContext.getNamingContainerSeparatorChar() + itemNum;

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement(HTML.INPUT_ELEM, uiComponent);

        if (itemId != null)
        {
            writer.writeAttribute(HTML.ID_ATTR, itemId, null);
        }
        else if (renderId) 
        {
            writer.writeAttribute(HTML.ID_ATTR, clientId, null);
        }
        writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_CHECKBOX, null);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, null);
        
        if (checked)
        {
            writer.writeAttribute(HTML.CHECKED_ATTR, 
                    org.apache.myfaces.shared.renderkit.html.HTML.CHECKED_ATTR, null);
        }
        
        if (disabled)
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, null);
        }

        if ((value != null) && (value.length() > 0))
        {
            writer.writeAttribute(HTML.VALUE_ATTR, value, null);
        }

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof UISelectBoolean)
        {
            if (uiComponent instanceof ClientBehaviorHolder)
            {
                behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
                
                if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
                {
                    long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(uiComponent);
                    CommonPropertyUtils.renderChangeEventProperty(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonPropertyUtils.renderEventProperties(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonPropertyUtils.renderFieldEventPropertiesWithoutOnchange(writer, 
                            commonPropertiesMarked, uiComponent);
                }
                else
                {
                    long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(uiComponent);
                    HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(
                            facesContext, writer, uiComponent, behaviors);
                    if (isCommonEventsOptimizationEnabled(facesContext))
                    {
                        Long commonEventsMarked = CommonEventUtils.getCommonEventsMarked(uiComponent);
                        CommonEventUtils.renderBehaviorizedEventHandlers(facesContext, writer, 
                                commonPropertiesMarked, commonEventsMarked, uiComponent, behaviors);
                        CommonEventUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                            facesContext, writer, commonPropertiesMarked, commonEventsMarked, uiComponent, behaviors);
                    }
                    else
                    {
                        HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, 
                                writer, uiComponent, behaviors);
                        HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                                facesContext, writer, uiComponent, behaviors);
                    }
                }
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.INPUT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.INPUT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);
            }
        }
        else
        {
            if (uiComponent instanceof ClientBehaviorHolder)
            {
                behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
                
                if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
                {
                    long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(uiComponent);
                    CommonPropertyUtils.renderChangeEventProperty(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonPropertyUtils.renderEventProperties(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonPropertyUtils.renderFieldEventPropertiesWithoutOnchange(writer, 
                            commonPropertiesMarked, uiComponent);
                }
                else
                {
                    long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(uiComponent);
                    HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(
                            facesContext, writer, uiComponent, behaviors);
                    if (isCommonEventsOptimizationEnabled(facesContext))
                    {
                        Long commonEventsMarked = CommonEventUtils.getCommonEventsMarked(uiComponent);
                        CommonEventUtils.renderBehaviorizedEventHandlers(facesContext, writer, 
                                commonPropertiesMarked, commonEventsMarked, uiComponent, behaviors);
                        CommonEventUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                            facesContext, writer, commonPropertiesMarked, commonEventsMarked,
                            uiComponent, behaviors);
                    }
                    else
                    {
                        HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer,
                                uiComponent, behaviors);
                        HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                                facesContext, writer, uiComponent, behaviors);
                    }
                }
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, 
                        HTML.INPUT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_STYLE_AND_EVENTS);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.INPUT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_STYLE);
            }
        }
        if (isDisabled(facesContext, uiComponent))
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, Boolean.TRUE, null);
        }
        
        writer.endElement(HTML.INPUT_ELEM);

        return itemId;
    }

    protected boolean isDisabled(FacesContext facesContext,
            UIComponent component)
    {
        //TODO: overwrite in extended HtmlCheckboxRenderer and check for
        // enabledOnUserRole
        if (component instanceof HtmlSelectBooleanCheckbox)
        {
            return ((HtmlSelectBooleanCheckbox) component).isDisabled();
        }
        else if (component instanceof HtmlSelectManyCheckbox)
        {
            return ((HtmlSelectManyCheckbox) component).isDisabled();
        }
        else
        {
            return org.apache.myfaces.shared.renderkit.RendererUtils.getBooleanAttribute(component,
                    HTML.DISABLED_ATTR, false);
        }
    }

    public void decode(FacesContext facesContext, UIComponent component)
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, component, null);
        if (component instanceof UISelectBoolean)
        {
            HtmlRendererUtils.decodeUISelectBoolean(facesContext, component);
        }
        else if (component instanceof UISelectMany)
        {
            HtmlRendererUtils.decodeUISelectMany(facesContext, component);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class "
                    + component.getClass().getName());
        }
        if (component instanceof ClientBehaviorHolder &&
                !HtmlRendererUtils.isDisabled(component))
        {
            HtmlRendererUtils.decodeClientBehaviors(facesContext, component);
        }
    }

    public Object getConvertedValue(FacesContext facesContext,
            UIComponent component, Object submittedValue)
            throws ConverterException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, component, null);
        if (component instanceof UISelectBoolean)
        {
            return submittedValue;
        }
        else if (component instanceof UISelectMany)
        {
            return org.apache.myfaces.shared.renderkit.RendererUtils.getConvertedUISelectManyValue(facesContext,
                    (UISelectMany) component, submittedValue);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class "
                    + component.getClass().getName());
        }
    }
    
    /**
     * Gets the converter for the given component rendered by this renderer.
     * @param facesContext
     * @param component
     * @return
     */
    protected Converter getConverter(FacesContext facesContext,
            UIComponent component)
    {
        if (component instanceof UISelectMany)
        {
            return HtmlRendererUtils.findUISelectManyConverterFailsafe(facesContext, 
                    (UISelectMany) component);
        }
        else if (component instanceof UISelectOne)
        {
            return HtmlRendererUtils.findUIOutputConverterFailSafe(facesContext, component);
        }
        return null;
    }
    
}
