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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlForm;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

public class HtmlFormRendererBase
        extends HtmlRenderer
{
    //private static final Log log = LogFactory.getLog(HtmlFormRenderer.class);
    private static final String FORM_TARGET = HTML.FORM_ELEM;
    private static final String HIDDEN_SUBMIT_INPUT_SUFFIX = "_SUBMIT";
    private static final String HIDDEN_SUBMIT_INPUT_VALUE = "1";

    private static final String HIDDEN_COMMAND_INPUTS_SET_ATTR
            = UIForm.class.getName() + ".org.apache.myfaces.HIDDEN_COMMAND_INPUTS_SET";

    private static final String SCROLL_HIDDEN_INPUT = "org.apache.myfaces.SCROLL_HIDDEN_INPUT";

    public void encodeBegin(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, component, UIForm.class);

        UIForm htmlForm = (UIForm)component;

        ResponseWriter writer = facesContext.getResponseWriter();
        String clientId = htmlForm.getClientId(facesContext);
        String acceptCharset = getAcceptCharset(facesContext, htmlForm);
        String actionURL = getActionUrl(facesContext, htmlForm);
        String method = getMethod(facesContext, htmlForm);

        Map<String, List<ClientBehavior>> behaviors = null;
        if (component instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) component).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
            }
        }
        
        writer.startElement(HTML.FORM_ELEM, htmlForm);
        writer.writeAttribute(HTML.ID_ATTR, clientId, null);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, null);
        writer.writeAttribute(HTML.METHOD_ATTR, method, null);
        if (acceptCharset != null)
        {
            writer.writeAttribute(HTML.ACCEPT_CHARSET_ATTR, acceptCharset, null);
        }
        String encodedActionURL = facesContext.getExternalContext().encodeActionURL(actionURL);
        
        writer.writeURIAttribute(HTML.ACTION_ATTR,
                encodedActionURL,
                null);
        
        if (htmlForm instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) htmlForm).getClientBehaviors();
            if (behaviors.isEmpty() && isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderEventProperties(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(htmlForm), htmlForm);
            }
            else
            {
                if (isCommonEventsOptimizationEnabled(facesContext))
                {
                    CommonEventUtils.renderBehaviorizedEventHandlers(facesContext, writer, 
                           CommonPropertyUtils.getCommonPropertiesMarked(htmlForm),
                           CommonEventUtils.getCommonEventsMarked(htmlForm), htmlForm, behaviors);
                }
                else
                {
                    HtmlRendererUtils.renderBehaviorizedEventHandlers(facesContext, writer, htmlForm, behaviors);
                }
            }
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderCommonPassthroughPropertiesWithoutEvents(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(component), component);
                HtmlRendererUtils.renderHTMLAttributes(writer, htmlForm, HTML.FORM_ATTRIBUTES);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, htmlForm, 
                        HTML.FORM_PASSTHROUGH_ATTRIBUTES_WITHOUT_EVENTS);
            }
        }
        else
        {
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderCommonPassthroughProperties(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(component), component);
                HtmlRendererUtils.renderHTMLAttributes(writer, htmlForm, HTML.FORM_ATTRIBUTES);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, htmlForm, HTML.FORM_PASSTHROUGH_ATTRIBUTES);
            }
        }

        writer.write(""); // force start element tag to be closed

        String encodedPartialActionURL =  facesContext.getExternalContext().encodePartialActionURL(actionURL);
        
        if (encodedActionURL != null && encodedPartialActionURL != null
            && (!encodedActionURL.equals(encodedPartialActionURL)))
        {
            HtmlRendererUtils.renderHiddenInputField(writer, "javax.faces.encodedURL", encodedPartialActionURL);
        }
        
        // not needed in this version as nothing is written to the form tag, but
        // included for backward compatibility to the 1.1.1 patch (JIRA MYFACES-1276)
        // However, might be needed in the future
        beforeFormElementsStart(facesContext, component);
        
        MyfacesConfig config = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext());
        if (config.isRenderFormViewStateAtBegin())
        {
            renderViewStateAndHiddenFields(facesContext, component);
        }
        
        afterFormElementsStart(facesContext, component);
    }

    protected String getActionUrl(FacesContext facesContext, UIForm form)
    {
        return getActionUrl(facesContext);
    }

    protected String getMethod(FacesContext facesContext, UIForm form)
    {
        return "post";
    }

    protected String getAcceptCharset(FacesContext facesContext, UIForm form )
    {
        return (String)form.getAttributes().get( JSFAttr.ACCEPTCHARSET_ATTR );
    }

    public void encodeEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        beforeFormElementsEnd(facesContext, component);

        //render hidden command inputs
        Set set =  (Set) facesContext.getExternalContext().getRequestMap().get(
                getHiddenCommandInputsSetName(facesContext, component)); 
        if (set != null && !set.isEmpty())
        {
            HtmlRendererUtils.renderHiddenCommandFormParams(writer, set);

            String target;
            if (component instanceof HtmlForm)
            {
                target = ((HtmlForm)component).getTarget();
            }
            else
            {
                target = (String)component.getAttributes().get(HTML.TARGET_ATTR);
            }
            HtmlRendererUtils.renderClearHiddenCommandFormParamsFunction(writer,
                                                                         component.getClientId(facesContext),
                                                                         set,
                                                                         target);
        }

        MyfacesConfig config = MyfacesConfig.getCurrentInstance(facesContext.getExternalContext());
        if (!config.isRenderFormViewStateAtBegin())
        {
            renderViewStateAndHiddenFields(facesContext, component);
        }

        afterFormElementsEnd(facesContext, component);
        
        writer.endElement(HTML.FORM_ELEM);
    }
    
    protected void renderViewStateAndHiddenFields(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();
        
                //write hidden input to determine "submitted" value on decode
        writer.startElement(HTML.INPUT_ELEM, null); // component);
        writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_HIDDEN, null);
        writer.writeAttribute(HTML.NAME_ATTR, component.getClientId(facesContext) +
                                              HIDDEN_SUBMIT_INPUT_SUFFIX, null);
        writer.writeAttribute(HTML.VALUE_ATTR, HIDDEN_SUBMIT_INPUT_VALUE, null);
        writer.endElement(HTML.INPUT_ELEM);

        renderScrollHiddenInputIfNecessary(component, facesContext, writer);

        //write state marker at the end of the form
        //Todo: this breaks client-side enabled AJAX components again which are searching for the state
        //we'll need to fix this
        ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
        viewHandler.writeState(facesContext);

        List<UIComponent> componentResources = facesContext.getViewRoot().getComponentResources(facesContext,
            FORM_TARGET);
        
        for (int i = 0, size = componentResources.size(); i < size; i++)
        {
           UIComponent child = componentResources.get(i);
           child.encodeAll (facesContext);
        }
    }

    private static String getHiddenCommandInputsSetName(FacesContext facesContext, UIComponent form)
    {
        StringBuilder buf = new StringBuilder(HIDDEN_COMMAND_INPUTS_SET_ATTR.length()+20);
        buf.append(HIDDEN_COMMAND_INPUTS_SET_ATTR);
        buf.append("_");
        buf.append(form.getClientId(facesContext));
        return buf.toString();
    }

    private static String getScrollHiddenInputName(FacesContext facesContext, UIComponent form)
    {
        StringBuilder buf = new StringBuilder(SCROLL_HIDDEN_INPUT.length()+20);
        buf.append(SCROLL_HIDDEN_INPUT);
        buf.append("_");
        buf.append(form.getClientId(facesContext));
        return buf.toString();
    }


    public void decode(FacesContext facesContext, UIComponent component)
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(facesContext, component, UIForm.class);

        /*
        if (HTMLUtil.isDisabled(component))
        {
            return;
        }
        */

        UIForm htmlForm = (UIForm)component;

        Map paramMap = facesContext.getExternalContext().getRequestParameterMap();
        String submittedValue = (String)paramMap.get(component.getClientId(facesContext) +
                                                     HIDDEN_SUBMIT_INPUT_SUFFIX);
        if (submittedValue != null && submittedValue.equals(HIDDEN_SUBMIT_INPUT_VALUE))
        {
            htmlForm.setSubmitted(true);
        }
        else
        {
            htmlForm.setSubmitted(false);
        }
        
        HtmlRendererUtils.decodeClientBehaviors(facesContext, component);
    }


    public static void addHiddenCommandParameter(FacesContext facesContext, UIComponent form, String paramName)
    {
        Set set = (Set) facesContext.getExternalContext().getRequestMap().get(
                getHiddenCommandInputsSetName(facesContext, form));
        if (set == null)
        {
            set = new HashSet();
            facesContext.getExternalContext().getRequestMap().put(
                    getHiddenCommandInputsSetName(facesContext, form), set);
        }
        set.add(paramName);
    }

    public static void renderScrollHiddenInputIfNecessary(
            UIComponent form, FacesContext facesContext, ResponseWriter writer)
        throws IOException
    {
        if (form == null)
        {
            return;
        }

        if (facesContext.getExternalContext().getRequestMap().get(
                getScrollHiddenInputName(facesContext, form)) == null)
        {
            if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isAutoScroll())
            {
                HtmlRendererUtils.renderAutoScrollHiddenInput(facesContext, writer);
            }
            facesContext.getExternalContext().getRequestMap().put(getScrollHiddenInputName(
                    facesContext, form), Boolean.TRUE);
        }
    }

    /**
     * Called before the state and any elements are added to the form tag in the
     * encodeBegin method
     */
    protected void beforeFormElementsStart(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        
    }

    /**
     * Called after the state and any elements are added to the form tag in the
     * encodeBegin method
     */
    protected void afterFormElementsStart(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        
    }

    /**
     * Called before the state and any elements are added to the form tag in the
     * encodeEnd method
     */
    protected void beforeFormElementsEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        
    }

    /**
     * Called after the state and any elements are added to the form tag in the
     * encodeEnd method
     */
    protected void afterFormElementsEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        
    }
}
