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

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.ConverterException;

import org.apache.myfaces.shared.component.EscapeCapable;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

public class HtmlTextRendererBase
        extends HtmlRenderer
{
    //private static final Log log = LogFactory.getLog(HtmlTextRendererBase.class);
    private static final Logger log = Logger.getLogger(HtmlTextRendererBase.class.getName());

    private static final String AUTOCOMPLETE_VALUE_OFF = "off";

    public void encodeEnd(FacesContext facesContext, UIComponent component)
        throws IOException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext,component,null);
        
        Map<String, List<ClientBehavior>> behaviors = null;
        if (component instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) component).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, facesContext.getResponseWriter());
            }
        }
        
        if (component instanceof UIInput)
        {
            renderInput(facesContext, component);
        }
        else if (component instanceof UIOutput)
        {
            renderOutput(facesContext, component);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }


    protected void renderOutput(FacesContext facesContext, UIComponent component)
        throws IOException
    {
        
        String text = org.apache.myfaces.shared.renderkit.RendererUtils.getStringValue(facesContext, component);
        if (log.isLoggable(Level.FINE))
        {
            log.fine("renderOutput '" + text + "'");
        }
        boolean escape;
        if (component instanceof HtmlOutputText || component instanceof EscapeCapable)
        {
            escape = ((HtmlOutputText)component).isEscape();
        }
        else
        {
            escape = RendererUtils.getBooleanAttribute(component, 
                    org.apache.myfaces.shared.renderkit.JSFAttr.ESCAPE_ATTR,
                                                       true); //default is to escape
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
                
                if (isRenderOutputEventAttributes())
                {
                    HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.EVENT_HANDLER_ATTRIBUTES);
                }
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

    protected boolean isRenderOutputEventAttributes()
    {
        return true;
    }

    protected void renderInput(FacesContext facesContext, UIComponent component)
        throws IOException
    {
        //allow subclasses to render custom attributes by separating rendering begin and end 
        renderInputBegin(facesContext, component);
        renderInputEnd(facesContext, component);
    }

    //Subclasses can set the value of an attribute before, or can render a custom attribute after calling this method
    protected void renderInputBegin(FacesContext facesContext,
            UIComponent component) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        String clientId = component.getClientId(facesContext);

        writer.startElement(HTML.INPUT_ELEM, component);
        writer.writeAttribute(HTML.ID_ATTR, clientId, null);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, null);
        
        //allow extending classes to modify html input element's type
        String inputHtmlType = getInputHtmlType(component);
        writer.writeAttribute(HTML.TYPE_ATTR, inputHtmlType, null);

        renderValue(facesContext, component, writer);

        Map<String, List<ClientBehavior>> behaviors = null;
        if (component instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) component).getClientBehaviors();
            
            long commonPropertiesMarked = 0L;
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(component);
            }
            if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderChangeEventProperty(writer, 
                        commonPropertiesMarked, component);
                CommonPropertyUtils.renderEventProperties(writer, 
                        commonPropertiesMarked, component);
                CommonPropertyUtils.renderFieldEventPropertiesWithoutOnchange(writer, 
                        commonPropertiesMarked, component);
            }
            else
            {
                HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(facesContext, writer, component, behaviors);
                if (isCommonEventsOptimizationEnabled(facesContext))
                {
                    Long commonEventsMarked = CommonEventUtils.getCommonEventsMarked(component);
                    CommonEventUtils.renderBehaviorizedEventHandlers(facesContext, writer, 
                            commonPropertiesMarked, commonEventsMarked, component, behaviors);
                    CommonEventUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                        facesContext, writer, commonPropertiesMarked, commonEventsMarked, component, behaviors);
                }
                else
                {
                    HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, component, behaviors);
                    HtmlRendererUtils.renderBehaviorizedFieldEventHandlersWithoutOnchange(
                            facesContext, writer, component, behaviors);
                }
            }
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderInputPassthroughPropertiesWithoutDisabledAndEvents(writer, 
                        commonPropertiesMarked, component);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, component, 
                        HTML.INPUT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
            }
        }
        else
        {
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderInputPassthroughPropertiesWithoutDisabled(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(component), component);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, component, 
                        HTML.INPUT_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);
            }
        }

        if (isDisabled(facesContext, component))
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, Boolean.TRUE, null);
        }

        if (isAutocompleteOff(facesContext, component))
        {
            writer.writeAttribute(HTML.AUTOCOMPLETE_ATTR, AUTOCOMPLETE_VALUE_OFF, HTML.AUTOCOMPLETE_ATTR);
        }
    }

    protected void renderValue(FacesContext facesContext, UIComponent component, ResponseWriter writer)
            throws IOException
    {
        String value = RendererUtils.getStringValue(facesContext, component);
        if (log.isLoggable(Level.FINE))
        {
           log.fine("renderInput '" + value + "'");
        }

        if (value != null)
        {
            writer.writeAttribute(HTML.VALUE_ATTR, value, JSFAttr.VALUE_ATTR);
        }
    }

    protected void renderInputEnd(FacesContext facesContext, UIComponent component) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter(); 

        writer.endElement(HTML.INPUT_ELEM);
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent component)
    {
        //TODO: overwrite in extended HtmlTextRenderer and check for enabledOnUserRole
        if (component instanceof HtmlInputText)
        {
            return ((HtmlInputText)component).isDisabled();
        }

        return org.apache.myfaces.shared.renderkit.RendererUtils.getBooleanAttribute(component, 
                HTML.DISABLED_ATTR, false);
        
    }

    /**
     * If autocomplete is "on" or not set, do not render it
     */
    protected boolean isAutocompleteOff(FacesContext facesContext, UIComponent component)
    {
        if (component instanceof HtmlInputText)
        {
            String autocomplete = ((HtmlInputText)component).getAutocomplete();
            if (autocomplete != null)
            {
                return autocomplete.equals(AUTOCOMPLETE_VALUE_OFF);
            }
        }

        return false;
    }


    public void decode(FacesContext facesContext, UIComponent component)
    {
        RendererUtils.checkParamValidity(facesContext,component,null);

        if (component instanceof UIInput)
        {
            HtmlRendererUtils.decodeUIInput(facesContext, component);
            if (component instanceof ClientBehaviorHolder &&
                    !HtmlRendererUtils.isDisabled(component))
            {
                HtmlRendererUtils.decodeClientBehaviors(facesContext, component);
            }
        }
        else if (component instanceof UIOutput)
        {
            //nothing to decode
        }
        else
        {
            throw new IllegalArgumentException("Unsupported component class " + component.getClass().getName());
        }
    }


    public Object getConvertedValue(FacesContext facesContext, UIComponent component, Object submittedValue)
        throws ConverterException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, component, UIOutput.class);
        return RendererUtils.getConvertedUIOutputValue(facesContext,
                                                       (UIOutput)component,
                                                       submittedValue);
    }

    /**
     * Returns the HTML type attribute of HTML input element, which is being rendered.
     */
    protected String getInputHtmlType(UIComponent component)
    {
        //subclasses may act on properties of the component
        return HTML.INPUT_TYPE_TEXT;
    }

    public static void renderOutputText(FacesContext facesContext,
            UIComponent component, String text, boolean escape)
            throws IOException
    {
        if (text != null)
        {
            ResponseWriter writer = facesContext.getResponseWriter();
            boolean span = false;

            if (component.getId() != null
                    && !component.getId().startsWith(
                            UIViewRoot.UNIQUE_ID_PREFIX))
            {
                span = true;

                writer.startElement(HTML.SPAN_ELEM, component);

                HtmlRendererUtils.writeIdIfNecessary(writer, component,
                        facesContext);

                HtmlRendererUtils.renderHTMLAttributes(writer, component,
                        HTML.COMMON_PASSTROUGH_ATTRIBUTES);

            }
            else
            {
                span = HtmlRendererUtils
                        .renderHTMLAttributesWithOptionalStartElement(writer,
                                component, HTML.SPAN_ELEM,
                                HTML.COMMON_PASSTROUGH_ATTRIBUTES);
            }

            if (escape)
            {
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("renderOutputText writing '" + text + "'");
                }
                writer.writeText(text,
                        org.apache.myfaces.shared.renderkit.JSFAttr.VALUE_ATTR);
            }
            else
            {
                writer.write(text);
            }

            if (span)
            {
                writer.endElement(org.apache.myfaces.shared.renderkit.html.HTML.SPAN_ELEM);
            }
        }
    }
}
