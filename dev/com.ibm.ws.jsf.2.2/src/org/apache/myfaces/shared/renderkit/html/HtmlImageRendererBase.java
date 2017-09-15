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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.ProjectStage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIGraphic;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;


public class HtmlImageRendererBase
        extends HtmlRenderer
{
    //private static final Log log = LogFactory.getLog(HtmlImageRendererBase.class);
    private static final Logger log = Logger.getLogger(HtmlImageRendererBase.class.getName());
    
    public void decode(FacesContext context, UIComponent component)
    {
        //check for npe
        super.decode(context, component);
        
        HtmlRendererUtils.decodeClientBehaviors(context, component);
    }

    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(
                facesContext, uiComponent, UIGraphic.class);

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
        
        writer.startElement(HTML.IMG_ELEM, uiComponent);

        if (uiComponent instanceof ClientBehaviorHolder
                && !behaviors.isEmpty())
        {
            HtmlRendererUtils.writeIdAndName(writer, uiComponent, facesContext);
        }
        else
        {
            HtmlRendererUtils.writeIdIfNecessary(writer, uiComponent, facesContext);
        }

        final String url = RendererUtils.getIconSrc(facesContext, uiComponent, JSFAttr.URL_ATTR);
        if (url != null)
        {
            writer.writeURIAttribute(HTML.SRC_ATTR, url,JSFAttr.VALUE_ATTR);
        }
        else
        {
          if (facesContext.isProjectStage(ProjectStage.Development) && log.isLoggable(Level.WARNING))
          {
              log.warning("Component UIGraphic " + uiComponent.getClientId(facesContext) 
                      + " has no attribute url, value, name or attribute resolves to null. Path to component " 
                      + RendererUtils.getPathToComponent(uiComponent));
          }
        }

        /* 
         * Warn the user if the ALT attribute is missing.
         */                
        if (uiComponent.getAttributes().get(HTML.ALT_ATTR) == null) 
        {
            if(facesContext.isProjectStage(ProjectStage.Development) && log.isLoggable(Level.WARNING))
            {
                log.warning("Component UIGraphic " + uiComponent.getClientId(facesContext) 
                        + " has no attribute alt or attribute resolves to null. Path to component " 
                        + RendererUtils.getPathToComponent(uiComponent));
            }
        }

        if (uiComponent instanceof ClientBehaviorHolder)
        {
            if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderEventProperties(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
            }
            else
            {
                if (isCommonEventsOptimizationEnabled(facesContext))
                {
                    CommonEventUtils.renderBehaviorizedEventHandlers(facesContext, writer, 
                           CommonPropertyUtils.getCommonPropertiesMarked(uiComponent),
                           CommonEventUtils.getCommonEventsMarked(uiComponent), uiComponent, behaviors);
                }
                else
                {
                    HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, uiComponent, behaviors);
                }
            }
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.IMG_ATTRIBUTES);
                CommonPropertyUtils.renderCommonPassthroughPropertiesWithoutEvents(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, 
                        HTML.IMG_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            }
        }
        else
        {
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, HTML.IMG_ATTRIBUTES);
                CommonPropertyUtils.renderCommonPassthroughProperties(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent, 
                        HTML.IMG_PASSTHROUGH_ATTRIBUTES);
            }
        }

        writer.endElement(org.apache.myfaces.shared.renderkit.html.HTML.IMG_ELEM);

    }

}
