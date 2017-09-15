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
import java.util.List;

import javax.faces.application.ProjectStage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;

/**
 * Renderer used by h:head component
 * 
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author: gpetracek $)
 * @version $Revision: 1523813 $ $Date: 2013-09-16 21:16:44 +0000 (Mon, 16 Sep 2013) $
 */
@JSFRenderer(renderKitId = "HTML_BASIC", family = "javax.faces.Output", type = "javax.faces.Head")
public class HtmlHeadRenderer extends Renderer
{
    //TODO: Move constants to shared HTML class
    private final static String HEAD_ELEM = "head";
    private final static String HEAD_TARGET = HEAD_ELEM;

    private final static String PROFILE_ATTR = "profile";

    private final static String[] HEAD_PASSTHROUGH_ATTRIBUTES = { HTML.DIR_ATTR,
            HTML.LANG_ATTR, PROFILE_ATTR};

    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeBegin(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();
        writer.startElement(HEAD_ELEM, component);
        HtmlRendererUtils.writeIdIfNecessary(writer, component, facesContext);
        HtmlRendererUtils.renderHTMLAttributes(writer, component,
                HEAD_PASSTHROUGH_ATTRIBUTES);
        HtmlRendererUtils.renderHTMLAttribute(writer, component, HTML.XMLNS_ATTR , HTML.XMLNS_ATTR);
    }

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component)
            throws IOException
    {
        super.encodeEnd(facesContext, component); //check for NP

        ResponseWriter writer = facesContext.getResponseWriter();
        UIViewRoot root = facesContext.getViewRoot();
        List<UIComponent> componentResources = root.getComponentResources(facesContext,
                HEAD_TARGET);
        
        for (int i = 0, childCount = componentResources.size(); i < childCount; i++)
        {
            UIComponent child = componentResources.get(i);
            child.encodeAll(facesContext);
        }
        writer.endElement(HEAD_ELEM);

        if (MyfacesConfig.getCurrentInstance(facesContext.getExternalContext()).isEarlyFlushEnabled() &&
                facesContext.isProjectStage(ProjectStage.Production))
        {
            writer.flush();
        }
    }
}
