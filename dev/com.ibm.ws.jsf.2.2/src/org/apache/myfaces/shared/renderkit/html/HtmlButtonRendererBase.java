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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.ValueHolder;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;

import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared.renderkit.html.util.JavascriptUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.shared.util._ComponentUtils;

public class HtmlButtonRendererBase
    extends HtmlRenderer
{
    private static final String IMAGE_BUTTON_SUFFIX_X = ".x";
    private static final String IMAGE_BUTTON_SUFFIX_Y = ".y";

    public static final String ACTION_FOR_LIST = "org.apache.myfaces.ActionForList";

    public void decode(FacesContext facesContext, UIComponent uiComponent)
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(
                facesContext, uiComponent, UICommand.class);

        //super.decode must not be called, because value is handled here
        boolean disabled = isDisabled(facesContext, uiComponent);
        // MYFACES-3960 Decode, decode client behavior and queue action event at the end
        boolean activateActionEvent = !isReset(uiComponent) && isSubmitted(facesContext, uiComponent) &&
            !disabled;
        if (activateActionEvent)
        {
            org.apache.myfaces.shared.renderkit.RendererUtils.initPartialValidationAndModelUpdate(
                    uiComponent, facesContext);
        }
        
        if (uiComponent instanceof ClientBehaviorHolder &&
                !disabled)
        {
            HtmlRendererUtils.decodeClientBehaviors(facesContext, uiComponent);
        }
        
        if (activateActionEvent)
        {
            uiComponent.queueEvent(new ActionEvent(uiComponent));
        }
    }

    private static boolean isReset(UIComponent uiComponent)
    {
        return "reset".equals((String) uiComponent.getAttributes().get(HTML.TYPE_ATTR));
    }
    
    private static boolean isButton(UIComponent uiComponent)
    {
        return "button".equals((String) uiComponent.getAttributes().get(HTML.TYPE_ATTR));
    }

    private static boolean isSubmitted(FacesContext facesContext, UIComponent uiComponent)
    {
        String clientId = uiComponent.getClientId(facesContext);
        Map paramMap = facesContext.getExternalContext().getRequestParameterMap();
        FormInfo formInfo = _ComponentUtils.findNestingForm(uiComponent, facesContext);
        String hiddenLink = null;
         
        if (formInfo != null)
        {
            hiddenLink = (String) facesContext.getExternalContext().getRequestParameterMap().get(
                HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo, facesContext));
        }
        return paramMap.containsKey(clientId) || paramMap.containsKey(clientId + IMAGE_BUTTON_SUFFIX_X) 
            || paramMap.containsKey(clientId + IMAGE_BUTTON_SUFFIX_Y)
            || (hiddenLink != null && hiddenLink.equals (clientId))
            || HtmlRendererUtils.isPartialOrBehaviorSubmit(facesContext, clientId);
    }

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        org.apache.myfaces.shared.renderkit.RendererUtils.checkParamValidity(
                facesContext, uiComponent, UICommand.class);

        String clientId = uiComponent.getClientId(facesContext);

        ResponseWriter writer = facesContext.getResponseWriter();
        
        // commandButton does not need to be nested in a form since JSF 2.0
        FormInfo formInfo = findNestingForm(uiComponent, facesContext);

        boolean reset = isReset(uiComponent);
        boolean button = isButton(uiComponent);

        Map<String, List<ClientBehavior>> behaviors = null;
        if (uiComponent instanceof ClientBehaviorHolder)
        {
            behaviors = ((ClientBehaviorHolder) uiComponent).getClientBehaviors();
            if (!behaviors.isEmpty())
            {
                ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
            }
        }
        
        // If we are nested in a form, and we have javascript enabled, and autoscroll is enabled, 
        // we should write the form submit script
        // (define oamSetHiddenInput, oamClearHiddenInput, oamSubmitForm)
        // because oamSetHiddenInput is called on onclick function
        List<UIComponent> childrenList = null;
        if (getChildCount(uiComponent) > 0)
        {
            childrenList = getChildren(uiComponent);
        }
        else
        {
           childrenList = Collections.emptyList();
        }
        List<UIParameter> validParams = HtmlRendererUtils.getValidUIParameterChildren(
                facesContext, childrenList, false, false);
        
        if (formInfo != null 
                && (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isAutoScroll() ||
                        (validParams != null && !validParams.isEmpty() )))
        {        
            HtmlRendererUtils.renderFormSubmitScript(facesContext);
        }
        
        String commandOnclick = (String)uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);
        
        if (commandOnclick != null && (validParams != null && !validParams.isEmpty() ) )
        {
            ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
        }

        writer.startElement(HTML.INPUT_ELEM, uiComponent);

        writer.writeAttribute(HTML.ID_ATTR, clientId, org.apache.myfaces.shared.renderkit.JSFAttr.ID_ATTR);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, JSFAttr.ID_ATTR);

        String image = RendererUtils.getIconSrc(facesContext, uiComponent, JSFAttr.IMAGE_ATTR);
        if (image != null)
        {
            writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_IMAGE, 
                    org.apache.myfaces.shared.renderkit.JSFAttr.TYPE_ATTR);
            writer.writeURIAttribute(HTML.SRC_ATTR, image, org.apache.myfaces.shared.renderkit.JSFAttr.IMAGE_ATTR);
        }
        else
        {
            String type = getType(uiComponent);

            if (type == null || (!reset && !button))
            {
                type = HTML.INPUT_TYPE_SUBMIT;
            }
            writer.writeAttribute(HTML.TYPE_ATTR, type, org.apache.myfaces.shared.renderkit.JSFAttr.TYPE_ATTR);
            Object value = getValue(uiComponent);
            if (value != null)
            {
                writer.writeAttribute(org.apache.myfaces.shared.renderkit.html.HTML.VALUE_ATTR, value, 
                        org.apache.myfaces.shared.renderkit.JSFAttr.VALUE_ATTR);
            }
        }
        
        if ((HtmlRendererUtils.hasClientBehavior(ClientBehaviorEvents.CLICK, behaviors, facesContext) ||
             HtmlRendererUtils.hasClientBehavior(ClientBehaviorEvents.ACTION, behaviors, facesContext)))
        {
            if (!reset && !button)
            {
                String onClick = buildBehaviorizedOnClick(
                        uiComponent, behaviors, facesContext, writer, formInfo, validParams);
                if (onClick.length() != 0)
                {
                    writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
                }
            }
            else
            {
                Collection<ClientBehaviorContext.Parameter> paramList = 
                    HtmlRendererUtils.getClientBehaviorContextParameters(
                        HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, uiComponent));
                    
                String onClick = HtmlRendererUtils.buildBehaviorChain(facesContext, uiComponent,
                        ClientBehaviorEvents.CLICK, paramList, ClientBehaviorEvents.ACTION, paramList, behaviors,
                        commandOnclick , null);
                if (onClick.length() != 0)
                {
                    writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
                }
            }
            
            Map<String, Object> attributes = uiComponent.getAttributes(); 
            
            HtmlRendererUtils.buildBehaviorChain(
                    facesContext, uiComponent, ClientBehaviorEvents.DBLCLICK, null, behaviors,   
                        (String) attributes.get(HTML.ONDBLCLICK_ATTR), "");
        }
        else
        {
            //fallback into the pre 2.0 code to keep backwards compatibility with libraries which rely on internals
            if (!reset && !button)
            {
                StringBuilder onClick = buildOnClick(uiComponent, facesContext, writer, validParams);
                if (onClick.length() != 0)
                {
                    writer.writeAttribute(HTML.ONCLICK_ATTR, onClick.toString(), null);
                }
            }
            else
            {
                HtmlRendererUtils.renderHTMLStringAttribute(writer, uiComponent, HTML.ONCLICK_ATTR, HTML.ONCLICK_ATTR);
            }
        }
        
        //if (javascriptAllowed)
        //{
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                CommonPropertyUtils.renderButtonPassthroughPropertiesWithoutDisabledAndEvents(writer, 
                        CommonPropertyUtils.getCommonPropertiesMarked(uiComponent), uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                                                       HTML.BUTTON_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
            }
        //}

        if (behaviors != null && !behaviors.isEmpty())
        {
            HtmlRendererUtils.renderBehaviorizedEventHandlersWithoutOnclick(
                    facesContext, writer, uiComponent, behaviors);
            HtmlRendererUtils.renderBehaviorizedFieldEventHandlers(facesContext, writer, uiComponent, behaviors);
        }
        else
        {
            if (isCommonPropertiesOptimizationEnabled(facesContext))
            {
                long commonPropertiesMarked = CommonPropertyUtils.getCommonPropertiesMarked(uiComponent);
                CommonPropertyUtils.renderEventPropertiesWithoutOnclick(writer, commonPropertiesMarked, uiComponent);
                CommonPropertyUtils.renderCommonFieldEventProperties(writer, commonPropertiesMarked, uiComponent);
            }
            else
            {
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.EVENT_HANDLER_ATTRIBUTES_WITHOUT_ONCLICK);
                HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                        HTML.COMMON_FIELD_EVENT_ATTRIBUTES);
            }
        }

        if (isDisabled(facesContext, uiComponent))
        {
            writer.writeAttribute(HTML.DISABLED_ATTR, Boolean.TRUE, 
                    org.apache.myfaces.shared.renderkit.JSFAttr.DISABLED_ATTR);
        }
        
        if (isReadonly(facesContext, uiComponent))
        {
            writer.writeAttribute(HTML.READONLY_ATTR, Boolean.TRUE, 
                    org.apache.myfaces.shared.renderkit.JSFAttr.READONLY_ATTR);
        }
    }
    
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent)
            throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        writer.endElement(HTML.INPUT_ELEM);
        
        FormInfo formInfo = findNestingForm(uiComponent, facesContext);
        if (formInfo != null)
        {
            HtmlFormRendererBase.renderScrollHiddenInputIfNecessary(
                    formInfo.getForm(), facesContext, writer);
        }
        
        // render the UIParameter children of the commandButton (since 2.0)
        /*
        List<UIParameter> validParams = HtmlRendererUtils.getValidUIParameterChildren(
                facesContext, uiComponent.getChildren(), false, false);
        for (UIParameter param : validParams)
        {
            HtmlInputHidden parameterComponent = new HtmlInputHidden();
            parameterComponent.setId(param.getName());
            parameterComponent.setValue(param.getValue());
            parameterComponent.encodeAll(facesContext);
        }*/
    }

    protected String buildBehaviorizedOnClick(UIComponent uiComponent, Map<String, List<ClientBehavior>> behaviors, 
                                              FacesContext facesContext, ResponseWriter writer, 
                                              FormInfo nestedFormInfo, List<UIParameter> validParams)
        throws IOException
    {
        //we can omit autoscroll here for now maybe we should check if it is an ajax 
        //behavior and omit it only in this case
        StringBuilder userOnClick = new StringBuilder();
        //user onclick part 
        String commandOnClick = (String) uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);

        if (commandOnClick != null)
        {
            userOnClick.append(commandOnClick);
            userOnClick.append(';');
        }

        StringBuilder rendererOnClick = new StringBuilder();

        if (nestedFormInfo != null) 
        {
            // There is no clean way to detect if a "submit" behavior has been added to the component, 
            // so to keep things simple, if the button is submit type, it is responsibility of the 
            // developer to add a client behavior that submit the form, for example using a f:ajax tag.
            // Otherwise, there will be a situation where a full submit could be trigger after an ajax
            // operation. Note we still need to append two scripts if necessary: autoscroll and clear
            // hidden fields, because this code is called for a submit button.
            //if (behaviors.isEmpty() && validParams != null && !validParams.isEmpty() )
            //{
            //    rendererOnClick.append(buildServerOnclick(facesContext, uiComponent, 
            //            uiComponent.getClientId(facesContext), nestedFormInfo, validParams));
            //}
            //else
            //{
                String formName = nestedFormInfo.getFormName();
                if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext()))
                {
                    //call the script to clear the form (clearFormHiddenParams_<formName>) method
                    HtmlRendererUtils.appendClearHiddenCommandFormParamsFunctionCall(rendererOnClick, formName);
                }
        
                if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isAutoScroll())
                {
                    HtmlRendererUtils.appendAutoScrollAssignment(rendererOnClick, formName);
                }
            //}
        }

        //according to the specification in jsf.util.chain jdocs and the spec document we have to use
        //jsf.util.chain to chain the functions and
        Collection<ClientBehaviorContext.Parameter> paramList = HtmlRendererUtils.getClientBehaviorContextParameters(
                HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, uiComponent));
        
        return HtmlRendererUtils.buildBehaviorChain(facesContext, uiComponent,
                ClientBehaviorEvents.CLICK, paramList, ClientBehaviorEvents.ACTION, paramList, behaviors,
                userOnClick.toString() , rendererOnClick.toString());
    }
    
    protected String buildServerOnclick(FacesContext facesContext, UIComponent component, 
            String clientId, FormInfo formInfo, List<UIParameter> validParams) throws IOException
    {
        UIComponent nestingForm = formInfo.getForm();
        String formName = formInfo.getFormName();

        StringBuilder onClick = new StringBuilder();

        if (RendererUtils.isAdfOrTrinidadForm(formInfo.getForm()))
        {
            onClick.append("submitForm('");
            onClick.append(formInfo.getForm().getClientId(facesContext));
            onClick.append("',1,{source:'");
            onClick.append(component.getClientId(facesContext));
            onClick.append("'});return false;");
        }
        else
        {
            StringBuilder params = addChildParameters(facesContext, nestingForm, validParams);

            String target = getTarget(component);

            onClick.append("return ").
                append(HtmlRendererUtils.SUBMIT_FORM_FN_NAME_JSF2).append("('").
                append(formName).append("','").
                append(component.getClientId(facesContext)).append("'");

            if (params.length() > 2 || target != null)
            {
                onClick.append(",").
                    append(target == null ? "null" : ("'" + target + "'")).append(",").
                    append(params);
            }
            onClick.append(");");

            //Not necessary since we are using oamSetHiddenInput to create input hidden fields
            //render hidden field - todo: in here for backwards compatibility
            //String hiddenFieldName = HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo);
            //addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);

        }
        return onClick.toString();
    }
    
    private StringBuilder addChildParameters(FacesContext context, 
            UIComponent nestingForm, List<UIParameter> validParams)
    {
        //add child parameters
        StringBuilder params = new StringBuilder();
        params.append("[");
        
        for (UIParameter param : validParams) 
        {
            String name = param.getName();

            //Not necessary, since we are using oamSetHiddenInput to create hidden fields
            if (MyfacesConfig.getCurrentInstance(context.getExternalContext()).isRenderHiddenFieldsForLinkParams())
            {
                addHiddenCommandParameter(context, nestingForm, name);
            }

            Object value = param.getValue();

            //UIParameter is no ValueHolder, so no conversion possible - calling .toString on value....
            // MYFACES-1832 bad charset encoding for f:param
            // if HTMLEncoder.encode is called, then
            // when is called on writer.writeAttribute, encode method
            // is called again so we have a duplicated encode call.
            // MYFACES-2726 All '\' and "'" chars must be escaped 
            // because there will be inside "'" javascript quotes, 
            // otherwise the value will not correctly restored when
            // the command is post.
            //String strParamValue = value != null ? value.toString() : "";
            String strParamValue = "";
            if (value != null)
            {
                strParamValue = value.toString();
                StringBuilder buff = null;
                for (int i = 0; i < strParamValue.length(); i++)
                {
                    char c = strParamValue.charAt(i); 
                    if (c == '\'' || c == '\\')
                    {
                        if (buff == null)
                        {
                            buff = new StringBuilder();
                            buff.append(strParamValue.substring(0,i));
                        }
                        buff.append('\\');
                        buff.append(c);
                    }
                    else if (buff != null)
                    {
                        buff.append(c);
                    }
                }
                if (buff != null)
                {
                    strParamValue = buff.toString();
                }
            }

            if (params.length() > 1) 
            {
                params.append(",");
            }

            params.append("['");
            params.append(name);
            params.append("','");
            params.append(strParamValue);
            params.append("']");
        }
        params.append("]");
        return params;
    }

    private String getTarget(UIComponent component)
    {
        // for performance reason: double check for the target attribute
        String target;
        if (component instanceof HtmlCommandLink)
        {
            target = ((HtmlCommandLink) component).getTarget();
        }
        else
        {
            target = (String) component.getAttributes().get(HTML.TARGET_ATTR);
        }
        return target;
    }

    protected StringBuilder buildOnClick(UIComponent uiComponent, FacesContext facesContext,
                                        ResponseWriter writer, List<UIParameter> validParams)
        throws IOException
    {
        /* DUMMY STUFF
        //Find form
        UIComponent parent = uiComponent.getParent();
        while (parent != null && !(parent instanceof UIForm))
        {
            parent = parent.getParent();
        }

        UIForm nestingForm = null;
        String formName;

        if (parent != null)
        {
            //link is nested inside a form
            nestingForm = (UIForm)parent;
            formName = nestingForm.getClientId(facesContext);

        }
        else
        {
            //not nested in form, we must add a dummy form at the end of the document
            formName = DummyFormUtils.DUMMY_FORM_NAME;
            //dummyFormResponseWriter = DummyFormUtils.getDummyFormResponseWriter(facesContext);
            //dummyFormResponseWriter.setWriteDummyForm(true);
            DummyFormUtils.setWriteDummyForm(facesContext, true);
        }
        */
        StringBuilder onClick = new StringBuilder();
        String commandOnClick = (String) uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);

        if (commandOnClick != null)
        {
            onClick.append("var cf = function(){");
            onClick.append(commandOnClick);
            onClick.append('}');
            onClick.append(';');
            onClick.append("var oamSF = function(){");
        }
        
        FormInfo nestedFormInfo = findNestingForm(uiComponent, facesContext);
        
        if (nestedFormInfo != null)
        {
            String formName = nestedFormInfo.getFormName();
            
            if (validParams != null && !validParams.isEmpty() )
            {
                StringBuilder params = addChildParameters(
                        facesContext, nestedFormInfo.getForm(), validParams);

                String target = getTarget(uiComponent);

                onClick.append("return ").
                    append(HtmlRendererUtils.SUBMIT_FORM_FN_NAME_JSF2).append("('").
                    append(formName).append("','").
                    append(uiComponent.getClientId(facesContext)).append("'");

                if (params.length() > 2 || target != null)
                {
                    onClick.append(",").
                        append(target == null ? "null" : ("'" + target + "'")).append(",").
                        append(params);
                }
                onClick.append(");");

                //Not necessary since we are using oamSetHiddenInput to create input hidden fields
                //render hidden field - todo: in here for backwards compatibility
                if (MyfacesConfig.getCurrentInstance(
                        facesContext.getExternalContext()).isRenderHiddenFieldsForLinkParams())
                {
                    String hiddenFieldName = HtmlRendererUtils.getHiddenCommandLinkFieldName(
                            nestedFormInfo, facesContext);
                    addHiddenCommandParameter(facesContext, nestedFormInfo.getForm(), hiddenFieldName);
                }
            }
            else
            {
        
                if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext()) ||
                        MyfacesConfig.getCurrentInstance(
                                facesContext.getExternalContext()).isRenderHiddenFieldsForLinkParams() )
                {
                    //call the script to clear the form (clearFormHiddenParams_<formName>) method
                    HtmlRendererUtils.appendClearHiddenCommandFormParamsFunctionCall(onClick, formName);
                }
        
                if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isAutoScroll())
                {
                    HtmlRendererUtils.appendAutoScrollAssignment(onClick, formName);
                }
            }
        }
        
        if (commandOnClick != null)
        {
            onClick.append('}');
            onClick.append(';');
            onClick.append("return (cf.apply(this, [])==false)? false : oamSF.apply(this, []); ");
        }  

        //The hidden field has only sense if isRenderClearJavascriptOnButton is
        //set to true. In other case, this hidden field should not be rendered.
        //if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext()))
        //{
            //add hidden field for the case there is no commandLink in the form
            //String hiddenFieldName = HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo);
            //addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);
        //}

        return onClick;
    }

    protected void addHiddenCommandParameter(FacesContext facesContext, 
            UIComponent nestingForm, String hiddenFieldName)
    {
        if (nestingForm != null)
        {
            HtmlFormRendererBase.addHiddenCommandParameter(facesContext, nestingForm, hiddenFieldName);
        }
    }

    /**
     * find nesting form<br />
     * need to be overrideable to deal with dummyForm stuff in tomahawk.
     */
    protected FormInfo findNestingForm(UIComponent uiComponent, FacesContext facesContext)
    {
        return RendererUtils.findNestingForm(uiComponent, facesContext);
    }

    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        //TODO: overwrite in extended HtmlButtonRenderer and check for enabledOnUserRole
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).isDisabled();
        }

        return org.apache.myfaces.shared.renderkit.RendererUtils.getBooleanAttribute(
                uiComponent, HTML.DISABLED_ATTR, false);
        
    }

    protected boolean isReadonly(FacesContext facesContext, UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).isReadonly();
        }
        return org.apache.myfaces.shared.renderkit.RendererUtils.getBooleanAttribute(
                uiComponent, HTML.READONLY_ATTR, false);
    }

    private String getType(UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).getType();
        }
        return (String)uiComponent.getAttributes().get(org.apache.myfaces.shared.renderkit.JSFAttr.TYPE_ATTR);
    }

    private Object getValue(UIComponent uiComponent)
    {
        if (uiComponent instanceof ValueHolder)
        {
            return ((ValueHolder)uiComponent).getValue();
        }
        return uiComponent.getAttributes().get(JSFAttr.VALUE_ATTR);
    }
}