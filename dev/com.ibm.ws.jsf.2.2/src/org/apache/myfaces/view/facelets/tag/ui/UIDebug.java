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
package org.apache.myfaces.view.facelets.tag.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.renderkit.ErrorPageWriter;
import org.apache.myfaces.view.facelets.util.FastWriter;

/**
 * The debug tag will capture the component tree and variables when it is encoded, 
 * storing the data for retrieval later. You may launch the debug window at any time 
 * from your browser by pressing 'CTRL' + 'SHIFT' + 'D' (by default).
 * 
 * The debug tag doesn't need to be used with the facelet.DEVELOPMENT parameter.
 * The best place to put this tag is in your site's main template where it can be 
 * enabled/disabled across your whole application. 
 * 
 * If your application uses multiple windows, you might want to assign different 
 * hot keys to each one.
 * 
 * @author Jacob Hookom
 * @version $Id: UIDebug.java 1593989 2014-05-12 14:59:28Z lu4242 $
 */
@JSFComponent(name="ui:debug")
@JSFJspProperty(name = "binding", tagExcluded=true)
public final class UIDebug extends UIComponentBase
{
    public static final String COMPONENT_TYPE = "facelets.ui.Debug";
    public static final String COMPONENT_FAMILY = "facelets";
    public static final String DEFAULT_HOTKEY = "D";
    
    private static final String KEY = "facelets.ui.DebugOutput";
    
    private static long nextId = System.currentTimeMillis();

    private String _hotkey = DEFAULT_HOTKEY;

    public UIDebug()
    {
        setTransient(true);
        setRendererType(null);
    }

    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    public List<UIComponent> getChildren()
    {
        return new ArrayList<UIComponent>()
        {
            public boolean add(UIComponent o)
            {
                throw new IllegalStateException("<ui:debug> does not support children");
            }

            public void add(int index, UIComponent o)
            {
                throw new IllegalStateException("<ui:debug> does not support children");
            }
        };
    }

    public void encodeBegin(FacesContext faces) throws IOException
    {
        boolean partialRequest = faces.getPartialViewContext().isPartialRequest();
        
        String actionId = faces.getApplication().getViewHandler()
                .getActionURL(faces, faces.getViewRoot().getViewId());
        
        StringBuilder sb = new StringBuilder(512);
        sb.append("<script language=\"javascript\" type=\"text/javascript\">\n");
        if (!partialRequest)
        {
            sb.append("//<![CDATA[\n");
        }
        sb.append("function faceletsDebug(URL) { day = new Date(); id = day.getTime(); eval(\"page\" + id + \" "
                  + "= window.open(URL, '\" + id + \"', 'toolbar=0,scrollbars=1,location=0,statusbar=0,menubar=0,"
                  + "resizable=1,width=800,height=600,left = 240,top = 212');\"); };");
        sb.append("var faceletsOrigKeyup = document.onkeyup; document.onkeyup = function(e) { "
                  + "if (window.event) e = window.event; if (String.fromCharCode(e.keyCode) == '"
                  + this.getHotkey() + "' & e.shiftKey & e.ctrlKey) faceletsDebug('");
        sb.append(actionId);
        
        int index = actionId.indexOf ("?");
        if (index != -1)
        {
            sb.append('&');
        }
        else
        {
            sb.append('?');
        }
        sb.append(KEY);
        sb.append('=');
        sb.append(writeDebugOutput(faces));
        sb.append("'); else if (faceletsOrigKeyup) faceletsOrigKeyup(e); };\n");
        if (!partialRequest)
        {
            sb.append("//]]>\n");
        }
        sb.append("</script>\n");

        ResponseWriter writer = faces.getResponseWriter();
        writer.write(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private static String writeDebugOutput(FacesContext faces) throws IOException
    {
        FastWriter fw = new FastWriter();
        ErrorPageWriter.debugHtml(fw, faces);

        Map<String, Object> session = faces.getExternalContext().getSessionMap();
        Map<String, String> debugs = (Map<String, String>) session.get(KEY);
        if (debugs == null)
        {
            debugs = new LinkedHashMap<String, String>()
            {
                protected boolean removeEldestEntry(Entry<String, String> eldest)
                {
                    return (this.size() > 5);
                }
            };
            
            session.put(KEY, debugs);
        }
        
        String id = String.valueOf(nextId++);
        
        debugs.put(id, fw.toString());
        
        return id;
    }

    @SuppressWarnings("unchecked")
    private static String fetchDebugOutput(FacesContext faces, String id)
    {
        Map<String, Object> session = faces.getExternalContext().getSessionMap();
        Map<String, String> debugs = (Map<String, String>) session.get(KEY);
        if (debugs != null)
        {
            return debugs.get(id);
        }
        
        return null;
    }

    public static boolean debugRequest(FacesContext faces)
    {
        String id = (String) faces.getExternalContext().getRequestParameterMap().get(KEY);
        if (id != null)
        {
            Object resp = faces.getExternalContext().getResponse();
            if (!faces.getResponseComplete() && resp instanceof HttpServletResponse)
            {
                try
                {
                    HttpServletResponse httpResp = (HttpServletResponse) resp;
                    String page = fetchDebugOutput(faces, id);
                    if (page != null)
                    {
                        httpResp.setContentType("text/html");
                        httpResp.getWriter().write(page);
                    }
                    else
                    {
                        httpResp.setContentType("text/plain");
                        httpResp.getWriter().write("No Debug Output Available");
                    }
                    httpResp.flushBuffer();
                    faces.responseComplete();
                }
                catch (IOException e)
                {
                    return false;
                }
                
                return true;
            }
        }
        
        return false;
    }

    @JSFProperty(tagExcluded=true)
    @Override
    public String getId()
    {
        // TODO Auto-generated method stub
        return super.getId();
    }

    /**
     * The hot key to use in combination with 'CTRL' + 'SHIFT' to launch the debug window. 
     * By default, when the debug tag is used, you may launch the debug window with 
     * 'CTRL' + 'SHIFT' + 'D'. This value cannot be an EL expression.
     * 
     * @return
     */
    @JSFProperty
    public String getHotkey()
    {
        return _hotkey;
    }

    public void setHotkey(String hotkey)
    {
        _hotkey = (hotkey != null) ? hotkey.toUpperCase() : "";
    }
}
