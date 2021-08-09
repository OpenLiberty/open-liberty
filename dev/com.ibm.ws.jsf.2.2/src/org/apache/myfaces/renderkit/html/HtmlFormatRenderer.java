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
package org.apache.myfaces.renderkit.html;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlOutputFormat;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.CommonPropertyConstants;
import org.apache.myfaces.shared.renderkit.html.CommonPropertyUtils;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.HtmlRenderer;
import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;

/**
 * 
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Thomas Spiegl
 * @version $Revision: 1364636 $ $Date: 2012-07-23 14:00:12 +0000 (Mon, 23 Jul 2012) $
 */
@JSFRenderer(renderKitId = "HTML_BASIC", family = "javax.faces.Output", type = "javax.faces.Format")
public class HtmlFormatRenderer extends HtmlRenderer
{
    //private static final Log log = LogFactory.getLog(HtmlFormatRenderer.class);
    private static final Logger log = Logger.getLogger(HtmlFormatRenderer.class.getName());

    private static final Object[] EMPTY_ARGS = new Object[0];
    
    @Override
    protected boolean isCommonPropertiesOptimizationEnabled(FacesContext facesContext)
    {
        return true;
    }

    @Override
    protected boolean isCommonEventsOptimizationEnabled(FacesContext facesContext)
    {
        return true;
    }

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
    }

    @Override
    public void encodeChildren(FacesContext facescontext, UIComponent uicomponent) throws IOException
    {
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
    {
        RendererUtils.checkParamValidity(facesContext, component, UIOutput.class);

        String text = getOutputFormatText(facesContext, component);
        boolean escape;
        if (component instanceof HtmlOutputFormat)
        {
            escape = ((HtmlOutputFormat) component).isEscape();
        }
        else
        {
            escape = RendererUtils.getBooleanAttribute(component, JSFAttr.ESCAPE_ATTR, true);
        }
        if (text != null)
        {
            ResponseWriter writer = facesContext.getResponseWriter();
            boolean span = false;

            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(component);
                
                if ( (commonPropertiesMarked & ~(CommonPropertyConstants.ESCAPE_PROP)) > 0)
                {
                    span = true;
                    writer.startElement(HTML.SPAN_ELEM, component);
                    HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
                }
                else if (CommonPropertyUtils.isIdRenderingNecessary(component))
                {
                    span = true;
                    writer.startElement(HTML.SPAN_ELEM, component);
                    writer.writeAttribute(HTML.ID_ATTR, component.getClientId(facesContext), null);
                }
                
                CommonPropertyUtils.renderUniversalProperties(writer, commonPropertiesMarked, component);
                CommonPropertyUtils.renderStyleProperties(writer, commonPropertiesMarked, component);
            }
            else
            {
                if(component.getId()!=null && !component.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
                {
                    span = true;
    
                    writer.startElement(HTML.SPAN_ELEM, component);
    
                    HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
    
                    HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
    
                }
                else
                {
                    span = HtmlRendererUtils.renderHTMLAttributesWithOptionalStartElement(writer,component,
                            HTML.SPAN_ELEM,HTML.COMMON_PASSTROUGH_ATTRIBUTES);
                }
            }

            if (escape)
            {
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("renderOutputText writing '" + text + "'");
                }
                writer.writeText(text, org.apache.myfaces.shared.renderkit.JSFAttr.VALUE_ATTR);
            }
            else
            {
                writer.write(text);
            }

            if(span)
            {
                writer.endElement(org.apache.myfaces.shared.renderkit.html.HTML.SPAN_ELEM);
            }
        }
    }

    private String getOutputFormatText(FacesContext facesContext, UIComponent htmlOutputFormat)
    {
        String pattern = RendererUtils.getStringValue(facesContext, htmlOutputFormat);
        Object[] args;
        if (htmlOutputFormat.getChildCount() == 0)
        {
            args = EMPTY_ARGS;
        }
        else
        {
            List<Object> argsList = null;
            
            if (htmlOutputFormat.getChildCount() > 0)
            {
                List<UIParameter> validParams = HtmlRendererUtils.getValidUIParameterChildren(
                        facesContext, htmlOutputFormat.getChildren(), false, false, false);
                for (int i = 0, size = validParams.size(); i < size; i++)
                {
                    UIParameter param = validParams.get(i);
                    if (argsList == null)
                    {
                        argsList = new ArrayList<Object>();
                    }
                    argsList.add(param.getValue());
                }
            }
            
            if (argsList != null)
            {
                args = argsList.toArray(new Object[argsList.size()]);
            }
            else
            {
                args = EMPTY_ARGS;
            }
        }

        MessageFormat format = new MessageFormat(pattern, facesContext.getViewRoot().getLocale());
        try
        {
            return format.format(args);
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Error formatting message of component "
                                  + htmlOutputFormat.getClientId(facesContext));
            return "";
        }
    }

}
