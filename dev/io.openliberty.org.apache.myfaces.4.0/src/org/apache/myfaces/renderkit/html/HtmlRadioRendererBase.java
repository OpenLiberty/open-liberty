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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.faces.FacesException;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UISelectOne;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.html.HtmlSelectOneRadio;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitHint;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import org.apache.myfaces.core.api.shared.AttributeUtils;
import org.apache.myfaces.core.api.shared.ComponentUtils;

import org.apache.myfaces.renderkit.RendererUtils;
import org.apache.myfaces.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.renderkit.html.util.HTML;
import org.apache.myfaces.renderkit.html.util.ComponentAttrs;

public class HtmlRadioRendererBase extends HtmlRenderer
{
    private static final Logger log = Logger.getLogger(HtmlRadioRendererBase.class.getName());

    private static final String PAGE_DIRECTION = "pageDirection";
    private static final String LINE_DIRECTION = "lineDirection";
    private static final String LAYOUT_LIST = "list";
    
    private static final Set<VisitHint> FIND_SELECT_LIST_HINTS = 
            Collections.unmodifiableSet(EnumSet.of(VisitHint.SKIP_UNRENDERED, VisitHint.SKIP_ITERATION));

    private Map<String, UISelectOne> groupFirst = new HashMap<String, UISelectOne>();
    
    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UISelectOne.class);

        UISelectOne selectOne = (UISelectOne)uiComponent;

        String layout = getLayout(selectOne);

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
                log.severe("Wrong layout '" + layout + "' defined for component "
                        + ComponentUtils.getPathToComponent(selectOne));
            }
        }

        ResponseWriter writer = facesContext.getResponseWriter();

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
            }
        }
        
        String group = selectOne instanceof HtmlSelectOneRadio ? ((HtmlSelectOneRadio) selectOne).getGroup() : null;
        if (group != null && !group.isEmpty())
        {
            if (!groupFirst.containsKey(group)) 
            {
                groupFirst.put(group, selectOne);
            }

            List selectItemList = RendererUtils.getSelectItemList(selectOne, facesContext);
            if (selectItemList != null && !selectItemList.isEmpty())
            {
                Converter converter = HtmlRendererUtils.findUIOutputConverterFailSafe(facesContext, selectOne);
                Object currentValue = null;

                // If the current selectOne radio contains a value expression, 
                // then get the current value using the current selectOne radio.
                // Otherwise get the current value using the first selectOne radio which
                // must contain a value expression.
                if (selectOne.getValueExpression("value") != null)
                {
                    currentValue = RendererUtils.getStringFromSubmittedValueOrLocalValueReturnNull(
                                facesContext, selectOne);
                }
                else
                {
                    currentValue = RendererUtils.getStringFromSubmittedValueOrLocalValueReturnNull(
                                facesContext, groupFirst.get(group));
                }
                SelectItem selectItem = (SelectItem) selectItemList.get(0);
                renderGroupOrItemRadio(facesContext, selectOne,
                                                     selectItem, currentValue,
                                                     converter, usingTable, group, 0);
            }
            else
            {
                // Deferred case: find real component with attached selectItems
                UIForm form = ComponentUtils.findClosest(UIForm.class, uiComponent);
                GetSelectItemListCallback callback = new GetSelectItemListCallback(selectOne, group);
                form.visitTree(
                        VisitContext.createVisitContext(facesContext, null, FIND_SELECT_LIST_HINTS),
                        callback);                
                renderGroupOrItemRadio(facesContext, selectOne, callback.getSelectItem(),
                        callback.getCurrentValue(), callback.getConverter(),
                            usingTable, group, callback.getIndex());
            }
        }
        else
        {
            // Render as single component
            writer.startElement(usingTable != null ? HTML.TABLE_ELEM : HTML.UL_ELEM, selectOne);
            if(usingTable != null) 
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, selectOne, HTML.SELECT_TABLE_PASSTHROUGH_ATTRIBUTES);
            }
            else 
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, selectOne, HTML.UL_PASSTHROUGH_ATTRIBUTES);
            }

            if (behaviors != null && !behaviors.isEmpty())
            {
                writer.writeAttribute(HTML.ID_ATTR, selectOne.getClientId(facesContext), null);
            }
            else
            {
                HtmlRendererUtils.writeIdIfNecessary(writer, selectOne, facesContext); 
            }        

            if (usingTable == Boolean.FALSE)
            {
                writer.startElement(HTML.TR_ELEM, null); // selectOne);
            }

            List selectItemList = RendererUtils.getSelectItemList(selectOne, facesContext);
            Converter converter = HtmlRendererUtils.findUIOutputConverterFailSafe(facesContext, selectOne);
            Object currentValue = RendererUtils.getStringFromSubmittedValueOrLocalValueReturnNull(
                        facesContext, selectOne);

            int itemNum = 0;

            for (int i = 0; i < selectItemList.size(); i++)
            {
                SelectItem selectItem = (SelectItem) selectItemList.get(i);

                itemNum = renderGroupOrItemRadio(facesContext, selectOne,
                                                 selectItem, currentValue,
                                                 converter, usingTable, itemNum);
            }

            if (usingTable == Boolean.FALSE)
            {
                writer.endElement(HTML.TR_ELEM);
            }
            writer.endElement(usingTable != null ? HTML.TABLE_ELEM : HTML.UL_ELEM);
        }
    }


    protected String getLayout(UIComponent selectOne)
    {
        if (selectOne instanceof HtmlSelectOneRadio)
        {
            return ((HtmlSelectOneRadio)selectOne).getLayout();
        }

        return (String)selectOne.getAttributes().get(ComponentAttrs.LAYOUT_ATTR);
    }


    protected String getStyleClass(UISelectOne selectOne)
     {
         if (selectOne instanceof HtmlSelectOneRadio)
         {
             return ((HtmlSelectOneRadio)selectOne).getStyleClass();
         }

         return (String)selectOne.getAttributes().get(ComponentAttrs.STYLE_CLASS_ATTR);
     }

    protected int renderGroupOrItemRadio(FacesContext facesContext,
                                         UIComponent uiComponent, SelectItem selectItem,
                                         Object currentValue,
                                         Converter converter, Boolean usingTable,
                                         Integer itemNum) throws IOException
    {
        return renderGroupOrItemRadio(facesContext, uiComponent, selectItem, 
                currentValue,converter, usingTable, null, itemNum);
    }

    /**
     * Renders the given SelectItem(Group)
     * @return the itemNum for the next item
     */
    protected int renderGroupOrItemRadio(FacesContext facesContext,
                                         UIComponent uiComponent, SelectItem selectItem,
                                         Object currentValue,
                                         Converter converter, Boolean usingTable, String group,
                                         Integer itemNum) throws IOException
    {

        ResponseWriter writer = facesContext.getResponseWriter();

        boolean isSelectItemGroup = (selectItem instanceof SelectItemGroup);

        // TODO : Check here for getSubmittedValue. Look at RendererUtils.getValue
        // this is useless object creation
        // Object itemValue = selectItem.getValue();

        UISelectOne selectOne = (UISelectOne)uiComponent;

        if (isSelectItemGroup) 
        {
            if (usingTable == Boolean.TRUE)
            {
                writer.startElement(HTML.TR_ELEM, null); // selectOne);
            }

            writer.startElement(usingTable != null ? HTML.TD_ELEM : HTML.LI_ELEM, null); // selectOne);
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
                writer.startElement(HTML.TR_ELEM, null); // selectOne);
            }

            if (usingTable != null)
            {
                writer.startElement(HTML.TD_ELEM, null);
            }

            writer.startElement(usingTable != null ? HTML.TABLE_ELEM : HTML.UL_ELEM, null); // selectOne);

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
                writer.startElement(HTML.TR_ELEM, null); // selectOne);
            }

            SelectItemGroup selectItemGroup = (SelectItemGroup) selectItem;
            SelectItem[] selectItems = selectItemGroup.getSelectItems();

            for (SelectItem groupSelectItem : selectItems)
            { 
                itemNum = renderGroupOrItemRadio(facesContext, selectOne, groupSelectItem, currentValue, 
                                                 converter, usingTable, itemNum);
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
            String itemStrValue =  org.apache.myfaces.core.api.shared.SharedRendererUtils.getConvertedStringValue(
                    facesContext, selectOne, converter, selectItem.getValue());
            boolean itemChecked = (itemStrValue == null) ? 
                    itemStrValue == currentValue : 
                    "".equals(itemStrValue) ? 
                            (currentValue == null || itemStrValue.equals(currentValue)) : 
                            itemStrValue.equals(currentValue);
            
            // IF the hideNoSelectionOption attribute of the component is true
            // AND this selectItem is the "no selection option"
            // AND there are currently selected items
            // AND this item (the "no selection option") is not selected
            if (HtmlRendererUtils.isHideNoSelectionOption(uiComponent) && selectItem.isNoSelectionOption() 
                    && currentValue != null && !"".equals(currentValue) && !itemChecked)
            {
                // do not render this selectItem
                return itemNum;
            }
            
            //writer.write("\t\t");
            boolean renderGroupId = false;
            if (group != null && !group.isEmpty())
            {
                //no op
                renderGroupId = true;
            }
            else
            {
                if (usingTable == Boolean.TRUE)
                {
                    writer.startElement(HTML.TR_ELEM, null); // selectOne);
                }
                writer.startElement(usingTable != null ? HTML.TD_ELEM : HTML.LI_ELEM, null); // selectOne);
            }
    
            boolean itemDisabled = selectItem.isDisabled();
    
            String itemId = renderRadio(facesContext, selectOne, itemStrValue, itemDisabled, 
                    itemChecked, renderGroupId, renderGroupId ? null : itemNum);
    
            // label element after the input
            boolean componentDisabled = isDisabled(facesContext, selectOne);
            boolean disabled = (componentDisabled || itemDisabled);
    
            HtmlRendererUtils.renderLabel(writer, selectOne,
                    renderGroupId ? selectOne.getClientId(facesContext) : itemId,
                    selectItem, disabled);

            if (group != null && group.length() > 0)
            {
                //no op
            }
            else
            {
                writer.endElement(usingTable != null ? HTML.TD_ELEM : HTML.LI_ELEM);
                if (usingTable == Boolean.TRUE)
                {
                    writer.endElement(HTML.TR_ELEM);
                }
            }
            
            // we rendered one radio --> increment itemNum
            itemNum++;
        }
        return itemNum;
    }

    /**
     * Renders the input item
     * @return the 'id' value of the rendered element
     */
    protected String renderRadio(FacesContext facesContext,
                               UIInput uiComponent,
                               String value,
                               boolean disabled,
                               boolean checked,
                               boolean renderId,
                               Integer itemNum)
            throws IOException
    {
        String clientId = uiComponent.getClientId(facesContext);

        String itemId = (itemNum == null) ? null : clientId + 
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
        writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_RADIO, null);
        
        String group = uiComponent instanceof HtmlSelectOneRadio ? ((HtmlSelectOneRadio) uiComponent).getGroup() : null;
        if (group != null && !group.isEmpty())
        {
            UIForm form = ComponentUtils.findClosest(UIForm.class, uiComponent);
            writer.writeAttribute(HTML.NAME_ATTR, form.getClientId(facesContext) +
                    facesContext.getNamingContainerSeparatorChar() + group, null);
        }
        else
        {
            writer.writeAttribute(HTML.NAME_ATTR, clientId, null);
        }

        if (disabled)
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, null);
        }

        if (checked)
        {
            writer.writeAttribute(HTML.CHECKED_ATTR, HTML.CHECKED_ATTR, null);
        }

        if (group != null && group.length() > 0)
        {
            if (value != null)
            {
                writer.writeAttribute(HTML.VALUE_ATTR, 
                        clientId + facesContext.getNamingContainerSeparatorChar() + value, null);
            }
            else
            {
                writer.writeAttribute(HTML.VALUE_ATTR, 
                        clientId + facesContext.getNamingContainerSeparatorChar() + "", null);
            }
        }
        else
        {
            if (value != null)
            {
                writer.writeAttribute(HTML.VALUE_ATTR, value, null);
            }
            else
            {
                writer.writeAttribute(HTML.VALUE_ATTR, "", null);
            }
        }
        
        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            
            long commonPropertiesMarked = 0L;
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                commonPropertiesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(uiComponent);
            }
            if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonHtmlAttributesUtil.renderChangeEventProperty(writer, 
                        commonPropertiesMarked, uiComponent);
                CommonHtmlAttributesUtil.renderEventProperties(writer, 
                        commonPropertiesMarked, uiComponent);
                CommonHtmlAttributesUtil.renderFieldEventPropertiesWithoutOnchange(writer, 
                        commonPropertiesMarked, uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(facesContext, writer, uiComponent, 
                        itemId != null ? itemId : clientId, behaviors);
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
                    HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, uiComponent,
                            itemId != null ? itemId : clientId, behaviors);
                    HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                            facesContext, writer, uiComponent,
                            itemId != null ? itemId : clientId, behaviors);
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

        if (isDisabled(facesContext, uiComponent))
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, null);
        }

        writer.endElement(HTML.INPUT_ELEM);

        return itemId;
    }


    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlSelectOneRadio)
        {
            return ((HtmlSelectOneRadio)uiComponent).isDisabled();
        }

        return AttributeUtils.getBooleanAttribute(uiComponent, HTML.DISABLED_ATTR, false);
    }

    @Override
    public void decode(FacesContext facesContext, UIComponent uiComponent)
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, null);
        if (uiComponent instanceof UIInput)
        {
            HtmlRendererUtils.decodeUISelectOne(facesContext, uiComponent);
        }
        if (uiComponent instanceof ClientBehaviorHolder && !HtmlRendererUtils.isDisabled(uiComponent))
        {
            ClientBehaviorRendererUtils.decodeClientBehaviors(facesContext, uiComponent);
        }
    }

    @Override
    public Object getConvertedValue(FacesContext facesContext, UIComponent uiComponent, Object submittedValue)
        throws ConverterException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UISelectOne.class);
        return RendererUtils.getConvertedUISelectOneValue(facesContext,
                (UISelectOne)uiComponent, submittedValue);
    }
    
    private static class GetSelectItemListCallback implements VisitCallback
    {
        private final UISelectOne selectOneRadio;
        private final String group;
        
        private final List<UISelectOne> selectOneRadios;
        
        private int index;
        private SelectItem selectItem;
        private Converter converter;
        private Object currentValue;

        public GetSelectItemListCallback(UISelectOne selectOneRadio, String group)
        {
            this.selectOneRadio = selectOneRadio;
            this.group = group;
            
            this.selectOneRadios = new ArrayList<>();
        }

        @Override
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (target instanceof UISelectOne)
            {
                UISelectOne targetSelectOneRadio = ((UISelectOne) target);
                String targetGroup = targetSelectOneRadio.getGroup();
                if (group.equals(targetGroup))
                {
                    selectOneRadios.add(targetSelectOneRadio);
                    
                    // check if the current selectOneRadio was already visited
                    index = selectOneRadios.indexOf(selectOneRadio);
                    if (index != -1)
                    {                        
                        UISelectOne first = selectOneRadios.get(0);

                        // if we were found,
                        // lets take the selectItems from the first selectOneRadio of our group
                        List<SelectItem> selectItemList = RendererUtils.getSelectItemList(first,
                                context.getFacesContext());
                        if (selectItemList == null || selectItemList.isEmpty())
                        {
                            throw new FacesException("UISelectOne with id=\"" + first.getId()
                                            + "\" and group=\"" + group + "\" does not have any UISelectItems!");
                        }
                        
                        // evaluate required infos from the first selectOneRadio of our group
                        selectItem = selectItemList.get(index);
                        converter = HtmlRendererUtils.findUIOutputConverterFailSafe(context.getFacesContext(), first);
                        currentValue = RendererUtils.getStringFromSubmittedValueOrLocalValueReturnNull(
                                context.getFacesContext(), first);
                        
                        return VisitResult.COMPLETE;
                    }
                    
                    return VisitResult.REJECT;
                }
                else
                {
                    return VisitResult.REJECT;
                }
            }

            return VisitResult.ACCEPT;
        }

        public int getIndex()
        {
            return index;
        }
        
        public SelectItem getSelectItem()
        {
            return selectItem;
        }
        
        public Converter getConverter()
        {
            return converter;
        }

        public Object getCurrentValue()
        {
            return currentValue;
        }
    }

}
