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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.apache.myfaces.shared.renderkit.ClientBehaviorEvents;

public class CommonEventUtils
{
    public static long getCommonEventsMarked(UIComponent component)
    {
        Long commonEvents = (Long) component.getAttributes().get(CommonEventConstants.COMMON_EVENTS_MARKED);
        
        if (commonEvents == null)
        {
            commonEvents = 0L;
        }
        return commonEvents;
    }
        
    /**
     * Render an attribute taking into account the passed event and
     * the component property. It will be rendered as "componentProperty"
     * attribute.
     *
     * @param facesContext
     * @param writer
     * @param componentProperty
     * @param component
     * @param eventName
     * @param clientBehaviors
     * @return
     * @throws IOException
     * @since 4.0.1
     */
    /*
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, eventName, clientBehaviors,
                componentProperty);
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String targetClientId, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, targetClientId, eventName,
                clientBehaviors, componentProperty);
    }*/

    /**
     * Render an attribute taking into account the passed event and
     * the component property. The event will be rendered on the selected
     * htmlAttrName
     *
     * @param facesContext
     * @param writer
     * @param component
     * @param clientBehaviors
     * @param eventName
     * @param componentProperty
     * @param htmlAttrName
     * @return
     * @throws IOException
     * @since 4.0.1
     */
    /*
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName) throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, eventName, null, clientBehaviors,
                htmlAttrName,
                (String) component.getAttributes().get(componentProperty));
    }*/

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String targetClientId, String eventName,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName) throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component, targetClientId, eventName, null,
                clientBehaviors, htmlAttrName, (String) component
                        .getAttributes().get(componentProperty));
    }

    /**
     * Render an attribute taking into account the passed event,
     * the component property and the passed attribute value for the component
     * property. The event will be rendered on the selected htmlAttrName.
     *
     * @param facesContext
     * @param writer
     * @param componentProperty
     * @param component
     * @param eventName
     * @param clientBehaviors
     * @param htmlAttrName
     * @param attributeValue
     * @return
     * @throws IOException
     */
    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue) throws IOException
    {
        return renderBehaviorizedAttribute(facesContext, writer,
                componentProperty, component,
                component.getClientId(facesContext), eventName,
                eventParameters, clientBehaviors, htmlAttrName, attributeValue);
    }

    public static boolean renderBehaviorizedAttribute(
            FacesContext facesContext, ResponseWriter writer,
            String componentProperty, UIComponent component,
            String targetClientId, String eventName,
            Collection<ClientBehaviorContext.Parameter> eventParameters,
            Map<String, List<ClientBehavior>> clientBehaviors,
            String htmlAttrName, String attributeValue) throws IOException
    {

        List<ClientBehavior> cbl = (clientBehaviors != null) ? clientBehaviors
                .get(eventName) : null;

        if (cbl == null || cbl.size() == 0)
        {
            return HtmlRendererUtils.renderHTMLStringAttribute(writer, componentProperty, htmlAttrName,
                    attributeValue);
        }

        if (cbl.size() > 1 || (cbl.size() == 1 && attributeValue != null))
        {
            return HtmlRendererUtils.renderHTMLStringAttribute(writer, componentProperty, htmlAttrName,
                    HtmlRendererUtils.buildBehaviorChain(facesContext,
                            component, targetClientId, eventName,
                            eventParameters, clientBehaviors, attributeValue,
                            HtmlRendererUtils.STR_EMPTY));
        }
        else
        {
            //Only 1 behavior and attrValue == null, so just render it directly
            return HtmlRendererUtils.renderHTMLStringAttribute(
                    writer,
                    componentProperty,
                    htmlAttrName,
                    cbl.get(0).getScript(
                            ClientBehaviorContext.createClientBehaviorContext(
                                    facesContext, component, eventName,
                                    targetClientId, eventParameters)));
        }
    }

    public static void renderBehaviorizedEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedEventHandlers(facesContext, writer, 
                commonPropertiesMarked, commonEventsMarked, uiComponent,
                uiComponent.getClientId(facesContext), clientBehaviors);
    }
    
    public static void renderBehaviorizedEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        if ((commonPropertiesMarked & CommonPropertyConstants.ONCLICK_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.CLICK_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONCLICK_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.CLICK,
                    clientBehaviors, HTML.ONCLICK_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONDBLCLICK_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.DBLCLICK_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONDBLCLICK_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.DBLCLICK,
                    clientBehaviors, HTML.ONDBLCLICK_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEDOWN_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEDOWN_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer,
                    HTML.ONMOUSEDOWN_ATTR, uiComponent, targetClientId,
                    ClientBehaviorEvents.MOUSEDOWN, clientBehaviors,
                    HTML.ONMOUSEDOWN_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEUP_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEUP_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEUP_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.MOUSEUP,
                    clientBehaviors, HTML.ONMOUSEUP_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEOVER_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEOVER_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer,
                    HTML.ONMOUSEOVER_ATTR, uiComponent, targetClientId,
                    ClientBehaviorEvents.MOUSEOVER, clientBehaviors,
                    HTML.ONMOUSEOVER_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEMOVE_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEMOVE_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer,
                    HTML.ONMOUSEMOVE_ATTR, uiComponent, targetClientId,
                    ClientBehaviorEvents.MOUSEMOVE, clientBehaviors,
                    HTML.ONMOUSEMOVE_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEOUT_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEOUT_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEOUT_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.MOUSEOUT,
                    clientBehaviors, HTML.ONMOUSEOUT_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONKEYPRESS_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.KEYPRESS_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYPRESS_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.KEYPRESS,
                    clientBehaviors, HTML.ONKEYPRESS_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONKEYDOWN_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.KEYDOWN_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYDOWN_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.KEYDOWN,
                    clientBehaviors, HTML.ONKEYDOWN_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONKEYUP_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.KEYUP_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYUP_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.KEYUP,
                    clientBehaviors, HTML.ONKEYUP_ATTR);
        }
    }

    public static void renderBehaviorizedEventHandlersWithoutOnclick(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, 
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedEventHandlersWithoutOnclick(facesContext, writer, 
                commonPropertiesMarked, commonEventsMarked, uiComponent,
                uiComponent.getClientId(facesContext), clientBehaviors);
    }

    /**
     * @param facesContext
     * @param writer
     * @param uiComponent
     * @param clientBehaviors
     * @throws IOException
     * @since 4.0.0
     */
    public static void renderBehaviorizedEventHandlersWithoutOnclick(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        if ((commonPropertiesMarked & CommonPropertyConstants.ONDBLCLICK_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.DBLCLICK_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONDBLCLICK_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.DBLCLICK,
                    clientBehaviors, HTML.ONDBLCLICK_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEDOWN_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEDOWN_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer,
                    HTML.ONMOUSEDOWN_ATTR, uiComponent, targetClientId,
                    ClientBehaviorEvents.MOUSEDOWN, clientBehaviors,
                    HTML.ONMOUSEDOWN_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEUP_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEUP_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEUP_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.MOUSEUP,
                    clientBehaviors, HTML.ONMOUSEUP_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEOVER_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEOVER_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer,
                    HTML.ONMOUSEOVER_ATTR, uiComponent, targetClientId,
                    ClientBehaviorEvents.MOUSEOVER, clientBehaviors,
                    HTML.ONMOUSEOVER_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEMOVE_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEMOVE_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer,
                    HTML.ONMOUSEMOVE_ATTR, uiComponent, targetClientId,
                    ClientBehaviorEvents.MOUSEMOVE, clientBehaviors,
                    HTML.ONMOUSEMOVE_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONMOUSEOUT_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.MOUSEOUT_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONMOUSEOUT_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.MOUSEOUT,
                    clientBehaviors, HTML.ONMOUSEOUT_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONKEYPRESS_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.KEYPRESS_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYPRESS_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.KEYPRESS,
                    clientBehaviors, HTML.ONKEYPRESS_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONKEYDOWN_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.KEYDOWN_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYDOWN_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.KEYDOWN,
                    clientBehaviors, HTML.ONKEYDOWN_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONKEYUP_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.KEYUP_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONKEYUP_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.KEYUP,
                    clientBehaviors, HTML.ONKEYUP_ATTR);
        }
    }

    /**
     * @param facesContext
     * @param writer
     * @param uiComponent
     * @param clientBehaviors
     * @throws IOException
     * @since 4.0.0
     */
    public static void renderBehaviorizedFieldEventHandlers(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        if ((commonPropertiesMarked & CommonPropertyConstants.ONFOCUS_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.FOCUS_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.FOCUS, clientBehaviors,
                    HTML.ONFOCUS_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONBLUR_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.BLUR_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.BLUR, clientBehaviors,
                    HTML.ONBLUR_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONCHANGE_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.CHANGE_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONCHANGE_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.CHANGE, clientBehaviors,
                    HTML.ONCHANGE_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONSELECT_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.SELECT_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.SELECT, clientBehaviors,
                    HTML.ONSELECT_ATTR);
        }
    }

    public static void renderBehaviorizedFieldEventHandlersWithoutOnfocus(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        if ((commonPropertiesMarked & CommonPropertyConstants.ONBLUR_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.BLUR_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.BLUR, clientBehaviors,
                    HTML.ONBLUR_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONCHANGE_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.CHANGE_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONCHANGE_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.CHANGE, clientBehaviors,
                    HTML.ONCHANGE_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONSELECT_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.SELECT_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.SELECT, clientBehaviors,
                    HTML.ONSELECT_ATTR);
        }
    }

    public static void renderBehaviorizedFieldEventHandlersWithoutOnchange(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, 
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedFieldEventHandlersWithoutOnchange(
                facesContext, writer, commonPropertiesMarked, commonEventsMarked, 
                uiComponent, uiComponent.getClientId(facesContext), clientBehaviors);
    }
    
    public static void renderBehaviorizedFieldEventHandlersWithoutOnchange(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        if ((commonPropertiesMarked & CommonPropertyConstants.ONFOCUS_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.FOCUS_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.FOCUS, clientBehaviors,
                    HTML.ONFOCUS_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONBLUR_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.BLUR_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.BLUR, clientBehaviors,
                    HTML.ONBLUR_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONSELECT_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.SELECT_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONSELECT_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.SELECT, clientBehaviors,
                    HTML.ONSELECT_ATTR);
        }
    }

    public static void renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(
                facesContext, writer, 
                commonPropertiesMarked, commonEventsMarked, 
                uiComponent, uiComponent.getClientId(facesContext), 
                clientBehaviors);
    }
    
    public static void renderBehaviorizedFieldEventHandlersWithoutOnchangeAndOnselect(
            FacesContext facesContext, ResponseWriter writer,
            long commonPropertiesMarked, long commonEventsMarked,
            UIComponent uiComponent, String targetClientId,
            Map<String, List<ClientBehavior>> clientBehaviors)
            throws IOException
    {
        if ((commonPropertiesMarked & CommonPropertyConstants.ONFOCUS_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.FOCUS_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONFOCUS_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.FOCUS, clientBehaviors,
                    HTML.ONFOCUS_ATTR);
        }
        if ((commonPropertiesMarked & CommonPropertyConstants.ONBLUR_PROP) != 0 ||
            (commonEventsMarked & CommonEventConstants.BLUR_EVENT) != 0)
        {
            renderBehaviorizedAttribute(facesContext, writer, HTML.ONBLUR_ATTR,
                    uiComponent, targetClientId, ClientBehaviorEvents.BLUR, clientBehaviors,
                    HTML.ONBLUR_ATTR);
        }
    }
}
