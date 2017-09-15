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

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;

import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;


public class HtmlTextareaRendererBase
        extends HtmlRenderer
{
    private static final String ADD_NEW_LINE_AT_START_ATTR = "org.apache.myfaces.addNewLineAtStart";
    
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UIInput.class);

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, 
                        facesContext.getResponseWriter());
            }
        }
        
        encodeTextArea(facesContext, uiComponent);

    }

    protected void encodeTextArea(FacesContext facesContext, UIComponent uiComponent) 
        throws IOException
    {
       //allow subclasses to render custom attributes by separating rendering begin and end
        renderTextAreaBegin(facesContext, uiComponent);
        renderTextAreaValue(facesContext, uiComponent);
        renderTextAreaEnd(facesContext, uiComponent);
        
    }

    //Subclasses can set the value of an attribute before, or can render a custom attribute after calling this method
    protected void renderTextAreaBegin(FacesContext facesContext,
            UIComponent uiComponent) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HTML.TEXTAREA_ELEM, uiComponent);

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                HtmlRendererUtils.writeIdAndName(writer, uiComponent, facesContext);
            }
            else
            {
                HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
                writer.writeAttribute(HTML.NAME_ATTR, uiComponent.getClientId(facesContext), null);
            }
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
                CommonPropertyUtils.renderFieldEventPropertiesWithoutOnchange(writer, 
                        commonPropertiesMarked, uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(facesContext, writer, uiComponent, behaviors);
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
                    HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, uiComponent, behaviors);
                    HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                            facesContext, writer, uiComponent, behaviors);
                }
            }
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderCommonFieldPassthroughPropertiesWithoutDisabledAndEvents(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.TEXTAREA_ATTRIBUTES);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, 
                        HTML.TEXTAREA_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
            }
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
            writer.writeAttribute(HTML.NAME_ATTR, uiComponent.getClientId(facesContext), null);
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderCommonFieldPassthroughPropertiesWithoutDisabled(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.TEXTAREA_ATTRIBUTES);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, 
                        HTML.TEXTAREA_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);
            }
        }

        if (isDisabled(facesContext, uiComponent))
        {
            writer.writeAttribute(org.apache.myfaces.shared.renderkit.html.HTML.DISABLED_ATTR, Boolean.TRUE, null);
        }
    }

    //Subclasses can override the writing of the "text" value of the textarea
    protected void renderTextAreaValue(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        
        Object addNewLineAtStart = uiComponent.getAttributes().get(ADD_NEW_LINE_AT_START_ATTR);
        if (addNewLineAtStart != null)
        {
            boolean addNewLineAtStartBoolean = false;
            if (addNewLineAtStart instanceof String)
            {
                addNewLineAtStartBoolean = Boolean.valueOf((String)addNewLineAtStart);
            }
            else if (addNewLineAtStart instanceof Boolean)
            {
                addNewLineAtStartBoolean = (Boolean) addNewLineAtStart;
            }
            if (addNewLineAtStartBoolean)
            {
                writer.writeText("\n", null);
            }
        }
        
        String strValue = org.apache.myfaces.shared.renderkit.RendererUtils.getStringValue(facesContext, uiComponent);
        if (strValue != null)
        {
            writer.writeText(strValue, org.apache.myfaces.shared.renderkit.JSFAttr.VALUE_ATTR);
        }
    }
    
    protected void renderTextAreaEnd(FacesContext facesContext,
            UIComponent uiComponent) throws IOException
    {
        facesContext.getResponseWriter().endElement(HTML.TEXTAREA_ELEM);
    }
    
    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        //TODO: overwrite in extended HtmlTextareaRenderer and check for enabledOnUserRole
        if (uiComponent instanceof HtmlInputTextarea)
        {
            return ((HtmlInputTextarea)uiComponent).isDisabled();
        }

        return org.apache.myfaces.shared.renderkit.RendererUtils.getBooleanAttribute(
                uiComponent, HTML.DISABLED_ATTR, false);
        
    }

    public void decode(FacesContext facesContext, UIComponent component)
    {
        RendererUtils.checkParamValidity(facesContext, component, UIInput.class);
        HtmlRendererUtils.decodeUIInput(facesContext, component);
        if (component instanceof ClientBehaviorHolder &&
            !HtmlRendererUtils.isDisabled(component))
        {
            HtmlRendererUtils.decodeClientBehaviors(facesContext, component);
        }
    }

    public Object getConvertedValue(FacesContext facesContext, UIComponent uiComponent, Object submittedValue)
        throws ConverterException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UIOutput.class);
        return org.apache.myfaces.shared.renderkit.RendererUtils.getConvertedUIOutputValue(facesContext,
                                                       (UIOutput)uiComponent,
                                                       submittedValue);
    }

}
