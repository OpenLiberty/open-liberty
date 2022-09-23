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
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.faces.component.UICommand;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIForm;
import jakarta.faces.component.UIParameter;
import jakarta.faces.component.ValueHolder;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorContext;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.html.HtmlCommandButton;
import jakarta.faces.component.html.HtmlCommandLink;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.event.ActionEvent;
import org.apache.myfaces.core.api.shared.AttributeUtils;
import org.apache.myfaces.core.api.shared.ComponentUtils;

import org.apache.myfaces.renderkit.ClientBehaviorEvents;
import org.apache.myfaces.renderkit.RendererUtils;
import org.apache.myfaces.renderkit.html.util.JavascriptUtils;
import org.apache.myfaces.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.renderkit.html.util.HTML;
import org.apache.myfaces.core.api.shared.lang.SharedStringBuilder;
import org.apache.myfaces.renderkit.html.util.ComponentAttrs;

public class HtmlButtonRendererBase extends HtmlRenderer
{
    private static final String SB_BUILD_BEHAVIORIZED_ONCLICK = HtmlButtonRendererBase.class.getName()
            + "#buildBehaviorizedOnClick";
    private static final String SB_BUILD_ONCLICK = HtmlButtonRendererBase.class.getName()
            + "#buildOnClick";
    private static final String SB_ADD_CHILD_PARAMETERS = HtmlButtonRendererBase.class.getName() +
            "#addChildParameters";
    
    private static final String IMAGE_BUTTON_SUFFIX_X = ".x";
    private static final String IMAGE_BUTTON_SUFFIX_Y = ".y";

    @Override
    public void decode(FacesContext facesContext, UIComponent uiComponent)
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UICommand.class);

        //super.decode must not be called, because value is handled here
        boolean disabled = isDisabled(facesContext, uiComponent);
        // MYFACES-3960 Decode, decode client behavior and queue action event at the end
        boolean activateActionEvent = !isReset(uiComponent) && isSubmitted(facesContext, uiComponent) && !disabled;
        
        if (uiComponent instanceof ClientBehaviorHolder && !disabled)
        {
            ClientBehaviorRendererUtils.decodeClientBehaviors(facesContext, uiComponent);
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
        String hiddenLink = null;

        UIForm form = ComponentUtils.findClosest(UIForm.class, uiComponent);
        if (form != null)
        {
            hiddenLink = (String) facesContext.getExternalContext().getRequestParameterMap().get(
                HtmlRendererUtils.getHiddenCommandLinkFieldName(form, facesContext));
        }
        return paramMap.containsKey(clientId) || paramMap.containsKey(clientId + IMAGE_BUTTON_SUFFIX_X) 
            || paramMap.containsKey(clientId + IMAGE_BUTTON_SUFFIX_Y)
            || (hiddenLink != null && hiddenLink.equals (clientId))
            || HtmlRendererUtils.isPartialOrBehaviorSubmit(facesContext, clientId);
    }

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        RendererUtils.checkParamValidity(facesContext, uiComponent, UICommand.class);

        String clientId = uiComponent.getClientId(facesContext);

        ResponseWriter writer = facesContext.getResponseWriter();
        
        // commandButton does not need to be nested in a form since JSF 2.0
        UIForm form = ComponentUtils.findClosest(UIForm.class, uiComponent);

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

        List<UIParameter> validParams = HtmlRendererUtils.getValidUIParameterChildren(
                facesContext, getChildren(uiComponent), false, false);

        String commandOnclick = (String)uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);
        
        if (commandOnclick != null && (validParams != null && !validParams.isEmpty()))
        {
            ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
        }

        writer.startElement(HTML.INPUT_ELEM, uiComponent);

        writer.writeAttribute(HTML.ID_ATTR, clientId, ComponentAttrs.ID_ATTR);
        writer.writeAttribute(HTML.NAME_ATTR, clientId, ComponentAttrs.ID_ATTR);

        String image = RendererUtils.getIconSrc(facesContext, uiComponent, ComponentAttrs.IMAGE_ATTR);
        if (image != null)
        {
            writer.writeAttribute(HTML.TYPE_ATTR, HTML.INPUT_TYPE_IMAGE, ComponentAttrs.TYPE_ATTR);
            writer.writeURIAttribute(HTML.SRC_ATTR, image, ComponentAttrs.IMAGE_ATTR);
        }
        else
        {
            String type = getType(uiComponent);

            if (type == null || (!reset && !button))
            {
                type = HTML.INPUT_TYPE_SUBMIT;
            }
            writer.writeAttribute(HTML.TYPE_ATTR, type, ComponentAttrs.TYPE_ATTR);
            Object value = getValue(uiComponent);
            if (value != null)
            {
                writer.writeAttribute(HTML.VALUE_ATTR, value, ComponentAttrs.VALUE_ATTR);
            }
        }
        
        if (ClientBehaviorRendererUtils.hasClientBehavior(ClientBehaviorEvents.CLICK, behaviors)
                || ClientBehaviorRendererUtils.hasClientBehavior(ClientBehaviorEvents.ACTION, behaviors))
        {
            if (!reset && !button)
            {
                String onClick = buildBehaviorizedOnClick(uiComponent, behaviors, facesContext, writer,
                        form, validParams);
                if (onClick.length() != 0)
                {
                    writer.writeAttribute(HTML.ONCLICK_ATTR, onClick, null);
                }
            }
            else
            {
                Collection<ClientBehaviorContext.Parameter> paramList = 
                    ClientBehaviorRendererUtils.getClientBehaviorContextParameters(
                        HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, uiComponent));
                    
                String onClick = ClientBehaviorRendererUtils.buildBehaviorChain(facesContext, uiComponent,
                        ClientBehaviorEvents.CLICK, paramList, ClientBehaviorEvents.ACTION, paramList, behaviors,
                        commandOnclick , null);
                if (onClick.length() != 0)
                {
                    writer.writeAttribute(HTML.ONCLICK_ATTR, onClick, null);
                }
            }
            
            Map<String, Object> attributes = uiComponent.getAttributes(); 
            
            ClientBehaviorRendererUtils.buildBehaviorChain(facesContext, uiComponent, ClientBehaviorEvents.DBLCLICK,
                    null, behaviors, (String) attributes.get(HTML.ONDBLCLICK_ATTR), "");
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
        
        if (isCommonPropertiesOptimizationEnabled(facesContext))
        {
            CommonHtmlAttributesUtil.renderButtonPassthroughPropertiesWithoutDisabledAndEvents(writer, 
                    CommonHtmlAttributesUtil.getMarkedAttributes(uiComponent), uiComponent);
        }
        else
        {
            HtmlRendererUtils.renderHTMLAttributes(writer, uiComponent,
                                                   HTML.BUTTON_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED_AND_EVENTS);
        }

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
                long commonAttributesMarked = CommonHtmlAttributesUtil.getMarkedAttributes(uiComponent);
                CommonHtmlAttributesUtil.renderEventPropertiesWithoutOnclick(writer,
                        commonAttributesMarked, uiComponent);
                CommonHtmlAttributesUtil.renderCommonFieldEventProperties(writer,
                        commonAttributesMarked, uiComponent);
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
            writer.writeAttribute(HTML.DISABLED_ATTR, HTML.DISABLED_ATTR, ComponentAttrs.DISABLED_ATTR);
        }
        
        if (isReadonly(facesContext, uiComponent))
        {
            writer.writeAttribute(HTML.READONLY_ATTR, HTML.READONLY_ATTR, ComponentAttrs.READONLY_ATTR);
        }
    }
    
    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent uiComponent) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        writer.endElement(HTML.INPUT_ELEM);
        
        UIForm form = ComponentUtils.findClosest(UIForm.class, uiComponent);
        if (form != null)
        {
            HtmlFormRendererBase.renderScrollHiddenInputIfNecessary(form, facesContext, writer);
        }
    }

    protected String buildBehaviorizedOnClick(UIComponent uiComponent, Map<String, List<ClientBehavior>> behaviors, 
                                              FacesContext facesContext, ResponseWriter writer, 
                                              UIComponent form, List<UIParameter> validParams)
        throws IOException
    {
        StringBuilder sb = SharedStringBuilder.get(facesContext, SB_BUILD_BEHAVIORIZED_ONCLICK);
        
        //user onclick part 
        String commandOnClick = (String) uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);
        if (commandOnClick != null)
        {
            sb.append(commandOnClick);
            sb.append(';');
        }
        String userOnClick = sb.toString();

        // reset SB and reuse
        sb.setLength(0);

        if (form != null) 
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
                if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext()))
                {
                    //call the script to clear the form (clearFormHiddenParams_<formName>) method
                    HtmlRendererUtils.appendClearHiddenCommandFormParamsFunctionCall(sb,
                            form.getClientId(facesContext));
                }
            //}
        }

        //according to the specification in faces.util.chain jdocs and the spec document we have to use
        //faces.util.chain to chain the functions and
        Collection<ClientBehaviorContext.Parameter> paramList =
                ClientBehaviorRendererUtils.getClientBehaviorContextParameters(
                        HtmlRendererUtils.mapAttachedParamsToStringValues(facesContext, uiComponent));
        
        return ClientBehaviorRendererUtils.buildBehaviorChain(facesContext, uiComponent,
                ClientBehaviorEvents.CLICK, paramList, ClientBehaviorEvents.ACTION, paramList, behaviors,
                userOnClick , sb.toString());
    }

    private StringBuilder addChildParameters(FacesContext context, List<UIParameter> validParams)
    {
        //add child parameters
        StringBuilder params = SharedStringBuilder.get(context, SB_ADD_CHILD_PARAMETERS);
        params.append('[');
        
        for (int i = 0, size = validParams.size(); i < size; i++)
        {
            UIParameter param = validParams.get(i);
            String name = param.getName();
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
                for (int j = 0; j < strParamValue.length(); j++)
                {
                    char c = strParamValue.charAt(j); 
                    if (c == '\'' || c == '\\')
                    {
                        if (buff == null)
                        {
                            buff = new StringBuilder();
                            buff.append(strParamValue.substring(0,j));
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
                params.append(',');
            }

            params.append("['");
            params.append(name);
            params.append("','");
            params.append(strParamValue);
            params.append("']");
        }
        params.append(']');
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
        StringBuilder onClick = SharedStringBuilder.get(facesContext, SB_BUILD_ONCLICK);
        String commandOnClick = (String) uiComponent.getAttributes().get(HTML.ONCLICK_ATTR);

        if (commandOnClick != null)
        {
            onClick.append("var cf = function(){");
            onClick.append(commandOnClick);
            onClick.append('}');
            onClick.append(';');
            onClick.append("var oamSF = function(){");
        }

        UIForm form = ComponentUtils.findClosest(UIForm.class, uiComponent);        
        if (form != null)
        {
            if (validParams != null && !validParams.isEmpty() )
            {
                StringBuilder params = addChildParameters(facesContext, validParams);

                String target = getTarget(uiComponent);

                onClick.append("return ").
                    append(HtmlRendererUtils.SUBMIT_FORM_FN_NAME_JSF2).append("('").
                    append(form.getClientId(facesContext)).append("','").
                    append(uiComponent.getClientId(facesContext)).append('\'');

                if (params.length() > 2 || target != null)
                {
                    onClick.append(',').
                        append(target == null ? "null" : ('\'' + target + '\'')).append(',').
                        append(params);
                }
                onClick.append(");");
            }
            else
            {
        
                if (JavascriptUtils.isRenderClearJavascriptOnButton(facesContext.getExternalContext()))
                {
                    //call the script to clear the form (clearFormHiddenParams_<formName>) method
                    HtmlRendererUtils.appendClearHiddenCommandFormParamsFunctionCall(onClick,
                            form.getClientId(facesContext));
                }
            }
        }
        
        if (commandOnClick != null)
        {
            onClick.append('}');
            onClick.append(';');
            onClick.append("return (cf.apply(this, [])==false)? false : oamSF.apply(this, []); ");
        }  

        return onClick;
    }


    protected boolean isDisabled(FacesContext facesContext, UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton) uiComponent).isDisabled();
        }

        return AttributeUtils.getBooleanAttribute(uiComponent, HTML.DISABLED_ATTR, false);
    }

    protected boolean isReadonly(FacesContext facesContext, UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).isReadonly();
        }
        return AttributeUtils.getBooleanAttribute(
                uiComponent, HTML.READONLY_ATTR, false);
    }

    private String getType(UIComponent uiComponent)
    {
        if (uiComponent instanceof HtmlCommandButton)
        {
            return ((HtmlCommandButton)uiComponent).getType();
        }
        return (String)uiComponent.getAttributes().get(ComponentAttrs.TYPE_ATTR);
    }

    private Object getValue(UIComponent uiComponent)
    {
        if (uiComponent instanceof ValueHolder)
        {
            return ((ValueHolder)uiComponent).getValue();
        }
        return uiComponent.getAttributes().get(ComponentAttrs.VALUE_ATTR);
    }
}
