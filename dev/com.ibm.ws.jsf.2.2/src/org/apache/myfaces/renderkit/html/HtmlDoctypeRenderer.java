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
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.Renderer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFRenderer;

/**
 * Rendered used by h:doctype tag
 * 
 * @since 2.1.0
 * @author Leonardo Uribe
 *
 */
@JSFRenderer(renderKitId = "HTML_BASIC", family = "javax.faces.Output", type = "javax.faces.Doctype")
public class HtmlDoctypeRenderer extends Renderer
{

    @Override
    public void encodeChildren(FacesContext context, UIComponent component)
            throws IOException
    {
    }
    
    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
            throws IOException
    {
        super.encodeEnd(context, component);
        
        ResponseWriter writer = context.getResponseWriter();
        
        Map<String, Object> attributes = component.getAttributes();
        //<!DOCTYPE html PUBLIC
        // "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
        writer.write("<!DOCTYPE ");
        writer.write((String) attributes.get("rootElement"));
        String publicValue = (String) attributes.get("public"); 
        if (publicValue != null && publicValue.length() > 0)
        {
            writer.write(" PUBLIC \"");
            writer.write(publicValue);
            writer.write("\"");
        }
        String systemValue = (String) attributes.get("system");
        if (systemValue != null && systemValue.length() > 0)
        {
            writer.write(" \"");
            writer.write(systemValue);
            writer.write("\"");
        }
        writer.write(">");
    }
}
