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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.AjaxBehavior;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.html.HtmlCommandScript;
import javax.faces.component.search.SearchExpressionContext;
import javax.faces.component.search.SearchExpressionHandler;
import javax.faces.component.search.SearchExpressionHint;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.PhaseId;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.HtmlRenderer;
import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared.renderkit.html.util.FormInfo;
import org.apache.myfaces.shared.renderkit.html.util.JavascriptUtils;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.shared.renderkit.html.util.SharedStringBuilder;
import org.apache.myfaces.shared.util.StringUtils;

/**
 *
 */
@JSFRenderer(
    renderKitId="HTML_BASIC",
    family="javax.faces.Command",
    type="javax.faces.Script")
public class HtmlCommandScriptRenderer extends HtmlRenderer
{
    private static final String QUOTE = "'";
    private static final String BLANK = " ";

    private static final String AJAX_KEY_ONERROR = "onerror";
    private static final String AJAX_KEY_ONEVENT = "onevent";
    private static final String AJAX_KEY_EXECUTE = "execute";
    private static final String AJAX_KEY_RENDER = "render";
    private static final String AJAX_KEY_DELAY = "delay";
    private static final String AJAX_KEY_RESETVALUES = "resetValues";

    private static final String AJAX_VAL_THIS = "this";
    private static final String AJAX_VAL_EVENT = "event";
    private static final String JS_AJAX_REQUEST = "jsf.ajax.request";

    private static final String COLON = ":";
    private static final String EMPTY = "";
    private static final String COMMA = ",";

    private static final String ERR_NO_AJAX_BEHAVIOR = "The behavior must be an instance of AjaxBehavior";
    private static final String L_PAREN = "(";
    private static final String R_PAREN = ")";

    /*if this marker is present in the request we have to dispatch a behavior event*/
    /*if an attached behavior triggers an ajax request this request param must be added*/
    private static final String BEHAVIOR_EVENT = "javax.faces.behavior.event";
    private static final String IDENTIFYER_MARKER = "@";
    
    private static final String AJAX_SB = "oam.renderkit.AJAX_SB";
    private static final String AJAX_PARAM_SB = "oam.renderkit.AJAX_PARAM_SB";
    
    private static final String VAL_FORM = "@form";
    private static final String VAL_ALL = "@all";
    private static final String VAL_THIS = "@this";
    private static final String VAL_NONE = "@none";

    private static final Collection<String> VAL_FORM_LIST = Collections.singletonList(VAL_FORM);
    private static final Collection<String> VAL_ALL_LIST = Collections.singletonList(VAL_ALL);
    private static final Collection<String> VAL_THIS_LIST = Collections.singletonList(VAL_THIS);
    private static final Collection<String> VAL_NONE_LIST = Collections.singletonList(VAL_NONE);
    
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException
    {
        super.encodeBegin(context, component); //

        HtmlCommandScript commandScript = (HtmlCommandScript) component;
        ResponseWriter writer = context.getResponseWriter();
        
        ResourceUtils.renderDefaultJsfJsInlineIfNecessary(context, writer);
        
        writer.startElement(HTML.SPAN_ELEM, component);
        writer.writeAttribute(HTML.ID_ATTR, component.getClientId(context), null);
        writer.startElement(HTML.SCRIPT_ELEM, component);
        writer.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);
        
        HtmlRendererUtils.ScriptContext script = new HtmlRendererUtils.ScriptContext();
        
        //Write content
        String cmdName = commandScript.getName();
        String name;
        if (cmdName != null && cmdName.length() > 0)
        {
            name = JavascriptUtils.getValidJavascriptName(cmdName, true);
        }
        else
        {
            name = JavascriptUtils.getValidJavascriptName(component.getClientId(context), true);
        }
        
        script.prettyLine();
        script.increaseIndent();
        script.append("var "+name+" = function(o){var o=(typeof o==='object')&&o?o:{};");
        script.prettyLine();
        
        AjaxBehavior ajaxBehavior = new AjaxBehavior();
        ajaxBehavior.setExecute(getCollectionFromSpaceSplitString(commandScript.getExecute()));
        ajaxBehavior.setRender(getCollectionFromSpaceSplitString(commandScript.getRender()));
        Boolean resetValues = commandScript.getResetValues();
        if (resetValues != null)
        {
            ajaxBehavior.setResetValues(resetValues);
        }
        ajaxBehavior.setOnerror(commandScript.getOnerror());
        ajaxBehavior.setOnevent(commandScript.getOnevent());
        ajaxBehavior.setDelay(commandScript.getOnevent());
        
        Collection<ClientBehaviorContext.Parameter> eventParameters = new ArrayList<ClientBehaviorContext.Parameter>();
        //eventParameters.add(new ClientBehaviorContext.Parameter("params", "o"));
        ClientBehaviorContext ccc = ClientBehaviorContext.createClientBehaviorContext(
                                    context, component, "action",
                                    commandScript.getClientId(context), eventParameters);
        
        script.append(makeAjax(ccc, ajaxBehavior).toString());
        script.decreaseIndent();
        script.append("}");
        
        
        if (commandScript.isAutorun())
        {
            script.append(";");
            script.append("myfaces._impl.core._Runtime.addOnLoad(window,");
            script.append(name);
            script.append(");");
        }
        
        writer.writeText(script.toString(), null);
    }
    
    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException
    {
        super.encodeEnd(context, component); //
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement(HTML.SCRIPT_ELEM);
        writer.endElement(HTML.SPAN_ELEM);
    }

    @Override
    public void decode(FacesContext facesContext, UIComponent component)
    {
        super.decode(facesContext, component); 
        
        HtmlCommandScript commandScript = (HtmlCommandScript) component;
        
        if (HtmlRendererUtils.isDisabled(component) || !commandScript.isRendered())
        {
            return;
        }
        
        Map<String, String> paramMap = facesContext
                .getExternalContext().getRequestParameterMap();
        String behaviorEventName = paramMap
                .get(ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME);
        if (behaviorEventName != null)
        {
            String sourceId = paramMap.get(ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME);
            String componentClientId = component.getClientId(facesContext);
            String clientId = sourceId;
            if (sourceId.startsWith(componentClientId) &&
                sourceId.length() > componentClientId.length())
            {
                String item = sourceId.substring(componentClientId.length()+1);
                // If is item it should be an integer number, otherwise it can be related to a child 
                // component, because that could conflict with the clientId naming convention.
                if (StringUtils.isInteger(item))
                {
                    clientId = componentClientId;
                }
            }
            if (component.getClientId(facesContext).equals(clientId))
            {
                boolean disabled = HtmlRendererUtils.isDisabled(component);
                FormInfo formInfo = RendererUtils.findNestingForm(component, facesContext);
                boolean activateActionEvent = false;
                if (formInfo != null && !disabled)
                {
                    String reqValue = (String) facesContext.getExternalContext().getRequestParameterMap().get(
                            HtmlRendererUtils.getHiddenCommandLinkFieldName(formInfo, facesContext));
                    activateActionEvent = reqValue != null && reqValue.equals(clientId)
                        || HtmlRendererUtils.isPartialOrBehaviorSubmit(facesContext, clientId);
                    if (activateActionEvent)
                    {
                        RendererUtils.initPartialValidationAndModelUpdate(component, facesContext);
                    }
                }
                if (activateActionEvent)
                {
                    component.queueEvent(new ActionEvent(component));
                }
            }
        }
        if (component instanceof ClientBehaviorHolder &&
                !HtmlRendererUtils.isDisabled(component))
        {
            HtmlRendererUtils.decodeClientBehaviors(facesContext, component);
        }
    }

    /*
    public void decode(FacesContext context, UIComponent component,
                       ClientBehavior behavior)
    {
        assertBehavior(behavior);
        AjaxBehavior ajaxBehavior = (AjaxBehavior) behavior;
        if (ajaxBehavior.isDisabled() || !component.isRendered())
        {
            return;
        }

        dispatchBehaviorEvent(component, ajaxBehavior);
    }*/

    public String getScript(ClientBehaviorContext behaviorContext,
                            ClientBehavior behavior)
    {
        assertBehavior(behavior);
        AjaxBehavior ajaxBehavior = (AjaxBehavior) behavior;

        if (ajaxBehavior.isDisabled())
        {
            return null;
        }

        return makeAjax(behaviorContext, ajaxBehavior).toString();
    }

    private final void dispatchBehaviorEvent(UIComponent component, AjaxBehavior ajaxBehavior)
    {
        AjaxBehaviorEvent event = new AjaxBehaviorEvent(component, ajaxBehavior);

        boolean isImmediate = false;
        if (ajaxBehavior.isImmediateSet())
        {
            isImmediate = ajaxBehavior.isImmediate();
        }
        else
        {
            isImmediate = isComponentImmediate(component);
        }
        PhaseId phaseId = isImmediate ?
                PhaseId.APPLY_REQUEST_VALUES :
                PhaseId.INVOKE_APPLICATION;

        event.setPhaseId(phaseId);

        component.queueEvent(event);
    }

    private final boolean isComponentImmediate(UIComponent component)
    {
        boolean isImmediate = false;
        if (component instanceof EditableValueHolder)
        {
            isImmediate = ((EditableValueHolder)component).isImmediate();
        }
        else if (component instanceof ActionSource)
        {
            isImmediate = ((ActionSource)component).isImmediate();
        }
        return isImmediate;
    }


    /**
     * builds the generic ajax call depending upon
     * the ajax behavior parameters
     *
     * @param context  the Client behavior context
     * @param behavior the behavior
     * @return a fully working javascript with calls into jsf.js
     */
    private final StringBuilder makeAjax(ClientBehaviorContext context, AjaxBehavior behavior)
    {
        StringBuilder retVal = SharedStringBuilder.get(context.getFacesContext(), AJAX_SB, 60);
        StringBuilder paramBuffer = SharedStringBuilder.get(context.getFacesContext(), AJAX_PARAM_SB, 20);

        String executes = mapToString(context, paramBuffer, AJAX_KEY_EXECUTE, behavior.getExecute());
        String render = mapToString(context, paramBuffer, AJAX_KEY_RENDER, behavior.getRender());

        String onError = behavior.getOnerror();
        if (onError != null && !onError.trim().equals(EMPTY))
        {
            //onError = AJAX_KEY_ONERROR + COLON + onError;
            paramBuffer.setLength(0);
            paramBuffer.append(AJAX_KEY_ONERROR);
            paramBuffer.append(COLON);
            paramBuffer.append(onError);
            onError = paramBuffer.toString();
        }
        else
        {
            onError = null;
        }
        String onEvent = behavior.getOnevent();
        if (onEvent != null && !onEvent.trim().equals(EMPTY))
        {
            paramBuffer.setLength(0);
            paramBuffer.append(AJAX_KEY_ONEVENT);
            paramBuffer.append(COLON);
            paramBuffer.append(onEvent);
            onEvent = paramBuffer.toString();
        }
        else
        {
            onEvent = null;
        }
        /*
         * since version 2.2
         */
        String delay = behavior.getDelay();
        if (delay != null && !delay.trim().equals(EMPTY))
        {
            paramBuffer.setLength(0);
            paramBuffer.append(AJAX_KEY_DELAY);
            paramBuffer.append(COLON);
            if ("none".equals(delay))
            {
                paramBuffer.append('\'');
                paramBuffer.append(delay);
                paramBuffer.append('\'');
            }
            else
            {
                paramBuffer.append(delay);
            }
            delay = paramBuffer.toString();
        }
        else
        {
            delay = null;
        }
        /*
         * since version 2.2
         */
        String resetValues = Boolean.toString(behavior.isResetValues());
        if (resetValues.equals("true"))
        {
            paramBuffer.setLength(0);
            paramBuffer.append(AJAX_KEY_RESETVALUES);
            paramBuffer.append(COLON);
            paramBuffer.append(resetValues);
            resetValues = paramBuffer.toString();
        }
        else
        {
            resetValues = null;
        }

        String sourceId = null;
        if (context.getSourceId() == null)
        {
            sourceId = AJAX_VAL_THIS;
        }
        else
        {
            paramBuffer.setLength(0);
            paramBuffer.append('\'');
            paramBuffer.append(context.getSourceId());
            paramBuffer.append('\'');
            sourceId = paramBuffer.toString();

            if (!context.getSourceId().trim().equals(
                context.getComponent().getClientId(context.getFacesContext())))
            {
                // Check if sourceId is not a clientId and there is no execute set
                UIComponent ref = context.getComponent();
                ref = (ref.getParent() == null) ? ref : ref.getParent();
                UIComponent instance = null;
                try
                {
                    instance = ref.findComponent(context.getSourceId());
                }
                catch (IllegalArgumentException e)
                {
                    // No Op
                }
                if (instance == null && executes == null)
                {
                    // set the clientId of the component so the behavior can be decoded later,
                    // otherwise the behavior will fail
                    List<String> list = new ArrayList<String>();
                    list.add(context.getComponent().getClientId(context.getFacesContext()));
                    executes = mapToString(context, paramBuffer, AJAX_KEY_EXECUTE, list);
                }
            }
        }


        String event = context.getEventName();

        retVal.append(JS_AJAX_REQUEST);
        retVal.append(L_PAREN);
        retVal.append(sourceId);
        retVal.append(COMMA);
        //retVal.append(AJAX_VAL_EVENT);
        retVal.append("window.event");
        retVal.append(COMMA);

        
        retVal.append("myfaces._impl._util._Lang.mixMaps");
        retVal.append(L_PAREN);
        
        Collection<ClientBehaviorContext.Parameter> params = context.getParameters();
        int paramSize = (params != null) ? params.size() : 0;

        List<String> parameterList = new ArrayList<String>(paramSize + 2);
        if (executes != null)
        {
            parameterList.add(executes);
        }
        if (render != null)
        {
            parameterList.add(render);
        }
        if (onError != null)
        {
            parameterList.add(onError);
        }
        if (onEvent != null)
        {
            parameterList.add(onEvent);
        }
        /*
         * since version 2.2
         */
        if (delay != null)
        {
            parameterList.add(delay);
        }
        /*
         * since version 2.2
         */
        if (resetValues != null)
        {
            parameterList.add(resetValues);
        }
        if (paramSize > 0)
        {
            /**
             * see ClientBehaviorContext.html of the spec
             * the param list has to be added in the post back
             */
            // params are in 99% RamdonAccess instace created in
            // HtmlRendererUtils.getClientBehaviorContextParameters(Map<String, String>)
            if (params instanceof RandomAccess)
            {
                List<ClientBehaviorContext.Parameter> list = (List<ClientBehaviorContext.Parameter>) params;
                for (int i = 0, size = list.size(); i < size; i++)
                {
                    ClientBehaviorContext.Parameter param = list.get(i);
                    append(paramBuffer, parameterList, param);
                }
            }
            else
            {
                for (ClientBehaviorContext.Parameter param : params)
                {
                    append(paramBuffer, parameterList, param);
                }
            }
        }

        //parameterList.add(QUOTE + BEHAVIOR_EVENT + QUOTE + COLON + QUOTE + event + QUOTE);
        paramBuffer.setLength(0);
        paramBuffer.append(QUOTE);
        paramBuffer.append(ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME);
        paramBuffer.append(QUOTE);
        paramBuffer.append(COLON);
        paramBuffer.append(QUOTE);
        paramBuffer.append(event);
        paramBuffer.append(QUOTE);
        parameterList.add(paramBuffer.toString());

        /**
         * I assume here for now that the options are the same which also
         * can be sent via the options attribute to javax.faces.ajax
         * this still needs further clarifications but I assume so for now
         */
        retVal.append(buildOptions(paramBuffer, parameterList));

        //mixMaps
        retVal.append(COMMA);
        retVal.append("o");
        retVal.append(COMMA);
        retVal.append("false");
        
        retVal.append(R_PAREN);
        
        retVal.append(R_PAREN);

        return retVal;
    }

    private void append(StringBuilder paramBuffer, List<String> parameterList, ClientBehaviorContext.Parameter param)
    {
        //TODO we may need a proper type handling in this part
        //lets leave it for now as it is
        //quotes etc.. should be transferred directly
        //and the rest is up to the toString properly implemented
        //ANS: Both name and value should be quoted
        paramBuffer.setLength(0);
        paramBuffer.append(QUOTE);
        paramBuffer.append(param.getName());
        paramBuffer.append(QUOTE);
        paramBuffer.append(COLON);
        paramBuffer.append(QUOTE);
        paramBuffer.append(param.getValue().toString());
        paramBuffer.append(QUOTE);
        parameterList.add(paramBuffer.toString());
    }


    private StringBuilder buildOptions(StringBuilder retVal, List<String> options)
    {
        retVal.setLength(0);

        retVal.append("{");

        boolean first = true;

        for (int i = 0, size = options.size(); i < size; i++)
        {
            String option = options.get(i);
            if (option != null && !option.trim().equals(EMPTY))
            {
                if (!first)
                {
                    retVal.append(COMMA);
                }
                else
                {
                    first = false;
                }
                retVal.append(option);
            }
        }
        retVal.append("}");
        return retVal;
    }

    private final String mapToString(ClientBehaviorContext context, StringBuilder retVal,
            String target, Collection<String> dataHolder)
    {
        //Clear buffer
        retVal.setLength(0);

        if (dataHolder == null)
        {
            dataHolder = Collections.emptyList();
        }
        int executeSize = dataHolder.size();
        if (executeSize > 0)
        {

            retVal.append(target);
            retVal.append(COLON);
            retVal.append(QUOTE);

            int cnt = 0;

            SearchExpressionContext searchExpressionContext = null;
            
            // perf: dataHolder is a Collection : ajaxBehaviour.getExecute()
            // and ajaxBehaviour.getRender() API
            // In most cases comes here a ArrayList, because
            // javax.faces.component.behavior.AjaxBehavior.getCollectionFromSpaceSplitString
            // creates it.
            if (dataHolder instanceof RandomAccess)
            {
                List<String> list = (List<String>) dataHolder;
                for (; cnt  < executeSize; cnt++)
                {
                    if (searchExpressionContext == null)
                    {
                        searchExpressionContext = SearchExpressionContext.createSearchExpressionContext(
                                context.getFacesContext(), context.getComponent(), EXPRESSION_HINTS, null);
                    }
                    
                    String strVal = list.get(cnt);
                    build(context, executeSize, retVal, cnt, strVal, searchExpressionContext);
                }
            }
            else
            {
                for (String strVal : dataHolder)
                {
                    if (searchExpressionContext == null)
                    {
                        searchExpressionContext = SearchExpressionContext.createSearchExpressionContext(
                                context.getFacesContext(), context.getComponent(), EXPRESSION_HINTS, null);
                    }
                    
                    cnt++;
                    build(context, executeSize, retVal, cnt, strVal, searchExpressionContext);
                }
            }

            retVal.append(QUOTE);
            return retVal.toString();
        }
        return null;

    }

    private static final Set<SearchExpressionHint> EXPRESSION_HINTS =
            EnumSet.of(SearchExpressionHint.RESOLVE_CLIENT_SIDE, SearchExpressionHint.RESOLVE_SINGLE_COMPONENT);
    
    public void build(ClientBehaviorContext context,
            int size, StringBuilder retVal, int cnt,
            String strVal, SearchExpressionContext searchExpressionContext)
    {
        strVal = strVal.trim();
        if (!EMPTY.equals(strVal))
        {
            SearchExpressionHandler handler = context.getFacesContext().getApplication().getSearchExpressionHandler();
            String clientId = handler.resolveClientId(searchExpressionContext, strVal);
            retVal.append(clientId);
            if (cnt < size)
            {
                retVal.append(BLANK);
            }
        }
    }

    private void assertBehavior(ClientBehavior behavior)
    {
        if (!(behavior instanceof AjaxBehavior))
        {
            throw new FacesException(ERR_NO_AJAX_BEHAVIOR);
        }
    }
    
    /**
     * Splits the String based on spaces and returns the 
     * resulting Strings as Collection.
     * @param stringValue
     * @return
     */
    private Collection<String> getCollectionFromSpaceSplitString(String stringValue)
    {
        //@special handling for @all, @none, @form and @this
        if (stringValue.equals(VAL_FORM)) 
        {
            return VAL_FORM_LIST;
        } 
        else if (stringValue.equals(VAL_ALL)) 
        {
            return VAL_ALL_LIST;
        } 
        else if (stringValue.equals(VAL_NONE)) 
        {
            return VAL_NONE_LIST;
        } 
        else if (stringValue.equals(VAL_THIS)) 
        {
            return VAL_THIS_LIST; 
        }

        // not one of the "normal" values - split it and return the Collection
        String[] arrValue = stringValue.split(" ");
        return Arrays.asList(arrValue);
    }
}
