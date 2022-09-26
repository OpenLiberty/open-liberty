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
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.html.HtmlInputText;
import jakarta.faces.component.html.HtmlOutputText;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.convert.ConverterException;
import org.apache.myfaces.core.api.shared.AttributeUtils;
import org.apache.myfaces.core.api.shared.CommonHtmlAttributes;

import org.apache.myfaces.renderkit.RendererUtils;
import org.apache.myfaces.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.renderkit.html.util.HTML;
import org.apache.myfaces.renderkit.html.util.ComponentAttrs;

public class HtmlTextRendererBase
        extends HtmlRenderer
{
    private static final Logger log = Logger.getLogger(HtmlTextRendererBase.class.getName());

    private static final String AUTOCOMPLETE_VALUE_OFF = "off";

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
    {
      RendererUtils.checkParamValidity(facesContext,component,null);
      
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
          renderInputBegin(facesContext, component);
      }
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException
    {
        RendererUtils.checkParamValidity(facesContext,component,null);
        
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
            renderInputEnd(facesContext, component);
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

    protected void renderOutput(FacesContext facesContext, UIComponent component) throws IOException
    {
        String text = RendererUtils.getStringValue(facesContext, component);
        if (log.isLoggable(Level.FINE))
        {
            log.fine("renderOutput '" + text + '\'');
        }
        
        boolean escape;
        if (component instanceof HtmlOutputText)
        {
            escape = ((HtmlOutputText) component).isEscape();
        }
        else
        {
            //default is to escape
            escape = AttributeUtils.getBooleanAttribute(component, ComponentAttrs.ESCAPE_ATTR, true);
        }

        if (text != null)
        {
            ResponseWriter writer = facesContext.getResponseWriter();
            boolean span = false;

            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                long commonPropertiesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(component);
                if (commonPropertiesMarked > 0 && (commonPropertiesMarked & ~(CommonHtmlAttributes.ESCAPE)) > 0)
                {
                    span = true;
                    writer.startElement(HTML.SPAN_ELEM, component);
                    HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
                }
                else if (CommonHtmlAttributesUtil.isIdRenderingNecessary(component))
                {
                    span = true;
                    writer.startElement(HTML.SPAN_ELEM, component);
                    writer.writeAttribute(HTML.ID_ATTR, component.getClientId(facesContext), null);
                }
                
                CommonHtmlAttributesUtil.renderUniversalProperties(writer, commonPropertiesMarked, component);
                CommonHtmlAttributesUtil.renderStyleProperties(writer, commonPropertiesMarked, component);
                
                if (isRenderOutputEventAttributes())
                {
                    HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.EVENT_HANDLER_ATTRIBUTES);
                }
            }
            else
            {
                if (shouldRenderId(facesContext, component))
                {
                    span = true;
    
                    writer.startElement(HTML.SPAN_ELEM, component);
    
                    HtmlRendererUtils.writeId(writer, component, facesContext);
    
                    HtmlRendererUtils.renderHTMLAttributes(writer, component, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
    
                }
                else
                {
                    span = HtmlRendererUtils.renderHTMLAttributesWithOptionalStartElement(writer,
                            component, HTML.SPAN_ELEM, HTML.COMMON_PASSTROUGH_ATTRIBUTES);
                }
            }

            if (escape)
            {
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("renderOutputText writing '" + text + '\'');
                }
                writer.writeText(text, ComponentAttrs.VALUE_ATTR);
            }
            else
            {
                writer.write(text);
            }

            if(span)
            {
                writer.endElement(HTML.SPAN_ELEM);
            }
        }
    }

    protected boolean isRenderOutputEventAttributes()
    {
        return true;
    }

    @Deprecated
    protected void renderInput(FacesContext facesContext, UIComponent component)
        throws IOException
    {
    }

    //Subclasses can set the value of an attribute before, or can render a custom attribute after calling this method
    protected void renderInputBegin(FacesContext facesContext, UIComponent component) throws IOException
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
                commonPropertiesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(component);
            }
            if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonHtmlAttributesUtil.renderChangeEventProperty(writer, 
                        commonPropertiesMarked, component);
                CommonHtmlAttributesUtil.renderEventProperties(writer, 
                        commonPropertiesMarked, component);
                CommonHtmlAttributesUtil.renderFieldEventPropertiesWithoutOnchange(writer, 
                        commonPropertiesMarked, component);
            }
            else
            {
                HtmlRendererUtils.renderBehaviorizedOnchangeEventHandler(facesContext, writer, component, behaviors);
                if (isCommonEventsOptimizationEnabled(facesContext))
                {
                    Long commonEventsMarked = CommonHtmlEventsUtil.getMarkedEvents(component);
                    CommonHtmlEventsUtil.renderBehaviorizedEventHandlers(facesContext, writer, 
                            commonPropertiesMarked, commonEventsMarked, component, behaviors);
                    CommonHtmlEventsUtil.renderBehaviorizedFieldEventHandlersWithoutOnchange(
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
                CommonHtmlAttributesUtil.renderInputPassthroughPropertiesWithoutDisabledAndEvents(writer, 
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
                CommonHtmlAttributesUtil.renderInputPassthroughPropertiesWithoutDisabled(writer, 
                        CommonHtmlAttributesUtil.getMarkedAttributes(component), component);
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
           log.fine("renderInput '" + value + '\'');
        }

        if (value != null)
        {
            writer.writeAttribute(HTML.VALUE_ATTR, value, ComponentAttrs.VALUE_ATTR);
        }
    }

    protected void renderInputEnd(FacesContext facesContext, UIComponent component) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter(); 

        writer.endElement(HTML.INPUT_ELEM);
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent component)
    {
        if (component instanceof HtmlInputText)
        {
            return ((HtmlInputText)component).isDisabled();
        }

        return AttributeUtils.getBooleanAttribute(component, HTML.DISABLED_ATTR, false);
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

    @Override
    public void decode(FacesContext facesContext, UIComponent component)
    {
        RendererUtils.checkParamValidity(facesContext,component,null);

        if (component instanceof UIInput)
        {
            if (!isValidLength(facesContext, (UIInput)component))
            {
                return;
            }

            HtmlRendererUtils.decodeUIInput(facesContext, component);
            if (component instanceof ClientBehaviorHolder && !HtmlRendererUtils.isDisabled(component))
            {
                ClientBehaviorRendererUtils.decodeClientBehaviors(facesContext, component);
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

    @Override
    public Object getConvertedValue(FacesContext facesContext, UIComponent component, Object submittedValue)
        throws ConverterException
    {
        RendererUtils.checkParamValidity(facesContext, component, UIOutput.class);
        return RendererUtils.getConvertedUIOutputValue(facesContext,
                                                       (UIOutput)component,
                                                       submittedValue);
    }

    protected boolean isValidLength(FacesContext facesContext, UIInput input)
    {
        if (input instanceof HtmlInputText)
        {
            int maxlength = ((HtmlInputText)input).getMaxlength();
            if (maxlength >= 0)
            {
                String clientId = input.getClientId(facesContext);
                String value = facesContext.getExternalContext().getRequestParameterMap().get(clientId);
                if (value != null && value.length() > maxlength)
                {
                    return false;
                }
            }

        }
        return true;
    }
    
    /**
     * Returns the HTML type attribute of HTML input element, which is being rendered.
     */
    protected String getInputHtmlType(UIComponent component)
    {
        //subclasses may act on properties of the component
        return HTML.INPUT_TYPE_TEXT;
    }
}
