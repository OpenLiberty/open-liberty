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
package org.apache.myfaces.renderkit.html.base;

import org.apache.myfaces.renderkit.html.util.HtmlRendererUtils;
import org.apache.myfaces.renderkit.html.util.ClientBehaviorRendererUtils;
import org.apache.myfaces.renderkit.html.util.CommonHtmlAttributesUtil;
import org.apache.myfaces.renderkit.html.util.CommonHtmlEventsUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UISelectBoolean;
import jakarta.faces.component.UISelectMany;
import jakarta.faces.component.UISelectOne;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.html.HtmlSelectBooleanCheckbox;
import jakarta.faces.component.html.HtmlSelectManyCheckbox;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import org.apache.myfaces.core.api.shared.AttributeUtils;
import org.apache.myfaces.renderkit.RendererUtils;

import org.apache.myfaces.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.renderkit.html.util.HTML;
import org.apache.myfaces.renderkit.html.util.ComponentAttrs;

public class HtmlCheckboxRendererBase extends HtmlRenderer
{
    private static final Logger log = Logger.getLogger(HtmlCheckboxRendererBase.class.getName());

    private static final String PAGE_DIRECTION = "pageDirection";

    private static final String LINE_DIRECTION = "lineDirection";

    private static final String LAYOUT_LIST = "list";

    private static final String EXTERNAL_TRUE_VALUE = "true";

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, null);

        if (uiComponent instanceof ClientBehaviorHolder)
        {
            Map<String, List<ClientBehavior>> behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, facesContext.getResponseWriter());
            }
        }
        
        if (uiComponent instanceof UISelectBoolean)
        {
            Boolean value = RendererUtils.getBooleanValue( uiComponent );
            boolean isChecked = value != null ? value : false;
            renderCheckbox(facesContext, uiComponent, EXTERNAL_TRUE_VALUE, false,isChecked, true, null);
            //TODO: the selectBoolean is never disabled
        }
        else if (uiComponent instanceof UISelectMany)
        {
            // let the current impl do what it does in encodeEnd do nothing here just don't want exception
            // throw if it is this case
            log.finest("encodeBegin() doing nothing intentionally for UISelectMany");
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class "
                + uiComponent.getClass().getName());
        }
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        if (uiComponent instanceof UISelectBoolean)
        {
            ResponseWriter writer = facesContext.getResponseWriter();
            writer.endElement(HTML.INPUT_ELEM);
        }
        else if (uiComponent instanceof UISelectMany)
        {
            renderCheckboxList(facesContext, (UISelectMany) uiComponent);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + uiComponent.getClass().getName());
        }
    }

    public void renderCheckboxList(FacesContext facesContext, UISelectMany selectMany) throws IOException
    {
        String layout = getLayout(selectMany);
        Boolean usingTable = Boolean.FALSE; // default to LINE_DIRECTION
        if (layout != null)
        {
            if (layout.equals(PAGE_DIRECTION))
            {
                usingTable = Boolean.TRUE;
            }
            else if (layout.equals(LINE_DIRECTION))
            {
                usingTable = Boolean.FALSE;
            }
            else if (layout.equals(LAYOUT_LIST))
            {
                usingTable = null;
            }
            else
            {
                log.severe("Wrong layout attribute for component "
                        + selectMany.getClientId(facesContext) + ": " + layout);
            }
        }

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.startElement(usingTable != null ? HTML.TABLE_ELEM : HTML.UL_ELEM, selectMany);
        if(usingTable != null) 
        {
            HtmlRendererUtils.renderHTMLAttributes(writer, selectMany, HTML.SELECT_TABLE_PASSTHROUGH_ATTRIBUTES);
        }
        else 
        {
            HtmlRendererUtils.renderHTMLAttributes(writer, selectMany, HTML.UL_PASSTHROUGH_ATTRIBUTES);
        }
        
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

        if (usingTable == Boolean.FALSE)
        {
            writer.startElement(HTML.TR_ELEM, null);
        }
        
        Converter converter = getConverter(facesContext, selectMany);

        Set lookupSet = RendererUtils.getSubmittedValuesAsSet(facesContext, selectMany, converter, selectMany);
        boolean useSubmittedValues = lookupSet != null;

        if (!useSubmittedValues)
        {
            lookupSet = RendererUtils.getSelectedValuesAsSet(facesContext, selectMany, converter, selectMany);
        }

        int itemNum = 0;

        
        List<SelectItem> selectItemList = RendererUtils.getSelectItemList(selectMany, facesContext);

        for (int i = 0; i < selectItemList.size(); i++)
        {
            SelectItem selectItem = (SelectItem) selectItemList.get(i);
            
            itemNum = renderGroupOrItemCheckbox(facesContext, selectMany, 
                                                selectItem, useSubmittedValues, lookupSet, 
                                                converter, usingTable, itemNum);
        }

        if (usingTable == Boolean.FALSE)
        {
            writer.endElement(HTML.TR_ELEM);
        }
        writer.endElement(usingTable != null ? HTML.TABLE_ELEM : HTML.UL_ELEM);
    }

    protected String getLayout(UISelectMany selectMany)
    {
        if (selectMany instanceof HtmlSelectManyCheckbox)
        {
            return ((HtmlSelectManyCheckbox) selectMany).getLayout();
        } 
        
        return (String) selectMany.getAttributes().get(ComponentAttrs.LAYOUT_ATTR);
    }

    protected int renderGroupOrItemCheckbox(FacesContext facesContext,
                                             UIComponent uiComponent, SelectItem selectItem,
                                             boolean useSubmittedValues, Set lookupSet,
                                             Converter converter, Boolean usingTable,
                                             Integer itemNum) throws IOException
    {

        ResponseWriter writer = facesContext.getResponseWriter();

        boolean isSelectItemGroup = (selectItem instanceof SelectItemGroup);

        UISelectMany selectMany = (UISelectMany) uiComponent;

        if (isSelectItemGroup)
        {
            if (usingTable == Boolean.TRUE)
            {
                writer.startElement(HTML.TR_ELEM, null);
            }

            writer.startElement(usingTable != null ? HTML.TD_ELEM : HTML.LI_ELEM, null);
            if (selectItem.isEscape())
            {
                writer.writeText(selectItem.getLabel(),HTML.LABEL_ATTR);
            }
            else
            {
                writer.write(selectItem.getLabel());
            }

            if (usingTable != null)
            {
                writer.endElement(HTML.TD_ELEM);
            }

            if (usingTable == Boolean.TRUE)
            {
                writer.endElement(HTML.TR_ELEM);
                writer.startElement(HTML.TR_ELEM, null);
            }

            if (usingTable != null)
            {
                writer.startElement(HTML.TD_ELEM, null);
            }

            writer.startElement(usingTable != null ? HTML.TABLE_ELEM : HTML.UL_ELEM, null);
            if (usingTable != null)
            {
                int border = 0;
                Object borderObj = uiComponent.getAttributes().get("border");
                if (null != borderObj)
                {
                    border = (Integer) borderObj;
                }
                if (Integer.MIN_VALUE != border)
                {
                    writer.writeAttribute(HTML.BORDER_ATTR, border, "border");
                }
            }

            if(usingTable == Boolean.FALSE)
            {
                writer.startElement(HTML.TR_ELEM, null);
            }

            SelectItemGroup group = (SelectItemGroup) selectItem;
            SelectItem[] selectItems = group.getSelectItems();
            
            for (SelectItem groupSelectItem : selectItems)
            {
                itemNum = renderGroupOrItemCheckbox(facesContext, selectMany, groupSelectItem, useSubmittedValues,
                                                    lookupSet, converter, usingTable, itemNum);
            }

            if(usingTable == Boolean.FALSE)
            {
                writer.endElement(HTML.TR_ELEM);
            }
            writer.endElement(usingTable != null ? HTML.TABLE_ELEM : HTML.UL_ELEM);
            writer.endElement(usingTable != null ? HTML.TD_ELEM : HTML.LI_ELEM);

            if (usingTable == Boolean.TRUE)
            {
                writer.endElement(HTML.TR_ELEM);
            }

        }
        else
        {
            Object itemValue = selectItem.getValue(); // TODO : Check here for getSubmittedValue. 
                                                      // Look at RendererUtils.getValue
            String itemStrValue =  org.apache.myfaces.core.api.shared.SharedRendererUtils.getConvertedStringValue(
                    facesContext, selectMany, converter, itemValue);
            
            boolean checked = lookupSet.contains(itemStrValue);
            
            // IF the hideNoSelectionOption attribute of the component is true
            // AND this selectItem is the "no selection option"
            // AND there are currently selected items
            // AND this item (the "no selection option") is not selected
            if (HtmlRendererUtils.isHideNoSelectionOption(uiComponent)
                    && selectItem.isNoSelectionOption() 
                    && !lookupSet.isEmpty()
                    && !checked)
            {
                // do not render this selectItem
                return itemNum;
            }

            writer.write("\t\t");
            if (usingTable == Boolean.TRUE)
            {
                writer.startElement(HTML.TR_ELEM, null);
            }
            writer.startElement(usingTable != null ? HTML.TD_ELEM : HTML.LI_ELEM, null);

            boolean disabled = selectItem.isDisabled();

            String itemId = renderCheckbox(facesContext, selectMany, itemStrValue, disabled, checked, false, itemNum);

            // label element after the input
            boolean componentDisabled = isDisabled(facesContext, selectMany);
            boolean itemDisabled = (componentDisabled || disabled);

            HtmlRendererUtils.renderLabel(writer, selectMany, itemId, selectItem, itemDisabled, checked);

            writer.endElement(usingTable != null ? HTML.TD_ELEM : HTML.LI_ELEM);
            if (usingTable == Boolean.TRUE)
            {
                writer.endElement(HTML.TR_ELEM);
            }
            
            // we rendered one checkbox --> increment itemNum
            itemNum++;
        }
        
        return itemNum;
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
            writer.writeAttribute(HTML.CHECKED_ATTR, HTML.CHECKED_ATTR, null);
        }
        
        if (disabled)
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, null);
        }

        value = value == null ? "" : value;
        writer.writeAttribute(HTML.VALUE_ATTR, value, null);

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof UISelectBoolean)
        {
            if (uiComponent instanceof ClientBehaviorHolder)
            {
                behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
                
                if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
                {
                    long commonPropertiesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(uiComponent);
                    CommonHtmlAttributesUtil.renderChangeEventProperty(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonHtmlAttributesUtil.renderEventProperties(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonHtmlAttributesUtil.renderFieldEventPropertiesWithoutOnchange(writer, 
                            commonPropertiesMarked, uiComponent);
                }
                else
                {
                    long commonPropertiesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(uiComponent);
                    HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(
                            facesContext, writer, uiComponent, itemId != null ? itemId : clientId,  behaviors);
                    if (isCommonEventsOptimizationEnabled(facesContext))
                    {
                        long commonEventsMarked = CommonHtmlEventsUtil.getMarkedEvents(uiComponent);
                        CommonHtmlEventsUtil.renderBehaviorizedEventHandlers(facesContext, writer, 
                                commonPropertiesMarked, commonEventsMarked, uiComponent,
                                itemId != null ? itemId : clientId, behaviors);
                        CommonHtmlEventsUtil.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                            facesContext, writer, commonPropertiesMarked, commonEventsMarked, uiComponent, 
                                itemId != null ? itemId : clientId, behaviors);
                    }
                    else
                    {
                        HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, 
                                writer, uiComponent, behaviors);
                        HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                                facesContext, writer, uiComponent, 
                                itemId != null ? itemId : clientId, behaviors);
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
                    long commonPropertiesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(uiComponent);
                    CommonHtmlAttributesUtil.renderChangeEventProperty(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonHtmlAttributesUtil.renderEventProperties(writer, 
                            commonPropertiesMarked, uiComponent);
                    CommonHtmlAttributesUtil.renderFieldEventPropertiesWithoutOnchange(writer, 
                            commonPropertiesMarked, uiComponent);
                }
                else
                {
                    long commonPropertiesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(uiComponent);
                    HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(
                            facesContext, writer, uiComponent, itemId != null ? itemId : clientId, behaviors);
                    if (isCommonEventsOptimizationEnabled(facesContext))
                    {
                        long commonEventsMarked = CommonHtmlEventsUtil.getMarkedEvents(uiComponent);
                        CommonHtmlEventsUtil.renderBehaviorizedEventHandlers(facesContext, writer, 
                                commonPropertiesMarked, commonEventsMarked, uiComponent, 
                                itemId != null ? itemId : clientId, behaviors);
                        CommonHtmlEventsUtil.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                            facesContext, writer, commonPropertiesMarked, commonEventsMarked,
                            uiComponent, itemId != null ? itemId : clientId, behaviors);
                    }
                    else
                    {
                        HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer,
                                uiComponent, itemId != null ? itemId : clientId, behaviors);
                        HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                                facesContext, writer, uiComponent, itemId != null ? itemId : clientId, behaviors);
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
            writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, null);
        }
        if (uiComponent instanceof UISelectMany)
        {
            writer.endElement(HTML.INPUT_ELEM);
        }

        return itemId;
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent component)
    {
        if (component instanceof HtmlSelectBooleanCheckbox)
        {
            return ((HtmlSelectBooleanCheckbox) component).isDisabled();
        }
        else if (component instanceof HtmlSelectManyCheckbox)
        {
            return ((HtmlSelectManyCheckbox) component).isDisabled();
        }

        return AttributeUtils.getBooleanAttribute(component, HTML.DISABLED_ATTR, false);
    }

    @Override
    public void decode(FacesContext facesContext, UIComponent component)
    {
        RendererUtils.checkParamValidity(facesContext, component, null);
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
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }

        if (component instanceof ClientBehaviorHolder && !HtmlRendererUtils.isDisabled(component))
        {
            ClientBehaviorRendererUtils.decodeClientBehaviors(facesContext, component);
        }
    }

    @Override
    public Object getConvertedValue(FacesContext facesContext, UIComponent component, Object submittedValue)
            throws ConverterException
    {
        RendererUtils.checkParamValidity(facesContext, component, null);
        if (component instanceof UISelectBoolean)
        {
            return submittedValue;
        }
        else if (component instanceof UISelectMany)
        {
            return RendererUtils.getConvertedUISelectManyValue(facesContext, (UISelectMany) component, submittedValue);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }
    
    /**
     * Gets the converter for the given component rendered by this renderer.
     * @param facesContext
     * @param component
     * @return
     */
    protected Converter getConverter(FacesContext facesContext, UIComponent component)
    {
        if (component instanceof UISelectMany)
        {
            return HtmlRendererUtils.findUISelectManyConverterFailsafe(facesContext, (UISelectMany) component);
        }
        else if (component instanceof UISelectOne)
        {
            return HtmlRendererUtils.findUIOutputConverterFailSafe(facesContext, component);
        }
        return null;
    }
    
}
