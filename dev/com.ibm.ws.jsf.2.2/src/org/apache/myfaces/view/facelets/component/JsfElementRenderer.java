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
package org.apache.myfaces.view.facelets.component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.CommonEventUtils;
import org.apache.myfaces.shared.renderkit.html.CommonPropertyUtils;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.HtmlRenderer;
import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

/**
 *
 * @author Leonardo Uribe
 */
@JSFRenderer(
    renderKitId = "HTML_BASIC",
    family = "javax.faces.Panel",
    type = "javax.faces.passthrough.Element")
public class JsfElementRenderer extends HtmlRenderer
{

    public boolean getRendersChildren()
    {
        return true;
    }
    
    @Override
    public void decode(FacesContext context, UIComponent component)
    {
        // Check for npe
        super.decode(context, component);
        
        HtmlRendererUtils.decodeClientBehaviors(context, component);
    }

    public void encodeBegin(FacesContext facesContext, UIComponent component)
        throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        String elementName = (String) 
            component.getPassThroughAttributes().get(Renderer.PASSTHROUGH_RENDERER_LOCALNAME_KEY);

        if (elementName == null)
        {
            throw new FacesException("jsf:element with clientId"
                + component.getClientId(facesContext) + " requires 'elementName' passthrough attribute");
        }
        JsfElement jsfElement = (JsfElement) component;
        Map<String, List<ClientBehavior>> behaviors = jsfElement.getClientBehaviors();
        
        if (behaviors != null && !behaviors.isEmpty())
        {
            ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
        }
        
        writer.startElement(elementName, component);

        if (!behaviors.isEmpty())
        {
            HtmlRendererUtils.writeIdAndName(writer, component, facesContext);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
        }
        
        // Write in the optimized way, because this is a renderer for internal use only
        long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(component);
        if (behaviors.isEmpty())
        {
            CommonPropertyUtils.renderEventProperties(writer, commonPropertiesMarked, component);
            CommonPropertyUtils.renderFocusBlurEventProperties(writer, commonPropertiesMarked, component);
            CommonPropertyUtils.renderChangeSelectEventProperties(writer, commonPropertiesMarked, component);
        }
        else
        {
            long commonEventsMarked = CommonEventUtils.getCommonEventsMarked(component);
            CommonEventUtils.renderBehaviorizedEventHandlers(facesContext, writer, 
                   commonPropertiesMarked, commonEventsMarked, component, behaviors);
            CommonEventUtils.renderBehaviorizedFieldEventHandlers(facesContext, writer, 
                   commonPropertiesMarked, commonEventsMarked, component, 
                   component.getClientId(facesContext), behaviors);
        }
        CommonPropertyUtils.renderStyleProperties(writer, commonPropertiesMarked, component);
        HtmlRendererUtils.renderBehaviorizedAttribute(facesContext, writer, HTML.ONLOAD_ATTR, component,
                ClientBehaviorEvents.LOAD, behaviors, HTML.ONLOAD_ATTR);
        HtmlRendererUtils.renderBehaviorizedAttribute(facesContext, writer, HTML.ONUNLOAD_ATTR, component,
                ClientBehaviorEvents.UNLOAD, behaviors, HTML.ONUNLOAD_ATTR);
        
    }

    public void encodeChildren(FacesContext facesContext, UIComponent component)
        throws IOException
    {
        RendererUtils.renderChildren(facesContext, component);
    }

    public void encodeEnd(FacesContext facesContext, UIComponent component)
        throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        String elementName = (String) component.getPassThroughAttributes().get(
            Renderer.PASSTHROUGH_RENDERER_LOCALNAME_KEY);
        writer.endElement(elementName);
    }
    
    protected boolean isCommonPropertiesOptimizationEnabled(FacesContext facesContext)
    {
        return true;
    }
    
    protected boolean isCommonEventsOptimizationEnabled(FacesContext facesContext)
    {
        return true;
    }
}
