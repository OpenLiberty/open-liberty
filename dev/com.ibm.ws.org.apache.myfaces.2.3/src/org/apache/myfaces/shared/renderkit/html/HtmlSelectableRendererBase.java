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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectMany;
import javax.faces.component.UISelectOne;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import org.apache.myfaces.shared.renderkit.html.util.SelectItemInfo;
import org.apache.myfaces.shared.renderkit.html.util.SelectItemsUtils;

public class HtmlSelectableRendererBase extends HtmlRenderer
{
    
    protected void internalRenderSelect(FacesContext facesContext,
            UIComponent uiComponent, boolean disabled, int size,
            boolean selectMany, Converter converter) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HTML.SELECT_ELEM, uiComponent);
        if (uiComponent instanceof ClientBehaviorHolder
                && !((ClientBehaviorHolder) uiComponent).getClientBehaviors()
                        .isEmpty())
        {
            writer.writeAttribute(HTML.ID_ATTR,
                    uiComponent.getClientId(facesContext), null);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent,
                    facesContext);
        }
        writer.writeAttribute(HTML.NAME_ATTR,
                uiComponent.getClientId(facesContext), null);
        List<SelectItemInfo> selectItemList;
        if (selectMany)
        {
            writer.writeAttribute(HTML.MULTIPLE_ATTR, HTML.MULTIPLE_ATTR, null);
            selectItemList = SelectItemsUtils.getSelectItemInfoList((UISelectMany) uiComponent, facesContext);
        }
        else
        {
            selectItemList = SelectItemsUtils.getSelectItemInfoList(
                    (UISelectOne) uiComponent, facesContext);
        }

        if (size == Integer.MIN_VALUE)
        {
            //No size given (Listbox) --> size is number of select items
            writer.writeAttribute(HTML.SIZE_ATTR,
                    Integer.toString(selectItemList.size()), null);
        }
        else
        {
            writer.writeAttribute(HTML.SIZE_ATTR, Integer.toString(size), null);
        }
        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent)
                    .getClientBehaviors();
            long commonPropertiesMarked = 0L;
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(uiComponent);
            }
            if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderChangeEventProperty(writer, 
                        commonPropertiesMarked, uiComponent);
                CommonPropertyUtils.renderEventProperties(writer, 
                        commonPropertiesMarked, uiComponent);
                // Note that on JSF 2.3, selectable components don't need onselect attribute
                // Please see https://issues.apache.org/jira/browse/MYFACES-4190
                CommonPropertyUtils.renderFieldEventPropertiesWithoutOnchangeAndOnselect(writer, 
                        commonPropertiesMarked, uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(facesContext, writer,
                        uiComponent, behaviors);
                if (isCommonEventsOptimizationEnabled(facesContext))
                {
                    Long commonEventsMarked = CommonEventUtils.getCommonEventsMarked(uiComponent);
                    CommonEventUtils.renderBehaviorizedEventHandlers(facesContext, writer, 
                            commonPropertiesMarked, commonEventsMarked, uiComponent, behaviors);
                    CommonEventUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(
                        facesContext, writer, commonPropertiesMarked, commonEventsMarked, uiComponent, behaviors);
                }
                else
                {
                    HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, uiComponent,
                            behaviors);
                    HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(facesContext,
                            writer, uiComponent, behaviors);
                }
            }
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderSelectPassthroughPropertiesWithoutDisabledAndEvents(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(
                        writer,
                        uiComponent,
                        HTML.SELECT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
            }
        }
        else
        {
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderSelectPassthroughPropertiesWithoutDisabled(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.SELECT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);
            }
        }

        if (disabled)
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, Boolean.TRUE, null);
        }

        if (HtmlRendererUtils.isReadOnly(uiComponent))
        {
            writer.writeAttribute(HTML.READONLY_ATTR, HTML.READONLY_ATTR, null);
        }

        Set lookupSet = HtmlRendererUtils.getSubmittedOrSelectedValuesAsSet(selectMany,
                uiComponent, facesContext, converter);

        SelectItemsUtils.renderSelectOptions(facesContext, uiComponent, converter, lookupSet,
                selectItemList);
        // bug #970747: force separate end tag
        writer.writeText(HtmlRendererUtils.STR_EMPTY, null);
        writer.endElement(HTML.SELECT_ELEM);
    }
    
}
