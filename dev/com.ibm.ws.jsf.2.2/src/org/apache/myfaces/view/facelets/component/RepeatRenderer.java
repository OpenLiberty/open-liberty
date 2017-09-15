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
package org.apache.myfaces.view.facelets.component;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;

@JSFRenderer(family="facelets",
        renderKitId="HTML_BASIC",
        type="facelets.ui.Repeat")
public class RepeatRenderer extends Renderer
{

    public RepeatRenderer()
    {
        super();
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException
    {

    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException
    {
        if (component.getChildCount() > 0)
        {
            Map<String, Object> a = component.getAttributes();
            String tag = (String) a.get("alias.element");
            if (tag != null)
            {
                ResponseWriter out = context.getResponseWriter();
                out.startElement(tag, component);
                String[] attrs = (String[]) a.get("alias.attributes");
                String attr;
                if (attrs != null)
                {
                    for (int i = 0; i < attrs.length; i++)
                    {
                        attr = attrs[i];
                        if ("styleClass".equals(attr))
                        {
                            attr = "class";
                        }
                        out.writeAttribute(attr, a.get(attrs[i]), attrs[i]);
                    }
                }
            }
            
            for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                child.encodeAll(context);
            }

            if (tag != null)
            {
                context.getResponseWriter().endElement(tag);
            }
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException
    {
        
    }

    @Override
    public boolean getRendersChildren()
    {
        return true;
    }

}
