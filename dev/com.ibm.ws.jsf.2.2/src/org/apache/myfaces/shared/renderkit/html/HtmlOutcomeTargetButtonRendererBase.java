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
import javax.faces.component.UIOutcomeTarget;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlOutcomeTargetButton;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

/**
 * @since 2.0
 */
public class HtmlOutcomeTargetButtonRendererBase extends HtmlRenderer
{

    public boolean getRendersChildren()
    {
        return true;
    }

    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        super.encodeBegin(facesContext, uiComponent); //check for NP

        String clientId = uiComponent.getClientId(facesContext);

        ResponseWriter writer = facesContext.getResponseWriter();

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, facesContext.getResponseWriter());
            }
        }

        writer.startElement(HTML.INPUT_ELEM, uiComponent);

        writer.writeAttribute(HTML.ID_ATTR, clientId, JSFAttr.ID_ATTR);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, JSFAttr.ID_ATTR);

        String image = getImage(uiComponent);
        ExternalContext externalContext = facesContext.getExternalContext();

        if (image != null)
        {
            // type="image"
            writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_IMAGE, JSFAttr.TYPE_ATTR);
            String src = facesContext.getApplication().getViewHandler().getResourceURL(facesContext, image);
            writer.writeURIAttribute(HTML.SRC_ATTR, externalContext.encodeResourceURL(src), JSFAttr.IMAGE_ATTR);
        }
        else
        {
            // type="button"
            writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_BUTTON, JSFAttr.TYPE_ATTR);
            Object value = RendererUtils.getStringValue(facesContext, uiComponent);
            if (value != null)
            {
                writer.writeAttribute(HTML.VALUE_ATTR, value, JSFAttr.VALUE_ATTR);
            }
        }

        String outcomeTargetHref = HtmlRendererUtils.getOutcomeTargetHref(
                    facesContext, (UIOutcomeTarget) uiComponent);

        if (HtmlRendererUtils.isDisabled(uiComponent) || outcomeTargetHref == null)
        {
            // disable the button - if disabled is true or no fitting NavigationCase was found
            HtmlRendererUtils.renderHTMLAttribute(writer, HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, true);
        }
        else
        {
            // render onClick attribute
            String href = facesContext.getExternalContext().encodeResourceURL(outcomeTargetHref);

            String commandOnClick = (String) uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);
            String navigationJavaScript = "window.location.href = '" + href + "'";

            if (behaviors != null && !behaviors.isEmpty())
            {
                HtmlRendererUtils.renderBehaviorizedAttribute(facesContext, writer, HTML.ONCLICK_ATTR, 
                        uiComponent, ClientBehaviorEvents.CLICK, null, behaviors, HTML.ONCLICK_ATTR, 
                        commandOnClick, navigationJavaScript);
            }
            else
            {
                StringBuilder onClick = new StringBuilder();
    
                if (commandOnClick != null)
                {
                    onClick.append(commandOnClick);
                    onClick.append(';');
                }
                onClick.append(navigationJavaScript);
    
                writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
            }
        }

        if (isCommonPropertiesOptimizationEnabled(facesContext))
        {
            long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(uiComponent);
            
            if (behaviors != null && !behaviors.isEmpty() && uiComponent instanceof ClientBehaviorHolder)
            {
                HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(
                        facesContext, writer, uiComponent, behaviors);
                HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(
                        facesContext, writer, uiComponent, behaviors);
            }
            else
            {
                CommonPropertyUtils.renderEventPropertiesWithoutOnclick(
                        writer, commonPropertiesMarked, uiComponent);
                CommonPropertyUtils.renderFocusBlurEventProperties(writer, commonPropertiesMarked, uiComponent);
            }
            
            CommonPropertyUtils.renderCommonFieldPassthroughPropertiesWithoutDisabledAndEvents(
                    writer, commonPropertiesMarked, uiComponent);
            if ((commonPropertiesMarked & CommonPropertyConstants.ALT_PROP) != 0)
            {
                HtmlRendererUtils.renderHTMLStringAttribute(writer, uiComponent,
                        HTML.ALT_ATTR, HTML.ALT_ATTR);
            }
        }
        else
        {
            if (uiComponent instanceof ClientBehaviorHolder)
            {
                HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(
                        facesContext, writer, uiComponent, behaviors);
                HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(
                        facesContext, writer, uiComponent, behaviors);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.EVENT_HANDLER_ATTRIBUTES_WITHOUT_ONCLICK);
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.COMMON_FIELD_EVENT_ATTRIBUTES_WITHOUT_ONSELECT_AND_ONCHANGE);
    
            }
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                    HTML.COMMON_FIELD_PASSTROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
            HtmlRendererUtils.renderHTMLStringAttribute(writer, uiComponent,
                    HTML.ALT_ATTR, HTML.ALT_ATTR);
        }

        writer.flush();
    }

    private String getImage(UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlOutcomeTargetButton)
        {
            return ((HtmlOutcomeTargetButton) uiComponent).getImage();
        }
        return (String) uiComponent.getAttributes().get(JSFAttr.IMAGE_ATTR);
    }

    public void encodeChildren(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        RendererUtils.renderChildren(facesContext, component);
    }

    public void encodeEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeEnd(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();

        writer.endElement(HTML.INPUT_ELEM);
    }
}
