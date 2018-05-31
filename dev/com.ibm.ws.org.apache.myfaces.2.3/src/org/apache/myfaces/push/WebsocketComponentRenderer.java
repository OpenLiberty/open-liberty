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
package org.apache.myfaces.push;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.component.UIComponent;
import javax.faces.component.UIWebsocket;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.render.Renderer;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.push.cdi.WebsocketApplicationBean;
import org.apache.myfaces.push.cdi.WebsocketChannelMetadata;
import org.apache.myfaces.push.cdi.WebsocketChannelTokenBuilderBean;
import org.apache.myfaces.push.cdi.WebsocketSessionBean;
import org.apache.myfaces.push.cdi.WebsocketViewBean;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;

/**
 *
 */
@JSFRenderer(
        renderKitId = "HTML_BASIC",
        family = "javax.faces.Script",
        type = "javax.faces.Websocket")
@ListenerFor(systemEventClass = PostAddToViewEvent.class)
public class WebsocketComponentRenderer extends Renderer implements ComponentSystemEventListener
{

    @Override
    public void processEvent(ComponentSystemEvent event)
    {
        if (event instanceof PostAddToViewEvent)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            UIWebsocket component = (UIWebsocket) event.getComponent();
            WebsocketInit initComponent = (WebsocketInit) facesContext.getViewRoot().findComponent(
                    (String) component.getAttributes().get("initComponentId"));
            if (initComponent == null)
            {
                initComponent = (WebsocketInit) facesContext.getApplication().createComponent(facesContext,
                        "org.apache.myfaces.WebsocketInit", "org.apache.myfaces.WebsocketInit");
                initComponent.setId((String) component.getAttributes().get("initComponentId"));
                facesContext.getViewRoot().addComponentResource(facesContext,
                        initComponent, "body");
            }
        }
    }

    private HtmlBufferResponseWriterWrapper getResponseWriter(FacesContext context)
    {
        return HtmlBufferResponseWriterWrapper.getInstance(context.getResponseWriter());
    }

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException
    {
        ResponseWriter writer = facesContext.getResponseWriter();

        ResourceUtils.renderDefaultJsfJsInlineIfNecessary(facesContext, writer);
        
        // Render the tag that will be embedded into the DOM tree that helps to detect if the message
        // must be processed or not and if the connection must be closed.
        writer.startElement(HTML.DIV_ELEM, component);
        writer.writeAttribute(HTML.ID_ATTR, component.getClientId() ,null);
        writer.writeAttribute(HTML.STYLE_ATTR, "display:none", null);
        writer.endElement(HTML.DIV_ELEM);
        
        if (!facesContext.getPartialViewContext().isAjaxRequest())
        {
            facesContext.setResponseWriter(getResponseWriter(facesContext));
        }
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent c) throws IOException
    {
        super.encodeEnd(facesContext, c); //check for NP

        UIWebsocket component = (UIWebsocket) c;

        WebsocketInit init = (WebsocketInit) facesContext.getViewRoot().findComponent(
                (String) component.getAttributes().get("initComponentId"));

        ResponseWriter writer = facesContext.getResponseWriter();

        String channel = component.getChannel();

        // TODO: use a single bean and entry point for this algorithm.
        BeanManager beanManager = CDIUtils.getBeanManager(facesContext.getExternalContext());

        WebsocketChannelTokenBuilderBean channelTokenBean = CDIUtils.lookup(
                beanManager,
                WebsocketChannelTokenBuilderBean.class);

        // This bean is required because you always need to register the token, so it can be properly destroyed
        WebsocketViewBean viewTokenBean = CDIUtils.lookup(
                beanManager,
                WebsocketViewBean.class);
        WebsocketSessionBean sessionTokenBean = CDIUtils.lookup(
                beanManager, WebsocketSessionBean.class);

        // Create channel token 
        // TODO: Use ResponseStateManager to create the token
        String scope = component.getScope() == null ? "application" : component.getScope();
        WebsocketChannelMetadata metadata = new WebsocketChannelMetadata(
                channel, scope, component.getUser(), component.isConnected());

        String channelToken = null;
        // Force a new channelToken if "connected" property is set to false, because in that case websocket
        // creation 
        if (!component.isConnected())
        {
            channelToken = viewTokenBean.getChannelToken(metadata);
        }
        if (channelToken == null)
        {
            // No channel token found for that combination, create a new token for this view
            channelToken = channelTokenBean.createChannelToken(facesContext, channel);
            
            // Register channel in view scope to chain discard view algorithm using @PreDestroy
            viewTokenBean.registerToken(channelToken, metadata);
            
            // Register channel in session scope to allow validation on handshake ( WebsocketConfigurator )
            sessionTokenBean.registerToken(channelToken, metadata);
        }

        // Ask these two scopes 
        WebsocketApplicationBean appTokenBean = CDIUtils.getInstance(
                beanManager, WebsocketApplicationBean.class, false);

        // Register token and metadata in the proper bean
        if (scope.equals("view"))
        {
            viewTokenBean.registerWebsocketSession(channelToken, metadata);
        }
        else if (scope.equals("session"))
        {
            sessionTokenBean = (sessionTokenBean != null) ? sessionTokenBean : CDIUtils.lookup(
                    CDIUtils.getBeanManager(facesContext.getExternalContext()),
                    WebsocketSessionBean.class);

            sessionTokenBean.registerWebsocketSession(channelToken, metadata);
        }
        else
        {
            //Default application
            appTokenBean = (appTokenBean != null) ? appTokenBean : CDIUtils.lookup(
                    CDIUtils.getBeanManager(facesContext.getExternalContext()),
                    WebsocketApplicationBean.class);

            appTokenBean.registerWebsocketSession(channelToken, metadata);
        }

        writer.startElement(HTML.SCRIPT_ELEM, component);
        writer.writeAttribute(HTML.SCRIPT_TYPE_ATTR, HTML.SCRIPT_TYPE_TEXT_JAVASCRIPT, null);

        StringBuilder sb = new StringBuilder(50);
        sb.append("jsf.push.init(");
        sb.append("'"+component.getClientId()+"'");
        sb.append(",");
        sb.append("'"+facesContext.getExternalContext().encodeWebsocketURL(
                facesContext.getApplication().getViewHandler().getWebsocketURL(
                        facesContext, component.getChannel()+"?"+channelToken))+"'");
        sb.append(",");
        sb.append("'"+component.getChannel()+"'");
        sb.append(",");
        sb.append(component.getOnopen());
        sb.append(",");
        sb.append(component.getOnmessage());
        sb.append(",");
        sb.append(component.getOnclose());
        sb.append(",");
        sb.append(getBehaviorScripts(facesContext, component));
        sb.append(",");
        sb.append(component.isConnected());
        sb.append(");");

        writer.write(sb.toString());

        writer.endElement(HTML.SCRIPT_ELEM);
        
        if (!facesContext.getPartialViewContext().isAjaxRequest())
        {
            HtmlBufferResponseWriterWrapper buffwriter = (HtmlBufferResponseWriterWrapper) 
                    facesContext.getResponseWriter();
            init.getUIWebsocketMarkupList().add(writer.toString());
            facesContext.setResponseWriter(buffwriter.getInitialWriter());
        }
    }
    
    private String getBehaviorScripts(FacesContext facesContext, UIWebsocket component)
    {
        Map<String, List<ClientBehavior>> clientBehaviorsByEvent = component.getClientBehaviors();

        if (clientBehaviorsByEvent.isEmpty())
        {
            return "{}";
        }

        String clientId = component.getClientId(facesContext);
        StringBuilder scripts = new StringBuilder("{");

        for (Entry<String, List<ClientBehavior>> entry : clientBehaviorsByEvent.entrySet())
        {
            String event = entry.getKey();
            List<ClientBehavior> clientBehaviors = entry.getValue();
            scripts.append(scripts.length() > 1 ? "," : "").append(event).append(":[");

            for (int i = 0; i < clientBehaviors.size(); i++)
            {
                scripts.append(i > 0 ? "," : "").append("function(event){");
                scripts.append(clientBehaviors.get(i).getScript(
                        ClientBehaviorContext.createClientBehaviorContext(
                                facesContext, component, event, clientId, null)));
                scripts.append("}");
            }

            scripts.append("]");
        }

        return scripts.append("}").toString();
    }
        
}
